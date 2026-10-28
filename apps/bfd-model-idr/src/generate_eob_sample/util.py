import sys
from pathlib import Path

import pandas as pd

from .param import Param


def read_multi_line_file(
    source_directory: str, file_name: str, not_found_fail: bool, params: list[Param]
) -> list[dict[str, str]]:
    file_path = f"{source_directory}/{file_name}.csv"
    if not Path(file_path).exists():
        print(f"EOB file {file_name} not found. Run the generator may have issues.")
        if not_found_fail:
            sys.exit(1)
        else:
            return []

    records_found = pd.read_csv(file_path, dtype=str, keep_default_na=False)

    param_matches = [records_found[param.field] == param.value for param in params]
    record_matches = (
        records_found[pd.concat(param_matches, axis=1).all(axis=1)]
        if param_matches
        else records_found
    )

    if not_found_fail and record_matches.empty:
        print(f"No record found for file name: {file_name}")
        sys.exit(1)

    return [] if record_matches.empty else record_matches.to_dict(orient="records")  # type: ignore[reportCallIssue]


def read_single_line_file(
    source_directory: str, file_name: str, not_found_fail: bool, params: list[Param]
) -> dict[str, str]:
    records = read_multi_line_file(source_directory, file_name, not_found_fail, params)
    return {} if not records else records[0]


def find_field_in_line_by_num(
    lines: list[dict[str, str]],
    clm_line: dict[str, str],
    extracted_field: str,
) -> str | None:
    if not lines:
        return None

    clm_line_num = extract_col_str(clm_line, "CLM_LINE_NUM")

    line_matches = [line for line in lines if extract_col_str(line, "CLM_LINE_NUM") == clm_line_num]

    if not line_matches:
        return None

    return extract_col_str(line_matches[0], extracted_field)


def find_linked_field_in_tgt_by_line_num_sk(
    sk_field: str,
    source_directory: str,
    src_lines: list[dict[str, str]],
    clm_line: dict[str, str],
    extracted_field: str,
) -> str | None:
    if sk_field not in "CLM_ANSI_SGNTR_SK":
        return None

    sk = find_field_in_line_by_num(src_lines, clm_line, sk_field)

    if not sk:
        return None

    # only 2nd order line read implemented so far. Add logic for more
    ansi_sig_line = read_ansi_sgntr(sk, source_directory)

    if not ansi_sig_line:
        return None

    return extract_col_str(ansi_sig_line, extracted_field)


def extract_col_str(row: dict[str, str], name: str) -> str | None:
    return str(row.get(name, "")).strip() or None


## 2nd order reads (clm_line --> supplementary_line.sk --> target_read.sk)


def read_ansi_sgntr(ansi_sgntr_sk: str, source_directory: str) -> dict[str, str]:
    return read_single_line_file(
        source_directory=source_directory,
        file_name="SYNTHETIC_CLM_ANSI_SGNTR",
        not_found_fail=False,
        params=[
            Param("CLM_ANSI_SGNTR_SK", ansi_sgntr_sk),
        ],
    )
