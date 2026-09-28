import os
import time
import requests
import logging
import cv2
from collections import Counter
from datetime import datetime
from app.detector.plate_detector import PlateDetector
from app.ocr.ocr_engine import OcrEngine
from app.pipeline.normalizer import PlateNormalizer
from app.utils.image_utils import load_image, preprocess_roi, annotate_debug_image

logger = logging.getLogger(__name__)

class AnprPipeline:
    """
    Production-Oriented ANPR Pipeline handling:
    1. Pretrained YOLOv8 license plate detection (license_plate_yolov8n.pt)
    2. ROI preprocessing & OCR text extraction
    3. Indian license plate normalization & false-positive structural validation
    4. Temporal stability across video & RTSP frames
    5. Cooldown duplicate suppression
    6. RTSP & Webcam live interactive debugging
    7. Detailed 6-stage logging ([CAMERA], [PLATE DETECTOR], [PLATE CROP], [OCR], [NORMALIZATION], [FINAL PLATE])
    8. Automatic HTTP dispatch to Spring Boot backend ANPR ingestion API
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

        self.min_confidence = min_confidence if min_confidence is not None else float(os.getenv("MIN_CONFIDENCE", "0.85"))
        self.min_frame_candidate_confidence = float(os.getenv("MIN_FRAME_CANDIDATE_CONFIDENCE", "0.55"))
        self.min_stable_frames = min_stable_frames if min_stable_frames is not None else int(os.getenv("MIN_STABLE_FRAMES", "3"))
        self.stability_window_seconds = stability_window_seconds if stability_window_seconds is not None else float(os.getenv("STABILITY_WINDOW_SECONDS", "2.0"))
        self.cooldown_seconds = cooldown_seconds if cooldown_seconds is not None else float(os.getenv("COOLDOWN_SECONDS", "30.0"))
        self.frame_skip = frame_skip if frame_skip is not None else int(os.getenv("FRAME_SKIP", "5"))
        
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

        self.detector = PlateDetector()
        self.ocr_engine = OcrEngine(use_gpu=use_gpu)

        self.last_dispatched = {}
        self.temporal_buffer = []

    def process_image(self, image_input, generate_debug=False, station_id=None, camera_id=None, frame_id=None, dispatch_to_backend=None):
        """
        Processes a single image frame (filepath, byte array, or numpy array).
        Applies false positive structure validation, confidence thresholding,
        and logs all 6 pipeline stages explicitly.
        """
        st_id = station_id if station_id is not None else self.station_id
        cam_id = camera_id if camera_id is not None else self.camera_id
        fid = frame_id if frame_id is not None else int(time.time() * 1000) % 100000
        should_dispatch = self.send_to_backend if dispatch_to_backend is None else dispatch_to_backend

        try:
            image_bgr = load_image(image_input)
        except Exception as e:
            print(f"[CAMERA] ERROR: Image load failure - {str(e)}")
            return {
                "plateDetected": False,
                "registrationNumber": None,
                "confidence": 0.0,
                "detectorConfidence": 0.0,
                "ocrConfidence": 0.0,
                "timestamp": datetime.now().isoformat(),
                "reason": f"IMAGE_LOAD_ERROR: {str(e)}"
            }, None

        h_img, w_img = image_bgr.shape[:2]
        print(f"[CAMERA] FRAME {fid} | RESOLUTION: {w_img}x{h_img}")

        # Stage 2: Plate Detection
        candidates = self.detector.detect_candidates(image_bgr)

        if not candidates:
            print(f"[PLATE DETECTOR] FRAME {fid} | PLATE NOT DETECTED")
            now_iso = datetime.now().isoformat()
            return {
                "plateDetected": False,
                "registrationNumber": None,
                "confidence": 0.0,
                "detectorConfidence": 0.0,
                "ocrConfidence": 0.0,
                "timestamp": now_iso,
                "reason": "NO_PLATE_DETECTED"
            }, image_bgr

        best_candidate = None
        best_overall_conf = 0.0
        best_detector_conf = 0.0
        best_ocr_conf = 0.0
        best_bbox = None
        best_raw_ocr = None
        rejection_reason = "NO_PLATE_DETECTED"

        for idx, (roi, bbox, det_conf) in enumerate(candidates):
            x, y, w, h = bbox
            if best_bbox is None:
                best_bbox = bbox
                best_detector_conf = det_conf

            print(f"[PLATE DETECTOR] FRAME {fid} | PLATE DETECTED | DETECTOR CONFIDENCE: {det_conf:.2f} | PLATE BOUNDING BOX: [{x}, {y}, {w}, {h}]")

            # Stage 3: Plate Crop & Preprocessing
            roi_h, roi_w = roi.shape[:2]
            crop_filename = f"crop_{fid}_{idx}.jpg"
            crop_path = os.path.join(self.crops_debug_dir, crop_filename)
            cv2.imwrite(crop_path, roi)
            print(f"[PLATE CROP] PLATE CROP CREATED ({roi_w}x{roi_h}) saved to: {crop_path}")

            # Stage 4: Multi-Variant OCR Extraction
            raw_text, ocr_conf, winning_prep_img, ocr_cands = self.ocr_engine.read_text_multi_variant(roi)

            # Save preprocessed crop image artifact
            os.makedirs("debug/ocr", exist_ok=True)
            cv2.imwrite("debug/ocr/preprocessed_latest.jpg", winning_prep_img)

            if not raw_text or not raw_text.strip():
                print(f"[OCR] PLATE DETECTED | OCR FAILED")
            else:
                print(f"[OCR] OCR RAW RESULT: \"{raw_text}\" | OCR CONFIDENCE: {ocr_conf:.2f}")
                for cand in ocr_cands:
                    print(f"[OCR VARIANT] Variant: {cand['variantLabel']} | Raw: \"{cand['rawText']}\" | Conf: {cand['confidence']:.2f}")

            # Stage 5: Normalization & Structure Validation
            is_valid_struct, struct_reason, normalized = PlateNormalizer.validate_registration_structure(raw_text)
            print(f"[NORMALIZATION] NORMALIZED PLATE: \"{normalized}\" | VALID STRUCTURE: {is_valid_struct} ({struct_reason})")

            combined_conf = (det_conf * 0.4) + (ocr_conf * 0.6)

            if is_valid_struct:
                if combined_conf > best_overall_conf:
                    best_candidate = normalized
                    best_overall_conf = combined_conf
                    best_detector_conf = det_conf
                    best_ocr_conf = ocr_conf
                    best_bbox = bbox
                    best_raw_ocr = raw_text
            else:
                if best_raw_ocr is None:
                    best_raw_ocr = raw_text
                    best_ocr_conf = ocr_conf
                if rejection_reason == "NO_PLATE_DETECTED":
                    rejection_reason = struct_reason

        plate_detected = len(candidates) > 0

        # Stage 6: Final Plate Resolution & Automatic Backend Dispatch
        if plate_detected and best_candidate and best_overall_conf >= self.min_confidence:
            final_registration = best_candidate
            final_reason = None
            print(f"[FINAL PLATE] CONFIRMED REGISTRATION NUMBER: {final_registration} | COMBINED CONFIDENCE: {best_overall_conf:.2f}")

            # Temporal stabilization check
            now = time.time()
            self.temporal_buffer.append({"plate": final_registration, "confidence": best_overall_conf, "time": now})
            self.temporal_buffer = [item for item in self.temporal_buffer if (now - item['time']) <= self.stability_window_seconds]
            counts = Counter(item['plate'] for item in self.temporal_buffer)
            matching_count = counts.get(final_registration, 0)
            print(f"[STABILITY] {matching_count}/{self.min_stable_frames} matching observations for plate {final_registration}")

            if matching_count >= self.min_stable_frames:
                last_sent = self.last_dispatched.get(final_registration, 0.0)
                if (now - last_sent) < self.cooldown_seconds:
                    print(f"[COOLDOWN] Plate {final_registration} suppressed for cooldown window ({int(self.cooldown_seconds)}s)")
                else:
                    if should_dispatch:
                        self._dispatch_to_backend(final_registration, st_id, cam_id)
                        self.last_dispatched[final_registration] = now
        else:
            # A structurally valid, readable candidate below the single-frame
            # dispatch threshold is returned as provisional. The browser's
            # three-frame vote can confirm it; it is never dispatched to Spring
            # from one uncertain frame.
            final_registration = best_candidate if best_candidate and best_overall_conf >= self.min_frame_candidate_confidence else None
            if plate_detected:
                final_reason = "LOW_CONFIDENCE" if (best_candidate and best_overall_conf < self.min_confidence) else (rejection_reason if best_raw_ocr else "OCR_FAILED")
                if not best_raw_ocr:
                    print(f"[FINAL PLATE] REJECTED: PLATE DETECTED | OCR FAILED")
                else:
                    print(f"[FINAL PLATE] REJECTED: {final_reason}")
            else:
                final_reason = "NO_PLATE_DETECTED"
                print(f"[FINAL PLATE] REJECTED: NO_PLATE_DETECTED")

        now_iso = datetime.now().isoformat()
        result = {
            "plateDetected": plate_detected,
            "registrationNumber": final_registration,
            "rawOcr": best_raw_ocr,
            "confidence": round(best_overall_conf, 2),
            "detectorConfidence": round(best_detector_conf, 2),
            "ocrConfidence": round(best_ocr_conf, 2),
            "bbox": best_bbox,
            "timestamp": now_iso,
            "reason": final_reason
        }

        # Save annotated debug frame
        annotated_image = annotate_debug_image(
            image_bgr,
            best_bbox,
            final_registration or best_raw_ocr or "Invalid",
            best_overall_conf
        )

        if generate_debug:
            frame_filename = f"frame_{fid}.jpg"
            frame_path = os.path.join(self.frames_debug_dir, frame_filename)
            cv2.imwrite(frame_path, annotated_image)
            print(f"[DEBUG] Saved debug frame to: {frame_path}")

        return result, annotated_image

    def process_video_frames(self, frames, station_id=None, camera_id=None):
        """
        Processes a sequence/list of video frames (numpy arrays) sequentially,
        applying frame processing and temporal stabilization.
        """
        st_id = station_id if station_id is not None else self.station_id
        cam_id = camera_id if camera_id is not None else self.camera_id
        last_result = {"plateDetected": False, "registrationNumber": None, "confidence": 0.0}
        for idx, frame in enumerate(frames):
            res, _ = self.process_image(frame, generate_debug=False, station_id=st_id, camera_id=cam_id, frame_id=idx + 1)
            last_result = res
        return last_result

    def run_interactive_webcam(self, camera_index=0, station_id=None, camera_id=None):
        """
        Launches an interactive live OpenCV GUI window showing webcam feed,
        detected plate bounding box, recognized text, detector & OCR confidence, and processing FPS.
        Quit by pressing 'q' or ESC. Save snapshot by pressing 's'.
        """
        st_id = station_id if station_id is not None else self.station_id
        cam_id = camera_id if camera_id is not None else self.camera_id

        print(f"\n========================================================")
        print(f"  SGL CNG ANPR - LIVE WEBCAM DEBUGGER (Camera {camera_index})")
        print(f"========================================================\n")
        print(f"[CAMERA] Opening camera device index: {camera_index}...")

        cap = cv2.VideoCapture(camera_index)
        if not cap.isOpened():
            print(f"[CAMERA] ERROR: Unable to open camera device {camera_index}")
            return {"error": f"Failed to open camera index {camera_index}"}

        w_img = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        h_img = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        fps_init = cap.get(cv2.CAP_PROP_FPS) or 30.0
        print(f"CAMERA CONNECTED")
        print(f"RESOLUTION: {w_img}x{h_img}")
        print(f"FPS: {fps_init:.1f}")
        print(f"Press 'q' or ESC in the window to quit. Press 's' to save snapshot.\n")

        window_name = "SGL CNG ANPR - Real-Time Camera Verification"
        cv2.namedWindow(window_name, cv2.WINDOW_NORMAL)

        prev_time = time.time()
        frame_counter = 0
        current_fps = 0.0

        latest_reg = None
        latest_det_conf = 0.0
        latest_ocr_conf = 0.0
        latest_raw_ocr = None
        latest_status = "INITIALIZING"
        latest_bbox = None

        try:
            while cap.isOpened():
                ret, frame = cap.read()
                if not ret or frame is None:
                    print(f"[CAMERA] ERROR: Failed to read frame from camera {camera_index}")
                    time.sleep(0.1)
                    continue

                frame_counter += 1
                curr_time = time.time()
                time_diff = curr_time - prev_time
                if time_diff >= 0.5:
                    current_fps = frame_counter / time_diff
                    frame_counter = 0
                    prev_time = curr_time

                # Process every 3rd frame for smooth FPS
                if frame_counter % 3 == 0:
                    res, annotated = self.process_image(
                        frame,
                        generate_debug=False,
                        station_id=st_id,
                        camera_id=cam_id,
                        frame_id=frame_counter
                    )

                    latest_bbox = res.get("bbox")
                    latest_det_conf = res.get("detectorConfidence", 0.0)
                    latest_ocr_conf = res.get("ocrConfidence", 0.0)
                    latest_raw_ocr = res.get("rawOcr")

                    if res.get("plateDetected") and res.get("registrationNumber"):
                        latest_reg = res["registrationNumber"]
                        latest_status = "PLATE CONFIRMED"
                    elif res.get("plateDetected"):
                        latest_reg = None
                        if latest_raw_ocr:
                            latest_status = f"PLATE DETECTED (OCR: {latest_raw_ocr})"
                        else:
                            latest_status = "PLATE DETECTED (OCR: FAILED)"
                    else:
                        latest_reg = None
                        latest_status = "PLATE: NOT DETECTED"

                display_frame = frame.copy()

                # Draw Bounding Box & Annotations on Display Frame
                if latest_bbox:
                    x, y, w, h = latest_bbox
                    box_color = (0, 255, 0) if latest_reg else (0, 215, 255)
                    cv2.rectangle(display_frame, (x, y), (x + w, y + h), box_color, 3)

                    plate_label = f"PLATE: {latest_reg or latest_raw_ocr or 'DETECTED'}"
                    cv2.putText(display_frame, plate_label, (x, max(30, y - 10)),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.7, box_color, 2)

                # Top HUD Banner Overlay
                hud_bg = display_frame.copy()
                cv2.rectangle(hud_bg, (0, 0), (display_frame.shape[1], 120), (15, 23, 42), -1)
                display_frame = cv2.addWeighted(hud_bg, 0.75, display_frame, 0.25, 0)

                # Render HUD Text Metrics
                cv2.putText(display_frame, f"CAMERA: CONNECTED ({w_img}x{h_img} @ {current_fps:.1f} FPS)",
                            (15, 25), cv2.FONT_HERSHEY_SIMPLEX, 0.55, (255, 255, 255), 1)

                if latest_bbox:
                    cv2.putText(display_frame, f"PLATE: DETECTED | CONF: {latest_det_conf:.2f}",
                                (15, 50), cv2.FONT_HERSHEY_SIMPLEX, 0.55, (52, 211, 153), 2)
                    ocr_disp = f"OCR: {latest_reg or latest_raw_ocr or 'FAILED'} (Conf: {latest_ocr_conf:.2f})"
                    cv2.putText(display_frame, ocr_disp,
                                (15, 75), cv2.FONT_HERSHEY_SIMPLEX, 0.55, (255, 255, 255), 1)
                else:
                    cv2.putText(display_frame, "PLATE: NOT DETECTED",
                                (15, 50), cv2.FONT_HERSHEY_SIMPLEX, 0.55, (148, 163, 184), 1)

                cv2.putText(display_frame, f"STATUS: {latest_status}",
                            (15, 100), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (52, 211, 153) if latest_reg else (248, 113, 113), 2)

                try:
                    cv2.imshow(window_name, display_frame)
                    key = cv2.waitKey(30) & 0xFF
                    if key == 27 or key == ord('q'):
                        print("[CAMERA] User exited live camera mode.")
                        break
                    elif key == ord('s'):
                        snap_path = f"debug_output/snapshot_{int(time.time())}.jpg"
                        cv2.imwrite(snap_path, display_frame)
                        print(f"[CAMERA] Saved manual snapshot to: {snap_path}")
                except Exception as gui_err:
                    # Headless CLI fallback if GUI window creation fails
                    time.sleep(0.05)

        finally:
            cap.release()
            try:
                cv2.destroyAllWindows()
            except Exception:
                pass
            print("[CAMERA] Camera closed.\n")

        return {"status": "COMPLETED", "lastStatus": latest_status}

    def process_stream(self, source, frame_skip=None, station_id=None, camera_id=None, max_frames=None, reconnect_retries=5, reconnect_delay=2.0):
        """
        Stream processing engine supporting RTSP streams, video files, and webcams with:
        - OpenCV VideoCapture
        - Frame skipping (FRAME_SKIP)
        - Temporal stability tracking across frames
        - Bounded RTSP reconnection on stream drop
        - Cooldown duplicate dispatch suppression
        - Spring Boot HTTP response logging
        """
        skip = frame_skip if frame_skip is not None else self.frame_skip
        st_id = station_id if station_id is not None else self.station_id
        cam_id = camera_id if camera_id is not None else self.camera_id

        is_rtsp = isinstance(source, str) and source.startswith("rtsp://")
        source_label = source if isinstance(source, str) else f"Webcam({source})"

        print(f"[RTSP] Connecting to stream source: {source_label}")
        logger.info(f"Opening VideoCapture for stream: {source_label}")

        cap = cv2.VideoCapture(source)
        reconnect_attempts = 0

        if not cap.isOpened():
            print(f"[RTSP] Connection failed to {source_label}")
            logger.warning(f"Failed to open VideoCapture source: {source_label}")
            if is_rtsp:
                while reconnect_attempts < reconnect_retries and not cap.isOpened():
                    reconnect_attempts += 1
                    print(f"[RTSP] Reconnecting to {source_label} in {reconnect_delay}s... (Attempt {reconnect_attempts}/{reconnect_retries})")
                    time.sleep(reconnect_delay)
                    cap = cv2.VideoCapture(source)

        if not cap.isOpened():
            print(f"[RTSP] Unable to establish connection to {source_label}")
            return {"error": f"Failed to connect to stream {source_label}"}

        print(f"[RTSP] Connected: {source_label}")
        
        frame_counter = 0
        processed_counter = 0
        confirmed_detections = []

        try:
            while cap.isOpened():
                if max_frames is not None and frame_counter >= max_frames:
                    print(f"[RTSP] Reached max frame limit ({max_frames}). Stopping stream.")
                    break

                ret, frame = cap.read()
                if not ret:
                    if is_rtsp and reconnect_attempts < reconnect_retries:
                        reconnect_attempts += 1
                        print(f"[RTSP] Connection lost. Reconnecting to {source_label} in {reconnect_delay}s... (Attempt {reconnect_attempts}/{reconnect_retries})")
                        logger.warning(f"RTSP stream dropped for {source_label}. Retrying...")
                        time.sleep(reconnect_delay)
                        cap.release()
                        cap = cv2.VideoCapture(source)
                        if cap.isOpened():
                            print(f"[RTSP] Reconnected successfully: {source_label}")
                            reconnect_attempts = 0
                        continue
                    else:
                        print(f"[RTSP] End of stream or unrecoverable disconnect for: {source_label}")
                        break

                frame_counter += 1

                if frame_counter % skip != 0:
                    continue

                processed_counter += 1

                res, _ = self.process_image(frame, generate_debug=False, station_id=st_id, camera_id=cam_id, frame_id=frame_counter)

                if res.get("plateDetected") and res.get("registrationNumber"):
                    plate = res["registrationNumber"]
                    conf = res["confidence"]
                    confirmed_detections.append({
                        "plate": plate,
                        "confidence": conf
                    })
                else:
                    if res.get("plateDetected"):
                        print(f"[PLATE] Rejected: {res.get('reason')}")

        finally:
            cap.release()
            print(f"[RTSP] Stream closed: {source_label}")

        return {
            "source": source_label,
            "totalFramesRead": frame_counter,
            "framesProcessed": processed_counter,
            "confirmedDetections": confirmed_detections
        }

    def _dispatch_to_backend(self, registration_number, station_id, camera_id):
        """
        Dispatches detection payload to Spring Boot backend POST /api/v1/anpr/detections
        with cooldown suppression & response logging.
        """
        now = time.time()
        last_sent = self.last_dispatched.get(registration_number, 0.0)

        if (now - last_sent) < self.cooldown_seconds:
            print(f"[COOLDOWN] {registration_number} ignored for {int(self.cooldown_seconds)} seconds")
            logger.info(f"Cooldown active ({self.cooldown_seconds}s) for plate {registration_number}. Suppressing duplicate backend dispatch.")
            return False, {"cooldown": True, "registrationNumber": registration_number}

        payload = {
            "stationId": station_id,
            "cameraId": camera_id,
            "registrationNumber": registration_number,
            "detectedAt": datetime.now().isoformat()
        }

        url = f"{self.backend_url}/api/v1/anpr/detections"
        print(f"[BACKEND] Sending detection {registration_number} (Station: {station_id}, Camera: {camera_id}) to {url}...")
        try:
            logger.info(f"Dispatching ANPR detection {registration_number} to Spring Boot backend: {url}")
            response = requests.post(url, json=payload, timeout=5.0)
            status_code = response.status_code
            print(f"[BACKEND] HTTP {status_code}")

            if status_code in (200, 201):
                self.last_dispatched[registration_number] = now
                resp_json = response.json()
                data = resp_json.get("data") if isinstance(resp_json.get("data"), dict) else resp_json

                journey_id = data.get("journeyId") or resp_json.get("journeyId")
                compliance = data.get("complianceStatus") or resp_json.get("complianceStatus")
                journey_status = data.get("journeyStatus") or resp_json.get("journeyStatus")
                msg = resp_json.get("message") or data.get("message")
                
                print(f"[BACKEND] Compliance={compliance} Journey={journey_id} JourneyStatus={journey_status}")
                if msg:
                    print(f"[BACKEND] Message: {msg}")

                logger.info(f"Successfully posted detection to Spring Boot: {response.text}")
                return True, {
                    "statusCode": status_code,
                    "journeyId": journey_id,
                    "complianceStatus": compliance,
                    "journeyStatus": journey_status,
                    "message": msg
                }
            else:
                print(f"[BACKEND] Backend returned status code {status_code}: {response.text}")
                logger.warning(f"Backend returned status code {status_code}: {response.text}")
                return False, {"statusCode": status_code, "error": response.text}
        except Exception as e:
            print(f"[BACKEND] Network error connecting to Spring Boot ({url}): {e}")
            logger.error(f"Failed to post ANPR detection to Spring Boot backend ({url}): {e}")
            return False, {"error": str(e)}
