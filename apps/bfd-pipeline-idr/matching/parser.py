import re
import unicodedata
import usaddress
from .constants import STATES, SECONDARY_UNITS, SUFFIX_MAP, DIRECTIONALS, DIACRITICS


def remove_diacritics(text: str) -> str:
    # Explicit mapping based on PDF specifications.
    # The Project US@ spec varies from the fallback in a few instances, so we can expand the constant mapping.
    mapped_chars = []
    for char in text:
        if char in DIACRITICS:
            mapped_chars.append(DIACRITICS[char])
        else:
            # Fallback for characters not in manual map
            decomposed = unicodedata.normalize("NFKD", char)
            mapped_chars.append(decomposed[0] if decomposed else char)
    text = "".join(mapped_chars)
    return text.encode("ASCII", "ignore").decode("utf-8")


def _apply_smart_state_abbreviations(text: str) -> str:
    from .constants import STATES

    words = text.split()
    if not words:
        return text

    i = 0
    while i < len(words):
        word_upper = words[i].upper()
        # Check for 2-word state name (e.g. NORTH CAROLINA)
        two_word = ""
        if i + 1 < len(words):
            two_word = (word_upper + " " + words[i + 1].upper()).strip()

        if two_word in STATES:
            state = two_word
            skip = 2
        elif word_upper in STATES and len(word_upper) > 2:
            # Only single words with len > 2 to avoid abbreviations
            state = word_upper
            skip = 1
        else:
            i += 1
            continue

        state_abbr = STATES[state]
        next_i = i + skip
        if next_i < len(words):
            next_word = words[next_i].upper()
            if next_word.isdigit():
                if len(next_word) >= 5:
                    # Likely a ZIP code, skip highway expansion
                    i += skip
                    continue
                words[i] = state_abbr
                for _ in range(skip - 1):
                    words.pop(i + 1)
                # Insert HIGHWAY if not already explicitly present
                if "HIGHWAY" not in [w.upper() for w in words]:
                    words.insert(i + 1, "HIGHWAY")
                break
            elif next_word in (
                "HIGHWAY",
                "ROAD",
                "ROUTE",
                "FM",
                "HWY",
                "ST",
                "COUNTY",
                "STATE",
            ):
                words[i] = state_abbr
                for _ in range(skip - 1):
                    words.pop(i + 1)
                break
        i += skip
    return " ".join(words)


def _apply_highway_fixes(text: str) -> str:
    text = _apply_smart_state_abbreviations(text)
    replaces = [
        # Support (RFD|RD|RT|RR) [Optional Route] [Number] [BOX|#] [AlphaNum] [Trailing Text]
        (
            r"\b(RFD|RD|RT|RR)\s+(?:ROUTE\s+)?(\d+)\s+(?:BOX|#)\s?(\w+)\s+(.+)\b",
            r"RR \2 BOX \3\n\4",
        ),
        (
            r"\b(RFD|RD|RT|RR)\s+(?:ROUTE\s+)?(\d+)\s+(?:BOX|#)\s?(\w+)\b",
            r"RR \2 BOX \3",
        ),
        (r"\bCNTY HWY\b", "COUNTY HIGHWAY"),
        (r"\bCOUNTY HWY\b", "COUNTY HIGHWAY"),
        (r"\bCNTY RD\b", "COUNTY ROAD"),
        (r"\bCOUNTY RD\b", "COUNTY ROAD"),
        (r"\bFARM TO MARKET\b", "FM"),
        (r"\bFARM 2 MARKET\b", "FM"),
        (r"\bHWY FM\b", "FM"),
        (r"\bINTERSTATE HWY\b", "INTERSTATE"),
        (r"\bRANCH RD\b", "RANCH ROAD"),
        (r"\bST HIGHWAY\b", "STATE HIGHWAY"),
        (r"\bSTATE HWY\b", "STATE HIGHWAY"),
        (r"\bST HWY\b", "STATE HIGHWAY"),
        (r"\bST RD\b", "STATE ROAD"),
        (r"\bST RT\b", "STATE ROUTE"),
        (r"\bSTATE RTE\b", "STATE ROUTE"),
        (r"\bTOWNSHIP RD\b", "TOWNSHIP ROAD"),
        (r"\bUS HWY\b", "US HIGHWAY"),
        (r"\bIH(\d+)\b", r"INTERSTATE \1"),
        (r"\bI\s?(\d+)\b", r"INTERSTATE \1"),
        (r"\bUS (\d+)\b", r"US HIGHWAY \1"),
        (r"\bBYP ROAD\b", "BYPASS ROAD"),
        (r"\bKY (\d{1,4})\b", r"KY HIGHWAY \1"),
        (r"\bCNTY\b", "COUNTY"),
        (r"\bCR\b", "COUNTY ROAD"),
        (r"\bHWY\b", "HIGHWAY"),
        (r"\bRD\b", "ROAD"),
        (r"\bRT\b", "ROUTE"),
        (r"\bRTE\b", "ROUTE"),
        (r"\bSR ([A-Z]+)\b", r"STATE ROUTE \1"),
        (r"\bSR (\d+)\b", r"STATE ROAD \1"),
        (r"\bSR\b", "STATE ROAD"),
        (r"\bTSR\b", "TOWNSHIP ROAD"),
    ]
    for pattern, replacement in replaces:
        text = re.sub(pattern, replacement, text)
    return text


