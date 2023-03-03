#################################################################
# To use...
# 
# 1. modify dataDictRoot and outputRoot variable expressions below to point to approp locations on local env
# 2. run all root var expressions (highlight expressions + F8)
# 3. run desired trasnformer expression (e.g. CSV, JSON, HTML)
#################################################################


########################
# setup root variables
########################

$dataDictRoot = "c:\Users\621222\dev\projects\beneficiary-fhir-data\apps\bfd-server\dev\bfd-data-dictionary"
$outputRoot = "c:\output"
#
$appRoot = "$dataDictRoot\app\dd-transformer"
$dataRoot = "$dataDictRoot\data"



###########
# CSV
###########

# V1
& python $appRoot\dd_to_csv.py --template $appRoot\template\v1-to-csv.json --source $dataRoot\V1 --target $outputRoot\BFD-V1-data-dictionary.csv 

# V2
& python $appRoot\dd_to_csv.py --template $appRoot\template\v2-to-csv.json --source $dataRoot\V2 --target $outputRoot\BFD-V2-data-dictionary.csv 

######################
# Consolidated JSON
######################

# V1
& python $appRoot\dd_to_json.py --source $dataRoot\V1 --target $outputRoot\BFD-V1-data-dictionary.json 

# V2
& python $appRoot\dd_to_json.py --source $dataRoot\V2 --target $outputRoot\BFD-V2-data-dictionary.json 


#######################################################
# HTML
# note: dependency on consolidated JSON file generated above
#######################################################

# V1
& python $appRoot\dd_to_html.py --templateDir $appRoot\template --templateFile v1-to-html.html --source $outputRoot\BFD-V1-data-dictionary.json --target $outputRoot\BFD-V1-data-dictionary.html


# V2
& python $appRoot\dd_to_html.py --templateDir $appRoot\template --templateFile v2-to-html.html --source $outputRoot\BFD-V2-data-dictionary.json --target $outputRoot\BFD-V2-data-dictionary.html


########################################################################################
# PDF
# assumes you have built HTML file (see above) and then use 3rd party tool to convert to PDF
# example: wkhtmltopdf;  
# TODO: assess security, licensing of this library; look for built-in python libraries e.g. fpdf, pdfconverter, etc.
########################################################################################

