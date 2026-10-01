import cv2
import numpy as np
import logging
import os

logger = logging.getLogger(__name__)

class VehicleDetector:
    """
    Stage 1: Vehicle Detection Model (YOLOv8).
    Detects vehicles (cars, motorcycles, buses, trucks, autorickshaws) in raw camera frames
    and extracts high-confidence vehicle crops before license plate localization.
    """

    # COCO Vehicle Classes: 2 (car), 3 (motorcycle), 5 (bus), 7 (truck)
    VEHICLE_CLASS_IDS = {2, 3, 5, 7}

    def __init__(self, model_name="yolov8n.pt", conf_threshold=0.25):
        self.conf_threshold = conf_threshold
        self.model = None
        self.model_loaded = False

        try:
            from ultralytics import YOLO
            self.model = YOLO(model_name)
            self.model_loaded = True
            logger.info(f"Loaded YOLO vehicle detector model ({model_name}).")
        except Exception as e:
            logger.warning(f"Could not load vehicle detector model ({e}). Using full-frame vehicle crop as fallback.")

    def detect_vehicles(self, image_bgr):
        """
        Detects vehicle bounding boxes in the camera frame.
        Returns list of tuples: (vehicle_crop, bbox_(x, y, w, h), class_id, confidence)
        """
        if image_bgr is None or image_bgr.size == 0:
            return []

        h_img, w_img = image_bgr.shape[:2]
        vehicles = []

        if self.model_loaded and self.model is not None:
            try:
                results = self.model(image_bgr, verbose=False, conf=self.conf_threshold)
                for r in results:
                    boxes = r.boxes
                    for box in boxes:
                        cls_id = int(box.cls[0].cpu().numpy())
                        conf = float(box.conf[0].cpu().numpy())

                        if cls_id in self.VEHICLE_CLASS_IDS or conf >= 0.40:
                            x1, y1, x2, y2 = box.xyxy[0].cpu().numpy().astype(float)
                            ix1, iy1 = max(0, int(x1)), max(0, int(y1))
                            ix2, iy2 = min(w_img, int(x2)), min(h_img, int(y2))
                            w, h = ix2 - ix1, iy2 - iy1

                            if w > 30 and h > 30:
                                crop = image_bgr[iy1:iy2, ix1:ix2]
                                vehicles.append((crop, (ix1, iy1, w, h), cls_id, conf))
            except Exception as e:
                logger.error(f"Vehicle detection error: {e}")

        # Fallback to full frame if no vehicle boxes were detected
        if not vehicles:
            vehicles.append((image_bgr, (0, 0, w_img, h_img), 2, 0.80))

        vehicles.sort(key=lambda v: v[3], reverse=True)
        return vehicles