def normalize_text(text: str) -> str:
    text = remove_diacritics(text)
    text = text.upper()
    text = _apply_highway_fixes(text)

    # Mask floating point periods to preserve them (e.g. 39.2)
    text = re.sub(r"(\d)\.(\d)", r"\1_DOT_\2", text)

    # Remove specific punctuation: *, ., (, ), ", :, ;, ', &, @
    text = re.sub(r'[*.,()":;\'&@]', "", text)

    # Restore floating point periods
    text = text.replace("_DOT_", ".")
    # Compress spaces around hyphens
    text = re.sub(r"\s*-\s*", "-", text)
    # Compress multiple spaces into one
    text = re.sub(r"[^\S\r\n]+", " ", text).strip()
    return text


CANADIAN_POSTAL_CODE_PATTERN = r"\b[A-Z]\d[A-Z]\s?\d[A-Z]\d\b"
CANADIAN_PROVINCES = (
    "AB",
    "BC",
    "MB",
    "NB",
    "NL",
    "NT",
    "NS",
    "NU",
    "ON",
    "PE",
    "QC",
    "SK",
    "YT",
)


def _apply_canada_fixes(lines: list) -> list:
    """Combines Canadian Province and Postal Code with double spacing if separate."""
    postal_code = ""
    postal_idx = -1
    for i, line in enumerate(lines):
        match = re.search(CANADIAN_POSTAL_CODE_PATTERN, line.upper())
        if match:
            postal_code = match.group(0)
            postal_idx = i
            break

    if postal_code and postal_idx != -1:
        for j, line in enumerate(lines):
            if j != postal_idx:
                line_upper = line.upper()
                if any(
                    f" {prov}" in line_upper
                    or line_upper == prov
                    or line_upper.startswith(prov + " ")
                    for prov in CANADIAN_PROVINCES
                ):
                    # Combine with double space
                    lines[j] = lines[j].strip() + "  " + postal_code
                    lines.pop(postal_idx)
                    break
    return lines
    return lines


