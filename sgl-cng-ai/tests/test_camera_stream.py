import pytest
import numpy as np
import cv2
import os
import time
from unittest.mock import MagicMock, patch
from app.pipeline.anpr_pipeline import AnprPipeline

@pytest.fixture
def sample_frame_with_plate(tmp_path):
    canvas = np.ones((400, 600, 3), dtype=np.uint8) * 180
    cv2.rectangle(canvas, (150, 180), (450, 260), (255, 255, 255), -1)
    cv2.rectangle(canvas, (150, 180), (450, 260), (0, 0, 0), 3)
    cv2.putText(canvas, "GJ01AB1234", (180, 235), cv2.FONT_HERSHEY_SIMPLEX, 1.0, (0, 0, 0), 3)
    return canvas

@pytest.fixture
def sample_frame_no_plate():
    return np.ones((400, 600, 3), dtype=np.uint8) * 100

# 1. Camera connection failure
def test_camera_connection_failure():
    pipeline = AnprPipeline(send_to_backend=False)
    res = pipeline.run_interactive_webcam(camera_index=9999) # invalid device
    assert "error" in res

# 2. Camera frame acquisition & stream processing
def test_camera_frame_acquisition(sample_frame_with_plate, tmp_path):
    video_path = str(tmp_path / "stream_test.mp4")
    out = cv2.VideoWriter(video_path, cv2.VideoWriter_fourcc(*'mp4v'), 10.0, (600, 400))
    for _ in range(5):
        out.write(sample_frame_with_plate)
    out.release()

    pipeline = AnprPipeline(send_to_backend=False)
    res = pipeline.process_stream(source=video_path, max_frames=5)
    assert res["totalFramesRead"] == 5
    assert res["framesProcessed"] > 0

# 3. No plate detected
def test_no_plate_detected(sample_frame_no_plate):
    pipeline = AnprPipeline(send_to_backend=False)
    res, _ = pipeline.process_image(sample_frame_no_plate)
    assert res["plateDetected"] is False
    assert res["registrationNumber"] is None
    assert res["reason"] == "NO_PLATE_DETECTED"

# 4. Plate detected
def test_plate_detected(sample_frame_with_plate):
    pipeline = AnprPipeline(send_to_backend=False)
    res, _ = pipeline.process_image(sample_frame_with_plate)
    assert res["plateDetected"] is True

# 5. OCR failure (Plate detected, but OCR empty/invalid)
def test_ocr_failure(sample_frame_no_plate):
    pipeline = AnprPipeline(send_to_backend=False)
    # Mock detector to return candidate box, but OCR to return empty string
    with patch.object(pipeline.detector, 'detect_candidates', return_value=[(sample_frame_no_plate[10:50, 10:50], (10, 10, 40, 40), 0.90)]):
        with patch.object(pipeline.ocr_engine, 'read_text', return_value=("", 0.0)):
            res, _ = pipeline.process_image(sample_frame_no_plate)
            assert res["plateDetected"] is True
            assert res["registrationNumber"] is None

# 6. Successful OCR & Normalization
def test_successful_ocr(sample_frame_with_plate):
    pipeline = AnprPipeline(send_to_backend=False)
    res, _ = pipeline.process_image(sample_frame_with_plate)
    assert res["plateDetected"] is True
    if res["registrationNumber"]:
        assert res["registrationNumber"] == "GJ01AB1234"

def test_low_confidence_candidate_is_provisional_not_dispatched(sample_frame_with_plate):
    pipeline = AnprPipeline(send_to_backend=True, min_confidence=0.85)
    roi = sample_frame_with_plate[180:260, 150:450]
    with patch.object(pipeline.detector, 'detect_candidates', return_value=[(roi, (150, 180, 300, 80), 0.8)]):
        with patch.object(pipeline.ocr_engine, 'read_text_multi_variant', return_value=("GJ01AB1234", 0.5, roi, [])):
            with patch.object(pipeline, '_dispatch_to_backend') as dispatch:
                res, _ = pipeline.process_image(sample_frame_with_plate)
    assert res["registrationNumber"] == "GJ01AB1234"
    assert res["reason"] == "LOW_CONFIDENCE"
    dispatch.assert_not_called()

# 7. Temporal stabilization (multiple matching observations)
def test_temporal_stabilization(sample_frame_with_plate):
    pipeline = AnprPipeline(send_to_backend=False, min_stable_frames=3, stability_window_seconds=2.0)
    frames = [sample_frame_with_plate, sample_frame_with_plate, sample_frame_with_plate]
    res = pipeline.process_video_frames(frames)
    assert res["plateDetected"] is True

# 8. Automatic backend dispatch
def test_automatic_backend_dispatch(sample_frame_with_plate):
    pipeline = AnprPipeline(send_to_backend=True, min_stable_frames=1)
    with patch.object(pipeline, '_dispatch_to_backend', return_value=(True, {"status": "SUCCESS", "journeyId": 1})) as mock_dispatch:
        res, _ = pipeline.process_image(sample_frame_with_plate)
        if res["registrationNumber"]:
            mock_dispatch.assert_called_once_with(res["registrationNumber"], pipeline.station_id, pipeline.camera_id)

# 9. Duplicate suppression / Cooldown
def test_duplicate_suppression(sample_frame_with_plate):
    pipeline = AnprPipeline(send_to_backend=True, min_stable_frames=1, cooldown_seconds=30.0)
    with patch.object(pipeline, '_dispatch_to_backend', return_value=(True, {"status": "SUCCESS", "journeyId": 1})) as mock_dispatch:
        # First call
        res1, _ = pipeline.process_image(sample_frame_with_plate)
        # Immediate second call within cooldown window
        res2, _ = pipeline.process_image(sample_frame_with_plate)
        if res1["registrationNumber"]:
            # Should only dispatch once due to 30s cooldown
            assert mock_dispatch.call_count == 1

# 10. Backend compliance response parsing
def test_backend_compliance_response_parsing():
    pipeline = AnprPipeline(send_to_backend=False)
    with patch("requests.post") as mock_post:
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {
            "id": 101,
            "registrationNumber": "GJ01AB1234",
            "complianceStatus": "ELIGIBLE",
            "message": "Vehicle is compliant and eligible for fueling"
        }
        mock_post.return_value = mock_response

        success, resp = pipeline._dispatch_to_backend("GJ01AB1234", station_id=1, camera_id=1)
        assert success is True
        assert resp["complianceStatus"] == "ELIGIBLE"
