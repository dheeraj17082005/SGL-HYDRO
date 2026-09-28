# Sabarmati Gas Limited (SGL) - AI / ANPR Prototype Service (`sgl-cng-ai`)

Standalone Python AI service prototype for Indian vehicle license plate detection, text recognition (OCR), plate normalization, confidence thresholding, and FastAPI web service.

---

## Architecture & Model Selection

- **Two-Stage Detection & Recognition Pipeline**:
  1. **Stage 1 (Plate Detector)**: Morphological filtering (TopHat, Sobel gradient), adaptive thresholding, and aspect-ratio contour detection tuned for Indian license plates.
  2. **Stage 2 (OCR Engine)**: EasyOCR / PyTesseract text extraction from preprocessed plate regions of interest (ROI).
- **Indian Plate Normalization (`PlateNormalizer`)**:
  - Upper-casing, removing spaces and non-alphanumeric characters.
  - Positional OCR error correction (e.g. converting `O` to `0` in RTO numbers and `0` to `O` in State codes).
- **Confidence Thresholding & Video Stabilization**:
  - Configurable `MIN_CONFIDENCE` thresholding (default `0.85`).
  - Temporal frame consensus selection for video frame sequences.

---

## Installation & Setup

### Prerequisites
- Python 3.10+
- Virtual Environment

### Installation Steps

```bash
cd sgl-cng-ai
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

---

## Usage

### 1. Generating Sample Images
Generate synthetic Indian license plate images for testing:

```bash
python samples/generate_sample_plate.py
```

### 2. Processing Single Image via CLI
Recognize license plate from image:

```bash
python main.py --image samples/car_gj01ab1234.jpg --debug
```

**Output:**
```json
{
  "registrationNumber": "GJ01AB1234",
  "confidence": 0.95,
  "plateDetected": true
}
```

### 3. Processing Video Frames via CLI
Recognize license plate across sequential video frames:

```bash
python main.py --video samples/traffic.mp4
```

### 4. Running the FastAPI Server

```bash
python main.py --server --port 8001
```

Or via Uvicorn directly:

```bash
uvicorn app.api.server:app --port 8001
```

#### API Endpoints:
- `GET /health`: Returns `{"status": "UP"}`
- `POST /api/v1/anpr/recognize`: Accepts image upload (`file`) and returns recognition result payload.

---

## Running Tests

Execute pytest suite:

```bash
pytest
```
