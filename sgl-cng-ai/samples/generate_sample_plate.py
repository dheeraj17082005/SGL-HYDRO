import cv2
import numpy as np
import os

def generate_synthetic_plate_image(output_path, text="GJ01AB1234", background_type="white"):
    """
    Generates a synthetic vehicle image with a realistic Indian license plate.
    """
    # Create vehicle image canvas (600x400)
    canvas = np.ones((400, 600, 3), dtype=np.uint8) * 180

    # Draw simple vehicle shape
    cv2.rectangle(canvas, (100, 100), (500, 320), (50, 50, 50), -1)

    # License Plate Box (260x70) centered
    plate_x, plate_y, plate_w, plate_h = 170, 200, 260, 70
    plate_color = (255, 255, 255) if background_type == "white" else (0, 215, 255) # Yellow for commercial/auto
    cv2.rectangle(canvas, (plate_x, plate_y), (plate_x + plate_w, plate_y + plate_h), plate_color, -1)
    cv2.rectangle(canvas, (plate_x, plate_y), (plate_x + plate_w, plate_y + plate_h), (0, 0, 0), 3)

    # IND hologram blue strip on left
    cv2.rectangle(canvas, (plate_x, plate_y), (plate_x + 25, plate_y + plate_h), (255, 0, 0), -1)
    cv2.putText(canvas, "IND", (plate_x + 2, plate_y + 40), cv2.FONT_HERSHEY_SIMPLEX, 0.35, (255, 255, 255), 1)

    # Draw plate number text
    cv2.putText(canvas, text, (plate_x + 35, plate_y + 48), cv2.FONT_HERSHEY_SIMPLEX, 0.9, (0, 0, 0), 3)

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    cv2.imwrite(output_path, canvas)
    return output_path

if __name__ == "__main__":
    generate_synthetic_plate_image("samples/car_gj01ab1234.jpg", "GJ01AB1234", "white")
    generate_synthetic_plate_image("samples/auto_gj02cd5678.jpg", "GJ02CD5678", "yellow")
    print("Generated sample Indian license plate images in samples/")
