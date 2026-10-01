import time
import math
import cv2
import numpy as np
import re
from typing import Dict, Any, List, Optional, Tuple
from collections import defaultdict, Counter
from anpr_service import ANPRService

class FrameQualityFilter:
    """
    Quality gate for plate crops.
    Rejects blurry, overexposed, underexposed, or undersized crops before wasting OCR resources.
    """
    def __init__(self,
                 min_width: int = 40,
                 min_height: int = 15,
                 min_confidence: float = 0.25,
                 min_sharpness: float = 20.0,
                 min_brightness: float = 20.0,
                 max_brightness: float = 245.0):
        self.min_width = min_width
        self.min_height = min_height
        self.min_confidence = min_confidence
        self.min_sharpness = min_sharpness
        self.min_brightness = min_brightness
        self.max_brightness = max_brightness

    def evaluate(self, crop: np.ndarray, bbox: Tuple[int, int, int, int], det_conf: float) -> Tuple[bool, str, Dict[str, float]]:
        h, w = crop.shape[:2]
        
        # 1. Dimension check
        if w < self.min_width or h < self.min_height:
            return False, f"Crop too small ({w}x{h} < {self.min_width}x{self.min_height})", {"width": w, "height": h}
            
        # 2. Detection confidence check
        if det_conf < self.min_confidence:
            return False, f"Low detection confidence ({det_conf:.2f} < {self.min_confidence:.2f})", {"det_conf": det_conf}
            
        gray = cv2.cvtColor(crop, cv2.COLOR_BGR2GRAY) if len(crop.shape) == 3 else crop
        
        # 3. Brightness check
        mean_brightness = float(np.mean(gray))
        if mean_brightness < self.min_brightness:
            return False, f"Too dark ({mean_brightness:.1f} < {self.min_brightness})", {"brightness": mean_brightness}
        if mean_brightness > self.max_brightness:
            return False, f"Too bright/glare ({mean_brightness:.1f} > {self.max_brightness})", {"brightness": mean_brightness}
            
        # 4. Blur / Sharpness check (Laplacian variance)
        sharpness = float(cv2.Laplacian(gray, cv2.CV_64F).var())
        if sharpness < self.min_sharpness:
            return False, f"Too blurry (sharpness {sharpness:.1f} < {self.min_sharpness})", {"sharpness": sharpness}
            
        return True, "Passed quality checks", {
            "width": w, "height": h, "det_conf": det_conf,
            "brightness": mean_brightness, "sharpness": sharpness
        }


class PlateTrack:
    """Represents a continuous track of a single vehicle plate."""
    def __init__(self, track_id: int, bbox: Tuple[int, int, int, int], timestamp: float):
        self.track_id = track_id
        self.bbox = bbox
        self.last_seen = timestamp
        self.first_seen = timestamp
        self.status = "DETECTED"  # DETECTED, READING, CONFIRMED, LOST
        
        # Observation buffer: stores last 10 OCR readings
        # Each item: {"text": str, "conf": float, "time": float, "sharpness": float}
        self.observations: List[Dict[str, Any]] = []
        
        self.confirmed_plate: Optional[str] = None
        self.confirmed_confidence: float = 0.0
        self.consensus_score: float = 0.0
        self.emitted: bool = False
        self.last_emitted_time: float = 0.0

    def add_observation(self, text: str, conf: float, timestamp: float, sharpness: float = 0.0, max_buffer: int = 10):
        if not text:
            return
        self.observations.append({
            "text": text,
            "conf": conf,
            "time": timestamp,
            "sharpness": sharpness
        })
        if len(self.observations) > max_buffer:
            self.observations.pop(0)
            
        if self.status == "DETECTED":
            self.status = "READING"

    def update_position(self, bbox: Tuple[int, int, int, int], timestamp: float):
        self.bbox = bbox
        self.last_seen = timestamp
        if self.status == "LOST":
            self.status = "READING" if self.observations else "DETECTED"


