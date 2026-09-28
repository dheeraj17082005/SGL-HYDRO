import pytest
from app.pipeline.normalizer import PlateNormalizer
from app.pipeline.anpr_pipeline import AnprPipeline

def test_validate_registration_structure():
    # Valid Indian registration numbers
    is_valid, reason, text = PlateNormalizer.validate_registration_structure("GJ 01 AB 1234")
    assert is_valid is True
    assert text == "GJ01AB1234"

    is_valid, reason, text = PlateNormalizer.validate_registration_structure("mh-12-de-5678")
    assert is_valid is True
    assert text == "MH12DE5678"

    is_valid, reason, text = PlateNormalizer.validate_registration_structure("DL3CCE9999")
    assert is_valid is True

    # Invalid state code prefix
    is_valid, reason, text = PlateNormalizer.validate_registration_structure("XX01AB1234")
    assert is_valid is False
    assert "INVALID_STATE_PREFIX" in reason

    # Invalid length
    is_valid, reason, text = PlateNormalizer.validate_registration_structure("GJ01A")
    assert is_valid is False
    assert "INVALID_LENGTH" in reason

def test_backend_cooldown():
    pipeline = AnprPipeline(cooldown_seconds=30.0, send_to_backend=False)
    # Register dispatched plate
    pipeline.last_dispatched["GJ01AB1234"] = 9999999999.0 # future timestamp
    dispatched, resp = pipeline._dispatch_to_backend("GJ01AB1234", station_id=1, camera_id=1)
    # Should suppress due to active cooldown
    assert dispatched is False
    assert resp.get("cooldown") is True

