import csv
import re
import sys
from pathlib import Path

import yaml


def extract_source_columns(yamp_paths: list[Path]) -> (set[str], set[str]):
    pattern = re.compile(r"ExplanationOfBenefit-(?:Base|Pharmacy)\.(.+)")
    columns_to_view: dict[str, str | None] = {}

    for yaml_path in yamp_paths:
        with open(yaml_path, "r") as f:
            data = yaml.safe_load(f)

        for definition in data:
            if not isinstance(definition, dict):
                continue

            col = None
            if definition.get("sourceColumn"):
                col = definition["sourceColumn"]
            elif "inputPath" in definition:
                match = pattern.search(definition["inputPath"])
                if match:
                    col = match.group(1)
            if col:
                view = definition.get("sourceView")
                columns_to_view[col.strip().lower()] = (view.strip().lower() if isinstance(view, str) else None)
    return columns_to_view


def extract_csv_columns(csv_path: Path) -> set[str]:
    with open(csv_path, newline="") as f:
        reader = csv.reader(f)
        next(reader, None)
        columns = [row[0].strip().lower() for row in reader if row and row[0].strip()]
    return set(columns)

def extract_cclf_fields(csv_path: Path) -> set[str]:
    fields = set()

    with open(csv_path, newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            populated = row.get("Populated in CCLF?", "").strip().lower()
            field = row.get("Field", "").strip()
            if populated == "yes" and field:
                fields.add(field.lower())
    return fields

def main(cclf_file: str, csv_file: str, *yaml_files: str) -> None:
    yaml_paths = [Path(yaml_file) for yaml_file in yaml_files]
    yaml_columns = extract_source_columns(yaml_paths)
    csv_columns = extract_csv_columns(Path(csv_file))
    cclf_columns = extract_cclf_fields(Path(cclf_file))

    missing_in_csv = set(yaml_columns.keys()) - csv_columns
    extra_in_csv = csv_columns - set(yaml_columns.keys())
    missing_from_cclf = cclf_columns - csv_columns

    print("\n=== Comparison Results ===")

    if missing_in_csv:
        print("YAML fields missing in schema")
        # for col in sorted(missing_in_csv):
        #     print(f" {col}")
        extracted_values = {key: yaml_columns[key] for key in missing_in_csv if key in yaml_columns}
        for k, v in extracted_values.items():
            print(k, ", ", v)
    else:
        print("All YAML fields are present in schema")

    if extra_in_csv:
        print("\nExtra columns in schema not found in YAML:")
        for col in sorted(extra_in_csv):
            print(f" {col}")

    if missing_from_cclf:
        print("\nCCLF fields missing in schema:")
        for col in sorted(missing_from_cclf):
            print(f" {col}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2],*sys.argv[3:])
