import random
import string
from datetime import date, timedelta

import field_constants as f
from claims_util import four_part_key
from generator_util import (
    BENE_HSTRY,
    CLM,
    CLM_LINE,
    CLM_LINE_DCMTN,
    PRVDR_HSTRY,
    GeneratorUtil,
    RowAdapter,
    gen_npi_id,
)


class PriorAuthGeneratorUtil:
    def gen_prior_auths(
        self,
        gen_utils: GeneratorUtil,
        files: dict[str, list[RowAdapter]],
        out_tables: dict[str, list[RowAdapter]],
        generated_type_1_npis: list[str],
        generated_type_2_npis: list[str],
    ) -> list[RowAdapter]:
        # Map BENE_SK to BENE_MBI_ID
        bene_sk_to_mbi = {}
        for row in files[BENE_HSTRY]:
            sk, mbi = str(row["BENE_SK"]), row.get("BENE_MBI_ID")
            if mbi and (row.get("IDR_LTST_TRANS_FLG") == "Y" or sk not in bene_sk_to_mbi):
                bene_sk_to_mbi[sk] = mbi

        # Build mapping of four_part_key and CLM_UNIQ_ID to the claim row
        fpk_to_clm = {four_part_key(row): row for row in out_tables[CLM]}
        uniq_id_to_clm = {row[f.CLM_UNIQ_ID]: row for row in out_tables[CLM]}

        def get_line_mbi(line_row):
            fpk = four_part_key(line_row)
            clm = fpk_to_clm.get(fpk) or uniq_id_to_clm.get(line_row.get(f.CLM_UNIQ_ID))
            bene_sk = clm[f.BENE_SK] if clm else line_row.get(f.BENE_SK)
            return bene_sk_to_mbi.get(str(bene_sk)) if bene_sk else None

        # Collect unique (mbi, utn) combinations from lines
        utn_combos = {}
        lines_to_scan = []
        for line in out_tables[CLM_LINE]:
            utn = line.get(f.CLM_LINE_PMD_UNIQ_TRKNG_NUM) or line.get(f.CLM_LINE_PA_UNIQ_TRKNG_NUM)
            if utn and str(utn).strip():
                lines_to_scan.append((line, utn))
        for line in out_tables[CLM_LINE_DCMTN]:
            utn = line.get(f.CLM_LINE_PA_UNIQ_TRKNG_NUM)
            if utn and str(utn).strip():
                lines_to_scan.append((line, utn))

        for line, utn in lines_to_scan:
            mbi = get_line_mbi(line)
            if mbi and (mbi, utn) not in utn_combos:
                clm = fpk_to_clm.get(four_part_key(line)) or uniq_id_to_clm.get(line.get(f.CLM_UNIQ_ID))
                utn_combos[(mbi, utn)] = {
                    "from_dt": clm[f.CLM_FROM_DT],
                    "clm_type": clm[f.CLM_TYPE_CD],
                    "mac_id": clm[f.CLM_CNTRCTR_NUM],
                }

        ccn_list = ["39T14", "001500", "001502", "001503", "001504", "001505", "001509", "001510"]
        type_2_npi_to_name = {
            row[f.PRVDR_NPI_NUM]: row[f.PRVDR_NAME]
            for row in out_tables[PRVDR_HSTRY]
            if row.get(f.PRVDR_NPI_NUM) and row.get(f.PRVDR_NPI_NUM) in generated_type_2_npis
        }

        def add_days_iso(dt_str: str, days: int) -> str:
            base_date = date.fromisoformat(dt_str.split()[0])
            return (base_date + timedelta(days=days)).isoformat()

        prauc_rows = []
        for (mbi, utn), details in utn_combos.items():
            seg_count = 1
            while random.random() < 0.70 and seg_count < 19:
                seg_count += 1

            utn_valid_st_dt = details["from_dt"]
            utn_valid_en_dt = add_days_iso(utn_valid_st_dt, 365)
            pa_ind = random.choice(["H004", "H006", "H002", "H007", "H003", "A009", "H005", "D001", "A007", "D004", "A006", "A008", "B517"])
            clm_type_cd = str(details["clm_type"])
            val = int(clm_type_cd) if clm_type_cd.isdigit() else 0
            if val in (50, 1900, 2900,1081,1082,2081,2082):
                clm_type = "C"
            elif val in (10, 60, 61, 62, 63, 64,1032,1033,1034,2032,2033,2034):
                clm_type = "H"
            elif val in (30, 72, 81, 82, 1800, 2800):
                clm_type = "D"
            elif val in (40, 1013, 2013, 1023, 2023):
                clm_type = "O"
            elif val in (20, 60, 61, 62, 63, 64,1011,1012,2011,2012,1021,1022,2021,2022,1018,2018):
                clm_type = "I"
            else:
                clm_type = "B"
            mac_id = str(details["mac_id"])
            icn_dcn = "".join(random.choices(string.digits, k=14))
            pa_dt_added = add_days_iso(utn_valid_st_dt, -10)
            pa_dt_updated = pa_dt_added
            pa_req_sub_dt = add_days_iso(pa_dt_added, 1)
            pa_req_rec_dt = add_days_iso(pa_req_sub_dt, -1)
            pa_decision_dt = pa_req_sub_dt
            pa_decision_exp_dt = add_days_iso(pa_decision_dt, 365)

            npi = random.choice(generated_type_2_npis) if generated_type_2_npis else gen_npi_id("PRVDR_SK")
            name = type_2_npi_to_name.get(npi) or random.choice(["CBS PHARMACY", "WAL-PART PHARMACY", "BITE AID PHARMACY", "HEALTHCARE CENTER"])

            cms_cert = random.choice(ccn_list)
            rev_code_1 = "    " if random.random() < 0.80 else "0024"
            tob = random.choices(["32X", "13X", "   "], weights=[80, 10, 10])[0]
            svc_render_st = random.choices(["TX", "FL", "IL", "OK"], weights=[50, 30, 10, 10])[0]

            for seg_idx in range(1, seg_count + 1):
                current_segment = str(seg_idx)
                hcpcs = random.choice(["G0299", "G0151", "G0300", "G0157", "G0495", "G0496", "G0493", "G0494", "G0152", "G0162"])
                service_cnts = random.randint(1, 15)
                pa_decision = random.choice(gen_utils.code_systems.get("PA_DECISION", ["A", "P", "N"]))

                order_refer_npi = random.choice(generated_type_1_npis) if generated_type_1_npis else gen_npi_id("PRVDR_SK")
                render_npi = random.choice(generated_type_1_npis + generated_type_2_npis) if (generated_type_1_npis or generated_type_2_npis) else gen_npi_id("PRVDR_SK")
                operate_npi = random.choice(generated_type_1_npis) if generated_type_1_npis else gen_npi_id("PRVDR_SK")

                price_mod1 = "  " if random.random() < 0.80 else random.choice(["NU", "RR", "50"])
                place_of_serv = "  " if random.random() < 0.80 else random.choice(["24", "11", "41"])

                mr_count_ind = random.randint(0, 100)
                mr_count_st_dt = add_days_iso(pa_req_rec_dt, random.randint(-21, 21))
                mr_count_end_dt = add_days_iso(mr_count_st_dt, 365)
                att_phy_npi = random.choice(generated_type_1_npis) if generated_type_1_npis else gen_npi_id("PRVDR_SK")
                rrb_excl_ind = " " if random.random() < 0.90 else "Y"

                row_data = {
                    "MBI_NUM": mbi,
                    "CAN": "",
                    "EQUAT_BIC": "",
                    "UTN": utn,
                    "SEGMENT_COUNT": seg_count,
                    "CURRENT_SEGMENT": current_segment,
                    "UTN_VALID_ST_DT": utn_valid_st_dt,
                    "UTN_VALID_EN_DT": utn_valid_en_dt,
                    "PA_IND": pa_ind,
                    "CLM_TYPE": clm_type,
                    "HCPCS_OR_CPT_OR_HIPPS": hcpcs,
                    "ICD_PROC_IND": " ",
                    "ICD_PROC_CODE": "       ",
                    "MAC_ID": mac_id,
                    "PA_FILLER": "    ",
                    "ICN_DCN": icn_dcn,
                    "PA_DT_ADDED": pa_dt_added,
                    "PA_DT_UPDATED": pa_dt_updated,
                    "SERVICE_CNTS": service_cnts,
                    "PA_DECISION": pa_decision,
                    "PA_REQ_SUB_DT": pa_req_sub_dt,
                    "PA_REQ_REC_DT": pa_req_rec_dt,
                    "PA_DECISION_DT": pa_decision_dt,
                    "PA_DECISION_EXP_DT": pa_decision_exp_dt,
                    "ORDER_REFER_NPI": order_refer_npi,
                    "RENDER_NPI": render_npi,
                    "OPERATE_NPI": operate_npi,
                    "SVC_RENDER_ST": svc_render_st,
                    "PRICE_MOD1": price_mod1,
                    "PRICE_MOD2": "  ",
                    "PLACE_OF_SERV": place_of_serv,
                    **{f"ICD_DIAG_IND_{i}": " " for i in range(1, 6)},
                    **{f"ICD_DIAG_CODE_{i}": "       " for i in range(1, 6)},
                    "NPI": npi,
                    "NAME": name,
                    "CMS_CERT": cms_cert,
                    "REV_CODE_1": rev_code_1,
                    **{f"REV_CODE_{i}": "    " for i in range(2, 21)},
                    **{f"COND_CODE_{i}": "  " for i in range(1, 5)},
                    **{f"OCCUR_CODE_{i}": "  " for i in range(1, 5)},
                    "TOB": tob,
                    "MR_COUNT_IND": mr_count_ind,
                    "MR_COUNT_ST_DT": mr_count_st_dt,
                    "MR_COUNT_END_DT": mr_count_end_dt,
                    "ATT_PHY_NPI": att_phy_npi,
                    "RRB_EXCL_IND": rrb_excl_ind,
                    "IDR_INSRT_TS": pa_req_rec_dt,
                }
                prauc_rows.append(RowAdapter(row_data))

        return prauc_rows
