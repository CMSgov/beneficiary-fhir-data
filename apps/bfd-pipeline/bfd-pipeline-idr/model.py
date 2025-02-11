from pydantic import BaseModel


class IdrBeneficiary(BaseModel):
    bene_sk: int
    bene_mbi_id: str
    bene_1st_name: str
    bene_last_name: str
