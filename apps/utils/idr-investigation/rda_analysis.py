import itertools
import multiprocessing
from dataclasses import asdict, dataclass, fields

from thefuzz import fuzz

from common import (
    BfdRdaColumnDataFieldRow,
    CsvWriteable,
    IdrDataFieldRow,
    RdaDataFieldRow,
    clean_str,
    extract_bfd_rda_columns_from_path,
    extract_idr_rows_from_path,
    extract_rda_rows_from_path,
    write_to_csv,
)


@dataclass
class BfdToRdaMappedField(CsvWriteable):
    bfd_column_name: str
    rda_source_system: str
    rda_api_field_label: str
    rda_field_name: str
    rda_source_copybook_field: str
    rda_source_system_def: str

    @staticmethod
    def get_header_field_mapping() -> dict[str, str]:
        return {
            f"bfd_{k}": v for k, v in BfdRdaColumnDataFieldRow.get_header_field_mapping().items()
        } | {f"rda_{k}": v for k, v in RdaDataFieldRow.get_header_field_mapping().items()}


@dataclass
class FullyMappedField(CsvWriteable):
    bfd_column_name: str
    idr_column_name: str
    bfd_cleaned_column_name: str
    rda_cleaned_copybook_field: str
    idr_cleaned_col_data_source_name: str
    rda_to_idr_cleaned_copybook_fuzz_ratio: int
    rda_source_system_def: str
    rda_cleaned_source_system_def: str
    idr_attribute_def: str
    idr_cleaned_attribute_def: str
    rda_to_idr_cleaned_def_fuzz_ratio: int
    rda_api_field_label: str
    rda_source_system: str
    rda_field_name: str
    rda_source_copybook_field: str
    idr_entity_name: str
    idr_col_data_source_name: str
    idr_attribute_name: str
    idr_table_name: str
    idr_view_name: str

    @staticmethod
    def get_header_field_mapping() -> dict[str, str]:
        return (
            {f"bfd_{k}": v for k, v in BfdRdaColumnDataFieldRow.get_header_field_mapping().items()}
            | {f"rda_{k}": v for k, v in RdaDataFieldRow.get_header_field_mapping().items()}
            | {f"idr_{k}": v for k, v in IdrDataFieldRow.get_header_field_mapping().items()}
            | {
                "bfd_cleaned_column_name": "Cleaned BFD Column Name",
                "rda_cleaned_copybook_field": "Cleaned RDA Source Copybook Field Label",
                "idr_cleaned_col_data_source_name": "Cleaned IDR Column Data Source Name",
                "rda_to_idr_cleaned_copybook_fuzz_ratio": "Cleaned RDA Copybook to Cleaned IDR Column Data Source Ratio",
                "rda_cleaned_source_system_def": "Cleaned RDA Source System Definition",
                "idr_cleaned_attribute_def": "Cleaned IDR Attribute Definition",
                "rda_to_idr_cleaned_def_fuzz_ratio": "Cleaned RDA Source Def to IDR Attr Def",
            }
        )


@dataclass
class BfdRdaColumnWithLikelyMatch(CsvWriteable):
    bfd_column_name: str
    idr_matched_column_names: list[str]
    ratio_type: str
    highest_ratio: int

    @staticmethod
    def get_header_field_mapping() -> dict[str, str]:
        return {
            "bfd_column_name": "BFD Column Name",
            "idr_matched_column_names": "Likeliest Matching IDR Column Names",
            "ratio_type": "Fuzz Ratio Type",
            "highest_ratio": "Highest Ratio Percentage",
        }


@dataclass
class BfdRdaColumnSubstringMatches(CsvWriteable):
    bfd_column_name: str
    idr_matched_column_names: list[str]

    @staticmethod
    def get_header_field_mapping() -> dict[str, str]:
        return {
            "bfd_column_name": "BFD Column Name",
            "idr_matched_column_names": "Substring Matched IDR Column Names",
        }


