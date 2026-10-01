import logging
import os
import cv2
import numpy as np

logger = logging.getLogger(__name__)

class OcrEngine:
    """
    Stage 4: OCR Engine supporting two-line Indian plates, pluggable OCR providers (EasyOCR / PyTesseract / Awiros candidate),
    and multi-variant image candidate evaluation.
    """

    def __init__(self, provider=None, use_gpu=False):
        self.provider = provider or os.getenv("ANPR_OCR_PROVIDER", "easyocr").lower()
        self.reader = None

        if self.provider == "easyocr" or self.provider == "awiros":
            try:
                import easyocr
                self.reader = easyocr.Reader(['en'], gpu=use_gpu)
                self.provider = "easyocr"
                logger.info("Initialized EasyOCR engine successfully.")
            except Exception as e:
                logger.warning(f"EasyOCR initialization failed ({e}). Falling back to PyTesseract.")
                self.provider = "tesseract"

        if self.provider == "tesseract":
            try:
                import pytesseract
                self.provider = "tesseract"
                logger.info("Initialized PyTesseract OCR engine.")
            except Exception as e2:
                logger.warning(f"PyTesseract not available ({e2}).")
                self.provider = "fallback"

    def read_text(self, roi_image):
        """
        Reads text from cropped license plate ROI.
        Detects single-row vs two-row Indian plate layouts, OCRs each line, and combines correctly.
        Returns tuple: (raw_text, confidence)
        """
        if roi_image is None or roi_image.size == 0:
            return "", 0.0

        h, w = roi_image.shape[:2]
        aspect_ratio = float(w) / float(h) if h > 0 else 1.0

        # Two-line Indian plate handling (aspect ratio < 2.2, e.g. MH 12 / AB 1234 stacked)
        if aspect_ratio < 2.2 and h >= 30:
            mid_y = h // 2
            top_half = roi_image[0:mid_y, :]
            bottom_half = roi_image[mid_y:h, :]

            t_text, t_conf = self._read_single_line(top_half)
            b_text, b_conf = self._read_single_line(bottom_half)

            if t_text and b_text:
                combined_text = f"{t_text}{b_text}"
                avg_conf = (t_conf + b_conf) / 2.0
                return combined_text, avg_conf

        return self._read_single_line(roi_image)

    def _read_single_line(self, roi_image):
        if roi_image is None or roi_image.size == 0:
            return "", 0.0

        # Downscale oversized crops so OCR inference remains ultra-fast
        h, w = roi_image.shape[:2]
        if w > 640 or h > 320:
            scale = min(640.0 / w, 320.0 / h)
            roi_image = cv2.resize(roi_image, (round(w * scale), round(h * scale)), interpolation=cv2.INTER_AREA)

        # 1. Fast PyTesseract check (runs in ~20ms and excels on HSRP license plate fonts)
        try:
            import pytesseract
            cfg = '--psm 7 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
            t_raw = pytesseract.image_to_string(roi_image, config=cfg).strip()
            if t_raw:
                from app.pipeline.normalizer import PlateNormalizer
                is_valid, _, norm = PlateNormalizer.validate_registration_structure(t_raw)
                if is_valid and norm:
                    return norm, 0.95
        except Exception:
            pass

        # 2. EasyOCR with alphanumeric allowlist
        if self.reader is not None:
            try:
                results = self.reader.readtext(roi_image, allowlist='ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789')
                if results:
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

        # 3. Fallback Tesseract if EasyOCR returned nothing
        try:
            import pytesseract
            cfg = '--psm 7 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
            t_raw = pytesseract.image_to_string(roi_image, config=cfg).strip()
            if t_raw:
                return t_raw, 0.60
        except Exception:
            pass

        return "", 0.0

    def read_text_multi_variant(self, roi_bgr):
        """
        Runs OCR across image crop variations (original, upscaled/sharpened, CLAHE, denoised, thresholded, deskewed)
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
                    import re
                    is_indian = reason in ("VALID_INDIAN_PLATE", "VALID_BHARAT_PLATE")
                    is_standard_hsrp = bool(re.match(r'^[A-Z]{2}\d{2}[A-Z]{1,3}\d{4}$', normalized))
                    # Early exit only on confirmed standard 2-digit RTO Indian plates
                    if is_standard_hsrp and conf >= 0.40:
                        return normalized, max(0.65, float(conf)), img_var, candidate_list
                else:
                    fallback_candidates.append(cand_info)

        if valid_struct_candidates:
            import re
            # Prioritize valid Indian state plates over general alphanumeric strings
            indian_pool = [c for c in valid_struct_candidates if c.get("reason") in ("VALID_INDIAN_PLATE", "VALID_BHARAT_PLATE")]
            pool = indian_pool if indian_pool else valid_struct_candidates
            winner = max(
                pool,
                key=lambda c: (
                    bool(re.match(r'^[A-Z]{2}\d{2}[A-Z]{1,3}\d{4}$', c["normalizedText"])),
                    len(c["normalizedText"]) >= 9,
                    float(c["confidence"])
                )
            )
            final_conf = max(0.65, float(winner["confidence"]))
            return winner["normalizedText"], final_conf, winner["image"], candidate_list
        elif fallback_candidates:
            fallback_candidates.sort(key=lambda c: c["confidence"], reverse=True)
            winner = fallback_candidates[0]
            return winner["normalizedText"] or winner["rawText"], float(winner["confidence"]), winner["image"], candidate_list
        else:
            return "", 0.0, roi_bgr, []
