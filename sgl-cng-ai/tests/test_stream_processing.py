import os
import pytest
import numpy as np
import cv2
from app.pipeline.anpr_pipeline import AnprPipeline
from samples.generate_sample_plate import generate_synthetic_plate_image

@pytest.fixture
def sample_video_path(tmp_path):
    video_file = os.path.join(tmp_path, "test_stream.mp4")
    plate_img_path = os.path.join(tmp_path, "plate.jpg")
    generate_synthetic_plate_image(plate_img_path, "GJ01AB1234", "white")
    
    img = cv2.imread(plate_img_path)
    h, w, c = img.shape
    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter(video_file, fourcc, 10.0, (w, h))
    
    for _ in range(25):
        out.write(img)
    out.release()
    return video_file

def test_stream_processing_flow(sample_video_path):
    pipeline = AnprPipeline(
        send_to_backend=False,
        min_confidence=0.50,
        min_stable_frames=3,
        cooldown_seconds=30.0,
        frame_skip=2
    )

    res = pipeline.process_stream(
        source=sample_video_path,
        frame_skip=2,
        station_id=1,
        camera_id=1,
        max_frames=20
    )

    assert res["source"] == sample_video_path
    assert res["totalFramesRead"] <= 20
    assert res["framesProcessed"] > 0

def test_network_failure_graceful_handling():
    # Configure unreachable backend URL
    pipeline = AnprPipeline(
        backend_url="http://localhost:9999",
        send_to_backend=True,
        cooldown_seconds=0.0
    )

    success, resp = pipeline._dispatch_to_backend("GJ01AB1234", station_id=1, camera_id=1)
    assert success is False
    assert "error" in resp

def test_rtsp_invalid_url_handling():
    pipeline = AnprPipeline(send_to_backend=False)
    # Attempting invalid RTSP stream should fail gracefully with reconnect retries limit
    res = pipeline.process_stream(
        source="rtsp://invalid_host:8554/live",
        reconnect_retries=1,
        reconnect_delay=0.1
    )
    assert "error" in res