def map_bfd_to_rda_to_idr(pair: tuple[BfdToRdaMappedField, IdrDataFieldRow]) -> FullyMappedField:
    bfd_rda_mapped_field = pair[0]
    idr_field = pair[1]
    idr_cleaned_col_data_source_name = clean_str(idr_field.col_data_source_name)
    idr_cleaned_attribute_def = clean_str(idr_field.attribute_def)
    rda_cleaned_copybook_field = clean_str(bfd_rda_mapped_field.rda_source_copybook_field)
    rda_cleaned_source_system_def = clean_str(bfd_rda_mapped_field.rda_source_system_def)
    return FullyMappedField(
        bfd_cleaned_column_name=clean_str(bfd_rda_mapped_field.bfd_column_name),
        idr_column_name=idr_field.column_name,
        idr_entity_name=idr_field.entity_name,
        idr_col_data_source_name=idr_field.col_data_source_name,
        idr_attribute_name=idr_field.attribute_name,
        idr_attribute_def=idr_field.attribute_def,
        idr_cleaned_col_data_source_name=idr_cleaned_col_data_source_name,
        idr_table_name=idr_field.table_name,
        idr_view_name=idr_field.view_name,
        idr_cleaned_attribute_def=idr_cleaned_attribute_def,
        rda_cleaned_copybook_field=rda_cleaned_copybook_field,
        rda_cleaned_source_system_def=rda_cleaned_source_system_def,
        rda_to_idr_cleaned_copybook_fuzz_ratio=int(
            fuzz.token_set_ratio(  # type: ignore
                rda_cleaned_copybook_field,
                idr_cleaned_col_data_source_name,
            )
        ),
        rda_to_idr_cleaned_def_fuzz_ratio=int(
            fuzz.token_set_ratio(  # type: ignore
                rda_cleaned_source_system_def,
                idr_cleaned_attribute_def,
            )
        ),
        **asdict(bfd_rda_mapped_field),
    )


