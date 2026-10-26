import random
import string
from datetime import date, timedelta
from typing import Any

import constants as f
from claims_util import four_part_key
from generator_util import GeneratorUtil, partition_rows
from row_adapter import RowAdapter

PA_INDS = [
    "H004",
    "H006",
    "H002",
    "H007",
    "H003",
    "A009",
    "H005",
    "D001",
    "A007",
    "D004",
    "A006",
    "A008",
    "B517",
]

NPI_TYPE_2_NAMES = ["CBS PHARMACY", "WAL-PART PHARMACY", "BITE AID PHARMACY", "HEALTHCARE CENTER"]

CCN_LIST = ["39T14", "001500", "001502", "001503", "001504", "001505", "001509", "001510"]

HCPCS_CODES = [
    "G0299",
    "G0151",
    "G0300",
    "G0157",
    "G0495",
    "G0496",
    "G0493",
    "G0494",
    "G0152",
    "G0162",
]


class PriorAuthGeneratorUtil:
    def _add_days_iso(self, dt_str: str | date, days: int) -> str:
        base_date = date.fromisoformat(dt_str.split()[0]) if isinstance(dt_str, str) else dt_str
        return (base_date + timedelta(days=days)).isoformat()

    def _roll_segment_count(self) -> int:
        seg_count = 1
        while random.random() < 0.70 and seg_count < 19:
            seg_count += 1
        return seg_count

    def _map_clm_type(self, clm_type_cd: Any) -> str:
        val = int(clm_type_cd) if clm_type_cd.isdigit() else 0
        if val in (50, 1900, 2900, 1081, 1082, 2081, 2082):
            return "C"
        if val in (10, 60, 61, 62, 63, 64, 1032, 1033, 1034, 2032, 2033, 2034):
            return "H"
        if val in (30, 72, 81, 82, 1800, 2800):
            return "D"
        if val in (40, 1013, 2013, 1023, 2023):
            return "O"
        if val in (
            20,
            60,
            61,
            62,
            63,
            64,
            1011,
            1012,
            2011,
            2012,
            1021,
            1022,
            2021,
            2022,
            1018,
            2018,
        ):
            return "I"

        return "B"

    def _gen_header_fields(
        self,
        gen_utils: GeneratorUtil,
        mbi: str,
        utn: str,
        details: dict[str, Any],
        seg_count: int,
        type_2_npis: list[str],
        type_2_npi_to_name: dict[str, str],
        init_prior_auth: RowAdapter,
    ) -> RowAdapter:
        header = init_prior_auth or RowAdapter({})

        header[f.MBI_NUM] = mbi
        header[f.UTN] = utn
        header[f.SEGMENT_COUNT] = seg_count
        header[f.UTN_VALID_ST_DT] = details["from_dt"]
        header[f.UTN_VALID_EN_DT] = self._add_days_iso(header[f.UTN_VALID_ST_DT], 365)
        header[f.PA_IND] = random.choice(PA_INDS)
        header[f.CLM_TYPE] = self._map_clm_type(details["mac_id"])
        header[f.MAC_ID] = str(details["mac_id"])
        header[f.ICN_DCN] = "".join(random.choices(string.digits, k=14))
        header[f.PA_DT_ADDED] = self._add_days_iso(header[f.UTN_VALID_ST_DT], -10)
        header[f.PA_DT_UPDATED] = header[f.PA_DT_ADDED]
        header[f.PA_REQ_SUB_DT] = self._add_days_iso(header[f.PA_DT_ADDED], 1)
        header[f.PA_REQ_REC_DT] = self._add_days_iso(header[f.PA_REQ_SUB_DT], -1)
        header[f.PA_DECISION_DT] = header[f.PA_REQ_SUB_DT]
        header[f.PA_DECISION_EXP_DT] = self._add_days_iso(header[f.PA_DECISION_DT], 365)
        header[f.NPI] = (
            random.choice(type_2_npis) if type_2_npis else gen_utils.id_gen.npi_id("PRVDR_SK")
        )
        header[f.NAME] = type_2_npi_to_name.get(header[f.NPI]) or random.choice(NPI_TYPE_2_NAMES)
        header[f.CMS_CERT] = random.choice(CCN_LIST)
        header[f.REV_CODE_1] = "    " if random.random() < 0.80 else "0024"
        header[f.TOB] = random.choices(["32X", "13X", "   "], weights=[80, 10, 10])[0]
        header[f.SVC_RENDER_ST] = random.choices(
            ["TX", "FL", "IL", "OK"], weights=[50, 30, 10, 10]
        )[0]

        return header

    def _gen_segment(
        self,
        gen_utils: GeneratorUtil,
        header_fields: RowAdapter,
        segment_num: int,
        type_1_npis: list[str],
        type_2_npis: list[str],
        init_prior_auth: RowAdapter,
    ) -> RowAdapter:
        segment = init_prior_auth or RowAdapter({})
        segment.extend(header_fields)
        segment[f.CURRENT_SEGMENT] = str(segment_num)
        segment[f.HCPCS_OR_CPT_OR_HIPPS] = random.choice(HCPCS_CODES)
        segment[f.SERVICE_CNTS] = random.randint(1, 15)
        segment[f.PA_DECISION] = random.choice(
            gen_utils.code_systems.get("PA_DECISION", ["A", "P", "N"])
        )
        segment[f.ORDER_REFER_NPI] = (
            random.choice(type_1_npis) if type_1_npis else gen_utils.id_gen.npi_id("PRVDR_SK")
        )
        segment[f.RENDER_NPI] = (
            random.choice(type_1_npis + type_2_npis)
            if (type_1_npis or type_2_npis)
            else gen_utils.id_gen.npi_id("PRVDR_SK")
        )
        segment[f.OPERATE_NPI] = (
            random.choice(type_1_npis) if type_1_npis else gen_utils.id_gen.npi_id("PRVDR_SK")
        )
        segment[f.ATT_PHY_NPI] = (
            random.choice(type_1_npis) if type_1_npis else gen_utils.id_gen.npi_id("PRVDR_SK")
        )
        segment[f.PRICE_MOD1] = (
            "  " if random.random() < 0.80 else random.choice(["NU", "RR", "50"])
        )
        segment[f.PLACE_OF_SERV] = (
            "  " if random.random() < 0.80 else random.choice(["24", "11", "41"])
        )
        segment[f.MR_COUNT_IND] = random.randint(0, 100)
        segment[f.MR_COUNT_ST_DT] = self._add_days_iso(
            segment[f.PA_REQ_REC_DT], random.randint(-21, 21)
        )
        segment[f.MR_COUNT_END_DT] = self._add_days_iso(segment[f.MR_COUNT_ST_DT], 365)
        segment[f.RRB_EXCL_IND] = " " if random.random() < 0.90 else "Y"
        segment[f.IDR_INSRT_TS] = segment[f.PA_REQ_REC_DT]

        segment.extend(
            {
                "CAN": "",
                "EQUAT_BIC": "",
                "ICD_PROC_IND": " ",
                "ICD_PROC_CODE": "       ",
                "PA_FILLER": "    ",
                "PRICE_MOD2": "  ",
                **{f"ICD_DIAG_IND_{i}": " " for i in range(1, 6)},
                **{f"ICD_DIAG_CODE_{i}": "       " for i in range(1, 6)},
                **{f"REV_CODE_{i}": "    " for i in range(2, 21)},
                **{f"COND_CODE_{i}": "  " for i in range(1, 5)},
                **{f"OCCUR_CODE_{i}": "  " for i in range(1, 5)},
            }
        )

        return segment

    def gen_prior_auths(
        self,
        gen_utils: GeneratorUtil,
        out_tables: dict[str, list[RowAdapter]],
        generated_type_1_npis: list[str],
        generated_type_2_npis: list[str],
        init_prior_auth: list[RowAdapter],
    ) -> list[RowAdapter]:
        # Map BENE_SK to BENE_MBI_ID
        bene_sk_to_mbi = {}
        for row in out_tables[f.BENE_HSTRY]:
            sk, mbi = str(row["BENE_SK"]), row.get("BENE_MBI_ID")
            if mbi and (row.get("IDR_LTST_TRANS_FLG") == "Y" or sk not in bene_sk_to_mbi):
                bene_sk_to_mbi[sk] = mbi

        # Build mapping of four_part_key and CLM_UNIQ_ID to the claim row
        fpk_to_clm = {four_part_key(row): row for row in out_tables[f.CLM]}
        uniq_id_to_clm = {row[f.CLM_UNIQ_ID]: row for row in out_tables[f.CLM]}

        def get_line_mbi(line_row):
            fpk = four_part_key(line_row)
            clm = fpk_to_clm.get(fpk) or uniq_id_to_clm.get(line_row.get(f.CLM_UNIQ_ID))
            bene_sk = clm[f.BENE_SK] if clm else line_row.get(f.BENE_SK)
            return bene_sk_to_mbi.get(str(bene_sk)) if bene_sk else None

        # Collect unique (mbi, utn) combinations from lines
        utn_combos = {}
        lines_to_scan = []
        for line in out_tables[f.CLM_LINE]:
            utn = line.get(f.CLM_LINE_PMD_UNIQ_TRKNG_NUM) or line.get(f.CLM_LINE_PA_UNIQ_TRKNG_NUM)
            if utn and str(utn).strip():
                lines_to_scan.append((line, utn))
        for line in out_tables[f.CLM_LINE_DCMTN]:
            utn = line.get(f.CLM_LINE_PA_UNIQ_TRKNG_NUM)
            if utn and str(utn).strip():
                lines_to_scan.append((line, utn))

        for line, utn in lines_to_scan:
            mbi = get_line_mbi(line)
            if mbi and (mbi, utn) not in utn_combos:
                clm = fpk_to_clm.get(four_part_key(line)) or uniq_id_to_clm.get(
                    line.get(f.CLM_UNIQ_ID)
                )
                utn_combos[(mbi, utn)] = {
                    "from_dt": clm[f.CLM_FROM_DT],
                    "clm_type": clm[f.CLM_TYPE_CD],
                    "mac_id": clm[f.CLM_CNTRCTR_NUM],
                }

        type_2_npi_to_name = {
            row[f.PRVDR_NPI_NUM]: row[f.PRVDR_NAME]
            for row in out_tables[f.PRVDR_HSTRY]
            if row.get(f.PRVDR_NPI_NUM) and row.get(f.PRVDR_NPI_NUM) in generated_type_2_npis
        }

        existing_prior_auth_by_key = partition_rows(
            llist=init_prior_auth,
            part_by=lambda x: (x["MBI_NUM"], x["UTN"]),
        )

        prior_auths = []
        for (mbi, utn), details in utn_combos.items():
            init_rows = existing_prior_auth_by_key[(mbi, utn)] or [
                RowAdapter({}) for _ in range(self._roll_segment_count())
            ]
            # One prior authorization can be stored as multiple rows. It's composed of a header and
            # N detail/segment lines. It's a flat table so header values are repeated on every row
            # that belongs to one UTN so we generate the header fields first per MBI and UTN then
            # hand them per segment generation
            header_fields = self._gen_header_fields(
                gen_utils=gen_utils,
                mbi=mbi,
                utn=utn,
                details=details,
                seg_count=len(init_rows),
                type_2_npis=generated_type_2_npis,
                type_2_npi_to_name=type_2_npi_to_name,
                init_prior_auth=init_rows[0],
            )
            prior_auths.extend(
                [
                    self._gen_segment(
                        gen_utils=gen_utils,
                        header_fields=header_fields,
                        segment_num=idx,
                        type_1_npis=generated_type_1_npis,
                        type_2_npis=generated_type_2_npis,
                        init_prior_auth=init_row,
                    )
                    for idx, init_row in enumerate(init_rows, start=1)
                ]
            )
        return prior_auths