def normalize_address(address_str: str) -> str:
    """Takes a multi-line address string and converts it to Project US@ format."""
    # First, split lines and normalize each line
    lines = [normalize_text(line) for line in address_str.split("\n")]
    # Remove empty lines
    lines = [line for line in lines if line]

    # Pre-process Canadian addresses to combine Province and Postal Code with double spacing
    lines = _apply_canada_fixes(lines)

    # Re-split lines if any rule added a newline (e.g. rural route splits)
    full_text = "\n".join(lines)
    lines = [line for line in full_text.split("\n") if line]

    # Pre-calculate flags for line-by-line backwards layout compatibility context
    is_military = any(re.search(r"\b(AP|AE|AA)\b", line.upper()) for line in lines)
    is_pr = any(re.search(r"\bPR\b", line.upper()) for line in lines)

    formatted_lines = []
    highway_keywords = (
        "COUNTY HIGHWAY",
        "COUNTY ROAD",
        "INTERSTATE",
        "HIGHWAY",
        "FM",
        "US HIGHWAY",
        "STATE HIGHWAY",
        "ROUTE",
        "ROAD",
        "STATE ROUTE",
        "STATE ROAD",
        "KY HIGHWAY",
        "KY STATE HIGHWAY",
        "KY ",
        "URB ",
        "EXT ",
    )

    for line in lines:
        try:
            # If we combined Canada on this line, skip usaddress to preserve spacing
            if re.search(CANADIAN_POSTAL_CODE_PATTERN, line.upper()) and any(
                f" {prov}" in line.upper() for prov in CANADIAN_PROVINCES
            ):
                formatted_lines.append(line)
                continue

            if line.startswith(highway_keywords) or (
                is_pr
                and re.search(r"\b(STA|STATION)\b", line.upper())
                and "PO BOX" not in line.upper()
            ):
                raw_parsed = usaddress.parse(line)
                formatted_lines.append(_format_from_raw(raw_parsed))
                continue

            parsed_tokens, _ = usaddress.tag(line)
            fmt_string = _format_from_dict(parsed_tokens, is_military=is_military)
            if not fmt_string.strip() and line.strip():
                # Dictionary returned empty for a non-empty line, fallback to raw sequential
                raw_parsed = usaddress.parse(line)
                fmt_string = _format_from_raw(raw_parsed)
            formatted_lines.append(fmt_string)
        except usaddress.RepeatedLabelError as e:
            raw_parsed = e.parsed_string
            formatted_lines.append(_format_from_raw(raw_parsed))
        except Exception:
            formatted_lines.append(line)

    # Ensure Country is on the bottom line for International
    from .constants import COUNTRIES

    normalized_lines = []
    country_line = ""
    for line in formatted_lines:
        if line.strip().upper() in COUNTRIES:
            country_line = line
        else:
            normalized_lines.append(line)

    if country_line:
        normalized_lines.append(country_line)

    res = "\n".join(normalized_lines)

    # Only apply PR exceptions for Puerto Rico addresses to preserve Canada spacing
    is_pr = any(re.search(r"\bPR\b", line.upper()) for line in lines)
    if is_pr:
        return _apply_pr_exceptions(res)
    return res


def _format_directional(word: str) -> str:
    return DIRECTIONALS.get(word, word)


def _format_suffix(word: str) -> str:
    return SUFFIX_MAP.get(word, word)


def _format_secondary_unit(word: str) -> str:
    return SECONDARY_UNITS.get(word, word)


def _format_state(word: str) -> str:
    return STATES.get(word, word)


