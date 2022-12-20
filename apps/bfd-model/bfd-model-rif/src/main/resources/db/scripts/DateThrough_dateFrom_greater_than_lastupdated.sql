SELECT clm_id,  bene_id, clm_from_dt, clm_thru_dt
FROM carrier_claims
WHERE last_updated >= clm_from_dt OR last_updated >= clm_thru_dt;


SELECT clm_id,  bene_id, clm_from_dt, clm_thru_dt
FROM dme_claims
WHERE last_updated >= clm_from_dt OR last_updated >= clm_thru_dt;


SELECT clm_id,  bene_id, clm_from_dt, clm_thru_dt
FROM hha_claims
WHERE last_updated >= clm_from_dt OR last_updated >= clm_thru_dt;


SELECT clm_id,  bene_id, clm_from_dt, clm_thru_dt
FROM hospice_claims
WHERE last_updated >= clm_from_dt OR last_updated >= clm_thru_dt;


SELECT clm_id,  bene_id, clm_from_dt, clm_thru_dt
FROM inpatient_claims
WHERE last_updated >= clm_from_dt OR last_updated >= clm_thru_dt;


SELECT clm_id,  bene_id, clm_from_dt, clm_thru_dt
FROM outpatient_claims
WHERE last_updated >= clm_from_dt OR last_updated >= clm_thru_dt;


SELECT clm_id,  bene_id, clm_from_dt, clm_thru_dt
FROM snf_claims
WHERE last_updated >= clm_from_dt OR last_updated >= clm_thru_dt;