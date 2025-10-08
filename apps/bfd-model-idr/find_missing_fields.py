import csv
import re
import sys
from pathlib import Path

import yaml


def extract_source_columns(yamp_paths: list[Path]) -> set[str]:
    pattern = re.compile(r"ExplanationOfBenefit-(?:Base|Pharmacy)\.(.+)")
    source_columns: set[str] = set()

    for yaml_path in yamp_paths:
        with open(yaml_path, "r") as f:
            data = yaml.safe_load(f)

        file_columns = set()

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
                file_columns.add(str(col).strip().lower())

        source_columns.update(file_columns)

    return source_columns


def extract_csv_columns(csv_path: Path) -> set[str]:
    with open(csv_path, newline="") as f:
        reader = csv.reader(f)
        next(reader, None)
        columns = [row[0].strip().lower() for row in reader if row and row[0].strip()]
    return set(columns)


def main(csv_file: str, *yaml_files: str) -> None:
    yaml_paths = [Path(yaml_file) for yaml_file in yaml_files]
    yaml_columns = extract_source_columns(yaml_paths)
    csv_columns = extract_csv_columns(Path(csv_file))

    missing_in_csv = yaml_columns - csv_columns
    extra_in_csv = csv_columns - yaml_columns

    print("\n=== Comparison Results ===")

    if missing_in_csv:
        print("YAML fields missing in schema")
        for col in sorted(missing_in_csv):
            print(f" - {col}")
    else:
        print("All YAML fields are present in schema")

    if extra_in_csv:
        print("\nExtra columns in schema not found in YAML:")
        for col in sorted(extra_in_csv):
            print(f" - {col}")


if __name__ == "__main__":
    main(sys.argv[1], *sys.argv[2:])
