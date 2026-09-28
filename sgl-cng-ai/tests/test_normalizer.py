import pytest
from app.pipeline.normalizer import PlateNormalizer

def test_normalize_basic_formatting():
    assert PlateNormalizer.normalize("gj 01 ab 1234") == "GJ01AB1234"
    assert PlateNormalizer.normalize("mh-12-de-5678") == "MH12DE5678"
    assert PlateNormalizer.normalize("  dl 3c ce 9999  ") == "DL3CCE9999"

def test_normalize_ocr_positional_fixes():
    # 0 in state position fixed to O
    assert PlateNormalizer.normalize("g001ab1234") == "GJ01AB1234" or PlateNormalizer.normalize("G001AB1234") == "GO01AB1234"
    # O in RTO position fixed to 0
    assert PlateNormalizer.normalize("GJ0OAB1234") == "GJ00AB1234"
    # O in numeric suffix fixed to 0
    assert PlateNormalizer.normalize("GJ01AB123O") == "GJ01AB1230"

def test_normalize_empty_or_invalid():
    assert PlateNormalizer.normalize("") == ""
    assert PlateNormalizer.normalize(None) == ""
    assert PlateNormalizer.normalize("ABC") == "ABC"
