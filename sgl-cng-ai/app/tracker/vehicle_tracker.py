import numpy as np
import time

class VehicleTracker:
    """
    Stage 2: Persistent Vehicle Multi-Object Tracker.
    Assigns persistent track IDs across consecutive video/RTSP frames based on IoU (Intersection over Union)
    and centroid distance matching.
    """

    def __init__(self, max_disappeared=15, iou_threshold=0.30):
        self.next_id = 101
        self.tracked_objects = {} # track_id -> {"bbox": (x,y,w,h), "last_seen": timestamp, "disappeared": count}
        self.max_disappeared = max_disappeared
        self.iou_threshold = iou_threshold

    def update(self, detected_bboxes):
        """
        Updates tracking IDs for a list of detected bounding boxes (x, y, w, h).
        Returns list of tuples: (track_id, bbox)
        """
        now = time.time()
        updated_tracks = []

        if not detected_bboxes:
            # Increment disappeared counter for all tracks
            to_delete = []
            for track_id, data in self.tracked_objects.items():
                data["disappeared"] += 1
                if data["disappeared"] > self.max_disappeared:
                    to_delete.append(track_id)
            for tid in to_delete:
                del self.tracked_objects[tid]
            return []

        if not self.tracked_objects:
            # Initialize tracks for all detections
            for bbox in detected_bboxes:
                tid = self.next_id
                self.next_id += 1
                self.tracked_objects[tid] = {"bbox": bbox, "last_seen": now, "disappeared": 0}
                updated_tracks.append((tid, bbox))
            return updated_tracks

        # Match new bboxes to existing tracks using IoU
        existing_ids = list(self.tracked_objects.keys())
        existing_bboxes = [self.tracked_objects[tid]["bbox"] for tid in existing_ids]

        iou_matrix = np.zeros((len(existing_bboxes), len(detected_bboxes)), dtype=np.float32)
        for i, eb in enumerate(existing_bboxes):
            for j, db in enumerate(detected_bboxes):
                iou_matrix[i, j] = self._calculate_iou(eb, db)

        matched_existing = set()
        matched_detected = set()

        # Greedy match highest IoU
        if iou_matrix.size > 0:
            flat_indices = np.argsort(-iou_matrix.ravel())
            for idx in flat_indices:
                i, j = np.unravel_index(idx, iou_matrix.shape)
                if i in matched_existing or j in matched_detected:
                    continue
                if iou_matrix[i, j] >= self.iou_threshold:
                    tid = existing_ids[i]
                    bbox = detected_bboxes[j]
                    self.tracked_objects[tid] = {"bbox": bbox, "last_seen": now, "disappeared": 0}
                    updated_tracks.append((tid, bbox))
                    matched_existing.add(i)
                    matched_detected.add(j)

        # Handle unmatched existing tracks
        for i, tid in enumerate(existing_ids):
            if i not in matched_existing:
                self.tracked_objects[tid]["disappeared"] += 1

        # Delete expired tracks
        to_delete = [tid for tid, data in self.tracked_objects.items() if data["disappeared"] > self.max_disappeared]
        for tid in to_delete:
            del self.tracked_objects[tid]

        # Handle new unmatched detections
        for j, bbox in enumerate(detected_bboxes):
            if j not in matched_detected:
                tid = self.next_id
                self.next_id += 1
                self.tracked_objects[tid] = {"bbox": bbox, "last_seen": now, "disappeared": 0}
                updated_tracks.append((tid, bbox))

        return updated_tracks

    def _calculate_iou(self, boxA, boxB):
        xA = max(boxA[0], boxB[0])
        yA = max(boxA[1], boxB[1])
        xB = min(boxA[0] + boxA[2], boxB[0] + boxB[2])
        yB = min(boxA[1] + boxA[3], boxB[1] + boxB[3])

        interArea = max(0, xB - xA) * max(0, yB - yA)
        boxAArea = boxA[2] * boxA[3]
        boxBArea = boxB[2] * boxB[3]

        denom = float(boxAArea + boxBArea - interArea)
        return interArea / denom if denom > 0 else 0.0
