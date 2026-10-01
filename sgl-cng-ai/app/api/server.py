import asyncio
import time
import cv2
import logging
import os
from fastapi import FastAPI, UploadFile, File, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional
from app.pipeline.anpr_pipeline import AnprPipeline

logger = logging.getLogger(__name__)

app = FastAPI(
    title="SGL CNG AI - Production ANPR API",
    description="AI-Powered Vehicle & License Plate Recognition Service for Sabarmati Gas Limited",
    version="2.0.0"
)

allowed_origins = [origin.strip() for origin in os.getenv("AI_ALLOWED_ORIGINS", "*").split(",") if origin.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins if allowed_origins else ["*"],
    allow_credentials=False,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type"],
)

pipeline = AnprPipeline(send_to_backend=True)
active_streams = {}

class HealthResponse(BaseModel):
    status: str

class AnprRecognitionResponse(BaseModel):
    plateDetected: bool
    registrationNumber: Optional[str] = None
    rawOcr: Optional[str] = None
    trackId: Optional[int] = None
    confidence: float = 0.0
    vehicleConfidence: Optional[float] = 0.0
    plateConfidence: Optional[float] = 0.0
    detectorConfidence: Optional[float] = 0.0
    ocrConfidence: Optional[float] = 0.0
    finalConfidence: Optional[float] = 0.0
    statusState: Optional[str] = "UNREADABLE"
    timestamp: str
    reason: Optional[str] = None

class StreamStartRequest(BaseModel):
    rtspUrl: str
    stationId: int = 1
    cameraId: int = 1
    frameInterval: float = 0.5

class StreamStatusResponse(BaseModel):
    status: str
    streamId: str
    message: str

@app.get("/health", response_model=HealthResponse)
def health_check():
    return {"status": "UP"}

@app.post("/api/v1/anpr/recognize", response_model=AnprRecognitionResponse)
async def recognize_plate(file: UploadFile = File(...)):
    if not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="Uploaded file must be an image")

    try:
        contents = await file.read()
        result, _ = pipeline.process_image(contents, dispatch_to_backend=False)
        return result
    except Exception as e:
        logger.error(f"Error in recognize endpoint: {e}")
        raise HTTPException(status_code=500, detail=f"Image processing error: {str(e)}")

@app.post("/api/v1/anpr/stream/start", response_model=StreamStatusResponse)
def start_stream(req: StreamStartRequest, background_tasks: BackgroundTasks):
    stream_id = f"st_{req.stationId}_cam_{req.cameraId}"

    if stream_id in active_streams and active_streams[stream_id].get("running"):
        return {
            "status": "ALREADY_RUNNING",
            "streamId": stream_id,
            "message": f"Stream worker {stream_id} is already active."
        }

    active_streams[stream_id] = {"running": True, "url": req.rtspUrl}
    background_tasks.add_task(run_stream_worker, stream_id, req.rtspUrl, req.stationId, req.cameraId, req.frameInterval)

    return {
        "status": "STARTED",
        "streamId": stream_id,
        "message": f"Started processing camera stream at {req.rtspUrl}"
    }

@app.post("/api/v1/anpr/stream/stop", response_model=StreamStatusResponse)
def stop_stream(streamId: str):
    if streamId in active_streams:
        active_streams[streamId]["running"] = False
        return {
            "status": "STOPPED",
            "streamId": streamId,
            "message": f"Stopped stream worker {streamId}"
        }
    return {
        "status": "NOT_FOUND",
        "streamId": streamId,
        "message": f"Stream worker {streamId} was not running."
    }

def run_stream_worker(stream_id, rtsp_url, station_id, camera_id, frame_interval):
    logger.info(f"RTSP Stream Worker started: {stream_id} ({rtsp_url})")
    cap = cv2.VideoCapture(rtsp_url)
    frame_count = 0

    while active_streams.get(stream_id, {}).get("running", False):
        ret, frame = cap.read()
        if not ret:
            logger.warning(f"RTSP stream read failed for {stream_id}. Retrying...")
            time.sleep(2.0)
            cap = cv2.VideoCapture(rtsp_url)
            continue

        frame_count += 1
        if frame_count % 3 == 0:
            try:
                pipeline.process_image(frame, station_id=station_id, camera_id=camera_id, frame_id=frame_count, dispatch_to_backend=True)
            except Exception as e:
                logger.error(f"Error in stream processing: {e}")

        time.sleep(frame_interval)

    cap.release()
    logger.info(f"RTSP Stream Worker stopped: {stream_id}")
