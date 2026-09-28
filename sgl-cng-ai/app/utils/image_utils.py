import cv2
import numpy as np

def load_image(image_input):
    """
    Loads an image from filepath or byte stream into OpenCV BGR numpy format.
    """
    if isinstance(image_input, str):
        image = cv2.imread(image_input)
        if image is None:
            raise ValueError(f"Failed to load image from path: {image_input}")
        return image
    elif isinstance(image_input, bytes):
        nparr = np.frombuffer(image_input, np.uint8)
        image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if image is None:
            raise ValueError("Failed to decode image from byte buffer")
        return image
    elif isinstance(image_input, np.ndarray):
        return image_input
    else:
        raise TypeError("Unsupported image input type")

def preprocess_roi(roi_bgr):
    """
    Preprocesses cropped license plate ROI for enhanced OCR accuracy:
    - Grayscale conversion
    - Contrast adjustment (CLAHE)
    - Bilateral noise filter
    """
    if roi_bgr is None or roi_bgr.size == 0:
        return roi_bgr

    if len(roi_bgr.shape) == 3:
        gray = cv2.cvtColor(roi_bgr, cv2.COLOR_BGR2GRAY)
    else:
        gray = roi_bgr.copy()

    filtered = cv2.bilateralFilter(gray, 11, 17, 17)
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    enhanced = clahe.apply(filtered)
    return enhanced

def get_crop_preprocessing_variants(roi_bgr):
    """
    Generates 6 distinct preprocessing image variants for multi-candidate OCR evaluation:
    1. Original BGR Crop
    2. 2x Bilinear Upscale + Sharpening
    3. Grayscale + CLAHE Contrast Enhancement
    4. Bilateral Denoised Filter
    5. Adaptive Gaussian Thresholding
    6. Deskewed / Perspective Corrected Crop
    """
    if roi_bgr is None or roi_bgr.size == 0:
        return [("Original BGR Crop", roi_bgr)]

    h, w = roi_bgr.shape[:2]
    variants = [("1. Original BGR Crop", roi_bgr)]

    # Small webcam detections need more than a fixed 2x resize. Enlarge short
    # plate crops to a useful character height, with a cap to control latency.
    scale = max(2, min(5, int(np.ceil(100 / max(1, h)))))
    upscaled = cv2.resize(roi_bgr, (w * scale, h * scale), interpolation=cv2.INTER_CUBIC)
    sharpen_kernel = np.array([[0, -1, 0], [-1, 5, -1], [0, -1, 0]])
    sharpened = cv2.filter2D(upscaled, -1, sharpen_kernel)
    variants.append(("2. 2x Upscaled & Sharpened", sharpened))

    # Variant 3: Grayscale + CLAHE Contrast Enhancement
    gray = cv2.cvtColor(roi_bgr, cv2.COLOR_BGR2GRAY) if len(roi_bgr.shape) == 3 else roi_bgr.copy()
    clahe = cv2.createCLAHE(clipLimit=2.5, tileGridSize=(8, 8))
    var_clahe = clahe.apply(gray)
    variants.append(("3. Grayscale + CLAHE Contrast", var_clahe))

    # Variant 4: Bilateral Denoised Filter
    var_denoised = cv2.bilateralFilter(gray, 11, 17, 17)
    variants.append(("4. Bilateral Denoised Filter", var_denoised))

    # Variant 5: Adaptive threshold on the enlarged crop so small glyphs are
    # not lost to a fixed-size threshold window.
    scaled_gray = cv2.cvtColor(upscaled, cv2.COLOR_BGR2GRAY) if len(upscaled.shape) == 3 else upscaled.copy()
    block_size = min(31, max(11, (min(scaled_gray.shape[:2]) // 2) | 1))
    var_thresh = cv2.adaptiveThreshold(scaled_gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, block_size, 2)
    variants.append(("5. Adaptive Thresholded Binarization", var_thresh))

    # Variant 6: Deskew / Perspective Correction for Tilted Plates
    try:
        coords = np.column_stack(np.where(gray < 128))
        if len(coords) > 10:
            angle = cv2.minAreaRect(coords)[-1]
            if angle < -45:
                angle = -(90 + angle)
            else:
                angle = -angle
            if abs(angle) > 2.0 and abs(angle) < 45.0:
                M = cv2.getRotationMatrix2D((w // 2, h // 2), angle, 1.0)
                rotated = cv2.warpAffine(roi_bgr, M, (w, h), flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_REPLICATE)
                variants.append(("7. Deskewed Perspective Correction", rotated))
    except Exception:
        pass

    return variants

def annotate_debug_image(image_bgr, bbox, label, confidence):
    """
    Draws bounding box, label, and confidence score on a copy of the image.
    """
    annotated = image_bgr.copy()
    if bbox is None:
        return annotated

    x, y, w, h = bbox
    color = (0, 255, 0) if confidence >= 0.85 else (0, 165, 255)

    # Draw bounding box
    cv2.rectangle(annotated, (x, y), (x + w, y + h), color, 3)

    # Label text
    text = f"{label} ({confidence:.2f})" if label else f"Plate ({confidence:.2f})"
    
    # Draw background box for text readability
    (text_w, text_h), baseline = cv2.getTextSize(text, cv2.FONT_HERSHEY_SIMPLEX, 0.7, 2)
    cv2.rectangle(annotated, (x, y - text_h - 10), (x + text_w + 10, y), color, -1)
    cv2.putText(annotated, text, (x + 5, y - 5), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

    return annotated
