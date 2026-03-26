from __future__ import annotations

import re
import typing
import unicodedata
from collections.abc import Callable

import usaddress  # type: ignore

from .constants import (
    COUNTRIES,
    DIACRITICS,
    DIRECTIONALS,
    PR_URBANIZATION_EXCEPTIONS,
    SECONDARY_UNITS,
    STATES,
    SUFFIX_MAP,
)


def remove_diacritics(text: str) -> str:
    """Remove diacritics from text based on Project US@ specifications."""
    mapped_chars: list[str] = []
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
    """Intelligently abbreviate state names when followed by Highway/Route keywords."""
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
            if next_word in (
                "HIGHWAY",
                "ROUTE",
                "FM",
                "HWY",
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
    """Normalize highway and route notations into standard Project US@ format."""
    text = _apply_smart_state_abbreviations(text)

    def repl_interstate(match: re.Match[str]) -> str:
        full = match.group(0)
        idx = match.start()
        if idx > 0:
            before = text[:idx].strip()
            words = before.split()
            if words:
                last_word = words[-1].upper()
                if last_word in SECONDARY_UNITS or last_word in SECONDARY_UNITS.values():
                    # Likely a secondary unit ID (eg APT I1), not an Interstate highway
                    return full
        return f"INTERSTATE {match.group(1)}"

    replaces: list[tuple[str, str | Callable[[re.Match[str]], str]]] = [
        (r"\bBX\s+(\d+)\b", r"BOX \1"),
        (r"\b(RR|HC|RFD|RD|RT)(\d+)\b", r"\1 \2"),
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
        (r"\bBUZON\b", "BOX"),
        (r"\bBZN\b", "BOX"),
        (r"\bB0X\b", "BOX"),
        (r"\bRUTA RURAL\b", "RR"),
        (r"\bIH(\d+[A-Z]?)\b", r"INTERSTATE \1"),
        (r"\bI\s?(\d+[A-Z]?)(?:\s+(?:HIGHWAY|HWY))?\b", repl_interstate),
        (r"\bUS(?:\s+HIGHWAY)?\s+(\d+[A-Z]?)(?:\s+(?:HIGHWAY|HWY))?\b", r"US HIGHWAY \1"),
        (r"\bBYP ROAD\b", "BYPASS ROAD"),
        (r"\bKY\s+(\d{1,4})(?:\s+(?:HIGHWAY|HWY))?\b", r"KY HIGHWAY \1"),
        (r"\bCNTY\b", "COUNTY"),
        (r"\bCR\s+(\d+[A-Z]?)(?:\s+(?:ROAD|RD))?\b", r"COUNTY ROAD \1"),
        (r"\bHWY\b", "HIGHWAY"),
        (r"\bRD\b", "ROAD"),
        (r"\bRT\b", "ROUTE"),
        (r"\bRTE\b", "ROUTE"),
        (r"\bSR\s+([A-Z]+)(?:\s+(?:ROUTE|RT|RTE|ROAD|RD))?\b", r"STATE ROUTE \1"),
        (r"\bSR\s+(\d+[A-Z]?)(?:\s+(?:ROAD|RD|ROUTE|RT|RTE))?\b", r"STATE ROAD \1"),
        (r"\bSR\b", "STATE ROAD"),
        (r"\bTSR\b", "TOWNSHIP ROAD"),
    ]
    for pattern, replacement in replaces:
        text = re.sub(pattern, replacement, text)
    return text


def normalize_text(text: str) -> str:
    """Uppercase text and normalize basic punctuation/spacing."""
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
    return re.sub(r"[^\S\r\n]+", " ", text).strip()


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


def _apply_canada_fixes(lines: list[str]) -> list[str]:
    """Combine Canadian Province and Postal Code with single spacing if separate."""
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
                    lines[j] = lines[j].strip() + " " + postal_code
                    lines.pop(postal_idx)
                    break
    return lines


def normalize_address(address_str: str) -> str:
    """Take a multi-line address string and convert it to Project US@ format."""
    # First, split lines and normalize each line
    lines = [normalize_text(line) for line in address_str.split("\n")]
    # Remove empty lines and deduplicate
    deduped_lines: list[str] = []
    for line in lines:
        if line and line not in deduped_lines:
            deduped_lines.append(line)
    lines = deduped_lines

    # Pre-process Canadian addresses to combine Province and Postal Code with single spacing
    lines = _apply_canada_fixes(lines)

    # Re-split lines if any rule added a newline (e.g. rural route splits)
    full_text = "\n".join(lines)
    lines = [line for line in full_text.split("\n") if line]

    # Pre-calculate flags for line-by-line backwards layout compatibility context
    is_military = any(re.search(r"\b(AP|AE|AA)\b", line.upper()) for line in lines)
    is_pr = any(re.search(r"\bPR\b", line.upper()) for line in lines)

    formatted_lines: list[str] = []
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
        "URB ",
        "EXT ",
    )

    for line in lines:
        try:
            if line.startswith(highway_keywords) or (
                is_pr
                and re.search(r"\b(STA|STATION)\b", line.upper())
                and "PO BOX" not in line.upper()
            ):
                raw_parsed: list[tuple[str, str]] = usaddress.parse(line)
                formatted_lines.append(_format_from_raw(raw_parsed))
                continue

            parsed_tokens, addr_type = usaddress.tag(line)  # type: ignore

            # Check if line contains a Canadian Province
            is_canada_line = False
            if "StateName" in parsed_tokens:
                state_val = parsed_tokens["StateName"].upper().split()[0]
                if state_val in CANADIAN_PROVINCES:
                    is_canada_line = True

            if (
                (addr_type == "Ambiguous" and not is_canada_line)
                or (
                    "StreetName" not in parsed_tokens
                    and "USPSBoxType" not in parsed_tokens
                    and "USPSBoxGroupType" not in parsed_tokens
                )
            ):
                raw_parsed_fallback: list[tuple[str, str]] = usaddress.parse(line)
                fmt_string = _format_from_raw(raw_parsed_fallback)
            else:
                fmt_string = _format_from_dict(parsed_tokens, is_military=is_military)
            if not fmt_string.strip() and line.strip():
                # Dictionary returned empty for a non-empty line, fallback to raw sequential
                raw_parsed_fallback: list[tuple[str, str]] = usaddress.parse(line)
                fmt_string = _format_from_raw(raw_parsed_fallback)
            formatted_lines.append(fmt_string)
        except Exception as e:
            if "RepeatedLabelError" in str(type(e)):
                raw_parsed_err: list[tuple[str, str]] = getattr(e, "parsed_string", [])
                formatted_lines.append(_format_from_raw(raw_parsed_err))
            else:
                formatted_lines.append(line)

    normalized_lines: list[str] = []
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


