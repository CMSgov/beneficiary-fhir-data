#!/usr/local/bin/bash
set -eo pipefail

# pseudo-env variables passed in by Jenkins?
# we'll default to 'test' and 10 bene's
TARGET_ENV="${TARGET_ENV:-test}"
NUM_GENERATED_BENES="${NUM_GENERATED_BENES:-10}"


# skip validation flag???

# global variables
PROGNAME=${0##*/}
CLEANUP="${CLEANUP:-true}" # defaults to removing inv on error, interupt, etc.

# the root will probably be passed in by Jenkins (maybe /opt?)...using /tmp for now
TARGET_SYNTHEA_DIR=/tmp/synthea
TARGET_BFD_DIR=/tmp/bfd

BEG_BENE_ID=
END_BENE_ID=


# thse are sort of immtuable, aren't they?
BFD_END_STATE_BUCKET="bfd-test-synthea-etl-577373831711"
BFD_END_STATE_PROPERTIES="end_state.properties"

BFD_SYNTHEA_AUTO_LOCATION="${TARGET_BFD_DIR}/ops/ccs-ops-misc/synthetic-data/scripts/synthea-automation"
BFD_SYNTHEA_OUTPUT_LOCATION="${TARGET_SYNTHEA_DIR}/output/bfd"

MAPPING_FILES_LOCATION="${TARGET_SYNTHEA_DIR}/src/main/resources/export"


# Git branch to build from...how does this actually work? from build params?
BFD_BRANCH="cmac/BFD-1912-Jenkins-Build-Synthea-Pipeline"

br_name="master" # name of the main branch
br_name_re="^(master|main)$"

mvn_dep_ver="2.10"
mvn_dep_cmd="mvn org.apache.maven.plugins:maven-dependency-plugin:${mvn_dep_ver}:list"


#-------------------- GENERAL SCRIPT/HOUSEKEEPING STUFF --------------------#

clean_up() {
  if $CLEANUP; then
    echo "performing cleanup"
    rm -fR "${TARGET_SYNTHEA_DIR}"
    rm -fR "${TARGET_BFD_DIR}"
  fi
}
#trap "clean_up" INT HUP


error_exit() {
  echo "Error: $*"
  echo
  clean_up
  exit 1
}


#-------------------- MAIN LOGIC --------------------#

install_synthea_from_git(){
  echo "installing synthea from git"
  git clone https://github.com/synthetichealth/synthea.git ${TARGET_SYNTHEA_DIR}
  cd ${TARGET_SYNTHEA_DIR}
  ./gradlew clean check
}


install_bfd_from_git(){
  echo "installing bfd from git"
  git clone https://github.com/CMSgov/beneficiary-fhir-data.git ${TARGET_BFD_DIR}
  cd ${TARGET_BFD_DIR}
  git checkout ${BFD_BRANCH}
  cd ${BFD_SYNTHEA_AUTO_LOCATION}
  chmod 644 *.py
}

activate_py_env(){
  echo "activating python env in: ${BFD_SYNTHEA_AUTO_LOCATION}"
  cd ${BFD_SYNTHEA_AUTO_LOCATION}
  python3 -m venv .venv
  source .venv/bin/activate
  pip3 install awscli boto3 psycopg2
  deactivate
}

download_s3_mapping_files(){
  echo "download mapping files from S3 to: ${MAPPING_FILES_LOCATION}"
  cd ${BFD_SYNTHEA_AUTO_LOCATION}
  source .venv/bin/activate
  python3 ./s3_utilities.py "${MAPPING_FILES_LOCATION}" "download_file"
  deactivate
}

download_s3_script_files(){
  echo "download script files from S3 from: ${TARGET_SYNTHEA_DIR}"
  cd ${BFD_SYNTHEA_AUTO_LOCATION}
  source .venv/bin/activate
  python3 ./s3_utilities.py "${TARGET_SYNTHEA_DIR}" "download_script"
  deactivate
}


download_s3_props_file(){
  echo "download BFD end_state.properties file from S3 to: ${BFD_SYNTHEA_AUTO_LOCATION}"
  cd ${BFD_SYNTHEA_AUTO_LOCATION}
  source .venv/bin/activate
  python3 s3_utilities.py "./" "download_prop"
  cat ./end_state.properties |grep bene_id_start |sed 's/.*=//' > end_state.properties_orig
  deactivate
}

upload_s3_props_file(){
  echo "upload end_state.properties file from S3"
  cd ${BFD_SYNTHEA_AUTO_LOCATION}
  source .venv/bin/activate
  python3 ./s3_utilities.py "${BFD_SYNTHEA_OUTPUT_LOCATION}/${BFD_END_STATE_PROPERTIES}" "upload_prop"
  deactivate
}

# Args:
# 1: end state properties file path
# 2: location of synthea git repo
# 3: number of beneficiaries to be generated
# 4: environment to load/validate; should be either: test, prod-sbx, prod
# 5: (optional) boolean to skip validation (true); useful if re-generating a bad batch, defaults to False
#
prepare_and_run_synthea(){
  cd ${BFD_SYNTHEA_AUTO_LOCATION}
  source .venv/bin/activate
  python3 prepare-and-run-synthea.py ${BFD_END_STATE_PROPERTIES} ${TARGET_SYNTHEA_DIR} ${NUM_GENERATED_BENES} "${TARGET_ENV}" True
  deactivate
}


upload_synthea_results(){
  echo "upload synthea results to S3"
  cd ${BFD_SYNTHEA_AUTO_LOCATION}
  source .venv/bin/activate
  python3 ./s3_utilities.py "${BFD_SYNTHEA_OUTPUT_LOCATION}" "upload_synthea_results"
  deactivate
}


# Args:
# 1: bene id start (inclusive, taken from previous end state properties / synthea properties file)
# 2: bene id end (exclusive, taken from new output end state properties)
# 3: file system location of synthea folder
# 4: which environments to check, should be a single comma separated string consisting of test,sbx,prod or any combo of the three (example "test,sbx,prod" or "test")
do_load_validation(){
  cd ${BFD_SYNTHEA_AUTO_LOCATION}
  cat ${BFD_SYNTHEA_OUTPUT_LOCATION}/end_state.properties |grep bene_id_start |sed 's/.*=//' > end_state.properties_new
  BEG_BENE_ID=`cat end_state.properties_orig`
  END_BENE_ID=`cat end_state.properties_new`
  source .venv/bin/activate
  #python3 generate-characteristics-file.py "${BEG_BENE_ID}" "${END_BENE_ID}"  "${BFD_SYNTHEA_AUTO_LOCATION}" "${TARGET_ENV}"
  deactivate
}


# Args:
# 1: bene id start (inclusive, taken from previous end state properties / synthea properties file)
# 2: bene id end (exclusive, taken from new output end state properties)
# 3: file system location to write the characteristics file
# 4: which environment to check, should be a single value from the list of [test prd-sbx prod]
gen_characteristics_file(){
  cd ${BFD_SYNTHEA_AUTO_LOCATION}
  source .venv/bin/activate
  python3 generate-characteristics-file.py "${BEG_BENE_ID}" "${END_BENE_ID}"  "${BFD_SYNTHEA_AUTO_LOCATION}" "${TARGET_ENV}"
  deactivate
}



#----------------- GO! ------------------#
clean_up

install_synthea_from_git

install_bfd_from_git

activate_py_env

download_s3_mapping_files

download_s3_script_files

download_s3_props_file

prepare_and_run_synthea

upload_synthea_results

do_load_validation

gen_characteristics_file


exit 0;