def _format_from_dict(tokens: dict, is_military: bool = False) -> str:
    from .constants import SECONDARY_UNITS

    # Standardize dictionary bad tags for non-unit strings ending with digits
    if (
        "OccupancyType" in tokens
        and "OccupancyIdentifier" in tokens
        and "AddressNumber" not in tokens
    ):
        orig_type = tokens["OccupancyType"].upper()
        if (
            orig_type not in SECONDARY_UNITS
            and orig_type not in SECONDARY_UNITS.values()
            and tokens["OccupancyIdentifier"].isdigit()
            and not tokens["OccupancyType"][0].isdigit()
        ):
            tokens["AddressNumber"] = tokens["OccupancyIdentifier"]
            tokens["StreetName"] = tokens["OccupancyType"]
            del tokens["OccupancyType"]
            del tokens["OccupancyIdentifier"]

    # Build up the address lines
    line1_parts = []
    line2_parts = []
    last_line_parts = []

    # Check for Business or Recipient/Building (e.g., URB)
    for key in ["Recipient", "BuildingName", "LandmarkName"]:
        if key in tokens:
            val = tokens[key].strip()
            line1_parts.append(val)
            # Do not break here because LandmarkName and Recipient could both be present, but usaddress usually gives one.
            # To be safe against duplicates, we only take the first of these that exists.
            break

    # Build Street Address Line
    street_parts = []

    # Handling PO Boxes / Rural Routes
    if "USPSBoxType" in tokens or "USPSBoxGroupType" in tokens:
        # Check for Military address to put UNIT/CMR etc. before BOX
        if (
            not is_military
            and tokens.get("OccupancyType") in ("UNIT", "CMR", "OMC", "PSC", "UMR")
            and tokens.get("StateName") in ("AP", "AE", "AA")
        ):
            is_military = True

        if is_military and tokens.get("OccupancyType") in (
            "UNIT",
            "CMR",
            "OMC",
            "PSC",
            "UMR",
        ):
            occ_val = _format_secondary_unit(tokens.pop("OccupancyType"))
            occ_id = tokens.pop("OccupancyIdentifier", "")
            street_parts.extend([occ_val, occ_id])

        group_type = tokens.get("USPSBoxGroupType", "")
        group_id = tokens.get("USPSBoxGroupID", "")
        box_type = tokens.get("USPSBoxType", "")
        box_id = tokens.get("USPSBoxID", "")

        # Format Group (e.g., RR, HC)
        if group_type:
            if group_type in ("RR", "RURAL ROUTE", "RFD", "RD", "RT"):
                group_type = "RR"
            elif group_type in ("HC", "HIGHWAY CONTRACT", "STAR ROUTE"):
                group_type = "HC"

            if group_id.isdigit():
                group_id = str(int(group_id))
            street_parts.extend([group_type, group_id])

        # Format Box (e.g., PO BOX, BOX)
        if box_type:
            if box_type in (
                "PO BOX",
                "POST OFFICE BOX",
                "CALLER",
                "FIRM CALLER",
                "BIN",
                "LOCKBOX",
                "DRAWER",
            ):
                box_type = "PO BOX"
            elif box_type == "BOX" and (group_type or is_military):
                box_type = "BOX"
            elif box_type == "BOX":
                box_type = "PO BOX"  # Standalone BOX typically implies PO BOX

            # Remove leading zeros from box ID
            if box_id.isdigit():
                box_id = str(int(box_id))

            street_parts.extend([box_type, box_id])

    # General Delivery
    elif (
        tokens.get("AddressNumber") == "GENERAL DELIVERY"
        or tokens.get("StreetName") == "GENERAL DELIVERY"
    ):
        street_parts.append("GENERAL DELIVERY")

    # Standard Street Address
    else:
        if "AddressNumber" in tokens:
            street_parts.append(tokens["AddressNumber"])
        if "AddressNumberSuffix" in tokens:
            street_parts.append(tokens["AddressNumberSuffix"])
        if "AddressNumberPrefix" in tokens:
            # AddressNumberPrefix usually goes before AddressNumber or after depending on format
            pass  # usaddress usually splits this strangely; we just use AddressNumber

        if "StreetNamePreModifier" in tokens:
            street_parts.append(tokens["StreetNamePreModifier"])

        if "StreetNamePreDirectional" in tokens:
            street_parts.append(_format_directional(tokens["StreetNamePreDirectional"]))

        if "StreetNamePreType" in tokens:
            # PreTypes like "COUNTY HIGHWAY" are typically NOT abbreviated in Project US@
            street_parts.append(tokens["StreetNamePreType"])

        if "StreetName" in tokens:
            street_parts.append(tokens["StreetName"])

        if "StreetNamePostType" in tokens:
            street_parts.append(_format_suffix(tokens["StreetNamePostType"]))

        if "StreetNamePostDirectional" in tokens:
            street_parts.append(
                _format_directional(tokens["StreetNamePostDirectional"])
            )

    # Secondary Unit
    if "OccupancyType" in tokens:
        occ_type = _format_secondary_unit(tokens["OccupancyType"])
        street_parts.append(occ_type)
        if "OccupancyIdentifier" in tokens:
            occ_id = (
                tokens["OccupancyIdentifier"].get_text()
                if hasattr(tokens["OccupancyIdentifier"], "get_text")
                else tokens["OccupancyIdentifier"]
            )
            if str(occ_id).startswith("#"):
                occ_id = str(occ_id)[1:].strip()
            street_parts.append(occ_id)
    elif "OccupancyIdentifier" in tokens:
        occ_id = (
            tokens["OccupancyIdentifier"].get_text()
            if hasattr(tokens["OccupancyIdentifier"], "get_text")
            else tokens["OccupancyIdentifier"]
        )
        street_parts.append(occ_id)

    # Private Mailbox (PMB)
    if "SubaddressType" in tokens:
        sub_type = tokens["SubaddressType"].strip()
        if sub_type in ("PMB", "PRIVATE MAILBOX"):
            sub_type = "PMB"
        # PDF says PMB or # identifier may be used
        street_parts.append(sub_type)
        if "SubaddressIdentifier" in tokens:
            street_parts.append(tokens["SubaddressIdentifier"])

    # City, State, ZIP — Moved up for boundary standalone passes
    if "StateName" in tokens:
        state = _format_state(tokens["StateName"])
        if "PlaceName" in tokens:
            last_line_parts.append(state)
        else:
            street_parts.insert(0, state)

    if "ZipCode" in tokens:
        if "PlaceName" in tokens:
            last_line_parts.append(tokens["ZipCode"])
        else:
            street_parts.append(tokens["ZipCode"])

    if street_parts:
        line2_str = " ".join(street_parts).strip()
        line2_parts.append(line2_str)

    if "PlaceName" in tokens:
        last_line_parts.insert(0, tokens["PlaceName"])

    # Combine lines
    final_lines = []
    if line1_parts:
        final_lines.append(_apply_pr_exceptions(" ".join(line1_parts)))
    if line2_parts:
        final_lines.append(_apply_pr_exceptions(" ".join(line2_parts)))
    if last_line_parts:
        # Construct City State Zip carefully
        city = last_line_parts[0] if len(last_line_parts) > 0 else ""
        state = last_line_parts[1] if len(last_line_parts) > 1 else ""
        zip_code = last_line_parts[2] if len(last_line_parts) > 2 else ""

        last_line = ""
        if city:
            last_line += city

        # Handle Canada spacing
        is_canada = False
        if state:
            state_parts = state.split()
            if state_parts[0] in (
                "AB",
                "BC",
                "MB",
                "NB",
                "NL",
                "NT",
                "NS",
                "NU",
                "ON",
                "PE",
                "QC",
                "SK",
                "YT",
            ):
                is_canada = True
                if len(state_parts) > 1:
                    # usaddress merged part of Zip into StateName
                    # e.g., 'ON K1A'
                    state = state_parts[0]
                    zip_code = " ".join(state_parts[1:]) + (
                        " " + zip_code if zip_code else ""
                    )

            last_line += (" " if last_line else "") + state

        if is_canada:
            if zip_code:
                last_line += ("  " if last_line else "") + zip_code
        else:
            if zip_code:
                last_line += (" " if last_line else "") + zip_code
        final_lines.append(last_line)

    return "\n".join(final_lines)


