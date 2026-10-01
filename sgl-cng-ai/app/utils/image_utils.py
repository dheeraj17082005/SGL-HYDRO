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
    Generates preprocessing image variants for multi-candidate OCR evaluation:
    1. Original BGR Crop
    2. IND-Masked Grayscale (masks the blue IND emblem / chakra on left 13% of plate)
    3. Morphological Open (suppresses HSRP holographic / carbon fiber textures inside digits)
    4. Grayscale + CLAHE Contrast Enhancement
    """
    if roi_bgr is None or roi_bgr.size == 0:
        return [("Original BGR Crop", roi_bgr)]

    h, w = roi_bgr.shape[:2]
    variants = [("1. Original BGR Crop", roi_bgr)]

    # Normalized height for reliable OCR
    normalized = roi_bgr
    if h > 180:
        scale = 140.0 / h
        normalized = cv2.resize(roi_bgr, (int(w * scale), 140), interpolation=cv2.INTER_AREA)
    elif h < 60:
        scale = 90.0 / h
        normalized = cv2.resize(roi_bgr, (int(w * scale), 90), interpolation=cv2.INTER_CUBIC)

    nh, nw = normalized.shape[:2]
    gray = cv2.cvtColor(normalized, cv2.COLOR_BGR2GRAY) if len(normalized.shape) == 3 else normalized.copy()

    # Variant 2: IND-Masked Grayscale (masks the left 13% of plate where blue IND emblem resides)
    var_masked = gray.copy()
    var_masked[:, :int(nw * 0.13)] = 255
    variants.append(("2. IND-Masked Grayscale", var_masked))

    # Variant 3: Morphological Open on masked grayscale (removes internal carbon fiber/hologram textures)
    opened = cv2.morphologyEx(var_masked, cv2.MORPH_OPEN, cv2.getStructuringElement(cv2.MORPH_RECT, (3, 3)))
    variants.append(("3. Texture Suppressed (Morph Open)", opened))

    # Variant 4: Grayscale + CLAHE (enhances contrast on glare / night / shadow shots)
    clahe = cv2.createCLAHE(clipLimit=2.5, tileGridSize=(8, 8))
    var_clahe = clahe.apply(var_masked)
    variants.append(("4. Grayscale + CLAHE Contrast", var_clahe))

    return variants

def annotate_debug_image(image_bgr, bbox, text, confidence):
    """
    Annotates image with bounding box, detected text and confidence score for debug visualization.
    """
    if image_bgr is None or image_bgr.size == 0:
        return image_bgr

    annotated = image_bgr.copy()
    if bbox is not None:
        x, y, w, h = bbox
        cv2.rectangle(annotated, (x, y), (x + w, y + h), (0, 255, 0), 2)
        label = f"{text} ({confidence:.2f})"
        cv2.putText(annotated, label, (x, max(20, y - 10)), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)
    return annotated
