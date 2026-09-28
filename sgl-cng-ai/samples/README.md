# Samples Directory

This directory contains sample images and video files for testing the ANPR pipeline.

## Generating Synthetic Test Images

You can generate sample Indian vehicle license plate images using:

```bash
python samples/generate_sample_plate.py
```

This creates:
- `samples/car_gj01ab1234.jpg` (White private plate)
- `samples/auto_gj02cd5678.jpg` (Yellow commercial plate)

## Testing Real Vehicle Samples

Place actual Indian vehicle images (`.jpg`, `.png`) or video clips (`.mp4`) in this directory:

- `samples/car.jpg`
- `samples/traffic.mp4`
