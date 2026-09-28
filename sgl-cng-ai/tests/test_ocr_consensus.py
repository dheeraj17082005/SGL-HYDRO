import numpy as np
from app.ocr.ocr_engine import OcrEngine


def test_ocr_variants_choose_consensus_after_normalization():
    engine = OcrEngine.__new__(OcrEngine)
    readings = iter([
        ("INDGJ01AB1234", 0.78),
        ("GJ01AB1234", 0.70),
        ("GJ01AB1238", 0.98),
        ("GJ01AB1234", 0.73),
        ("GJ01AB1234", 0.69),
    ])
    engine.read_text = lambda _image: next(readings)

    crop = np.full((40, 160, 3), 255, dtype=np.uint8)
    raw, confidence, _winning_image, candidates = engine.read_text_multi_variant(crop)

    assert len(candidates) == 5
    assert raw in {"INDGJ01AB1234", "GJ01AB1234"}
    assert confidence >= 0.69
    assert candidates[0]["normalizedText"] == "GJ01AB1234"
