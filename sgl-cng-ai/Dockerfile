FROM python:3.11-slim
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
    libglib2.0-0 libgomp1 libgl1 tesseract-ocr \
    && rm -rf /var/lib/apt/lists/*
COPY requirements.txt ./
RUN pip install --no-cache-dir --index-url https://download.pytorch.org/whl/cpu torch==2.5.1 torchvision==0.20.1 \
    && pip install --no-cache-dir -r requirements.txt
COPY . .
RUN mkdir -p /root/.EasyOCR/model && cp ocr_models/*.pth /root/.EasyOCR/model/
EXPOSE 8001
CMD ["python", "main.py", "--server", "--port", "8001", "--backend-url", "http://app:8080"]
