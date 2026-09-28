import logging

logger = logging.getLogger(__name__)

class OcrEngine:
    """
    Stage 2: OCR engine for extracting text and confidence scores
    from localized license plate images using EasyOCR / PyTesseract.
    """

    def __init__(self, use_gpu=False):
        self.reader = None
        self.engine_type = "none"

        # Attempt initializing EasyOCR
        try:
            import easyocr
            self.reader = easyocr.Reader(['en'], gpu=use_gpu)
            self.engine_type = "easyocr"
            logger.info("Initialized EasyOCR engine successfully.")
        except Exception as e:
            logger.warning(f"EasyOCR initialization failed ({e}). Checking PyTesseract fallback.")
            try:
                import pytesseract
                self.engine_type = "pytesseract"
                logger.info("Initialized PyTesseract OCR engine.")
            except Exception as e2:
                logger.warning(f"PyTesseract not available ({e2}). Using pattern fallback engine.")
                self.engine_type = "fallback"

    def read_text(self, roi_image):
        """
        Reads text from cropped license plate ROI image.
        Returns tuple: (raw_text, confidence)
        """
        if roi_image is None or roi_image.size == 0:
            return "", 0.0

        if self.engine_type == "easyocr" and self.reader is not None:
            try:
                results = self.reader.readtext(roi_image)
                if not results:
                    return "", 0.0

                # Combine text blocks & calculate average confidence
                texts = []
                confidences = []
                for _, text, conf in results:
                    if text and text.strip():
                        texts.append(text.strip())
                        confidences.append(float(conf))

                combined_text = "".join(texts)
                avg_confidence = sum(confidences) / len(confidences) if confidences else 0.0
                return combined_text, avg_confidence
            except Exception as e:
                logger.error(f"EasyOCR read failed: {e}")

        elif self.engine_type == "pytesseract":
            try:
                import pytesseract
                config = r'--oem 3 --psm 7 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
                data = pytesseract.image_to_data(roi_image, config=config, output_type=pytesseract.Output.DICT)
                
                texts = []
                confs = []
                for i in range(len(data['text'])):
                    t = data['text'][i].strip()
                    c = float(data['conf'][i])
                    if t and c > 0:
                        texts.append(t)
                        confs.append(c / 100.0)

                text = "".join(texts)
                conf = (sum(confs) / len(confs)) if confs else 0.0
                return text, conf
            except Exception as e:
                logger.error(f"PyTesseract read failed: {e}")

        return "", 0.0

    def read_text_multi_variant(self, roi_bgr):
        """
        Runs OCR across 6 image crop variations (original, upscaled/sharpened, CLAHE, denoised, thresholded, deskewed)
        and evaluates candidates against Indian PlateNormalizer.
        Returns tuple: (best_raw_text, best_confidence, winning_variant_img, candidate_list)
        """
        from app.utils.image_utils import get_crop_preprocessing_variants
        from app.pipeline.normalizer import PlateNormalizer

        if roi_bgr is None or roi_bgr.size == 0:
            return "", 0.0, roi_bgr, []

        variants = get_crop_preprocessing_variants(roi_bgr)
        candidate_list = []
        valid_struct_candidates = []
        fallback_candidates = []

        for label, img_var in variants:
            raw_text, conf = self.read_text(img_var)
            if raw_text and raw_text.strip():
                clean_text = raw_text.strip()
                is_valid, reason, normalized = PlateNormalizer.validate_registration_structure(clean_text)
                cand_info = {
                    "rawText": clean_text,
                    "confidence": conf,
                    "variantLabel": label,
                    "normalizedText": normalized,
                    "validStructure": is_valid,
                    "reason": reason,
                    "image": img_var
                }
                candidate_list.append(cand_info)

                if is_valid:
                    valid_struct_candidates.append(cand_info)
                else:
                    fallback_candidates.append(cand_info)

        # Vote after normalization so harmless differences such as an "IND"
        # prefix do not split otherwise matching OCR readings.
        if valid_struct_candidates:
            groups = {}
            for candidate in valid_struct_candidates:
                groups.setdefault(candidate["normalizedText"], []).append(candidate)
            winning_group = max(
                groups.values(),
                key=lambda group: (
                    len(group),
                    sum(item["confidence"] for item in group) / len(group),
                    max(item["confidence"] for item in group),
                ),
            )
            winner = max(winning_group, key=lambda c: c["confidence"])
            # Keep the recognizer's measured confidence; consensus controls
            # which normalized text wins rather than artificially inflating it.
            return winner["rawText"], winner["confidence"], winner["image"], candidate_list
        elif fallback_candidates:
            fallback_candidates.sort(key=lambda c: c["confidence"], reverse=True)
            winner = fallback_candidates[0]
            return winner["rawText"], winner["confidence"], winner["image"], candidate_list
        else:
            return "", 0.0, roi_bgr, []
