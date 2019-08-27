Bluebutton CSV Codesets
=======================

This repo contains the codesets and variable metadata that may be found in 
BlueButton API responses.


Below are explanations of the different CSV file types contained herein.


All Meta
========

This single csv file `all_meta.csv` contains a list of all code sets enumerated here.

Meta
====

Metadata on a per variable basis.  These filenames follow the form 
`variable_meta.csv`.

Codes
=====

Code sets per variable. Note that not all variables have enumerated code sets.
These filenames follow the form `variable.csv`.

Most Popular Codes
==================

These are the most common CMS variables/codes seen in  API responses. 


1. Claim ID: https://bluebutton.cms.gov/resources/variables/clm_id/
2. Claim Source Inpatient Admission Code: https://bluebutton.cms.gov/resources/variables/clm_src_ip_admsn_cd/ 
3. Claim Attending Physician Specialty Code: https://bluebutton.cms.gov/resources/variables/at_physn_spclty_cd/
4. Carrier Claim Provider Assignment Indicator Switch: https://bluebutton.cms.gov/resources/variables/asgmntcd/
5. Medicare Status Code: https://bluebutton.cms.gov/resources/variables/ms_cd/
6. Carrier Claim Payment Denial: Code https://bluebutton.cms.gov/resources/variables/carr_clm_pmt_dnl_cd/
7. NCH Near Line Record Identification Code (RIC): https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd/
8. Carrier Line Provider Type Code: https://bluebutton.cms.gov/resources/variables/carr_line_prvdr_type_cd
9. Carrier or MAC Number: https://bluebutton.cms.gov/resources/variables/carr_num/
10. Line CMS Type Service Code: https://bluebutton.cms.gov/resources/variables/line_cms_type_srvc_cd/
11. Line Berenson-Eggers Type of Service (BETOS) Code: https://bluebutton.cms.gov/resources/variables/betos_cd/
12. Claim Healthcare Common Procedure Coding System (HCPCS) Year Code: https://bluebutton.cms.gov/resources/variables/carr_clm_hcpcs_yr_cd/







How These File are Generated
=============================

The CSVs here are generated running the script
`build_tables.py` against 
https://github.com/cmsgovbluebutton-static-site using a screen scraper 
BeautifulSoup.  There is a better way to do this, but this is how its currently 
done so we can have a published baseline.