def main() -> None:
    # RDA
    rda_fields = extract_rda_rows_from_path(
        "/Users/mitchalessio/Repositories/beneficiary-fhir-data/apps/utils/idr-investigation/RDA API Data Dictionary.xlsx"
    )
    write_to_csv(filename="all_rda_fields.csv", list_to_write=rda_fields)

    # IDR
    idr_fields = [
        idr_field
        for idr_field in extract_idr_rows_from_path(
            "/Users/mitchalessio/Downloads/SF-IDR-Data-Dictionary-R205-2024-09-30.xlsm"
        )
        if idr_field.col_data_source_name.lower()
        not in [
            "idr derived",
            "derived",
            "unknown",
            "-",
            " ",
        ]
    ]

    # BFD
    bfd_fields = extract_bfd_rda_columns_from_path(
        "/Users/mitchalessio/Repositories/beneficiary-fhir-data/apps/utils/idr-investigation/rda_columns.xlsx"
    )

    # Map BFD's rda fields to RDA's source_copybook_field_labels
    bfd_to_rda_source_field_mapping = [
        BfdToRdaMappedField(
            bfd_column_name=bfd_field.column_name,
            rda_source_system=rda_field.source_system,
            rda_api_field_label=rda_field.api_field_label,
            rda_field_name=rda_field.field_name,
            rda_source_copybook_field=rda_field.source_copybook_field,
            rda_source_system_def=rda_field.source_system_def,
        )
        for (bfd_field, rda_field) in itertools.product(bfd_fields, rda_fields)
        if clean_str(bfd_field.column_name) in clean_str(rda_field.source_copybook_field)
    ]
    # unique_fields = set()

    field_combos = [
        pair
        for pair in itertools.product(bfd_to_rda_source_field_mapping, idr_fields)
        if clean_str(pair[0].rda_source_copybook_field) and clean_str(pair[1].col_data_source_name)
    ]
    print(f"Mapping {len(field_combos)} field combinations...")
    with multiprocessing.Pool() as pool:
        fully_mapped_fields = pool.map(
            map_bfd_to_rda_to_idr,
            field_combos,
        )
    print(f"Done mapping {len(field_combos)} field combinations")

    for fuzz_ratio_field in (x.name for x in fields(FullyMappedField) if "ratio" in x.name.lower()):
        ratio_threshold = 70
        mapped_fields_exceeding_threshold = [
            x
            for x in fully_mapped_fields
            if (dx := asdict(x))
            and dx[fuzz_ratio_field] is not None
            and int(dx[fuzz_ratio_field]) >= ratio_threshold
        ]
        bfd_fields_exceeding_threshold = set(
            x.bfd_column_name for x in mapped_fields_exceeding_threshold
        )
        print(len(bfd_fields_exceeding_threshold))
        print(len(mapped_fields_exceeding_threshold))

        high_ratio_mapped_fields_filename = (
            f"./all_rda_{ratio_threshold}_{fuzz_ratio_field}_mapped_columns.csv"
        )
        print(f"Writing {high_ratio_mapped_fields_filename}...")
        write_to_csv(
            filename=high_ratio_mapped_fields_filename,
            list_to_write=mapped_fields_exceeding_threshold,
        )
        print(f"Done writing {high_ratio_mapped_fields_filename}")

        ratio_max_per_bfd_orig_field = {
            x: max(
                int(asdict(y)[fuzz_ratio_field])
                for y in mapped_fields_exceeding_threshold
                if y.bfd_column_name == x
            )
            for x in bfd_fields_exceeding_threshold
        }
        fields_with_likely_matches = [
            BfdRdaColumnWithLikelyMatch(
                bfd_column_name=k,
                idr_matched_column_names=[
                    x.idr_column_name
                    for x in mapped_fields_exceeding_threshold
                    if x.bfd_column_name == k and asdict(x)[fuzz_ratio_field] == v
                ],
                ratio_type=fuzz_ratio_field,
                highest_ratio=v,
            )
            for k, v in ratio_max_per_bfd_orig_field.items()
        ]
        high_ratio_bfd_fields_filename = (
            f"./all_{ratio_threshold}_{fuzz_ratio_field}_bfd_rda_columns.csv"
        )
        print(f"Writing {high_ratio_bfd_fields_filename}...")
        write_to_csv(
            filename=high_ratio_bfd_fields_filename, list_to_write=fields_with_likely_matches
        )
        print(f"Done writing {high_ratio_bfd_fields_filename}")

    substring_matches_filename = "./all_rda_substring_matched_mapped_columns.csv"
    substring_matches = [
        x
        for x in fully_mapped_fields
        if x.rda_cleaned_copybook_field.replace("idr", "") in x.idr_cleaned_col_data_source_name
    ]
    print(f"Writing {len(substring_matches)} fields to {substring_matches_filename}...")
    write_to_csv(
        filename=substring_matches_filename,
        list_to_write=substring_matches,
    )
    print(f"Done writing {len(substring_matches)} fields to {substring_matches_filename}...")

    substring_matches_by_field_filename = "./all_rda_substring_matched_bfd_rda_columns.csv"
    substring_matched_by_field_dict: dict[str, list[str]] = {}
    for mapped_field in substring_matches:
        if substring_matched_by_field_dict.get(mapped_field.bfd_column_name):
            substring_matched_by_field_dict[mapped_field.bfd_column_name].append(
                mapped_field.idr_column_name
            )
        else:
            substring_matched_by_field_dict[mapped_field.bfd_column_name] = [
                mapped_field.idr_column_name
            ]
    substring_matched_rda_columns = [
        BfdRdaColumnSubstringMatches(bfd_column_name=k, idr_matched_column_names=v)
        for k, v in substring_matched_by_field_dict.items()
    ]
    print(
        f"Writing {len(substring_matched_rda_columns)} fields to {substring_matches_by_field_filename}..."
    )
    write_to_csv(
        filename=substring_matches_by_field_filename,
        list_to_write=substring_matched_rda_columns,
    )
    print(
        f"Done writing {len(substring_matched_rda_columns)} fields to {substring_matches_by_field_filename}..."
    )


if __name__ == "__main__":
    multiprocessing.freeze_support()
    main()
