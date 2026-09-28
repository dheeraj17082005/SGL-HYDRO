import re

class PlateNormalizer:
    """
    Indian Vehicle Registration Plate Normalizer & Structure Validator.
    Applies context-aware OCR error correction and validates against standard
    Indian vehicle registration number patterns.
    """

    # Comprehensive list of Indian States & Union Territories
    STATE_CODES = {
        "AN", "AP", "AR", "AS", "BR", "CG", "CH", "DD", "DL", "DN", "GA",
        "GJ", "HR", "HP", "JH", "JK", "KA", "KL", "LA", "LD", "MH", "ML",
        "MN", "MP", "MZ", "NL", "OD", "OR", "PB", "PY", "RJ", "SK", "TN",
        "TR", "TS", "UK", "UP", "WB", "BH"
    }

    CHAR_TO_NUM = {
        'O': '0', 'Q': '0', 'D': '0',
        'I': '1', 'L': '1', 'T': '1',
        'Z': '2',
        'S': '5',
        'B': '8',
        'G': '6'
    }

    NUM_TO_CHAR = {
        '0': 'O',
        '1': 'I',
        '2': 'Z',
        '5': 'S',
        '8': 'B',
        '6': 'G'
    }

    @classmethod
    def normalize(cls, text: str) -> str:
        """
        Cleans and normalizes registration plate text.
        Converts to uppercase, strips hyphens/spaces, and applies positional OCR corrections.
        """
        if not text:
            return ""

        # Uppercase & strip non-alphanumeric
        cleaned = re.sub(r'[^A-Za-z0-9]', '', text.strip().upper())

        # Strip HSRP country prefix ('IND', '1ND') if present before a valid state code
        for prefix in ("IND", "1ND", "IND0"):
            if cleaned.startswith(prefix) and len(cleaned) >= len(prefix) + 8:
                rem = cleaned[len(prefix):]
                state_candidate = cls.NUM_TO_CHAR.get(rem[0], rem[0]) + cls.NUM_TO_CHAR.get(rem[1], rem[1])
                if state_candidate in cls.STATE_CODES:
                    cleaned = rem
                    break

        if len(cleaned) < 4:
            return cleaned

        chars = list(cleaned)

        # Positional Correction: Position 0..1 must be State Code (Letters)
        if len(chars) >= 2:
            chars[0] = cls.NUM_TO_CHAR.get(chars[0], chars[0])
            chars[1] = cls.NUM_TO_CHAR.get(chars[1], chars[1])

        # Positional Correction: Position 2..3 must be RTO Code (Digits)
        if len(chars) >= 4:
            chars[2] = cls.CHAR_TO_NUM.get(chars[2], chars[2])
            chars[3] = cls.CHAR_TO_NUM.get(chars[3], chars[3])

        # Positional Correction: Last 4 characters must be Vehicle Sequence (Digits)
        if len(chars) >= 8:
            for i in range(len(chars) - 4, len(chars)):
                chars[i] = cls.CHAR_TO_NUM.get(chars[i], chars[i])

        return "".join(chars)

    @classmethod
    def validate_registration_structure(cls, text: str) -> tuple[bool, str, str]:
        """
        Validates whether normalized text matches expected Indian registration structure.
        Returns: (is_valid, reason, normalized_text)
        """
        if not text:
            return False, "EMPTY_TEXT", ""

        normalized = cls.normalize(text)

        # Length check (Standard Indian plates are 8 to 10 characters long)
        if len(normalized) < 8 or len(normalized) > 10:
            return False, f"INVALID_LENGTH ({len(normalized)})", normalized

        # State code prefix check
        state_prefix = normalized[:2]
        if state_prefix not in cls.STATE_CODES:
            return False, f"INVALID_STATE_PREFIX ({state_prefix})", normalized

        # Regex format validation:
        # Standard: SS DD AA NNNN, SS D AA NNNN, or BH DD AAAA NNNN
        pattern = r'^[A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{4}$'
        if not re.match(pattern, normalized):
            return False, "INVALID_FORMAT_PATTERN", normalized

        return True, "VALID", normalized
