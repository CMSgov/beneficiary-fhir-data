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



How These File are Generated
=============================

The CSVs here are generated running the script
`build_tables.py` against 
https://github.com/cmsgovbluebutton-static-site using a screen scraper 
BeautifulSoup.  There is a better way to do this, but this is how its currently 
done so we can have a published baseline.