class PlateTracker:
    """Associates detections across consecutive frames via IoU."""
    def __init__(self, iou_threshold: float = 0.25, track_timeout: float = 2.0, cleanup_timeout: float = 5.0):
        self.iou_threshold = iou_threshold
        self.track_timeout = track_timeout
        self.cleanup_timeout = cleanup_timeout
        self.next_track_id = 1
        self.tracks: Dict[int, PlateTrack] = {}

    @staticmethod
    def compute_iou(box1: Tuple[int, int, int, int], box2: Tuple[int, int, int, int]) -> float:
        x1_a, y1_a, x2_a, y2_a = box1
        x1_b, y1_b, x2_b, y2_b = box2
        
        xA = max(x1_a, x1_b)
        yA = max(y1_a, y1_b)
        xB = min(x2_a, x2_b)
        yB = min(y2_a, y2_b)
        
        inter_area = max(0, xB - xA) * max(0, yB - yA)
        areaA = (x2_a - x1_a) * (y2_a - y1_a)
        areaB = (x2_b - x1_b) * (y2_b - y1_b)
        union_area = float(areaA + areaB - inter_area)
        
        if union_area <= 0:
            return 0.0
        return inter_area / union_area

    def update(self, detected_bboxes: List[Tuple[int, int, int, int]], timestamp: float) -> List[Tuple[PlateTrack, Optional[Tuple[int, int, int, int]]]]:
        matched_tracks = []
        unmatched_bboxes = list(detected_bboxes)
        
        # Match against active tracks
        active_tracks = [t for t in self.tracks.values() if t.status != "LOST"]
        for track in active_tracks:
            best_iou = 0.0
            best_bbox = None
            
            for bbox in unmatched_bboxes:
                iou = self.compute_iou(track.bbox, bbox)
                if iou > best_iou:
                    best_iou = iou
                    best_bbox = bbox
                    
            if best_iou >= self.iou_threshold and best_bbox is not None:
                track.update_position(best_bbox, timestamp)
                unmatched_bboxes.remove(best_bbox)
                matched_tracks.append((track, best_bbox))
            else:
                # Track was not matched in this frame
                if timestamp - track.last_seen > self.track_timeout:
                    track.status = "LOST"
                matched_tracks.append((track, None))
                
        # Create new tracks for remaining unmatched bboxes
        for bbox in unmatched_bboxes:
            new_track = PlateTrack(self.next_track_id, bbox, timestamp)
            self.tracks[self.next_track_id] = new_track
            self.next_track_id += 1
            matched_tracks.append((new_track, bbox))
            
        # Clean up stale tracks
        expired_ids = [
            tid for tid, t in self.tracks.items()
            if (timestamp - t.last_seen) > self.cleanup_timeout and t.status == "LOST"
        ]
        for tid in expired_ids:
            del self.tracks[tid]
            
        return matched_tracks


class TemporalConsensusEngine:
    """
    Computes multi-frame weighted consensus across buffered OCR observations.
    Implements:
    - String-level weighted voting
    - Character-level position-aware consensus
    - Indian plate structure enforcement
    - Confirmation thresholds
    """
    def __init__(self,
                 min_observations: int = 3,
                 min_consensus_score: float = 0.80,
                 min_ocr_confidence: float = 0.55):
        self.min_observations = min_observations
        self.min_consensus_score = min_consensus_score
        self.min_ocr_confidence = min_ocr_confidence

    def compute_consensus(self, track: PlateTrack) -> Dict[str, Any]:
        obs = track.observations
        if len(obs) < self.min_observations:
            return {
                "ready": False,
                "reason": f"Insufficient observations ({len(obs)}/{self.min_observations})",
                "plateText": obs[-1]["text"] if obs else "",
                "confidence": obs[-1]["conf"] if obs else 0.0,
                "consensusScore": 0.0
            }
            
        # 1. Whole-string weighted voting
        string_weights = defaultdict(float)
        total_weight = 0.0
        for o in obs:
            weight = max(0.1, o["conf"])
            string_weights[o["text"]] += weight
            total_weight += weight
            
        best_string, best_weight = max(string_weights.items(), key=lambda x: x[1])
        string_consensus_score = best_weight / total_weight if total_weight > 0 else 0.0
        
        # 2. Character-level consensus across dominant length strings
        candidate_lengths = [len(o["text"]) for o in obs]
        dominant_len = Counter(candidate_lengths).most_common(1)[0][0]
        matching_obs = [o for o in obs if len(o["text"]) == dominant_len]
        
        char_consensus_chars = []
        char_agreement_sum = 0.0
        
        for pos in range(dominant_len):
            char_votes = defaultdict(float)
            pos_weight = 0.0
            for o in matching_obs:
                w = max(0.1, o["conf"])
                char_votes[o["text"][pos]] += w
                pos_weight += w
                
            winning_char, win_w = max(char_votes.items(), key=lambda x: x[1])
            char_consensus_chars.append(winning_char)
            char_agreement_sum += (win_w / pos_weight) if pos_weight > 0 else 0.0
            
        char_consensus_text = "".join(char_consensus_chars)
        char_consensus_score = char_agreement_sum / dominant_len if dominant_len > 0 else 0.0
        
        # Normalize through Indian plate disambiguator
        final_plate = ANPRService.validate_and_normalize_indian_plate(char_consensus_text)
        is_valid = ANPRService.is_valid_indian_format(final_plate)
        
        # Blended consensus score
        final_consensus_score = round(max(string_consensus_score, char_consensus_score), 4)
        avg_ocr_conf = round(float(np.mean([o["conf"] for o in obs])), 4)
        
        # Check confirmation threshold:
        # High consensus across multiple frames + valid Indian registration format confirms plate
        is_confirmed = (
            len(obs) >= self.min_observations and
            final_consensus_score >= self.min_consensus_score and
            (avg_ocr_conf >= self.min_ocr_confidence or is_valid)
        )
        
        return {
            "ready": is_confirmed,
            "plateText": final_plate,
            "confidence": avg_ocr_conf,
            "consensusScore": final_consensus_score,
            "isValidFormat": is_valid,
            "observationsCount": len(obs)
        }


