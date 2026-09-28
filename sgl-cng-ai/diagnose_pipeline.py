import os
import sys
import time
import json
import requests
import cv2
import numpy as np

from app.detector.plate_detector import PlateDetector
from app.ocr.ocr_engine import OcrEngine
from app.pipeline.normalizer import PlateNormalizer
from app.utils.image_utils import preprocess_roi, load_image

def run_diagnostics(source=0, backend_url="http://localhost:8080", station_id=1, camera_id=1, max_frames=10):
    print("==================================================")
    print("  SGL CNG ANPR - COMPREHENSIVE DIAGNOSTIC PIPELINE")
    print("==================================================")

    # Prepare debug output directories
    os.makedirs("debug/camera", exist_ok=True)
    os.makedirs("debug/detections", exist_ok=True)
    os.makedirs("debug/plates", exist_ok=True)

    stage_status = {
        "CAMERA": "FAIL",
        "PLATE_DETECTOR": "FAIL",
        "PLATE_CROP": "FAIL",
        "OCR": "FAIL",
        "NORMALIZATION": "FAIL",
        "SPRING_BOOT_ANPR_API": "FAIL",
        "COMPLIANCE_ENGINE": "FAIL"
    }

    # ==================================================
    # STAGE 1 — CAMERA
    # ==================================================
    print("\n==================================================")
    print("STAGE 1 — CAMERA DIAGNOSTICS")
    print("==================================================")
    
    cap = None
    frame_bgr = None

    if isinstance(source, int) or (isinstance(source, str) and source.isdigit()):
        cam_idx = int(source)
        print(f"[STAGE 1 CAMERA] Connecting to camera device index: {cam_idx}...")
        cap = cv2.VideoCapture(cam_idx)
    elif isinstance(source, str) and os.path.exists(source):
        print(f"[STAGE 1 CAMERA] Loading image file source: {source}...")
        frame_bgr = cv2.imread(source)
        if frame_bgr is None:
            print(f"[STAGE 1 CAMERA] ERROR: Unable to read image file: {source}")
            print("\n[DIAGNOSTIC] FIRST FAILING STAGE: CAMERA")
            return stage_status
    else:
        print(f"[STAGE 1 CAMERA] Opening video stream source: {source}...")
        cap = cv2.VideoCapture(source)

    if cap is not None:
        if not cap.isOpened():
            print(f"[STAGE 1 CAMERA] ERROR: Unable to connect to camera source: {source}")
            print("\n[DIAGNOSTIC] FIRST FAILING STAGE: CAMERA")
            return stage_status

        w = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        h = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        fps = cap.get(cv2.CAP_PROP_FPS) or 30.0

        print("CAMERA CONNECTED")
        print(f"RESOLUTION: {w}x{h}")
        print(f"FPS: {fps:.1f}")

        # Capture a real frame
        ret, frame_bgr = cap.read()
        cap.release()

        if not ret or frame_bgr is None:
            print("[STAGE 1 CAMERA] ERROR: Failed to acquire frame from camera.")
            return stage_status

    if frame_bgr is not None:
        stage_status["CAMERA"] = "PASS"
        h, w = frame_bgr.shape[:2]
        latest_cam_path = "debug/camera/latest.jpg"
        cv2.imwrite(latest_cam_path, frame_bgr)
        print(f"[STAGE 1 CAMERA] PASS | Frame resolution: {w}x{h} | Saved: {latest_cam_path}")

    # ==================================================
    # STAGE 2 — LICENSE PLATE DETECTION
    # ==================================================
    print("\n==================================================")
    print("STAGE 2 — LICENSE PLATE DETECTION DIAGNOSTICS")
    print("==================================================")

    detector = PlateDetector()
    candidates = detector.detect_candidates(frame_bgr)

    best_conf = candidates[0][2] if candidates else 0.0
    num_detections = len(candidates)

    print("\nMODEL DIAGNOSTICS:")
    print("Model: Murd0ck/LicensePlateDetector_YOLOv8n (license_plate_yolov8n.pt)")
    print("Inference image size: 640")
    print(f"Confidence threshold: {detector.conf_threshold}")
    print(f"Number of detections: {num_detections}")
    print(f"Best detection confidence: {best_conf:.2f}")

    annotated_frame = frame_bgr.copy()

    if not candidates:
        print("[STAGE 2 PLATE DETECTOR] PLATE DETECTOR: FAILED")
        latest_det_path = "debug/detections/latest.jpg"
        cv2.putText(annotated_frame, "PLATE DETECTOR: FAILED", (30, 50), cv2.FONT_HERSHEY_SIMPLEX, 1.0, (0, 0, 255), 2)
        cv2.imwrite(latest_det_path, annotated_frame)
        print("\n[DIAGNOSTIC] FIRST FAILING STAGE: PLATE DETECTOR")
        return stage_status

    stage_status["PLATE_DETECTOR"] = "PASS"
    best_candidate = candidates[0]
    crop_roi, bbox, det_conf = best_candidate
    x, y, bw, bh = bbox

    cv2.rectangle(annotated_frame, (x, y), (x + bw, y + bh), (0, 255, 0), 3)
    cv2.putText(annotated_frame, f"PLATE ({det_conf:.2f})", (x, max(30, y - 10)), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2)
    latest_det_path = "debug/detections/latest.jpg"
    cv2.imwrite(latest_det_path, annotated_frame)
    print(f"[STAGE 2 PLATE DETECTOR] PASS | Bounding Box: [{x}, {y}, {bw}, {bh}] | Conf: {det_conf:.2f} | Saved: {latest_det_path}")

    # ==================================================
    # STAGE 3 — PLATE CROP
    # ==================================================
    print("\n==================================================")
    print("STAGE 3 — PLATE CROP DIAGNOSTICS")
    print("==================================================")

    if crop_roi is None or crop_roi.size == 0:
        print("[STAGE 3 PLATE CROP] ERROR: Empty crop ROI.")
        return stage_status

    stage_status["PLATE_CROP"] = "PASS"
    crop_h, crop_w = crop_roi.shape[:2]
    latest_plate_path = "debug/plates/latest.jpg"
    cv2.imwrite(latest_plate_path, crop_roi)
    print(f"[STAGE 3 PLATE CROP] PASS | Crop Size: {crop_w}x{crop_h} | Saved: {latest_plate_path}")

    # Prepare debug output directories
    os.makedirs("debug/camera", exist_ok=True)
    os.makedirs("debug/detections", exist_ok=True)
    os.makedirs("debug/plates", exist_ok=True)
    os.makedirs("debug/ocr", exist_ok=True)

    # ==================================================
    # STAGE 4 — OCR DIAGNOSTICS (6 Variations)
    # ==================================================
    print("\n==================================================")
    print("STAGE 4 — OCR DIAGNOSTICS")
    print("==================================================")

    ocr_engine = OcrEngine()
    best_raw_ocr, best_ocr_conf, winning_img, cands = ocr_engine.read_text_multi_variant(crop_roi)

    latest_prep_path = "debug/ocr/preprocessed_latest.jpg"
    cv2.imwrite(latest_prep_path, winning_img)

    print("Evaluating OCR Candidates across Preprocessing Variations:")
    for c in cands:
        print(f"  - {c['variantLabel']}: Raw=\"{c['rawText']}\" | Confidence={c['confidence']:.2f} | Valid: {c['validStructure']}")

    if not best_raw_ocr or not best_raw_ocr.strip():
        print("\n[STAGE 4 OCR] OCR: FAILED")
        print("\n[DIAGNOSTIC] FIRST FAILING STAGE: OCR")
        return stage_status

    stage_status["OCR"] = "PASS"
    print(f"\nRAW OCR: {best_raw_ocr}")
    print(f"confidence={best_ocr_conf:.2f}")
    print(f"Saved winning preprocessed crop: {latest_prep_path}")

    # ==================================================
    # STAGE 5 — NORMALIZATION
    # ==================================================
    print("\n==================================================")
    print("STAGE 5 — NORMALIZATION DIAGNOSTICS")
    print("==================================================")

    is_valid, reason, normalized_plate = PlateNormalizer.validate_registration_structure(best_raw_ocr)

    print(f"RAW: {best_raw_ocr}")
    print(f"NORMALIZED: {normalized_plate}")

    if not is_valid:
        print(f"NORMALIZATION REJECTED: {reason}")
        print("\n[DIAGNOSTIC] FIRST FAILING STAGE: NORMALIZATION")
        return stage_status

    stage_status["NORMALIZATION"] = "PASS"
    print(f"[STAGE 5 NORMALIZATION] PASS | Confirmed Plate Structure: {normalized_plate}")

    # ==================================================
    # STAGE 6 & 7 — SPRING BOOT ANPR API & COMPLIANCE
    # ==================================================
    print("\n==================================================")
    print("STAGE 6 & 7 — SPRING BOOT BACKEND COMPLIANCE ENGINE")
    print("==================================================")

    url = f"{backend_url.rstrip('/')}/api/v1/anpr/detections"
    
    test_scenarios = [
        ("Scenario 1: Compliant Test Vehicle", normalized_plate),
        ("Scenario 2: Invalid Registration", "GJ01AB9999"),
        ("Scenario 3: Expired Hydro-Test", "GJ01EXPHYDRO"),
        ("Scenario 4: Unregistered / Unknown", "GJK5123")
    ]

    print(f"Testing Spring Boot ANPR Ingestion Endpoint ({url}) across 4 Compliance Scenarios:\n")

    for title, plate_num in test_scenarios:
        payload = {
            "stationId": station_id,
            "cameraId": camera_id,
            "registrationNumber": plate_num,
            "detectedAt": time.strftime("%Y-%m-%dT%H:%M:%S")
        }

        try:
            resp = requests.post(url, json=payload, timeout=5.0)
            if resp.status_code in (200, 201):
                stage_status["SPRING_BOOT_ANPR_API"] = "PASS"
                resp_data = resp.json()
                data = resp_data.get("data") if isinstance(resp_data.get("data"), dict) else resp_data
                comp_status = data.get("complianceStatus")
                j_id = data.get("journeyId")
                msg = resp_data.get("message") or data.get("message") or ""

                if comp_status:
                    stage_status["COMPLIANCE_ENGINE"] = "PASS"

                print(f"[{title}]")
                print(f"  - Plate: {plate_num} | HTTP {resp.status_code}")
                print(f"  - Compliance Status: {comp_status} | Journey ID: #{j_id}")
                print(f"  - Backend Message: \"{msg}\"")
                print(f"  - Verification Source: MOCK (MockVehicleRegistrationClient & MockHydroTestVerificationClient)\n")
            else:
                print(f"[{title}] FAILED: HTTP {resp.status_code} - {resp.text}\n")
        except Exception as e:
            print(f"[{title}] ERROR connecting to backend: {e}\n")

    # ==================================================
    # FINAL DIAGNOSTIC REPORT
    # ==================================================
    print("\n==================================================")
    print("  FINAL DIAGNOSTIC REPORT")
    print("==================================================")
    for stage, res in stage_status.items():
        print(f"{stage.ljust(22)}: {res}")

    first_fail = None
    for stage, res in stage_status.items():
        if res == "FAIL":
            first_fail = stage
            break

    print("==================================================")
    print(f"FIRST FAILING STAGE: {first_fail if first_fail else 'NONE (ALL STAGES PASSED)'}")
    print("==================================================\n")

    return stage_status

if __name__ == "__main__":
    src = sys.argv[1] if len(sys.argv) > 1 else 0
    run_diagnostics(source=src)
