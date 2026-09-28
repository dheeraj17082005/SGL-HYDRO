import os
import time
import pytest
import cv2
import numpy as np

from app.pipeline.anpr_pipeline import AnprPipeline
from samples.generate_sample_plate import generate_synthetic_plate_image

def create_temp_video(path, plate_text, bg_color="white", num_frames=30):
    tmp_img = path + ".tmp.jpg"
    generate_synthetic_plate_image(tmp_img, plate_text, bg_color)
    img = cv2.imread(tmp_img)
    h, w, c = img.shape
    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter(path, fourcc, 10.0, (w, h))
    for i in range(num_frames):
        out.write(img)
    out.release()
    if os.path.exists(tmp_img):
        os.remove(tmp_img)

def test_scenario_1_valid_vehicle(tmp_path):
    video_path = os.path.join(tmp_path, "valid_car.mp4")
    create_temp_video(video_path, "GJ01AB1234", "white", num_frames=20)

    # Unit pipeline test without live backend
    pipeline = AnprPipeline(send_to_backend=False, min_confidence=0.50, min_stable_frames=3, cooldown_seconds=30.0)
    result = pipeline.process_stream(video_path, frame_skip=2)

    assert result["totalFramesRead"] > 0
    assert result["framesProcessed"] > 0
    # Verified plate in temporal buffer
    assert len(pipeline.temporal_buffer) > 0

def test_scenario_2_invalid_vehicle(tmp_path):
    video_path = os.path.join(tmp_path, "invalid_car.mp4")
    create_temp_video(video_path, "GJ01AB9999", "yellow", num_frames=20)

    pipeline = AnprPipeline(send_to_backend=False, min_confidence=0.50, min_stable_frames=3, cooldown_seconds=30.0)
    result = pipeline.process_stream(video_path, frame_skip=2)

    assert result["totalFramesRead"] > 0
    assert result["framesProcessed"] > 0

def test_scenario_3_expired_hydro(tmp_path):
    video_path = os.path.join(tmp_path, "expired_hydro.mp4")
    create_temp_video(video_path, "GJ01EXPHYDRO", "white", num_frames=20)

    pipeline = AnprPipeline(send_to_backend=False, min_confidence=0.50, min_stable_frames=3, cooldown_seconds=30.0)
    result = pipeline.process_stream(video_path, frame_skip=2)

    assert result["totalFramesRead"] > 0
    assert result["framesProcessed"] > 0

def test_duplicate_read_suppression(tmp_path):
    video_path = os.path.join(tmp_path, "dup_stream.mp4")
    create_temp_video(video_path, "GJ01AB1234", "white", num_frames=20)

    pipeline = AnprPipeline(send_to_backend=False, min_confidence=0.50, min_stable_frames=3, cooldown_seconds=30.0)
    pipeline.last_dispatched["GJ01AB1234"] = time.time()
    
    # Process stream with pre-registered cooldown plate
    dispatched, resp = pipeline._dispatch_to_backend("GJ01AB1234", station_id=1, camera_id=1)
    assert dispatched is False
    assert resp.get("cooldown") is True

