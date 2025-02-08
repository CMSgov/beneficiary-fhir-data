from pydantic import BaseModel
from datetime import date


class IdrBeneficiary(BaseModel):
    bene_sk: int
    bene_mbi_id: str
    bene_1st_name: str
    bene_last_name: str
    # idr_trans_efctv_ts: date
    # idr_trans_obslt_ts: date