def _format_from_dict(tokens: dict[str, str], is_military: bool = False) -> str:
    """Format an address from a dictionary of usaddress tags."""
    # Standardize dictionary bad tags for non-unit strings ending with digits
    if (
        "OccupancyType" in tokens
        and "OccupancyIdentifier" in tokens
        and "AddressNumber" not in tokens
    ):
        orig_type: str = tokens.get("OccupancyType", "").upper()
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
    line1_parts: list[str] = []
    line2_parts: list[str] = []
    last_line_parts: list[str] = []

    # Check for Business or Recipient/Building (e.g., URB)
    for key in ["Recipient", "BuildingName", "LandmarkName"]:
        if key in tokens:
            val: str = tokens[key].strip()
            # If the value consists solely of secondary units, re-categorize it to OccupancyType
            val_upper: str = val.upper()
            # Extract basic words removing numbers/punctuation for comparison
            words: list[str] = re.sub(r"[^A-Z\s]", "", val_upper).split()
            if words and all(w in SECONDARY_UNITS or w in SECONDARY_UNITS.values() for w in words):
                tokens["OccupancyType"] = val
                del tokens[key]
                break
            line1_parts.append(val)
            # If this is General Delivery, remove it from tokens to prevent doubling
            if val.upper() == "GENERAL DELIVERY":
                del tokens[key]
            break

    # Build Street Address Line
    street_parts: list[str] = []
    if "AddressNumber" not in tokens and "StreetName" not in tokens and line1_parts:
        # For single-line isolated setups, merge Recipient continuous to address vectors
        street_parts.append(line1_parts.pop(0))

    # Re-categorize false-positive BoxType as Secondary Unit
    if "USPSBoxType" in tokens:
        box_type_str: str = tokens["USPSBoxType"].upper()
        if (
            box_type_str in SECONDARY_UNITS
            or box_type_str in SECONDARY_UNITS.values()
            or "AddressNumber" in tokens
        ):
            tokens["OccupancyType"] = tokens["USPSBoxType"]
            if "USPSBoxID" in tokens:
                tokens["OccupancyIdentifier"] = tokens["USPSBoxID"]
                del tokens["USPSBoxID"]
            del tokens["USPSBoxType"]

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
            occ_val: str = _format_secondary_unit(tokens.pop("OccupancyType", ""))
            occ_id_raw: str = tokens.pop("OccupancyIdentifier", "")
            street_parts.extend([occ_val, occ_id_raw])

        group_type = tokens.get("USPSBoxGroupType", "")
        group_id = tokens.get("USPSBoxGroupID", "")
        box_type = tokens.get("USPSBoxType", "")
        box_id = tokens.get("USPSBoxID", "")

        # Fallback for usaddress occasionally tagging RR number as box_id
        if group_type and not group_id and box_id and not box_type:
            group_id = box_id
            box_id = ""

        # Format Group (e.g., RR, HC)
        if group_type:
            if group_type in ("RR", "RURAL ROUTE", "RFD", "RD", "RT", "RURAL"):
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
        or tokens.get("Recipient", "").upper() == "GENERAL DELIVERY"
        or tokens.get("BuildingName", "").upper() == "GENERAL DELIVERY"
    ):
        street_parts.append("GENERAL DELIVERY")
        # If there's other info, we'll keep it as secondary
        if "AddressNumber" in tokens and tokens["AddressNumber"] != "GENERAL DELIVERY":
            street_parts.append(tokens["AddressNumber"])
        if "StreetName" in tokens and tokens["StreetName"] != "GENERAL DELIVERY":
            street_parts.append(tokens["StreetName"])
        if "StreetNamePostType" in tokens:
            street_parts.append(_format_suffix(tokens["StreetNamePostType"]))

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
            if "StreetName" in tokens and (
                tokens["StreetName"].strip().startswith("AND ")
                or "StreetName" in tokens
            ):
                # Standard case: Abbreviate if a street name exists
                # (except for compound names handled by the 'AND' check)
                if tokens["StreetName"].strip().startswith("AND "):
                    street_parts.append(tokens["StreetNamePreDirectional"])
                else:
                    street_parts.append(_format_directional(tokens["StreetNamePreDirectional"]))
            else:
                # If StreetName is missing, keep full spelling (e.g. SOUTH BLVD)
                street_parts.append(tokens["StreetNamePreDirectional"])

        if "StreetNamePreType" in tokens:
            # PreTypes like "COUNTY HIGHWAY" are typically NOT abbreviated in Project US@
            street_parts.append(tokens["StreetNamePreType"])

        if "StreetName" in tokens:
            street_parts.append(tokens["StreetName"])

        if "StreetNamePostType" in tokens:
            street_parts.append(_format_suffix(tokens["StreetNamePostType"]))

        if "StreetNamePostDirectional" in tokens:
            if "StreetName" in tokens:
                street_parts.append(_format_directional(tokens["StreetNamePostDirectional"]))
            else:
                # If StreetName is missing, keep full spelling
                street_parts.append(tokens["StreetNamePostDirectional"])

    # Secondary Unit
    if "OccupancyType" in tokens:
        occ_type: str = _format_secondary_unit(tokens["OccupancyType"])
        street_parts.append(occ_type)
        if "OccupancyIdentifier" in tokens:

            def get_val_txt(obj: object) -> str:
                if hasattr(obj, "get_text") and callable(typing.cast(typing.Any, obj).get_text):
                    return str(typing.cast(typing.Any, obj).get_text())
                return str(obj)

            occ_id: str = get_val_txt(tokens["OccupancyIdentifier"])
            if str(occ_id).startswith("#"):
                occ_id = str(occ_id)[1:].strip()
            street_parts.append(occ_id)
    elif "OccupancyIdentifier" in tokens:

        def get_val_simple(obj: object) -> str:
            if hasattr(obj, "get_text") and callable(typing.cast(typing.Any, obj).get_text):
                return str(typing.cast(typing.Any, obj).get_text())
            return str(obj)

        occ_id_simple: str = get_val_simple(tokens["OccupancyIdentifier"])
        street_parts.append(occ_id_simple)

    # Private Mailbox (PMB)
    if "SubaddressType" in tokens:
        sub_type: str = tokens["SubaddressType"].strip()
        if sub_type in ("PMB", "PRIVATE MAILBOX"):
            sub_type = "PMB"
        # PDF says PMB or # identifier may be used
        street_parts.append(sub_type)
        if "SubaddressIdentifier" in tokens:
            street_parts.append(tokens["SubaddressIdentifier"])

    # City, State, ZIP — Moved up for boundary standalone passes
    if "StateName" in tokens:
        state_val_dict = _format_state(tokens["StateName"])
        if "PlaceName" in tokens:
            last_line_parts.append(state_val_dict)
        else:
            street_parts.insert(0, state_val_dict)

    if "ZipCode" in tokens:
        if "PlaceName" in tokens:
            last_line_parts.append(tokens["ZipCode"])
        else:
            street_parts.append(tokens["ZipCode"])

    if "PlaceName" in tokens:
        if "StateName" not in tokens and "ZipCode" not in tokens:
            street_parts.append(tokens["PlaceName"])
        else:
            last_line_parts.insert(0, tokens["PlaceName"])

    if street_parts:
        line2_str = " ".join(street_parts).strip()
        line2_parts.append(line2_str)

    # Combine lines
    final_lines: list[str] = []
    if line1_parts:
        final_lines.append(_apply_pr_exceptions(" ".join(line1_parts)))
    if line2_parts:
        final_lines.append(_apply_pr_exceptions(" ".join(line2_parts)))
    if last_line_parts:
        # Construct City State Zip carefully
        city = last_line_parts[0] if len(last_line_parts) > 0 else ""
        state = last_line_parts[1] if len(last_line_parts) > 1 else ""
        zip_code_val = last_line_parts[2] if len(last_line_parts) > 2 else ""

        last_line = ""
        if city:
            last_line += city

        # Handle Canada spacing
        if state:
            state_parts = state.split()
            if (
                state_parts[0]
                in (
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
                and len(state_parts) > 1
            ):
                # usaddress merged part of Zip into StateName
                # e.g., 'ON K1A'
                state = state_parts[0]
                zip_code_val = (
                    " ".join(state_parts[1:]) + (" " + zip_code_val if zip_code_val else "")
                )

            last_line += (" " if last_line else "") + state

        if zip_code_val:
            last_line += (" " if last_line else "") + zip_code_val
        final_lines.append(last_line)

    return "\n".join(final_lines)


def _format_from_raw(raw_parsed: list[tuple[str, str]]) -> str:
    """Fallback formatter for complex addresses that usaddress cannot tag."""
    # smart swap numbers to front for address lines ending with digit (e.g., ACHENSEEWEG 25)
    if raw_parsed:
        v_last, l_last = raw_parsed[-1]
        if v_last.isdigit() and len(raw_parsed) > 1 and l_last != "ZipCode":
            highway_keywords_check = (
                "HIGHWAY", "ROAD", "ROUTE", "FM", "US", "INTERSTATE", "SR", "ST",
                "COUNTY", "STATE", "EXPRESSWAY", "PARKWAY", "BOULEVARD", "DRIVE",
                "AVENUE", "WAY", "CALLE", "C/", "AVENIDA", "KM",
            )
            can_swap = True
            for v_chk, _label in raw_parsed[:-1]:
                v_up = v_chk.upper()
                if (
                    v_up in SECONDARY_UNITS
                    or v_up in SECONDARY_UNITS.values()
                    or v_up in highway_keywords_check
                ):
                    can_swap = False
                    break
            if can_swap and not any(v.isdigit() for v, _label in raw_parsed[:-1]):
                raw_parsed = [raw_parsed[-1], *raw_parsed[:-1]]

    has_street_name = any(label == "StreetName" for _val, label in raw_parsed)
    reconstructed: list[str] = []
    for p_val, p_label in raw_parsed:
        # apply directional checks
        res_val = p_val
        if "Directional" in p_label:
            # Preservation for directional street names (e.g. SOUTH BLVD)
            res_val = _format_directional(p_val) if has_street_name else p_val
        # apply suffix checks
        elif "PostType" in p_label or p_label == "StreetNamePostType":
            res_val = _format_suffix(p_val)
        # apply state checks
        elif p_label == "StateName":
            res_val = _format_state(p_val)
        # apply secondary unit
        elif p_label == "OccupancyType":
            res_val = _format_secondary_unit(p_val)
        elif p_label == "SubaddressType" and p_val.upper() in ("PMB", "PRIVATE MAILBOX"):
            res_val = "PMB"
        reconstructed.append(res_val)

    # Reassemble and try to figure out lines based on newlines that were in the original
    # usaddress strips \n from values sometimes, so we'll just join with space
    # and do a naive newline check
    final_raw = (
        " ".join(reconstructed).replace(" \n ", "\n").replace("\n ", "\n").replace(" \n", "\n")
    )
    lines_raw = final_raw.split("\n")
    return "\n".join(_apply_pr_exceptions(line_item) for line_item in lines_raw)


def _apply_pr_exceptions(text: str) -> str:
    """Apply Project US@ Puerto Rico specific formatting and reordering rules."""
    # Reorder Station lines to be ABOVE delivery lines if they are below (e.g., Sta below PO Box)
    lines = [line.strip() for line in text.split("\n") if line.strip()]
    if not lines:
        return text

    last_line_val = ""
    # If the last line contains a State and Zip code, isolate it from reordering
    if re.search(r"\b[A-Z]{2}\b\s+\d{5}", lines[-1].upper()):
        last_line_val = lines.pop(-1)

    urb_lines: list[str] = []
    condo_lines: list[str] = []
    street_lines_list: list[str] = []
    postal_lines: list[str] = []
    other_lines: list[str] = []

    for line in lines:
        line_clean = line.strip()
        if not line_clean:
            continue
        line_up = line_clean.upper()
        if line_up.startswith(("URB", "EXT", "URBANIZATION")):
            urb_lines.append(line_clean)
        elif any(
            k in line_up for k in ("COND", "EDIF", "CONDOMINIO", "EDIFICIO", "APT", "APARTAMENT")
        ):
            condo_lines.append(line_clean)
        elif any(k in line_up for k in ("PO BOX", "STA", "STATION")):
            postal_lines.append(line_clean)
        elif any(
            k in line_up for k in ("CALLE", "C/ ", "AVE", "AVENIDA", "KM", "ROAD", "ROUTE", "RR")
        ) or re.search(r"^\d", line_clean):
            street_lines_list.append(line_clean)
        else:
            other_lines.append(line_clean)

    # Deduplicate Urbanization lines (keep longest to avoid listed duplicates)
    if len(urb_lines) > 1:
        urb_lines = [sorted(urb_lines, key=len)[-1]]

    # Reorder Station lines to be ABOVE delivery lines
    if len(postal_lines) > 1:
        for i in range(len(postal_lines) - 1):
            if "PO BOX" in postal_lines[i].upper() and any(
                k in postal_lines[i + 1].upper() for k in ("STA", "STATION")
            ):
                postal_lines[i], postal_lines[i + 1] = postal_lines[i + 1], postal_lines[i]
                break

    lines = urb_lines + condo_lines + street_lines_list + other_lines + postal_lines
    if last_line_val:
        lines.append(last_line_val)
    text = "\n".join(lines)

    # Standardize Spanish boxes on the combined text (e.g., APARTADO -> PO BOX)
    text = re.sub(r"\b(APARTADO|APDO|GPO BOX)\b", "PO BOX", text, flags=re.IGNORECASE)
    # Standardize Spanish boxes for Rural routes (e.g., BUZON -> BOX)
    text = re.sub(r"\b(BUZON|BZN)\b", "BOX", text, flags=re.IGNORECASE)
    # Standardize Spanish rural routes (e.g., RUTA RURAL -> RR)
    text = re.sub(r"\b(RUTA RURAL|RURAL)\b", "RR", text, flags=re.IGNORECASE)

    lines_pr = text.split("\n")
    final_lines_pr: list[str] = []
    for line_pr in lines_pr:
        line_str = line_pr.strip()
        # Remove hyphens from alphanumeric house numbers at the absolute front (e.g., A-17 -> A17)
        line_str = re.sub(r"^([A-Z]{1,2})-(\d+[A-Z]?)\b", r"\1\2", line_str)
        words_pr = line_str.split()
        new_words: list[str] = []
        skip = False
        for i, w_pr in enumerate(words_pr):
            if skip:
                skip = False
                continue
            w_upper_pr = w_pr.upper()
            if w_upper_pr in ("URB", "URBANIZATION") and i + 1 < len(words_pr):
                next_word_pr = words_pr[i + 1].upper()
                if (
                    next_word_pr in PR_URBANIZATION_EXCEPTIONS
                    or next_word_pr in PR_URBANIZATION_EXCEPTIONS.values()
                ):
                    mapped_val_pr = PR_URBANIZATION_EXCEPTIONS.get(next_word_pr, next_word_pr)
                    new_words.append(mapped_val_pr)
                    skip = True
                    continue
            w_mapped_pr = w_pr
            if w_upper_pr in PR_URBANIZATION_EXCEPTIONS:
                w_mapped_pr = PR_URBANIZATION_EXCEPTIONS[w_upper_pr]
            elif w_upper_pr in PR_URBANIZATION_EXCEPTIONS.values():
                w_mapped_pr = w_upper_pr
            elif w_upper_pr == "URBANIZATION":
                w_mapped_pr = "URB"
            new_words.append(w_mapped_pr)
        final_lines_pr.append(" ".join(new_words))
    return "\n".join(final_lines_pr)