class CameraStreamProcessor:
    """
    Stage 2 Main Controller:
    Coordinates frame decimation, plate tracking, quality filtering,
    re-using ANPRService for OCR, computing temporal consensus,
    and managing duplicate event suppression.
    """
    def __init__(self,
                 anpr_service: Optional[ANPRService] = None,
                 frame_skip: int = 3,
                 min_observations: int = 3,
                 min_consensus_score: float = 0.80,
                 min_ocr_confidence: float = 0.55,
                 cooldown_seconds: float = 30.0,
                 backend_url: Optional[str] = None,
                 station_id: int = 1,
                 camera_id: int = 1,
                 send_to_backend: bool = True,
                 debug_mode: bool = True):
        import os
        self.anpr_service = anpr_service or ANPRService.get_instance()
        self.frame_skip = frame_skip
        self.cooldown_seconds = cooldown_seconds
        self.debug_mode = debug_mode
        self.backend_url = backend_url or os.getenv("BACKEND_URL", "http://app:8080")
        self.station_id = station_id
        self.camera_id = camera_id
        self.send_to_backend = send_to_backend
        
        self.quality_filter = FrameQualityFilter()
        self.tracker = PlateTracker()
        self.consensus_engine = TemporalConsensusEngine(
            min_observations=min_observations,
            min_consensus_score=min_consensus_score,
            min_ocr_confidence=min_ocr_confidence
        )
        
        # Duplicate suppression: plate_text -> last_emitted_timestamp
        self.emitted_plates_cooldown: Dict[str, float] = {}
        
        # Telemetry & Performance
        self.total_frames = 0
        self.detection_frames = 0
        self.ocr_calls = 0
        self.total_ocr_latency_ms = 0.0
        self.start_time = time.time()
        self.last_frame_time = time.time()

    def process_frame(self, frame_bgr: np.ndarray, timestamp: Optional[float] = None) -> Dict[str, Any]:
        if timestamp is None:
            timestamp = time.time()
            
        frame_start = time.time()
        self.total_frames += 1
        frame_h, frame_w = frame_bgr.shape[:2]
        
        should_detect = (self.total_frames % self.frame_skip == 0) or (self.total_frames == 1)
        active_events = []
        tracks_summary = []
        
        if should_detect:
            self.detection_frames += 1
            
            # Step 1: Detect plates using unified ANPRService
            detections = self.anpr_service.detect_plates(frame_bgr)
            detected_bboxes = [d["bbox"] for d in detections]
            
            # Step 2: Track plates across frames
            matched_pairs = self.tracker.update(detected_bboxes, timestamp)
            
            # Map detections by bbox for quick lookup
            det_map = {d["bbox"]: d for d in detections}
            
            # Step 3: For each matched track, evaluate quality and run OCR if eligible
            for track, bbox in matched_pairs:
                if bbox is not None and bbox in det_map:
                    det = det_map[bbox]
                    crop = det["crop"]
                    det_conf = det["confidence"]
                    
                    # Quality gate
                    passed_quality, reason, metrics = self.quality_filter.evaluate(crop, bbox, det_conf)
                    
                    if passed_quality:
                        # Only run OCR if track is not yet confirmed, or periodically to verify
                        if track.status != "CONFIRMED" or len(track.observations) < 5:
                            ocr_start = time.time()
                            ocr_res = self.anpr_service.read_crop(crop)
                            ocr_latency = (time.time() - ocr_start) * 1000
                            
                            self.ocr_calls += 1
                            self.total_ocr_latency_ms += ocr_latency
                            
                            if ocr_res["normalized_text"]:
                                track.add_observation(
                                    text=ocr_res["normalized_text"],
                                    conf=ocr_res["ocr_confidence"],
                                    timestamp=timestamp,
                                    sharpness=metrics.get("sharpness", 0.0)
                                )
                                
                    # Step 4: Compute temporal consensus
                    consensus = self.consensus_engine.compute_consensus(track)
                    track.consensus_score = consensus["consensusScore"]
                    
                    if consensus["ready"] and track.status != "CONFIRMED":
                        track.status = "CONFIRMED"
                        track.confirmed_plate = consensus["plateText"]
                        track.confirmed_confidence = consensus["confidence"]
                        
                        # Step 5: Duplicate event suppression
                        last_sent = self.emitted_plates_cooldown.get(track.confirmed_plate, 0.0)
                        if (timestamp - last_sent) >= self.cooldown_seconds:
                            event = {
                                "trackId": track.track_id,
                                "plateText": track.confirmed_plate,
                                "confidence": round(track.confirmed_confidence, 4),
                                "consensusScore": round(track.consensus_score, 4),
                                "status": "CONFIRMED",
                                "observations": consensus["observationsCount"],
                                "timestamp": timestamp,
                                "bbox": {"x1": bbox[0], "y1": bbox[1], "x2": bbox[2], "y2": bbox[3]}
                            }
                            active_events.append(event)
                            self.emitted_plates_cooldown[track.confirmed_plate] = timestamp
                            track.emitted = True
                            track.last_emitted_time = timestamp
                            
                            if self.debug_mode:
                                print(f"[EVENT EMITTED] Track #{track.track_id} -> {track.confirmed_plate} "
                                      f"(conf: {track.confirmed_confidence:.2f}, consensus: {track.consensus_score:.2f})")
                                      
                            # Automatically dispatch confirmed event to Spring Boot backend / PostgreSQL
                            self._dispatch_to_backend(event)
                
                tracks_summary.append({
                    "trackId": track.track_id,
                    "bbox": track.bbox,
                    "status": track.status,
                    "plate": track.confirmed_plate or (track.observations[-1]["text"] if track.observations else ""),
                    "observations": len(track.observations),
                    "consensusScore": track.consensus_score
                })
        else:
            # On skipped frames, maintain current tracks
            for tid, track in self.tracker.tracks.items():
                tracks_summary.append({
                    "trackId": track.track_id,
                    "bbox": track.bbox,
                    "status": track.status,
                    "plate": track.confirmed_plate or (track.observations[-1]["text"] if track.observations else ""),
                    "observations": len(track.observations),
                    "consensusScore": track.consensus_score
                })
                
        now = time.time()
        elapsed_sec = max(0.001, now - self.start_time)
        fps = round(self.total_frames / elapsed_sec, 1)
        det_fps = round(self.detection_frames / elapsed_sec, 1)
        ocr_rate = round(self.ocr_calls / elapsed_sec, 2)
        avg_ocr_latency = round(self.total_ocr_latency_ms / max(1, self.ocr_calls), 2)
        e2e_latency = round((now - frame_start) * 1000, 2)
        
        return {
            "frameIndex": self.total_frames,
            "detectedFrame": should_detect,
            "resolution": f"{frame_w}x{frame_h}",
            "activeEvents": active_events,
            "tracks": tracks_summary,
            "telemetry": {
                "fps": fps,
                "detectionFps": det_fps,
                "ocrCallsPerSec": ocr_rate,
                "avgOcrLatencyMs": avg_ocr_latency,
                "endToEndLatencyMs": e2e_latency,
                "totalFrames": self.total_frames,
                "totalOcrCalls": self.ocr_calls
            }
        }

    def _dispatch_to_backend(self, event: Dict[str, Any]):
        """Dispatches confirmed ANPR detection event to Spring Boot backend."""
        if not self.send_to_backend or not self.backend_url:
            return
            
        import urllib.request
        import json
        
        target = f"{self.backend_url}/api/v1/anpr/detections"
        payload = {
            "stationId": self.station_id,
            "cameraId": self.camera_id,
            "registrationNumber": event["plateText"]
        }
        
        try:
            data = json.dumps(payload).encode("utf-8")
            req = urllib.request.Request(target, data=data, headers={"Content-Type": "application/json"})
            with urllib.request.urlopen(req, timeout=2.5) as resp:
                if self.debug_mode:
                    print(f"[BACKEND INGESTED] Track #{event['trackId']} -> {event['plateText']} (HTTP {resp.status})")
        except Exception as e:
            if self.debug_mode:
                print(f"[BACKEND DISPATCH FAILED] {e}")

