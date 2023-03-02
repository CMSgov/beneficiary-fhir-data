#################################################################
# To use...
# 
# Switch to dd-transformer directory  
# e.g. > cd \beneficiary-fhir-data\apps\bfd-server\dev\bfd-data-dictionary\app\dd-transformer
#
# Change target param in commands below to desired location (default is c:\output)
#
# cd c:\Users\621222\dev\projects\beneficiary-fhir-data\apps\bfd-server\dev\bfd-data-dictionary\app\dd-transformer
#################################################################


###########
# CSV
###########

# V1
python dd_to_csv.py --template .\template\v1-to-csv.json --source ..\..\data\V1\ --target c:\output\BFD-V1-data-dictionary.csv 

# V2
python dd_to_csv.py --template .\template\v2-to-csv.json --source ..\..\data\V2\ --target c:\output\BFD-V2-data-dictionary.csv 

###########
# JSON
###########

# V1
python dd_to_json.py --source ..\..\data\V1\ --target c:\output\BFD-V1-data-dictionary.json 

# V2
python dd_to_json.py --source ..\..\data\V2\ --target c:\output\BFD-V2-data-dictionary.json 





###########
# HTML
###########

# V1
python dd_to_html.py --template v1-to-html.html --source c:\output\BFD-V1-data-dictionary.json --target c:\output\BFD-V1-data-dictionary.html 

# V2
python dd_to_html.py --template v2-to-html.html --source c:\output\BFD-V2-data-dictionary.json --target c:\output\BFD-V2-data-dictionary.html 



###########
# PDF
# assumes you have built HTML file (see above) and then using 3rd party tool to convert to PDF
###########


# V1
cd c:\users\621222\dev\projects\bfd-data-dictionary\dist\v1
$path = "C:\users\621222\dev\devTools\wkhtmltopdf\bin\wkhtmltopdf.exe"
& $path BFD-V1-data-dictionary.html BFD-V1-data-dictionary.pdf

# V2
cd c:\users\621222\dev\projects\bfd-data-dictionary\dist\v2
$path = "C:\users\621222\dev\devTools\wkhtmltopdf\bin\wkhtmltopdf.exe"
& $path BFD-V2-data-dictionary.html BFD-V2-data-dictionary.pdf
