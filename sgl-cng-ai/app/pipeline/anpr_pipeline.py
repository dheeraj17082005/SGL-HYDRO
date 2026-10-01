import os
import time
import requests
import logging
import cv2
import json
from collections import Counter
from datetime import datetime
from app.detector.vehicle_detector import VehicleDetector
from app.tracker.vehicle_tracker import VehicleTracker
from app.detector.plate_detector import PlateDetector
from app.ocr.ocr_engine import OcrEngine
from app.pipeline.normalizer import PlateNormalizer
from app.utils.image_utils import load_image, preprocess_roi, annotate_debug_image

logger = logging.getLogger(__name__)

class AnprPipeline:
    """
    Production End-to-End Indian ANPR Pipeline:
    Camera Frame → Vehicle Detection → Vehicle Tracking → Vehicle Crop → Indian Plate Detector → Perspective Correction → Preprocessing → Indian OCR Model → Format Validation → Temporal Voting → Final ANPR Result → Backend Dispatch
    """

    def __init__(self,
                 min_confidence=None,
                 min_stable_frames=None,
                 stability_window_seconds=None,
                 cooldown_seconds=None,
                 frame_skip=None,
                 backend_url=None,
                 station_id=None,
                 camera_id=None,
                 send_to_backend=None,
                 use_gpu=False,
                 debug_dir="debug_output"):

        self.min_confidence = min_confidence if min_confidence is not None else float(os.getenv("MIN_CONFIDENCE", "0.45"))
        self.min_frame_candidate_confidence = float(os.getenv("MIN_FRAME_CANDIDATE_CONFIDENCE", "0.30"))
        self.min_stable_frames = min_stable_frames if min_stable_frames is not None else int(os.getenv("MIN_STABLE_FRAMES", "1"))
        self.stability_window_seconds = stability_window_seconds if stability_window_seconds is not None else float(os.getenv("STABILITY_WINDOW_SECONDS", "2.0"))
        self.cooldown_seconds = cooldown_seconds if cooldown_seconds is not None else float(os.getenv("COOLDOWN_SECONDS", "20.0"))
        self.frame_skip = frame_skip if frame_skip is not None else int(os.getenv("FRAME_SKIP", "5"))
        self.debug_mode = os.getenv("ANPR_DEBUG_MODE", "true").lower() in ("true", "1", "yes")

        url_from_env = os.getenv("BACKEND_URL", "http://localhost:8080")
        self.backend_url = (backend_url if backend_url is not None else url_from_env).rstrip("/")
        
        self.station_id = station_id if station_id is not None else int(os.getenv("STATION_ID", "1"))
        self.camera_id = camera_id if camera_id is not None else int(os.getenv("CAMERA_ID", "1"))
        
        if send_to_backend is not None:
            self.send_to_backend = send_to_backend
        else:
            self.send_to_backend = os.getenv("SEND_BACKEND", "true").lower() in ("true", "1", "yes")

        self.debug_dir = debug_dir
        self.frames_debug_dir = os.path.join(debug_dir, "frames")
        self.crops_debug_dir = os.path.join(debug_dir, "crops")
        os.makedirs(self.frames_debug_dir, exist_ok=True)
        os.makedirs(self.crops_debug_dir, exist_ok=True)

        # Pipeline Components
        self.vehicle_detector = VehicleDetector()
        self.vehicle_tracker = VehicleTracker()
        self.plate_detector = PlateDetector()
        self.ocr_engine = OcrEngine(use_gpu=use_gpu)

        self.last_dispatched = {} # (track_id, plate) -> timestamp
        self.temporal_track_buffer = {} # track_id -> list of {"plate": str, "conf": float, "time": float}

    def process_image(self, image_input, generate_debug=False, station_id=None, camera_id=None, frame_id=None, dispatch_to_backend=None):
        """
        Processes a single camera frame through the full hierarchical pipeline:
        Frame → Vehicle Crop → Track ID → Plate Crop → Rectification → OCR → Validation → Temporal Voting
        """
        st_id = station_id if station_id is not None else self.station_id
        cam_id = camera_id if camera_id is not None else self.camera_id
        fid = frame_id if frame_id is not None else int(time.time() * 1000) % 100000
        should_dispatch = self.send_to_backend if dispatch_to_backend is None else dispatch_to_backend

        try:
            image_bgr = load_image(image_input)
        except Exception as e:
            logger.error(f"Image load failure: {e}")
            return {
                "plateDetected": False,
                "registrationNumber": None,
                "confidence": 0.0,
                "vehicleConfidence": 0.0,
                "plateConfidence": 0.0,
                "ocrConfidence": 0.0,
                "finalConfidence": 0.0,
                "statusState": "UNREADABLE",
                "timestamp": datetime.now().isoformat(),
                "reason": f"IMAGE_LOAD_ERROR: {str(e)}"
            }, None

        h_img, w_img = image_bgr.shape[:2]

        # Stage 1: Vehicle Detection
        vehicle_candidates = self.vehicle_detector.detect_vehicles(image_bgr)
        vehicle_bboxes = [v[1] for v in vehicle_candidates]

        # Stage 2: Vehicle Tracking
        has_detected_vehicles = bool(vehicle_bboxes)
        tracked_vehicles = self.vehicle_tracker.update(vehicle_bboxes)
        if not tracked_vehicles:
            tracked_vehicles = [(101, (0, 0, w_img, h_img))]

        best_result = None
        best_overall_score = -1.0

        for track_id, (vx, vy, vw, vh) in tracked_vehicles:
            v_crop = image_bgr[vy:vy+vh, vx:vx+vw]
            if v_crop.size == 0:
                v_crop = image_bgr

            # Find vehicle conf
            veh_conf = next((v[3] for v in vehicle_candidates if v[1] == (vx, vy, vw, vh)), 0.80)

            # Stage 3: Indian License Plate Detection & Perspective Rectification
            plate_candidates = self.plate_detector.detect_candidates(v_crop, is_vehicle_crop=has_detected_vehicles)

            for p_crop, p_bbox, p_conf in plate_candidates:
                # Stage 4: OCR Extraction across Preprocessing Variants
                raw_text, ocr_conf, winning_img, ocr_candidates = self.ocr_engine.read_text_multi_variant(p_crop)

                # Stage 5: Format Validation & Positional Character Correction
                is_valid, reason, normalized = PlateNormalizer.validate_registration_structure(raw_text)

                if is_valid and normalized:
                    import re
                    is_indian = reason in ("VALID_INDIAN_PLATE", "VALID_BHARAT_PLATE")
                    is_standard_hsrp = bool(re.match(r'^[A-Z]{2}\d{2}[A-Z]{1,3}\d{4}$', normalized))

                    # Weighted score prioritizing confirmed Indian HSRP formats
                    cand_score = (
                        (veh_conf * 0.15)
                        + (p_conf * 0.25)
                        + (ocr_conf * 0.60)
                        + (2.5 if is_standard_hsrp else 1.0 if is_indian else 0.0)
                        + (0.3 if len(normalized) >= 9 else 0.0)
                    )

                    if cand_score > best_overall_score:
                        best_overall_score = cand_score
                        final_conf = (veh_conf * 0.20) + (p_conf * 0.30) + (ocr_conf * 0.50)
                        status_state = "CONFIRMED" if (is_indian or final_conf >= 0.50) else "LOW_CONFIDENCE"

                        best_result = {
                            "plateDetected": True,
                            "registrationNumber": normalized,
                            "rawOcr": raw_text,
                            "trackId": track_id,
                            "confidence": round(max(0.75, final_conf) if is_indian else final_conf, 2),
                            "vehicleConfidence": round(veh_conf, 2),
                            "plateConfidence": round(p_conf, 2),
                            "ocrConfidence": round(ocr_conf, 2),
                            "finalConfidence": round(max(0.75, final_conf) if is_indian else final_conf, 2),
                            "statusState": status_state,
                            "timestamp": datetime.now().isoformat(),
                            "reason": None
                        }
                        if is_standard_hsrp and ocr_conf >= 0.85:
                            break

            if best_result and best_overall_score >= 3.0:
                break

        # Stage 6: Temporal Voting & Cooldown Backend Dispatch
        if best_result and best_result["registrationNumber"]:
            reg_num = best_result["registrationNumber"]
            tr_id = best_result["trackId"]
            now = time.time()

            # Temporal voting accumulator per track_id
            buf = self.temporal_track_buffer.setdefault(tr_id, [])
            buf.append({"plate": reg_num, "conf": best_result["finalConfidence"], "time": now})
            self.temporal_track_buffer[tr_id] = [item for item in buf if (now - item["time"]) <= self.stability_window_seconds]

            counts = Counter(item["plate"] for item in self.temporal_track_buffer[tr_id])
            matching_count = counts.get(reg_num, 0)

            if matching_count >= self.min_stable_frames:
                last_sent = self.last_dispatched.get((tr_id, reg_num), 0.0)
                if (now - last_sent) >= self.cooldown_seconds:
                    if should_dispatch:
                        self._dispatch_to_backend(reg_num, st_id, cam_id, tr_id, best_result)
                        self.last_dispatched[(tr_id, reg_num)] = now

            # Save debug telemetry if enabled
            if self.debug_mode:
                self._write_debug_telemetry(fid, best_result)

            return best_result, image_bgr

        # Fallback when no plate recognized
        now_iso = datetime.now().isoformat()
        return {
            "plateDetected": False,
            "registrationNumber": None,
            "confidence": 0.0,
            "vehicleConfidence": 0.0,
            "plateConfidence": 0.0,
            "ocrConfidence": 0.0,
            "finalConfidence": 0.0,
            "statusState": "UNREADABLE",
            "timestamp": now_iso,
            "reason": "NO_PLATE_DETECTED"
        }, image_bgr

    def _dispatch_to_backend(self, registration_number, station_id, camera_id, track_id, result_info):
        payload = {
            "stationId": station_id,
            "cameraId": camera_id,
            "trackId": track_id,
            "registrationNumber": registration_number,
            "vehicleConfidence": result_info.get("vehicleConfidence", 0.80),
            "plateConfidence": result_info.get("plateConfidence", 0.80),
            "ocrConfidence": result_info.get("ocrConfidence", 0.80),
            "finalConfidence": result_info.get("finalConfidence", 0.80),
            "statusState": result_info.get("statusState", "CONFIRMED"),
            "detectedAt": datetime.now().isoformat()
        }

        target_endpoint = f"{self.backend_url}/api/v1/anpr/detections"
        try:
            resp = requests.post(target_endpoint, json=payload, timeout=3.0)
            logger.info(f"[BACKEND DISPATCH] Track {track_id} | Plate {registration_number} | HTTP {resp.status_code}")
        except Exception as e:
            logger.warning(f"[BACKEND DISPATCH ERROR] {e}")

    def _write_debug_telemetry(self, fid, result_info):
        try:
            debug_path = os.path.join(self.debug_dir, f"anpr_debug_{fid}.json")
            with open(debug_path, "w") as f:
                json.dump(result_info, f, indent=2)
        except Exception:
            pass
