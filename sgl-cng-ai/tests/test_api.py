import pytest
import cv2
import numpy as np
import io
from fastapi.testclient import TestClient
from app.api.server import app

client = TestClient(app)

def test_health_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "UP"}

def test_recognize_endpoint_with_image():
    # Create synthetic image in memory
    canvas = np.ones((400, 600, 3), dtype=np.uint8) * 180
    cv2.rectangle(canvas, (150, 180), (450, 260), (255, 255, 255), -1)
    cv2.putText(canvas, "GJ01AB1234", (180, 235), cv2.FONT_HERSHEY_SIMPLEX, 1.0, (0, 0, 0), 3)

    _, encoded = cv2.imencode(".jpg", canvas)
    img_bytes = io.BytesIO(encoded.tobytes())

    response = client.post(
        "/api/v1/anpr/recognize",
        files={"file": ("test_car.jpg", img_bytes, "image/jpeg")}
    )

    assert response.status_code == 200
    json_resp = response.json()
    assert "plateDetected" in json_resp
    assert "confidence" in json_resp
    assert "registrationNumber" in json_resp

def test_recognize_endpoint_invalid_file_type():
    response = client.post(
        "/api/v1/anpr/recognize",
        files={"file": ("test.txt", io.BytesIO(b"not an image"), "text/plain")}
    )
    assert response.status_code == 400
