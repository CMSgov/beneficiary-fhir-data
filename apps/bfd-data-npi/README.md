BFD NPI to Organization Name Lookup
=======================================

This maven project produces a jar file that provides functionality to retrieve organization names given an NPI as input. This lookup is backed by a data file stored in the jar files as a resource.

For more information about our NPI source can be found here:
https://www.cms.gov/Regulations-and-Guidance/Administrative-Simplification/NationalProvIdentStand/DataDissemination

The latest data source file can be downloaded here:
https://download.cms.gov/nppes/NPI_Files.html

The data file is constructed from 
NPI|Entity Type Code|Replacement NPI|Employer Identification Number (EIN)|Provider Organization Name (Legal Business Name)|Provider Last Name (Legal Name)|Provider First Name|
--- | --- | --- | --- |--- |--- |--- 
1497758544|2| |<UNAVAIL>|CUMBERLAND COUNTY HOSPITAL SYSTEM, INC| |
1215930367|1| | | |GRESSOT|LAURENT|
1023011178|2| |<UNAVAIL>|COLLABRIA CARE| |
1932102084|1| | | |ADUSUMILLI|RAVI|
1841293990|2| | | |WORTSMAN|SUSAN|
1750384806|3| | | |BISBEE|ROBERT|
1669475711|4| | | |SUNG|BIN|


The data file is constructed by stripping out all the entity code type 1 (Individual NPI Numbers) and only leaving the entity type code 2 (Organization NPI Numbers). With only the Organization NPI Numbers left, a file is constructed that contains the NPI Number and the Provider Organization Name (Legal Business Name).

NPI|Provider Organization Name (Legal Business Name)
--- | --- 
1497758544|CUMBERLAND COUNTY HOSPITAL SYSTEM, INC
1023011178|COLLABRIA CARE