def _format_from_raw(raw_parsed: list) -> str:
    # For complex/failing parses, we recreate the string processing parts manually mapped to basic rules
    # This is a fallback

    # smart swap numbers to front for address lines ending with digit (e.g., ACHENSEEWEG 25)
    if raw_parsed:
        from .constants import SECONDARY_UNITS

        v_last, l_last = raw_parsed[-1]
        if v_last.isdigit() and len(raw_parsed) > 1 and l_last != "ZipCode":
            highway_keywords = (
                "HIGHWAY",
                "ROAD",
                "ROUTE",
                "FM",
                "US",
                "INTERSTATE",
                "SR",
                "ST",
                "COUNTY",
                "STATE",
            )
            can_swap = True
            for v, _label in raw_parsed[:-1]:
                v_up = v.upper()
                if v_up in SECONDARY_UNITS or v_up in highway_keywords:
                    can_swap = False
                    break
            if can_swap and not any(v.isdigit() for v, _label in raw_parsed[:-1]):
                raw_parsed = [raw_parsed[-1]] + raw_parsed[:-1]

    reconstructed = []
    for val, label in raw_parsed:
        # apply directional checks
        if "Directional" in label:
            val = _format_directional(val)
        # apply suffix checks
        if "PostType" in label or label == "StreetNamePostType":
            val = _format_suffix(val)
        # apply state checks
        if label == "StateName":
            val = _format_state(val)
        # apply secondary unit
        if label == "OccupancyType":
            val = _format_secondary_unit(val)
        if label == "SubaddressType":
            if val.upper() in ("PMB", "PRIVATE MAILBOX"):
                val = "PMB"
        reconstructed.append(val)

    # Reassemble and try to figure out lines based on newlines that were in the original
    # usaddress strips \n from values sometimes, so we'll just join with space and do a naive newline check
    final_raw = (
        " ".join(reconstructed)
        .replace(" \n ", "\n")
        .replace("\n ", "\n")
        .replace(" \n", "\n")
    )
    lines = final_raw.split("\n")
    return "\n".join(_apply_pr_exceptions(line) for line in lines)


