import cv2
import numpy as np
import logging
import os

logger = logging.getLogger(__name__)

class PlateDetector:
    """
    Stage 3: Indian License Plate Localizer & Perspective Rectifier.
    Extracts license plate bounding boxes (x, y, w, h) from vehicle crops, calculates plate confidence,
    crops with pad, and rectifies perspective distortion for angled/tilted plates.
    """

    def __init__(self, model_name=None, conf_threshold=0.10):
        self.conf_threshold = conf_threshold
        self.indian_model = None
        self.model = None
        self.model_loaded = False

        # 1. Dedicated Indian License Plate YOLOv8 Model
        indian_paths = [
            "plate_model_indian.pt",
            os.path.join(os.path.dirname(__file__), "../../plate_model_indian.pt"),
            os.path.join(os.path.dirname(__file__), "../../../plate_model_indian.pt")
        ]
        target_indian = None
        for p in indian_paths:
            if p and os.path.exists(p):
                target_indian = p
                break
        if not target_indian:
            try:
                from huggingface_hub import hf_hub_download
                target_indian = hf_hub_download(repo_id="maazsajid/license-plate-yolov8", filename="best.pt")
            except Exception as e:
                logger.warning(f"Could not download Indian plate detector ({e}).")

        if target_indian:
            try:
                from ultralytics import YOLO
                self.indian_model = YOLO(target_indian)
                logger.info(f"Loaded dedicated Indian YOLO plate detector from ({target_indian}).")
            except Exception as e:
                logger.warning(f"Could not load Indian YOLO model ({e}).")

        # 2. General License Plate YOLOv8 Model
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
                target_model = hf_hub_download(repo_id="Murd0ck/LicensePlateDetector_YOLOv8n", filename="best.pt")
            except Exception as e:
                logger.warning(f"Could not download general plate detector ({e}). Falling back to yolov8n.pt.")
                target_model = "yolov8n.pt"

        try:
            from ultralytics import YOLO
            self.model = YOLO(target_model)
            self.model_loaded = True
            logger.info(f"Loaded general YOLO plate detector model from ({target_model}).")
        except Exception as e:
            logger.warning(f"Could not load general YOLO model ({e}). Using OpenCV morphological detector as fallback.")

    def detect_candidates(self, image_bgr, is_vehicle_crop=False):
        """
        Detects potential license plate regions in the vehicle crop or scene.
        Returns list of tuples: (cropped_roi, (x, y, w, h), detector_confidence)
        """
        if image_bgr is None or image_bgr.size == 0:
            return []

        h_img, w_img = image_bgr.shape[:2]
        candidates = []

        # 1. Primary: dedicated Indian plate model
        if self.indian_model is not None:
            try:
                results_ind = self.indian_model(image_bgr, verbose=False, conf=self.conf_threshold)
                cands_ind = self._extract_boxes(results_ind, image_bgr, scale_factor=1.0)
                if cands_ind:
                    candidates.extend(cands_ind)
            except Exception as e:
                logger.error(f"Indian plate model inference error: {e}")

        # 2. General model
        if self.model_loaded and self.model is not None:
            try:
                results = self.model(image_bgr, verbose=False, conf=self.conf_threshold)
                cands_gen = self._extract_boxes(results, image_bgr, scale_factor=1.0)
                if cands_gen:
                    candidates.extend(cands_gen)
            except Exception as e:
                logger.error(f"General YOLO inference error: {e}.")

        # 3. Direct candidate if image itself has plate proportions (e.g. uploaded close-up)
        aspect = float(w_img) / float(max(1, h_img))
        if 2.0 <= aspect <= 5.5:
            candidates.append((image_bgr, (0, 0, w_img, h_img), 0.50))
        elif not candidates:
            if is_vehicle_crop:
                contour_candidates = self._opencv_fallback_detect(image_bgr)
                if contour_candidates:
                    candidates.append(contour_candidates[0])
            else:
                cx1, cy1 = int(0.12 * w_img), int(0.15 * h_img)
                cx2, cy2 = int(0.88 * w_img), int(0.85 * h_img)
                center_roi = image_bgr[cy1:cy2, cx1:cx2]
                if center_roi.size > 0:
                    candidates.append((center_roi, (cx1, cy1, cx2 - cx1, cy2 - cy1), 0.35))

        # Deduplicate and sort candidates
        candidates.sort(key=lambda item: item[2], reverse=True)

        rectified_candidates = []
        seen_boxes = []
        for roi, bbox, conf in candidates:
            # Avoid duplicate overlapping crops
            bx, by, bw, bh = bbox
            overlap = False
            for ox, oy, ow, oh in seen_boxes:
                if abs(bx - ox) < 40 and abs(by - oy) < 40:
                    overlap = True
                    break
            if overlap:
                continue
            seen_boxes.append(bbox)
            rectified_roi = self._rectify_perspective(roi)
            rectified_candidates.append((rectified_roi, bbox, conf))
            if len(rectified_candidates) >= 3:
                break

        return rectified_candidates

    def _extract_boxes(self, results, original_image, scale_factor=1.0):
        h_orig, w_orig = original_image.shape[:2]
        candidates = []

        for r in results:
            boxes = r.boxes
            for box in boxes:
                x1, y1, x2, y2 = box.xyxy[0].cpu().numpy().astype(float)
                conf = float(box.conf[0].cpu().numpy())

                if scale_factor != 1.0:
                    x1 /= scale_factor
                    y1 /= scale_factor
                    x2 /= scale_factor
                    y2 /= scale_factor

                ix1, iy1 = max(0, int(x1)), max(0, int(y1))
                ix2, iy2 = min(w_orig, int(x2)), min(h_orig, int(y2))
                w, h = ix2 - ix1, iy2 - iy1

                if w > 10 and h > 10:
                    # If aspect ratio is narrow (< 3.2), Indian plates often have the left IND/state code clipped
                    if w / max(1, h) < 3.2 and ix1 > 0:
                        expand_left = int(0.40 * w)
                        ix1 = max(0, ix1 - expand_left)
                        w = ix2 - ix1

                    pad_x = max(2, int(0.08 * w))
                    pad_y = max(2, int(0.15 * h))
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

            if 300 <= area <= (0.25 * w_img * h_img) and 1.8 <= aspect_ratio <= 5.5:
                roi = image_bgr[y:y+h, x:x+w]
                candidates.append((roi, (x, y, w, h), 0.50))

        candidates.sort(key=lambda item: item[1][2] * item[1][3], reverse=True)
        return candidates

    def _rectify_perspective(self, roi_bgr):
        """
        Detects plate boundary corners and applies perspective transformation matrix
        to produce a rectified horizontal license plate crop.
        """
        if roi_bgr is None or roi_bgr.size == 0:
            return roi_bgr

        h, w = roi_bgr.shape[:2]
        if h < 20 or w < 40:
            return roi_bgr

        gray = cv2.cvtColor(roi_bgr, cv2.COLOR_BGR2GRAY) if len(roi_bgr.shape) == 3 else roi_bgr.copy()
        blur = cv2.GaussianBlur(gray, (5, 5), 0)
        edges = cv2.Canny(blur, 50, 150)

        contours, _ = cv2.findContours(edges, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
        contours = sorted(contours, key=cv2.contourArea, reverse=True)[:5]

        for c in contours:
            peri = cv2.arcLength(c, True)
            approx = cv2.approxPolyDP(c, 0.04 * peri, True)
            if len(approx) == 4 and cv2.isContourConvex(approx):
                pts = approx.reshape(4, 2)
                rect = self._order_points(pts)
                dst = np.array([
                    [0, 0],
                    [w - 1, 0],
                    [w - 1, h - 1],
                    [0, h - 1]
                ], dtype="float32")
                M = cv2.getPerspectiveTransform(rect, dst)
                rectified = cv2.warpPerspective(roi_bgr, M, (w, h))
                return rectified

        return roi_bgr

    def _order_points(self, pts):
        rect = np.zeros((4, 2), dtype="float32")
        s = pts.sum(axis=1)
        rect[0] = pts[np.argmin(s)]
        rect[2] = pts[np.argmax(s)]
        diff = np.diff(pts, axis=1)
        rect[1] = pts[np.argmin(diff)]
        rect[3] = pts[np.argmax(diff)]
        return rect
