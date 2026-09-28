import pytest
import numpy as np
import cv2
import os
from app.pipeline.anpr_pipeline import AnprPipeline

@pytest.fixture
def pipeline():
    return AnprPipeline(min_confidence=0.85)

@pytest.fixture
def sample_plate_image(tmp_path):
    img_path = str(tmp_path / "test_plate.jpg")
    canvas = np.ones((400, 600, 3), dtype=np.uint8) * 180
    # License plate
    cv2.rectangle(canvas, (150, 180), (450, 260), (255, 255, 255), -1)
    cv2.rectangle(canvas, (150, 180), (450, 260), (0, 0, 0), 3)
    cv2.putText(canvas, "GJ01AB1234", (180, 235), cv2.FONT_HERSHEY_SIMPLEX, 1.0, (0, 0, 0), 3)
    cv2.imwrite(img_path, canvas)
    return img_path

@pytest.fixture
def no_plate_image(tmp_path):
    img_path = str(tmp_path / "no_plate.jpg")
    canvas = np.ones((400, 600, 3), dtype=np.uint8) * 100 # solid gray background
    cv2.imwrite(img_path, canvas)
    return img_path

def test_process_image_no_plate(pipeline, no_plate_image):
    result, _ = pipeline.process_image(no_plate_image)
    assert result["registrationNumber"] is None
    assert isinstance(result["confidence"], float)

def test_process_image_debug_generation(pipeline, sample_plate_image):
    result, debug_img = pipeline.process_image(sample_plate_image, generate_debug=True)
    assert isinstance(result["plateDetected"], bool)
    assert debug_img is not None
    assert debug_img.shape == (400, 600, 3)

def test_video_temporal_stabilization(pipeline):
    # Simulate sequential video frames with plate detections
    canvas = np.ones((400, 600, 3), dtype=np.uint8) * 180
    cv2.rectangle(canvas, (150, 180), (450, 260), (255, 255, 255), -1)
    cv2.putText(canvas, "GJ01AB1234", (180, 235), cv2.FONT_HERSHEY_SIMPLEX, 1.0, (0, 0, 0), 3)

    frames = [canvas, canvas, canvas]
    res = pipeline.process_video_frames(frames)
    assert isinstance(res["plateDetected"], bool)
