Get all MBIs and BENE_SKs for a bene

```sql
SELECT 
    hstry.bene_mbi_id, 
    hstry.bene_sk, 
    hstry.bene_mbi_id = bene.bene_mbi_id AS is_current_mbi
FROM CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE bene
JOIN CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_HSTRY hstry ON bene.bene_xref_efctv_sk_computed = hstry.bene_xref_efctv_sk_computed
WHERE 
    bene.bene_mbi_id = '<mbi>' 
    AND (hstry.bene_mbi_id IS NOT NULL OR hstry.bene_sk != bene.bene_sk)
    AND NOT EXISTS (SELECT 1 FROM idr.overshare_mbis iom WHERE iom.bene_mbi_id = bene.bene_mbi_id or iom.bene_mbi_id = hstry.bene_mbi_id)
GROUP BY hstry.bene_mbi_id, hstry.bene_sk, bene.bene_mbi_id
```
