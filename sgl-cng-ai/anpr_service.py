import os
import cv2
import numpy as np
import easyocr
import torch
from ultralytics import YOLO
from huggingface_hub import hf_hub_download
import re
from typing import Dict, Any, List, Optional, Tuple

class ANPRService:
    """
    Unified, Deterministic ANPR Service.
    Shared by both static image processing and live camera streaming.
    Contains the exact verified pipeline:
    - YOLOv8 plate detector
    - Exact bbox crop (pad=0)
    - Dual candidate preprocessing: HSRP Morph Open & Edge-Preserving Bilateral Filter
    - EasyOCR alphanumeric extraction
    - Indian plate format normalization & positional disambiguation
    """
    _instance = None

    @classmethod
    def get_instance(cls, model_path: Optional[str] = None):
        if cls._instance is None:
            cls._instance = cls(model_path=model_path)
        return cls._instance

    def __init__(self, model_path: Optional[str] = None):
        print("[ANPRService] Initializing unified ANPR engine...")
        
        # 1. Load YOLOv8 plate detector
        candidate_paths = [
            model_path,
            "plate_model_indian.pt",
            "/app/plate_model_indian.pt",
            "license_plate_yolov8n.pt",
            "/app/license_plate_yolov8n.pt",
            "best.pt",
            "/app/best.pt"
        ]
        self.model_path = None
        for p in candidate_paths:
            if p and os.path.exists(p):
                self.model_path = p
                break

        if not self.model_path:
            print("[ANPRService] No local plate model found, attempting download...")
            try:
                self.model_path = hf_hub_download(
                    repo_id="maazsajid/license-plate-yolov8",
                    filename="best.pt",
                    local_dir="."
                )
            except Exception as e:
                print(f"[ANPRService] Hugging Face download failed: {e}. Falling back to default YOLOv8n.")
                self.model_path = "yolov8n.pt"
            
        print(f"[ANPRService] Loading YOLO model from {self.model_path}...")
        self.detector = YOLO(self.model_path)
        
        # 2. Load EasyOCR engine (quantize=False prevents CPU LSTM state corruption across calls)
        use_gpu = torch.cuda.is_available()
        print(f"[ANPRService] Initializing EasyOCR (GPU: {use_gpu}, quantize=False)...")
        self.reader = easyocr.Reader(['en'], gpu=use_gpu, quantize=False)
        print("[ANPRService] Ready.")

    @staticmethod
    def clean_hsrp_plate(cropped_bgr: np.ndarray) -> np.ndarray:
        """
        Suppresses the holographic carbon-fiber pattern on Indian HSRP plates.
        Removes laser-etched honeycomb patterns so OCR does not read 'LELELE'.
        """
        h, w, _ = cropped_bgr.shape
        clean_img = cropped_bgr.copy()
        
        # Mask out the blue 'IND' strip on the left (approx 13% of width)
        mask_width = int(w * 0.13)
        clean_img[:, :mask_width] = (255, 255, 255)
        
        # Convert to grayscale
        gray = cv2.cvtColor(clean_img, cv2.COLOR_BGR2GRAY)
        
        # Morphological opening melts reflective dots into solid characters
        kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (3, 3))
        opened = cv2.morphologyEx(gray, cv2.MORPH_OPEN, kernel)
        
        return opened

    @staticmethod
    def validate_and_normalize_indian_plate(text: str) -> str:
        """
        Indian Registration Format Validation & Positional Character Disambiguation.
        Normalizes:
        - Uppercase, alphanumeric only
        - Strips country prefix (IND)
        - Strips phantom leading character before valid state code (e.g. HHR -> HR, IHR -> HR)
        - Corrects state code misreads (RI -> RJ, GI -> GJ, 0L -> DL, H8 -> HR)
        - Positional disambiguation: State (letters) -> RTO (digits) -> Series (letters) -> Number (digits)
        """
        clean = "".join(c for c in text.upper() if c.isalnum())
        if clean.startswith("IND") and len(clean) > 7:
            clean = clean[3:]
            
        clean = clean.replace("RI", "RJ").replace("GI", "GJ").replace("0L", "DL")
        if clean.startswith("H8"):
            clean = "HR" + clean[2:]
            
        # Strip leading phantom character if followed by valid 2-letter state code
        state_codes = [
            'HR', 'RJ', 'DL', 'GJ', 'MH', 'UP', 'KA', 'TN', 'PB', 'CH',
            'AP', 'TS', 'MP', 'WB', 'BR', 'KL', 'UK', 'HP', 'JK', 'GA'
        ]
        for sc in state_codes:
            if clean.startswith(sc):
                break
            if len(clean) >= 3 and clean[1:3] == sc:
                clean = clean[1:]
                break
            
        if len(clean) >= 9:
            state = clean[:2]
            # Digits 2-3 (RTO code): letters commonly misread as numbers
            rto = clean[2:4].replace("I", "1").replace("L", "4").replace("O", "0") \
                            .replace("B", "8").replace("S", "5").replace("Z", "2").replace("G", "4")
            rest = clean[4:]
            
            # Series and trailing registration number
            if len(rest) >= 5:
                raw_series = rest[:-4]
                # In series (letters), digits are misread letters:
                digit_to_letter = {'0': 'O', '1': 'I', '8': 'B', '5': 'S', '2': 'Z', '6': 'G'}
                series = ''.join(digit_to_letter.get(c, c) for c in raw_series)
                
                # In registration number (last 4), letters are misread digits:
                letter_to_digit = {'O': '0', 'Q': '0', 'I': '1', 'L': '1', 'B': '8', 'S': '5', 'Z': '2'}
                digits = ''.join(letter_to_digit.get(c, c) for c in rest[-4:])
                clean = state + rto + series + digits
            else:
                clean = state + rto + rest
                
        return clean

    @staticmethod
    def is_valid_indian_format(plate: str) -> bool:
        """Validates if string matches standard Indian plate or BH series."""
        std_pattern = r'^[A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{4}$'
        bh_pattern = r'^[0-9]{2}BH[0-9]{4}[A-Z]{1,2}$'
        return bool(re.match(std_pattern, plate)) or bool(re.match(bh_pattern, plate))

    def detect_plates(self, img: np.ndarray, conf: float = 0.25) -> List[Dict[str, Any]]:
        """
        Runs YOLOv8 detection on the full image and returns all detected plates.
        """
        h, w, _ = img.shape
        results = self.detector(img, conf=conf, verbose=False)
        boxes = results[0].boxes
        
        detections = []
        for box in boxes:
            x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
            det_conf = float(box.conf[0])
            
            # Exact bounding box crop (pad=0 ensures no outer black holder frame)
            x1, y1 = max(0, x1), max(0, y1)
            x2, y2 = min(w, x2), min(h, y2)
            crop = img[y1:y2, x1:x2]
            
            if crop.size > 0:
                detections.append({
                    "bbox": (x1, y1, x2, y2),
                    "confidence": det_conf,
                    "crop": crop
                })
                
        # Sort by confidence descending
        detections.sort(key=lambda d: d["confidence"], reverse=True)
        return detections

    def read_crop(self, cropped_plate: np.ndarray) -> Dict[str, Any]:
        """
        Preprocesses a plate crop with dual complementary filters and runs EasyOCR with validation.
        Candidate 1: HSRP Morphological Opening (neutralizes honeycomb hologram texture)
        Candidate 2: Edge-Preserving Bilateral Filter (neutralizes high-frequency noise without character damage)
        """
        if cropped_plate is None or cropped_plate.size == 0:
            return {"raw_text": "", "normalized_text": "", "ocr_confidence": 0.0, "is_valid": False}
            
        candidates = []
        
        # Candidate 1: HSRP Morphological Opening
        p1 = self.clean_hsrp_plate(cropped_plate)
        res1 = self.reader.readtext(p1, allowlist="ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")
        t1 = "".join([t[1] for t in res1 if t[1] != "IND"])
        c1 = float(np.mean([t[2] for t in res1])) if res1 else 0.0
        n1 = self.validate_and_normalize_indian_plate(t1)
        v1 = self.is_valid_indian_format(n1)
        candidates.append({"normalized": n1, "raw": t1, "conf": c1, "valid": v1, "method": "hsrp_morph"})
        
        # Candidate 2: Bilateral Filter (Edge-preserving noise suppression)
        gray = cv2.cvtColor(cropped_plate, cv2.COLOR_BGR2GRAY)
        p2 = cv2.bilateralFilter(gray, 5, 50, 50)
        res2 = self.reader.readtext(p2, allowlist="ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")
        t2 = "".join([t[1] for t in res2 if t[1] != "IND"])
        c2 = float(np.mean([t[2] for t in res2])) if res2 else 0.0
        n2 = self.validate_and_normalize_indian_plate(t2)
        v2 = self.is_valid_indian_format(n2)
        candidates.append({"normalized": n2, "raw": t2, "conf": c2, "valid": v2, "method": "bilateral"})
        
        # Selection: Valid Indian format strictly prioritized over confidence
        # If both are valid, select higher OCR confidence
        valid_candidates = [c for c in candidates if c["valid"]]
        if valid_candidates:
            best = max(valid_candidates, key=lambda c: c["conf"])
        else:
            best = max(candidates, key=lambda c: c["conf"])
            
        return {
            "raw_text": best["raw"],
            "normalized_text": best["normalized"],
            "ocr_confidence": round(best["conf"], 4),
            "is_valid": best["valid"],
            "method": best["method"]
        }

    def detect_and_read(self, img: np.ndarray, conf: float = 0.25) -> Dict[str, Any]:
        """
        Unified method: Detects plate, crops exact bbox, preprocesses, and reads OCR.
        """
        if img is None:
            return {"plateDetected": False, "error": "Invalid image"}
            
        detections = self.detect_plates(img, conf=conf)
        if not detections:
            return {"plateDetected": False, "error": "No plate found"}
            
        best_det = detections[0]
        x1, y1, x2, y2 = best_det["bbox"]
        det_conf = best_det["confidence"]
        
        ocr_res = self.read_crop(best_det["crop"])
        plate_text = ocr_res["normalized_text"]
        
        return {
            "plateDetected": True,
            "registrationNumber": plate_text,
            "rawOcr": ocr_res["raw_text"],
            "confidence": round(det_conf, 2),
            "ocrConfidence": ocr_res["ocr_confidence"],
            "bbox": {"x1": x1, "y1": y1, "x2": x2, "y2": y2},
            "isValidIndianFormat": ocr_res["is_valid"],
            "statusState": "CONFIRMED" if ocr_res["is_valid"] or plate_text else "UNCONFIRMED"
        }
