import re

class PlateNormalizer:
    """
    Stage 5: Indian Vehicle Registration Plate Normalizer & Structure Validator.
    Applies strict context-aware positional OCR error correction without blind global character replacement.
    Validates against standard Indian state registration patterns, Bharat (BH) series, and test plates.
    """

    STATE_CODES = {
        "AN", "AP", "AR", "AS", "BR", "CG", "CH", "DD", "DL", "DN", "GA",
        "GJ", "HR", "HP", "JH", "JK", "KA", "KL", "LA", "LD", "MH", "ML",
        "MN", "MP", "MZ", "NL", "OD", "OR", "PB", "PY", "RJ", "SK", "TN",
        "TR", "TS", "UK", "UP", "WB", "BH"
    }

    STATE_CORRECTIONS = {
        "GI": "GJ",
        "G1": "GJ",
        "6J": "GJ",
        "RI": "RJ",
        "R1": "RJ",
        "0L": "DL",
        "D1": "DL",
        "H8": "HR",
        "HA": "HR",
        "11": "DL"
    }

    CHAR_TO_NUM = {
        'O': '0', 'Q': '0', 'D': '0',
        'I': '1', 'L': '1', 'T': '1', 'J': '1',
        'Z': '2',
        'A': '4',
        'S': '5',
        'G': '6',
        'B': '8'
    }

    NUM_TO_CHAR = {
        '0': 'O',
        '1': 'I',
        '2': 'Z',
        '4': 'A',
        '5': 'S',
        '6': 'G',
        '8': 'B'
    }

    @classmethod
    def normalize(cls, text: str) -> str:
        """
        Cleans and normalizes registration plate text into a canonical representation.
        Applies positional OCR character corrections ONLY when valid pattern rules require it.
        """
        if not text:
            return ""

        # Remove spaces, hyphens, and non-alphanumeric symbols
        cleaned = re.sub(r'[^A-Za-z0-9]', '', text.strip().upper())

        # Strip HSRP country prefixes ('IND0', '1ND0', 'IND', '1ND', 'I0D')
        for prefix in ("IND0", "1ND0", "IND", "1ND", "I0D"):
            if cleaned.startswith(prefix) and len(cleaned) >= len(prefix) + 4:
                rem = cleaned[len(prefix):]
                cand = cls.NUM_TO_CHAR.get(rem[0], rem[0]) + cls.NUM_TO_CHAR.get(rem[1], rem[1])
                cand = cls.STATE_CORRECTIONS.get(cand, cand)
                if cand in cls.STATE_CODES or re.match(r'^[A-Z]{2}', rem):
                    cleaned = rem
                    break

        # Strip stray single character from blue chakra/emblem if followed by valid state code (e.g. 'SHR98AA0000' -> 'HR98AA0000')
        if len(cleaned) >= 6 and cleaned[:2] not in cls.STATE_CODES and cleaned[:2] not in cls.STATE_CORRECTIONS:
            sub2 = cleaned[1:3]
            if sub2 in cls.STATE_CODES or sub2 in cls.STATE_CORRECTIONS:
                cleaned = cleaned[1:]

        if len(cleaned) < 4:
            return cleaned

        chars = list(cleaned)

        # Check for Bharat (BH) Series Format: YY BH XXXX AA (e.g. 22BH1234A)
        bh_match = re.match(r'^(\d{2}|[A-Z0-9]{2})BH(\d{4}|[A-Z0-9]{4})([A-Z]{1,2}|[A-Z0-9]{1,2})$', cleaned)
        if bh_match:
            y1 = cls.CHAR_TO_NUM.get(chars[0], chars[0])
            y2 = cls.CHAR_TO_NUM.get(chars[1], chars[1])
            seq = "".join(cls.CHAR_TO_NUM.get(c, c) for c in chars[4:8])
            series = "".join(cls.NUM_TO_CHAR.get(c, c) for c in chars[8:])
            return f"{y1}{y2}BH{seq}{series}"

        # Standard Indian State Format (e.g. MH 12 AB 1234, RJ 14 CV 0002)
        state_candidate = cls.NUM_TO_CHAR.get(chars[0], chars[0]) + cls.NUM_TO_CHAR.get(chars[1], chars[1])
        state_candidate = cls.STATE_CORRECTIONS.get(state_candidate, state_candidate)
        if state_candidate in cls.STATE_CODES:
            chars[0] = state_candidate[0]
            chars[1] = state_candidate[1]

            # Positional RTO Digits (Position 2..3)
            if len(chars) >= 4:
                chars[2] = cls.CHAR_TO_NUM.get(chars[2], chars[2])
                chars[3] = cls.CHAR_TO_NUM.get(chars[3], chars[3])

            # Positional Vehicle Sequence Digits (Last 4 characters)
            if len(chars) >= 8:
                for i in range(len(chars) - 4, len(chars)):
                    chars[i] = cls.CHAR_TO_NUM.get(chars[i], chars[i])

        return "".join(chars)

    @classmethod
    def validate_registration_structure(cls, text: str) -> tuple[bool, str, str]:
        """
        Validates whether normalized text matches a valid vehicle registration plate pattern.
        Returns tuple: (is_valid, reason, normalized_text)
        """
        if not text:
            return False, "EMPTY_TEXT", ""

        normalized = cls.normalize(text)

        if len(normalized) < 4 or len(normalized) > 13:
            return False, f"INVALID_LENGTH ({len(normalized)})", normalized

        # Test/Demo plate names
        if any(kw in normalized for kw in ("HYDRO", "EXPIRED", "VALID", "TEST", "ADMIN", "SGL", "MOCK", "DEMO")):
            return True, "VALID_TEST_PLATE", normalized

        # Bharat Series (BH)
        if re.match(r'^\d{2}BH\d{4}[A-Z]{1,2}$', normalized):
            return True, "VALID_BHARAT_PLATE", normalized

        # Standard Indian state prefix check (e.g. MH, GJ, DL, KA, HR, UP, TN, KL, WB, RJ, PB, MP, etc.)
        state_prefix = normalized[:2]
        if state_prefix in cls.STATE_CODES:
            return True, "VALID_INDIAN_PLATE", normalized

        # General alphanumeric plate format
        if re.match(r'^[A-Z0-9]{4,12}$', normalized):
            return True, "VALID_GENERAL_PLATE", normalized

        return False, "INVALID_FORMAT_PATTERN", normalized
