import itertools
import re
from dataclasses import dataclass, fields
from multiprocessing import Pool, freeze_support

from thefuzz import fuzz

from common import (
    BfdV2DataFieldRow,
    CsvWriteable,
    IdrDataFieldRow,
    clean_str,
    extract_bfd_rows_from_path,
    extract_idr_rows_from_path,
    write_to_csv,
)


@dataclass
class BfdToIdrMappedField(CsvWriteable):
    bfd_field_name: str
    bfd_description: str
    bfd_ccw_mapping: str
    bfd_cclf_mapping: str
    bfd_column_name: str
    cleaned_bfd_description: str
    cleaned_bfd_ccw_mapping: str
    cleaned_bfd_cclf_mapping: str
    idr_column_name: str
    idr_entity_name: str
    idr_col_data_source_name: str
    idr_attribute_name: str
    idr_attribute_def: str
    idr_table_name: str
    idr_view_name: str
    cleaned_idr_attribute_def: str
    cleaned_idr_column_name: str
    cleaned_bfd_ccw_map_to_idr_colname_ratio: int
    cleaned_bfd_cclf_map_to_idr_colname_ratio: int
    cleaned_bfd_desc_to_idr_attrdef_ratio: int

    @staticmethod
    def get_header_field_mapping() -> dict[str, str]:
        return (
            {f"bfd_{k}": v for k, v in BfdV2DataFieldRow.get_header_field_mapping().items()}
            | {f"idr_{k}": v for k, v in IdrDataFieldRow.get_header_field_mapping().items()}
            | {
                "cleaned_bfd_description": "Cleaned BFD Field Description",
                "cleaned_bfd_ccw_mapping": "Cleaned BFD Field CCW Mapping",
                "cleaned_bfd_cclf_mapping": "Cleaned BFD Field CCLF Mapping",
                "cleaned_idr_attribute_def": "Cleaned IDR Attribute Definition",
                "cleaned_idr_column_name": "Cleaned IDR Column Name",
                "cleaned_bfd_ccw_map_to_idr_colname_ratio": "Cleaned BFD Field CCW Mapping to Cleaned IDR Colname",
                "cleaned_bfd_cclf_map_to_idr_colname_ratio": "Cleaned BFD Field CCLF Mapping to Cleaned IDR Colname",
                "cleaned_bfd_desc_to_idr_attrdef_ratio": "Cleaned BFD Field Description to Cleaned IDR Attr Desc",
            }
        )


@dataclass
class BfdFieldWithLikelyMatch(CsvWriteable):
    bfd_column_name: str
    idr_match_column_names: list[str]
    ratio_type: str
    highest_ratio: int

    @staticmethod
    def get_header_field_mapping() -> dict[str, str]:
        return {
            "bfd_column_name": "BFD Column Name",
            "idr_match_column_names": "Likeliest Matching IDR Column Names",
            "ratio_type": "Fuzz Ratio Type",
            "highest_ratio": "Highest Ratio Percentage",
        }


def map_bfd_to_idr_fields(
    object_pair: tuple[BfdV2DataFieldRow, IdrDataFieldRow],
) -> BfdToIdrMappedField:
    bfd_field = object_pair[0]
    idr_field = object_pair[1]
    cleaned_bfd_ccw_mapping = clean_str(
        bfd_field.ccw_mapping.replace("1ST", "1")
        .replace("2ND", "2")
        .replace("3RD", "3")
        .replace("4TH", "4")
    )
    cleaned_idr_column_name = clean_str(idr_field.column_name)
    cleaned_bfd_cclf_mapping = clean_str(
        re.sub(r"^cclf\d{0,1}\.", "", bfd_field.cclf_mapping.lower())
    )
    cleaned_bfd_description = clean_str(bfd_field.description)
    cleaned_idr_attribute_def = clean_str(idr_field.attribute_def)
    return BfdToIdrMappedField(
        **{
            f"bfd_{cls_field.name}": getattr(bfd_field, cls_field.name)
            for cls_field in fields(bfd_field)
        },
        **{
            f"idr_{cls_field.name}": getattr(idr_field, cls_field.name)
            for cls_field in fields(idr_field)
        },
        cleaned_bfd_description=cleaned_bfd_description,
        cleaned_bfd_ccw_mapping=cleaned_bfd_ccw_mapping,
        cleaned_bfd_cclf_mapping=cleaned_bfd_cclf_mapping,
        cleaned_idr_column_name=cleaned_idr_column_name,
        cleaned_idr_attribute_def=cleaned_idr_attribute_def,
        cleaned_bfd_ccw_map_to_idr_colname_ratio=fuzz.token_set_ratio(  # type: ignore
            cleaned_bfd_ccw_mapping, cleaned_idr_column_name
        ),
        cleaned_bfd_cclf_map_to_idr_colname_ratio=fuzz.token_set_ratio(  # type: ignore
            cleaned_bfd_cclf_mapping, cleaned_idr_column_name
        ),
        cleaned_bfd_desc_to_idr_attrdef_ratio=fuzz.token_set_ratio(  # type: ignore
            cleaned_bfd_description, cleaned_idr_attribute_def
        ),
    )