def _apply_pr_exceptions(text: str) -> str:
    from .constants import PR_URBANIZATION_EXCEPTIONS

    # Reorder Station lines to be ABOVE delivery lines if they are below (e.g., Sta below PO Box)
    lines = text.split("\n")
    for i in range(len(lines) - 1):
        if re.search(r"\bPO BOX\b", lines[i].upper()) and re.search(
            r"\b(STA|STATION)\b", lines[i + 1].upper()
        ):
            lines[i], lines[i + 1] = lines[i + 1], lines[i]
            break
    text = "\n".join(lines)

    # Standardize Spanish boxes on the combined text (e.g., APARTADO -> PO BOX)
    text = re.sub(r"\b(APARTADO|APDO|GPO BOX)\b", "PO BOX", text, flags=re.IGNORECASE)

    lines = text.split("\n")
    final_lines = []
    for line in lines:
        line_str = line.strip()
        # Remove hyphens from alphanumeric house numbers at the absolute front (e.g., A-17 -> A17)
        line_str = re.sub(r"^([A-Z]{1,2})-(\d+[A-Z]?)\b", r"\1\2", line_str)
        words = line_str.split()
        new_words = []
        skip = False
        for i, w in enumerate(words):
            if skip:
                skip = False
                continue
            w_upper = w.upper()
            if w_upper in ("URB", "URBANIZATION") and i + 1 < len(words):
                next_word = words[i + 1].upper()
                if (
                    next_word in PR_URBANIZATION_EXCEPTIONS
                    or next_word in PR_URBANIZATION_EXCEPTIONS.values()
                ):
                    mapped_val = PR_URBANIZATION_EXCEPTIONS.get(next_word, next_word)
                    new_words.append(mapped_val)
                    skip = True
                    continue
            if w_upper in PR_URBANIZATION_EXCEPTIONS:
                w = PR_URBANIZATION_EXCEPTIONS[w_upper]
            elif w_upper in PR_URBANIZATION_EXCEPTIONS.values():
                w = w_upper
            elif w_upper == "URBANIZATION":
                w = "URB"
            new_words.append(w)
        final_lines.append(" ".join(new_words))
    return "\n".join(final_lines)
