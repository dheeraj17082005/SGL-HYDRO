import os
import cv2
import numpy as np
import time
import json
from collections import defaultdict
from anpr_service import ANPRService
from temporal_consensus import CameraStreamProcessor

def apply_motion_blur(img: np.ndarray, size: int = 7) -> np.ndarray:
    kernel = np.zeros((size, size))
    kernel[int((size - 1) / 2), :] = np.ones(size)
    kernel = kernel / size
    return cv2.filter2D(img, -1, kernel)

def apply_perspective_angle(img: np.ndarray, angle_deg: float = 12.0) -> np.ndarray:
    h, w = img.shape[:2]
    rad = np.radians(angle_deg)
    offset = int(w * 0.08 * np.tan(rad))
    src = np.float32([[0, 0], [w, 0], [0, h], [w, h]])
    dst = np.float32([[offset, 0], [w - offset, 0], [0, h], [w, h]])
    matrix = cv2.getPerspectiveTransform(src, dst)
    return cv2.warpPerspective(img, matrix, (w, h), borderMode=cv2.BORDER_REPLICATE)

def run_stress_test():
    print("=" * 80)
    print("FORMAL ANPR STRESS TEST & PRODUCTION EVALUATION SUITE")
    print("=" * 80)
    
    service = ANPRService.get_instance(model_path="/app/best.pt" if os.path.exists("/app/best.pt") else "best.pt")
    
    # -------------------------------------------------------------------------
    # TEST DATASET DEFINITION
    # -------------------------------------------------------------------------
    dataset_dir = "/app/eval_dataset" if os.path.exists("/app/eval_dataset") else "eval_dataset"
    
    test_cases = [
        # 1. Ten+ Indian Vehicles & States (White and Yellow)
        {"id": "TC01_RJ_KIA", "file": "rj_kia.jpg", "ground_truth": "RJ14CV0002", "category": "White Plate (RJ)", "type": "single"},
        {"id": "TC02_HR_HSRP", "file": "hr_hsrp.jpg", "ground_truth": "HR98AA0000", "category": "White HSRP (HR)", "type": "single"},
        {"id": "TC03_GJ_CAR", "file": "gj_car.jpg", "ground_truth": "GJ01AB1234", "category": "White Plate (GJ)", "type": "single"},
        {"id": "TC04_DL_CAR", "file": "dl_car.jpg", "ground_truth": "DL2CCE9999", "category": "White Plate (DL)", "type": "single"},
        {"id": "TC05_KA_CAR", "file": "ka_car.jpg", "ground_truth": "KA05MN4321", "category": "White Plate (KA)", "type": "single"},
        {"id": "TC06_MH_CAR", "file": "mh_car.jpg", "ground_truth": "MH12DE5678", "category": "White Plate (MH)", "type": "single"},
        {"id": "TC07_TN_CAR", "file": "tn_car.jpg", "ground_truth": "TN09XY8765", "category": "White Plate (TN)", "type": "single"},
        {"id": "TC08_UP_AUTO", "file": "up_auto.jpg", "ground_truth": "UP32BZ1122", "category": "Commercial Yellow (UP / 2-Line)", "type": "single"},
        {"id": "TC09_RJ_AUTO", "file": "rj_auto.jpg", "ground_truth": "RJ45TC0987", "category": "Commercial Yellow (RJ / 2-Line)", "type": "single"},
        
        # 2. Negative Control (No Plate / False Positive Check)
        {"id": "TC10_NO_PLATE", "file": "no_plate.jpg", "ground_truth": None, "category": "Negative Control (No Plate)", "type": "single"},
        
        # 3. Distance & Scale Variations
        {"id": "TC11_DIST_FAR", "file": "rj_kia.jpg", "ground_truth": "RJ14CV0002", "category": "Distance: Far (0.75x Scale)", "type": "transform", "fn": lambda im: cv2.resize(im, (int(im.shape[1]*0.75), int(im.shape[0]*0.75)))},
        {"id": "TC12_DIST_NEAR", "file": "rj_kia.jpg", "ground_truth": "RJ14CV0002", "category": "Distance: Near (1.20x Scale)", "type": "transform", "fn": lambda im: cv2.resize(im, (int(im.shape[1]*1.20), int(im.shape[0]*1.20)))},
        
        # 4. Viewing Angle Variations
        {"id": "TC13_ANGLE_LEFT", "file": "rj_kia.jpg", "ground_truth": "RJ14CV0002", "category": "Viewing Angle: +12° Skew", "type": "transform", "fn": lambda im: apply_perspective_angle(im, 12.0)},
        {"id": "TC14_ANGLE_RIGHT", "file": "rj_kia.jpg", "ground_truth": "RJ14CV0002", "category": "Viewing Angle: -12° Skew", "type": "transform", "fn": lambda im: apply_perspective_angle(im, -12.0)},
        
        # 5. Lighting Conditions
        {"id": "TC15_LOW_LIGHT", "file": "rj_kia.jpg", "ground_truth": "RJ14CV0002", "category": "Lighting: Low-Light (Night 0.4x)", "type": "transform", "fn": lambda im: np.clip(im.astype(np.float32) * 0.40, 0, 255).astype(np.uint8)},
        {"id": "TC16_HIGH_EXPOSURE", "file": "rj_kia.jpg", "ground_truth": "RJ14CV0002", "category": "Lighting: Direct Sunlight (1.4x)", "type": "transform", "fn": lambda im: np.clip(im.astype(np.float32) * 1.35, 0, 255).astype(np.uint8)},
        
        # 6. Motion Blur
        {"id": "TC17_MOTION_BLUR", "file": "rj_kia.jpg", "ground_truth": "RJ14CV0002", "category": "Motion Blur (Kernel 7)", "type": "transform", "fn": lambda im: apply_motion_blur(im, 7)},
        
        # 7. Partial Occlusion
        {"id": "TC18_PARTIAL_OCCLUSION", "file": "rj_kia.jpg", "ground_truth": "RJ14CV0002", "category": "Partial Occlusion (15% Top Border)", "type": "transform", "fn": lambda im: cv2.rectangle(im.copy(), (0, 0), (im.shape[1], int(im.shape[0]*0.15)), (40, 40, 40), -1)}
    ]
    
    records = []
    
    total_samples = len(test_cases)
    det_expected_count = sum(1 for tc in test_cases if tc["ground_truth"] is not None)
    det_actual_count = 0
    ocr_correct_count = 0
    complete_correct_count = 0
    fp_count = 0
    total_latency_ms = 0.0
    
    failure_breakdown = defaultdict(int)
    
    print(f"\nExecuting {total_samples} test conditions...\n")
    
    for tc in test_cases:
        filepath = os.path.join(dataset_dir, tc["file"])
        if not os.path.exists(filepath):
            # Fallback path
            filepath = os.path.join("/app", tc["file"])
            
        img = cv2.imread(filepath)
        if img is None:
            print(f"[WARN] File not found: {filepath}. Skipping.")
            continue
            
        # Apply transformation if specified
        if tc.get("type") == "transform" and "fn" in tc:
            img = tc["fn"](img)
            
        gt = tc["ground_truth"]
        t_start = time.time()
        
        # Step 1: Detect plates
        dets = service.detect_plates(img)
        t_det = (time.time() - t_start) * 1000
        
        detected_plate = None
        det_conf = 0.0
        ocr_conf = 0.0
        is_pass = False
        failure_component = None
        
        if gt is None:
            # Negative control test: Must NOT detect any plate
            if not dets:
                is_pass = True
            else:
                is_pass = False
                fp_count += 1
                failure_component = "DETECTION (False Positive)"
        else:
            if not dets:
                failure_component = "DETECTION (Missed Plate)"
                failure_breakdown["DETECTION"] += 1
            else:
                det_actual_count += 1
                best_det = dets[0]
                det_conf = round(best_det["confidence"], 4)
                
                # Step 2: Read Crop
                ocr_start = time.time()
                ocr_res = service.read_crop(best_det["crop"])
                t_ocr = (time.time() - ocr_start) * 1000
                
                detected_plate = ocr_res["normalized_text"]
                ocr_conf = round(ocr_res["ocr_confidence"], 4)
                
                # Evaluation
                if detected_plate == gt:
                    ocr_correct_count += 1
                    complete_correct_count += 1
                    is_pass = True
                else:
                    # Diagnose failure component
                    if not detected_plate:
                        failure_component = "OCR (No characters extracted)"
                        failure_breakdown["OCR"] += 1
                    elif not ocr_res["is_valid"]:
                        failure_component = "VALIDATION (Malformed format)"
                        failure_breakdown["VALIDATION"] += 1
                    else:
                        failure_component = f"OCR (Expected {gt}, read {detected_plate})"
                        failure_breakdown["OCR"] += 1
                        
        latency = round((time.time() - t_start) * 1000, 2)
        total_latency_ms += latency
        
        record = {
            "id": tc["id"],
            "category": tc["category"],
            "groundTruth": gt or "NONE",
            "detectedPlate": detected_plate or "NONE",
            "detConfidence": det_conf,
            "ocrConfidence": ocr_conf,
            "latencyMs": latency,
            "status": "PASS" if is_pass else "FAIL",
            "failureComponent": failure_component
        }
        records.append(record)
        
        status_symbol = "✓ PASS" if is_pass else "✗ FAIL"
        print(f"[{record['id']}] {status_symbol} | GT: {record['groundTruth']} | Det: {record['detectedPlate']} | Latency: {latency}ms" +
              (f" | Cause: {failure_component}" if failure_component else ""))
              
    # -------------------------------------------------------------------------
    # STAGE 2 MULTI-FRAME TEMPORAL CONSENSUS STRESS TESTS
    # -------------------------------------------------------------------------
    print("\n--- Live Camera Multi-Frame Consensus Stress Tests ---")
    
    # Temporal Test A: 10-Frame Continuous Stream with Sensor Noise
    kia_img = cv2.imread(os.path.join(dataset_dir, "rj_kia.jpg"))
    if kia_img is None:
        kia_img = cv2.imread("/app/kia.jpg")
    proc = CameraStreamProcessor(anpr_service=service, frame_skip=1, min_observations=3, cooldown_seconds=15.0)
    
    consensus_events = []
    t_start_seq = time.time()
    for f in range(10):
        # Add slight natural jitter to coordinates / pixel intensities
        noise = np.random.randint(-3, 3, kia_img.shape, dtype=np.int16)
        noisy_frame = np.clip(kia_img.astype(np.int16) + noise, 0, 255).astype(np.uint8)
        
        t = 5000.0 + f * 0.1
        out = proc.process_frame(noisy_frame, timestamp=t)
        if out["activeEvents"]:
            consensus_events.extend(out["activeEvents"])
            
    temporal_pass = len(consensus_events) == 1 and consensus_events[0]["plateText"] == "RJ14CV0002"
    print(f"[TEMPORAL_STREAM] {'✓ PASS' if temporal_pass else '✗ FAIL'} | Events: {len(consensus_events)} | Consensus: {consensus_events[0]['plateText'] if consensus_events else 'None'}")
    
    # -------------------------------------------------------------------------
    # METRICS CALCULATION
    # -------------------------------------------------------------------------
    det_recall = round((det_actual_count / det_expected_count) * 100, 2) if det_expected_count > 0 else 0.0
    ocr_accuracy = round((ocr_correct_count / det_actual_count) * 100, 2) if det_actual_count > 0 else 0.0
    complete_accuracy = round((complete_correct_count / det_expected_count) * 100, 2) if det_expected_count > 0 else 0.0
    fp_rate = round((fp_count / 1) * 100, 2)
    avg_latency = round(total_latency_ms / max(1, total_samples), 2)
    
    report = {
        "summary": {
            "totalTestCases": total_samples,
            "detectionRecall": f"{det_recall}%",
            "ocrAccuracy": f"{ocr_accuracy}%",
            "completeANPRAccuracy": f"{complete_accuracy}%",
            "falsePositiveRate": f"{fp_rate}%",
            "averageLatencyMs": f"{avg_latency} ms"
        },
        "failureBreakdownByComponent": dict(failure_breakdown),
        "detailedRecords": records
    }
    
    print("\n" + "=" * 80)
    print("STRESS TEST EVALUATION REPORT")
    print("=" * 80)
    print(json.dumps(report["summary"], indent=2))
    print(f"\nFailure Breakdown: {json.dumps(report['failureBreakdownByComponent'], indent=2)}")
    
    with open("/app/anpr_stress_test_report.json", "w") as f:
        json.dump(report, f, indent=2)
    print(f"\nDetailed report written to /app/anpr_stress_test_report.json")
    return report

if __name__ == "__main__":
    run_stress_test()