def main() -> None:
    # IDR
    unfiltered_idr_fields = extract_idr_rows_from_path(
        "/Users/mitchalessio/Downloads/SF-IDR-Data-Dictionary-R205-2024-09-30.xlsm"
    )
    print("Writing all IDR fields extracted from IDR Data Dictionary")
    write_to_csv(filename="./all_idr_fields.csv", list_to_write=unfiltered_idr_fields)

    idr_fields = [
        idr_field
        for idr_field in unfiltered_idr_fields
        if not any(
            invalid_substr.lower() in idr_field.table_name.lower()
            for invalid_substr in ["MCS", "FISS", "MDCD", "medicaid"]
        )
        and "medicaid" not in idr_field.entity_name.lower()
    ]
    print(
        "Writing filtered IDR fields that are candidates for matching with CCW-provided BFD fields"
    )
    write_to_csv(filename="./all_filtered_idr_fields.ccw_prov_fields.csv", list_to_write=idr_fields)

    # BFD
    bfd_fields = extract_bfd_rows_from_path(
        "/Users/mitchalessio/Downloads/data-dictionary-2.160.0.xlsx"
    )
    print("Writing all unique BFD fields extracted from BFD DD")
    write_to_csv(filename="./all_bfd_fields.csv", list_to_write=bfd_fields)

    bfd_fields_to_idr_fields = list(itertools.product(bfd_fields, idr_fields))
    print(f"Mapping and computing ratios for {len(bfd_fields_to_idr_fields)} field combinations")
    with Pool() as pool:
        mappings = pool.map(map_bfd_to_idr_fields, bfd_fields_to_idr_fields)
    print(f"Finished mapping {len(mappings)} field combinations")

    # print(f"Writing all {len(mappings)} mapped field combinations")
    # with Path("./all_mapped_fields.csv").open("w") as csv_file:
    #     wr = csv.writer(csv_file, delimiter=",")
    #     wr.writerow(x.name for x in fields(BfdToIdrMappedField))
    #     for field_object in mappings:
    #         wr.writerow(asdict(field_object).values())
    # print(f"Finished writing all {len(mappings)} mapped field combinations")

    for fuzz_ratio_field in (
        x.name for x in fields(BfdToIdrMappedField) if "ratio" in x.name.lower()
    ):
        ratio_threshold = 70
        mapped_fields_exceeding_threshold = [
            (mapped_field, fuzz_ratio)
            for mapped_field in mappings
            if (fuzz_ratio := getattr(mapped_field, fuzz_ratio_field))
            and fuzz_ratio is not None
            and int(fuzz_ratio) >= ratio_threshold
        ]
        bfd_fields_exceeding_threshold = set(
            mapped_field.bfd_column_name for (mapped_field, _) in mapped_fields_exceeding_threshold
        )

        high_ratio_mapped_fields_filename = (
            f"./all_{ratio_threshold}_{fuzz_ratio_field}_mapped_fields.csv"
        )
        write_to_csv(
            filename=high_ratio_mapped_fields_filename,
            list_to_write=[mapped_field for (mapped_field, _) in mapped_fields_exceeding_threshold],
        )

        ratio_max_per_bfd_orig_field = {
            bfd_column_name: max(
                fuzz_ratio
                for (mapped_field, fuzz_ratio) in mapped_fields_exceeding_threshold
                if mapped_field.bfd_column_name == bfd_column_name
            )
            for bfd_column_name in bfd_fields_exceeding_threshold
        }
        fields_with_likely_matches = [
            BfdFieldWithLikelyMatch(
                bfd_column_name=k,
                idr_match_column_names=[
                    mapped_field.idr_column_name
                    for (mapped_field, fuzz_ratio) in mapped_fields_exceeding_threshold
                    if mapped_field.bfd_column_name == k and fuzz_ratio == v
                ],
                ratio_type=fuzz_ratio_field,
                highest_ratio=v,
            )
            for k, v in ratio_max_per_bfd_orig_field.items()
        ]
        high_ratio_bfd_fields_filename = (
            f"./all_{ratio_threshold}_{fuzz_ratio_field}_bfd_fields.csv"
        )
        write_to_csv(
            filename=high_ratio_bfd_fields_filename, list_to_write=fields_with_likely_matches
        )


if __name__ == "__main__":
    freeze_support()
    main()
