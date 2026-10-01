import os
import cv2
import numpy as np
import time
from fastapi import FastAPI, UploadFile, File, Form, Query
from fastapi.middleware.cors import CORSMiddleware
from typing import Optional, Dict, Any

from anpr_service import ANPRService
from temporal_consensus import CameraStreamProcessor

app = FastAPI(title="SGL CNG Station Operations - Unified ANPR Service")

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Global services (initialized on startup)
anpr_service: Optional[ANPRService] = None
stream_processor: Optional[CameraStreamProcessor] = None

@app.on_event("startup")
async def startup_event():
    global anpr_service, stream_processor
    print("[SERVER STARTUP] Loading Unified ANPR Service...")
    model_path = "/app/best.pt" if os.path.exists("/app/best.pt") else "best.pt"
    anpr_service = ANPRService.get_instance(model_path=model_path)
    
    # Initialize Camera Stream Processor with temporal consensus
    stream_processor = CameraStreamProcessor(
        anpr_service=anpr_service,
        frame_skip=int(os.getenv("FRAME_SKIP", "3")),
        min_observations=int(os.getenv("MIN_OBSERVATIONS", "3")),
        min_consensus_score=float(os.getenv("MIN_CONSENSUS_SCORE", "0.80")),
        min_ocr_confidence=float(os.getenv("MIN_OCR_CONFIDENCE", "0.55")),
        cooldown_seconds=float(os.getenv("COOLDOWN_SECONDS", "30.0")),
        debug_mode=True
    )
    print("[SERVER STARTUP] Unified ANPR Service & Temporal Consensus Processor ready.")

@app.get("/health")
def health():
    return {
        "status": "UP",
        "service": "Unified ANPR & Temporal Consensus",
        "detectorLoaded": anpr_service is not None
    }

# =========================================================================
# 1. STATIC IMAGE ENDPOINT (Verified in Stage 1)
# =========================================================================
@app.post("/api/v1/anpr/recognize")
async def recognize_static_image(file: UploadFile = File(...)):
    """
    Accepts full vehicle image or crop.
    Re-uses unified ANPRService:
    Detection -> Exact Crop (pad=0) -> HSRP Morph Open -> EasyOCR -> Indian Normalization.
    """
    global anpr_service
    if anpr_service is None:
        anpr_service = ANPRService.get_instance()
        
    image_bytes = await file.read()
    np_arr = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
    
    if img is None:
        return {"plateDetected": False, "error": "Invalid image format"}
        
    result = anpr_service.detect_and_read(img)
    return result

# =========================================================================
# 2. LIVE CAMERA STREAM FRAME ENDPOINT (Stage 2 Temporal Consensus)
# =========================================================================
@app.post("/api/v1/anpr/stream/frame")
async def process_camera_frame(
    file: UploadFile = File(...),
    station_id: int = Query(default=1),
    camera_id: int = Query(default=1),
    timestamp: Optional[float] = Query(default=None)
):
    """
    Accepts camera frame stream.
    Applies:
    - Frame decimation (every N frames)
    - Quality filtering (rejects blurry / occluded frames)
    - IoU plate tracking across frames
    - Multi-frame temporal buffer (5-10 frames)
    - Weighted voting + character-level consensus
    - Duplicate event suppression (30s cooldown)
    """
    global stream_processor
    if stream_processor is None:
        return {"error": "Stream processor not initialized"}
        
    image_bytes = await file.read()
    np_arr = np.frombuffer(image_bytes, np.uint8)
    frame = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
    
    if frame is None:
        return {"error": "Invalid frame bytes"}
        
    ts = timestamp or time.time()
    result = stream_processor.process_frame(frame, timestamp=ts)
    return result

# =========================================================================
# 3. LIVE TRACKER INSPECTOR & TELEMETRY
# =========================================================================
@app.get("/api/v1/anpr/tracks")
def get_active_tracks():
    """Returns live active tracks, consensus state, and streaming FPS metrics."""
    global stream_processor
    if stream_processor is None:
        return {"tracks": [], "metrics": {}}
        
    tracks_info = []
    for tid, t in stream_processor.tracker.tracks.items():
        tracks_info.append({
            "trackId": t.track_id,
            "status": t.status,
            "bbox": t.bbox,
            "lastSeen": t.last_seen,
            "observations": len(t.observations),
            "confirmedPlate": t.confirmed_plate,
            "consensusScore": t.consensus_score,
            "recentCandidates": [o["text"] for o in t.observations[-5:]]
        })
        
    now = time.time()
    elapsed = max(0.001, now - stream_processor.start_time)
    
    return {
        "activeTracksCount": len([t for t in tracks_info if t["status"] != "LOST"]),
        "tracks": tracks_info,
        "metrics": {
            "fps": round(stream_processor.total_frames / elapsed, 1),
            "detectionFps": round(stream_processor.detection_frames / elapsed, 1),
            "ocrCallsPerSec": round(stream_processor.ocr_calls / elapsed, 2),
            "avgOcrLatencyMs": round(stream_processor.total_ocr_latency_ms / max(1, stream_processor.ocr_calls), 2),
            "totalFrames": stream_processor.total_frames,
            "totalOcrCalls": stream_processor.ocr_calls
        }
    }

if __name__ == "__main__":
    import argparse
    import uvicorn
    parser = argparse.ArgumentParser(description="Unified ANPR FastAPI Service")
    parser.add_argument("--port", type=int, default=int(os.getenv("PORT", "8001")))
    parser.add_argument("--host", type=str, default="0.0.0.0")
    parser.add_argument("--server", action="store_true")
    parser.add_argument("--backend-url", type=str, default=None)
    args, _ = parser.parse_known_args()
    if args.backend_url:
        os.environ["BACKEND_URL"] = args.backend_url
    uvicorn.run(app, host=args.host, port=args.port)

