import argparse
import json
import os
import sys
import cv2
import uvicorn
from app.pipeline.anpr_pipeline import AnprPipeline

def main():
    parser = argparse.ArgumentParser(description="SGL CNG AI - Production ANPR Service CLI & Stream Processor")
    parser.add_argument("--image", type=str, help="Path to input image file")
    parser.add_argument("--video", type=str, help="Path to input video file")
    parser.add_argument("--rtsp", type=str, default=os.getenv("RTSP_URL"), help="RTSP stream URL (rtsp://...) [env: RTSP_URL]")
    parser.add_argument("--webcam", action="store_true", help="Capture from default webcam")
    parser.add_argument("--camera", type=int, nargs="?", const=0, default=None, help="Run interactive webcam debugger with camera index (e.g. --camera 0)")
    parser.add_argument("--interactive", action="store_true", help="Run interactive OpenCV HUD GUI window")
    parser.add_argument("--send-backend", action="store_true", default=os.getenv("SEND_BACKEND", "true").lower() in ("true", "1", "yes"), help="Send verified detections to Spring Boot backend [env: SEND_BACKEND]")
    parser.add_argument("--backend-url", type=str, default=os.getenv("BACKEND_URL", "http://localhost:8080"), help="Spring Boot backend URL [env: BACKEND_URL]")
    parser.add_argument("--station-id", type=int, default=int(os.getenv("STATION_ID", "1")), help="CNG Station ID [env: STATION_ID]")
    parser.add_argument("--camera-id", type=int, default=int(os.getenv("CAMERA_ID", "1")), help="ANPR Camera ID [env: CAMERA_ID]")
    parser.add_argument("--frame-skip", type=int, default=int(os.getenv("FRAME_SKIP", "5")), help="Frame skip interval (process every Nth frame) [env: FRAME_SKIP]")
    parser.add_argument("--cooldown-seconds", type=float, default=float(os.getenv("COOLDOWN_SECONDS", "30.0")), help="Cooldown seconds for duplicate suppression [env: COOLDOWN_SECONDS]")
    parser.add_argument("--max-frames", type=int, default=None, help="Max frames to process before exiting")
    parser.add_argument("--debug", action="store_true", help="Generate annotated debug image")
    parser.add_argument("--diagnose", action="store_true", help="Run 7-stage diagnostic pipeline and output detailed report")
    parser.add_argument("--server", action="store_true", help="Launch FastAPI server")
    parser.add_argument("--port", type=int, default=8001, help="FastAPI server port (default 8001)")

    args = parser.parse_args()

    if args.diagnose:
        from diagnose_pipeline import run_diagnostics
        src = args.image or args.video or (args.camera if args.camera is not None else 0)
        run_diagnostics(source=src, backend_url=args.backend_url, station_id=args.station_id, camera_id=args.camera_id)
        return

    if args.server:
        print(f"Starting SGL CNG ANPR API Server on port {args.port}...")
        uvicorn.run("app.api.server:app", host="0.0.0.0", port=args.port, reload=False)
        return

    pipeline = AnprPipeline(
        backend_url=args.backend_url,
        station_id=args.station_id,
        camera_id=args.camera_id,
        send_to_backend=args.send_backend,
        frame_skip=args.frame_skip,
        cooldown_seconds=args.cooldown_seconds
    )

    if args.camera is not None:
        pipeline.run_interactive_webcam(camera_index=args.camera, station_id=args.station_id, camera_id=args.camera_id)

    elif args.image:
        if not os.path.exists(args.image):
            print(json.dumps({"error": f"Image file not found: {args.image}"}))
            sys.exit(1)

        result, debug_img = pipeline.process_image(args.image, generate_debug=args.debug)
        print(json.dumps(result, indent=2))

        if args.debug and debug_img is not None:
            debug_path = "debug_output.jpg"
            cv2.imwrite(debug_path, debug_img)
            print(f"Saved debug image to: {debug_path}")

    elif args.video or args.rtsp or args.webcam:
        if args.interactive and args.webcam:
            pipeline.run_interactive_webcam(camera_index=0, station_id=args.station_id, camera_id=args.camera_id)
        else:
            source = 0 if args.webcam else (args.rtsp if args.rtsp else args.video)
            if isinstance(source, str) and not source.startswith("rtsp://") and not os.path.exists(source):
                print(json.dumps({"error": f"Video source file not found: {source}"}))
                sys.exit(1)

            result = pipeline.process_stream(
                source=source,
                frame_skip=args.frame_skip,
                station_id=args.station_id,
                camera_id=args.camera_id,
                max_frames=args.max_frames
            )
            print(json.dumps(result, indent=2))

    else:
        parser.print_help()

if __name__ == "__main__":
    main()
