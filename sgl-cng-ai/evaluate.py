import os
import time
import json
from app.pipeline.anpr_pipeline import AnprPipeline
from samples.generate_sample_plate import generate_synthetic_plate_image

def create_evaluation_dataset(dataset_dir="eval_dataset"):
    """
    Creates a representative evaluation dataset covering:
    - Clear Indian plates from different states (GJ, MH, DL, KA, TN, UP, RJ)
    - Commercial vs Private plate backgrounds
    - Blank/no-plate images
    """
    os.makedirs(dataset_dir, exist_ok=True)
    dataset = [
        ("eval_dataset/gj_car.jpg", "GJ01AB1234", "white"),
        ("eval_dataset/mh_car.jpg", "MH12DE5678", "white"),
        ("eval_dataset/dl_car.jpg", "DL3CCE9999", "white"),
        ("eval_dataset/ka_car.jpg", "KA05MN4321", "white"),
        ("eval_dataset/tn_car.jpg", "TN09XY8765", "white"),
        ("eval_dataset/up_auto.jpg", "UP32BZ1122", "yellow"),
        ("eval_dataset/rj_auto.jpg", "RJ14CD3344", "yellow"),
        ("eval_dataset/no_plate.jpg", None, "none")
    ]

    for path, expected_text, bg in dataset:
        if bg == "none":
            # Generate image with no plate
            import numpy as np, cv2
            canvas = np.ones((400, 600, 3), dtype=np.uint8) * 120
            cv2.imwrite(path, canvas)
        else:
            generate_synthetic_plate_image(path, expected_text, bg)

    return dataset

def run_evaluation():
    dataset = create_evaluation_dataset()
    pipeline = AnprPipeline(send_to_backend=False, min_confidence=0.50)

    total_images = len(dataset)
    detected_count = 0
    ocr_correct_count = 0
    e2e_correct_count = 0
    total_time_ms = 0.0

    failure_categories = {
        "NO_PLATE_DETECTED": 0,
        "LOW_CONFIDENCE": 0,
        "OCR_ERROR": 0,
        "INVALID_STRUCTURE": 0
    }

    print("\n========================================================")
    print("       SGL CNG ANPR BENCHMARK EVALUATION REPORT         ")
    print("========================================================\n")

    for path, expected_text, _ in dataset:
        t0 = time.time()
        res, _ = pipeline.process_image(path)
        t1 = time.time()

        elapsed_ms = (t1 - t0) * 1000.0
        total_time_ms += elapsed_ms

        detected = res["plateDetected"]
        predicted_text = res["registrationNumber"]

        if expected_text is not None: # Image has a plate
            if detected:
                detected_count += 1
            else:
                failure_categories["NO_PLATE_DETECTED"] += 1

            if predicted_text == expected_text:
                ocr_correct_count += 1
                e2e_correct_count += 1
            else:
                if res.get("reason") in failure_categories:
                    failure_categories[res["reason"]] += 1
                elif predicted_text is not None and predicted_text != expected_text:
                    failure_categories["OCR_ERROR"] += 1
        else: # Image has no plate
            if not detected and predicted_text is None:
                e2e_correct_count += 1
                detected_count += 1
                ocr_correct_count += 1

    avg_time_ms = total_time_ms / total_images if total_images > 0 else 0

    print(f"Images Tested:          {total_images}")
    print(f"Plate Detected Correctly: {detected_count} / {total_images} ({(detected_count/total_images)*100:.1f}%)")
    print(f"OCR Correct:            {ocr_correct_count} / {total_images} ({(ocr_correct_count/total_images)*100:.1f}%)")
    print(f"End-to-End Correct:     {e2e_correct_count} / {total_images} ({(e2e_correct_count/total_images)*100:.1f}%)")
    print(f"Average Inference Time: {avg_time_ms:.2f} ms/image\n")

    print("--- Failure Categorization ---")
    for cat, count in failure_categories.items():
        print(f" - {cat}: {count}")

    print("\n========================================================\n")

if __name__ == "__main__":
    run_evaluation()
