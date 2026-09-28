import cv2
import numpy as np
import logging
import os

logger = logging.getLogger(__name__)

class PlateDetector:
    """
    Pretrained YOLOv8 License Plate Object Detector.
    Localizes dedicated license plate bounding boxes (x, y, w, h) and returns cropped ROI
    along with detection confidence.
    """

    def __init__(self, model_name=None, conf_threshold=0.25):
        self.conf_threshold = conf_threshold
        self.model = None
        self.model_loaded = False

        # Prioritize dedicated license plate detection model weights
        possible_paths = [
            model_name,
            "license_plate_yolov8n.pt",
            os.path.join(os.path.dirname(__file__), "../../license_plate_yolov8n.pt"),
            os.path.join(os.path.dirname(__file__), "../../../license_plate_yolov8n.pt")
        ]

        target_model = None
        for path in possible_paths:
            if path and os.path.exists(path):
                target_model = path
                break

        if not target_model:
            try:
                from huggingface_hub import hf_hub_download
                logger.info("Downloading pretrained YOLO license plate model from Hugging Face (Murd0ck/LicensePlateDetector_YOLOv8n)...")
                target_model = hf_hub_download(repo_id="Murd0ck/LicensePlateDetector_YOLOv8n", filename="best.pt")
            except Exception as e:
                logger.warning(f"Could not download dedicated plate detector from HF ({e}). Falling back to yolov8n.pt.")
                target_model = "yolov8n.pt"

        try:
            from ultralytics import YOLO
            self.model = YOLO(target_model)
            self.model_loaded = True
            logger.info(f"Loaded pretrained YOLO plate detector model from ({target_model}).")
        except Exception as e:
            logger.warning(f"Could not load YOLO model ({e}). Using OpenCV morphological detector as fallback.")

    def detect_candidates(self, image_bgr):
        """
        Detects potential license plate regions in the image using YOLOv8.
        Supports a two-level multi-scale detection strategy:
        Level 1: Direct inference on full frame
        Level 2: Fallback inference on 1.5x upscaled frame for distant/small plates
        Returns list of tuples: (cropped_roi, (x, y, w, h), detector_confidence)
        """
        if image_bgr is None or image_bgr.size == 0:
            return []

        h_img, w_img = image_bgr.shape[:2]

        if self.model_loaded and self.model is not None:
            try:
                # Preserve the model's proven default pass, then use a larger
                # retry for distant/small plates below.
                results = self.model(image_bgr, verbose=False, conf=self.conf_threshold)
                candidates = self._extract_boxes(results, image_bgr, scale_factor=1.0)

                # Retry small crops as well as empty detections. Previously a
                # weak, tiny box prevented the upscaled pass from ever running.
                small_candidate = candidates and max(min(box[1][2], box[1][3] * 4) for box in candidates) < 120
                if (not candidates or small_candidate) and max(w_img, h_img) < 1600:
                    scale = min(2.0, 1600.0 / max(w_img, h_img))
                    upscaled = cv2.resize(image_bgr, (round(w_img * scale), round(h_img * scale)), interpolation=cv2.INTER_CUBIC)
                    results_upscaled = self.model(upscaled, verbose=False, conf=self.conf_threshold, imgsz=1280)
                    retry = self._extract_boxes(results_upscaled, image_bgr, scale_factor=scale)
                    if retry:
                        candidates = (candidates or []) + retry
                        print(f"[PLATE DETECTOR] HIGH-RES RETRY ({scale:.2f}x) added {len(retry)} candidate(s)")

                if candidates:
                    candidates.sort(key=lambda item: item[2], reverse=True)
                    print(f"[PLATE DETECTOR] Threshold: {self.conf_threshold:.2f} | Detections Found: {len(candidates)} | Top Confidence: {candidates[0][2]:.2f}")
                    return candidates
                else:
                    print(f"[PLATE DETECTOR] Threshold: {self.conf_threshold:.2f} | Detections Found: 0")
            except Exception as e:
                logger.error(f"YOLO inference error: {e}. Falling back to OpenCV contour detector.")

        # Fallback OpenCV contour & aspect-ratio detector if model is unavailable
        return self._opencv_fallback_detect(image_bgr)

    def _extract_boxes(self, results, original_image, scale_factor=1.0):
        h_orig, w_orig = original_image.shape[:2]
        candidates = []

        for r in results:
            boxes = r.boxes
            for box in boxes:
                x1, y1, x2, y2 = box.xyxy[0].cpu().numpy().astype(float)
                conf = float(box.conf[0].cpu().numpy())

                # Map coordinates back if upscaled
                if scale_factor != 1.0:
                    x1 /= scale_factor
                    y1 /= scale_factor
                    x2 /= scale_factor
                    y2 /= scale_factor

                ix1, iy1 = max(0, int(x1)), max(0, int(y1))
                ix2, iy2 = min(w_orig, int(x2)), min(h_orig, int(y2))
                w, h = ix2 - ix1, iy2 - iy1

                if w > 10 and h > 10:
                    # Keep a little more border around plates so OCR does not
                    # lose edge characters when the detector box is tight.
                    pad_x = max(2, int(0.08 * w))
                    pad_y = max(2, int(0.16 * h))
                    px1 = max(0, ix1 - pad_x)
                    py1 = max(0, iy1 - pad_y)
                    px2 = min(w_orig, ix2 + pad_x)
                    py2 = min(h_orig, iy2 + pad_y)

                    roi = original_image[py1:py2, px1:px2]
                    candidates.append((roi, (ix1, iy1, w, h), conf))

        return candidates

    def _opencv_fallback_detect(self, image_bgr):
        h_img, w_img = image_bgr.shape[:2]
        gray = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2GRAY)
        blur = cv2.bilateralFilter(gray, 9, 75, 75)
        tophat = cv2.morphologyEx(blur, cv2.MORPH_TOPHAT, cv2.getStructuringElement(cv2.MORPH_RECT, (17, 3)))
        
        grad_x = cv2.Sobel(tophat, ddepth=cv2.CV_32F, dx=1, dy=0, ksize=-1)
        grad_x = np.absolute(grad_x)
        min_val, max_val = np.min(grad_x), np.max(grad_x)
        if max_val > min_val:
            grad_x = (255 * ((grad_x - min_val) / (max_val - min_val))).astype("uint8")
        else:
            grad_x = grad_x.astype("uint8")

        grad_x = cv2.GaussianBlur(grad_x, (5, 5), 0)
        _, thresh = cv2.threshold(grad_x, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (21, 5))
        closed = cv2.morphologyEx(thresh, cv2.MORPH_CLOSE, kernel)

        contours, _ = cv2.findContours(closed.copy(), cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        candidates = []

        for c in contours:
            x, y, w, h = cv2.boundingRect(c)
            aspect_ratio = float(w) / float(h) if h > 0 else 0
            area = w * h

            if 600 <= area <= (0.5 * w_img * h_img) and 2.0 <= aspect_ratio <= 6.5:
                roi = image_bgr[y:y+h, x:x+w]
                candidates.append((roi, (x, y, w, h), 0.85))

        candidates.sort(key=lambda item: item[1][2] * item[1][3], reverse=True)
        return candidates
