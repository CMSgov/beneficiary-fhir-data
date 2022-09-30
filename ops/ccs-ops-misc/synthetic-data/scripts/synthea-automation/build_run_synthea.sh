#!/usr/local/bin/bash
set -eo pipefail

# global variables
CLEANUP="${CLEANUP:-false}" # defaults to removing inv on error, interupt, etc.

# pseudo-env variables passed in by Jenkins?
# we'll default to 'test' and 10 bene's for now
BUILD_ROOT_DIR="${BUILD_ROOT_DIR:-/opt/dev}"
TARGET_ENV="${TARGET_ENV:-test}"
BFD_BRANCH="${BFD_DEPLOY_BRANCH:-master}"

NUM_GENERATED_BENES="${NUM_GENERATED_BENES:-10}"
SKIP_VALIDATION="${SKIP_SYNTHEA_VALIDATION:-False}"

# the root will probably be passed in by Jenkins (maybe /opt?)...using /opt/dev for now
TARGET_SYNTHEA_DIR=${BUILD_ROOT_DIR}/synthea
TARGET_BFD_DIR=${BUILD_ROOT_DIR}/bfd

# we'll need to keep track of 'begin' and 'end' bene_id values necessary to perform
# various Synthea generation and validation tasks.
BEG_BENE_ID=
END_BENE_ID=
BFD_CHARACTERISTICS=

# Need to support up to 3 concurrent environments (prod, prod-sbx, test) for a given run;
# each environment uses a distinct S3 bucket; lower-case the TARGET_ENV string to ensure
# deterministic outcome(s)
TARGET_ENV=$(echo "$TARGET_ENV" | tr '[:upper:]' '[:lower:]')
declare -A S3_BUCKETS

# various S3 buckets used by the ETL pipeline
PROD_S3_BUCKET="bfd-prod-etl-577373831711"
TEST_S3_BUCKET="bfd-test-etl-577373831711"
PROD_SBX_S3_BUCKET="bfd-prod-sbx-etl-577373831711"

# save IFS context (in case bash doesn't...)
oIFS=${IFS}
# Set comma as delimiter
IFS=','
# Read the split words into an array based on comma delimiter; make
# sure there are no dupe entries
read -a arr <<< "${TARGET_ENV}"

# use assocaite array to ensure uniqueness (i.e., only one instance of ea env)
# lots of other ways to do this, but nice to have key/value pairs at our disposal
for nam in "${arr[@]}"; do
  case "${nam}" in
    test)
      S3_BUCKETS[test]="${TEST_S3_BUCKET}"
      ;;
    prod)
      S3_BUCKETS[prod]="${PROD_S3_BUCKET}"
      ;;
    prod-sbx)
      S3_BUCKETS[prod-sbx]="${PROD_SBX_S3_BUCKET}"
      ;;
    *)
	  echo "Unrecognized environemnt: ${nam}"
	  ;;
  esac
done

# create single string that can be passed to validation .py scripts; this is a comma-separated
# list of target deployment tags (i.e., prod,test)
UNIQUE_ENVS_PARAM=$(echo "${!S3_BUCKETS[*]}")
echo "enviroments to pass to .py validation scripts: ${UNIQUE_ENVS_PARAM}"

# restore IFS to original state
IFS=${oIFS}
# these names are immtuable
BFD_END_STATE_PROPERTIES="end_state.properties"
# file that is a copy of the end_state.properties file from a
# previous synthea generation run.
BFD_END_STATE_PROPERTIES_ORIG="${BFD_END_STATE_PROPERTIES}_orig"
# BFD characteristics file
BFD_CHARACTERISTICS_FILE_NAME="characteristics.csv"
# assorted variables used by the script.
BFD_SYNTHEA_AUTO_LOCATION="${TARGET_BFD_DIR}/ops/ccs-ops-misc/synthetic-data/scripts/synthea-automation"
# the directory for synthea output 
BFD_SYNTHEA_OUTPUT_LOCATION="${TARGET_SYNTHEA_DIR}/output/bfd"
# directory where Mitre synthea mapping files will be downlowded to.
MAPPING_FILES_LOCATION="${TARGET_SYNTHEA_DIR}/src/main/resources/export"

#-------------------- GENERAL SCRIPT/HOUSEKEEPING STUFF --------------------#
# Utility function to perform clean up of files generated during the course
# of executing this script; actual cleanup will be performed depending on the
# script variable CLEANUP (True == do cleanup; False == no cleanup performed)
clean_up() {
  if $CLEANUP; then
    echo "performing cleanup"
    rm -fR "${TARGET_SYNTHEA_DIR}"
    rm -fR "${TARGET_BFD_DIR}"
  fi
}
# we'll trap system interrupts and perform cleanup.
trap "clean_up" INT HUP

# utility function that can be invoked to terminate (exit) the script with a system
# status denoting non-success.
error_exit() {
  echo "Error: $*"
  clean_up
  echo
  exit 1
}

#-------------------- MAIN LOGIC --------------------#

# Function to clone the synthea generation application, scripts, and ancillary
# files from GitHub; it then builds the application via gradle.
install_synthea_from_git(){
  echo "installing synthea from git"
  git clone https://github.com/synthetichealth/synthea.git ${TARGET_SYNTHEA_DIR}
  cd ${TARGET_SYNTHEA_DIR}
  ./gradlew clean check
}

# Function to clone BFD repository from GitHub; this shell script and associated python
# scripts do not need to be built, but will need read/execute.
install_bfd_from_git(){
  echo "installing bfd from git"
  git clone https://github.com/CMSgov/beneficiary-fhir-data.git ${TARGET_BFD_DIR}
  cd ${TARGET_BFD_DIR}
  git checkout ${BFD_BRANCH}
  chmod 644 "${BFD_SYNTHEA_AUTO_LOCATION}/generate-characteristics-file.py"
  chmod 644 "${BFD_SYNTHEA_AUTO_LOCATION}/prepare-and-run-synthea.py"
  chmod 644 "${BFD_SYNTHEA_AUTO_LOCATION}/validate-synthea-load.py"
  chmod 644 "${BFD_SYNTHEA_AUTO_LOCATION}/s3_utilities.py"
  chmod 644 "${BFD_SYNTHEA_AUTO_LOCATION}/ssmutil.py"
}

# Utility function to create a python virtual environment that will be used during the
# course of executing the various python scripts used by this shell script.
activate_py_env(){
  echo "activating python env in: ${BFD_SYNTHEA_AUTO_LOCATION}"
  cd ${BFD_SYNTHEA_AUTO_LOCATION}
  python3 -m venv .venv
  source .venv/bin/activate
  pip3 install awscli boto3 psycopg2
  deactivate
}

# Function to download proprietary Mitre synthea mapping files necessary for generating
# synthetic beneficiaries and claims.
download_mapping_files_from_s3(){
  echo "download mapping files from S3 to: ${MAPPING_FILES_LOCATION}"
  cd ${BFD_SYNTHEA_AUTO_LOCATION}
  source .venv/bin/activate
  python3 ./s3_utilities.py "${MAPPING_FILES_LOCATION}" "download_file"
  deactivate
}

# Function that invokes a python S3 utility to download synthea script files.
download_script_files_from_s3(){
  echo "download script files from S3 from: ${TARGET_SYNTHEA_DIR}"
  cd ${BFD_SYNTHEA_AUTO_LOCATION}
  source .venv/bin/activate
  python3 ./s3_utilities.py "${TARGET_SYNTHEA_DIR}" "download_script"
  deactivate
}

# Function that invokes a python S3 utility to download the synthea end_state.properties file.
download_props_file_from_s3(){
  echo "download BFD ${BFD_END_STATE_PROPERTIES} file from S3 to: ${BFD_SYNTHEA_AUTO_LOCATION}"
  cd ${BFD_SYNTHEA_AUTO_LOCATION}
  source .venv/bin/activate
  python3 s3_utilities.py "./" "download_prop"

  # Make a copy of the downloaded end state properties file.
  cat ./${BFD_END_STATE_PROPERTIES} > ${BFD_END_STATE_PROPERTIES_ORIG}

  # extract the bene_id_start variable from the downloaded end state properties file.
  # It will be used in a later function/operation.
  BEG_BENE_ID=$(cat ./${BFD_END_STATE_PROPERTIES_ORIG} |grep bene_id_start |sed 's/.*=//')
  echo "BEG_BENE_ID=${BEG_BENE_ID}"
  deactivate
}

# Args:
# 1: end state properties file path
# 2: location of synthea git repo
# 3: number of beneficiaries to be generated
# 4: environment to load/validate; either: test, prod-sbx, prod or comma-delimited string containing any of them
# 5: (optional) boolean to skip validation (true); useful if re-generating a bad batch, defaults to False
#
prepare_and_run_synthea(){
  cd ${BFD_SYNTHEA_AUTO_LOCATION}
  source .venv/bin/activate
  python3 prepare-and-run-synthea.py "${BFD_END_STATE_PROPERTIES}" "${TARGET_SYNTHEA_DIR}" "${NUM_GENERATED_BENES}" "${UNIQUE_ENVS_PARAM}" "${SKIP_VALIDATION}"
  deactivate
}

# Function that invokes an S3 utility to upload new generated synthetic data to an
# S3 folder where the BFD ETL pipeline will discover the synthetic RIF files and
# load them data into the appropriate BFD database.
upload_synthea_results_to_s3(){
  echo "upload synthea results to S3"
  cd ${BFD_SYNTHEA_AUTO_LOCATION}

  # first make a copy of all the .csv files into a "backup" directory
  mkdir ${BFD_SYNTHEA_OUTPUT_LOCATION}/backup
  cp ${BFD_SYNTHEA_OUTPUT_LOCATION}/*.csv ${BFD_SYNTHEA_OUTPUT_LOCATION}/backup

  # now upload the RIF (.csv) files to S3 ETL bucket(s); one per environment, passed in as arg
  source .venv/bin/activate
  for s3_bucket in "${S3_BUCKETS[@]}"; do
    echo "uploading RIF files to: ${s3_bucket}"
    python3 ./s3_utilities.py "${BFD_SYNTHEA_OUTPUT_LOCATION}" "upload_synthea_results" "${s3_bucket}"
  done
  deactivate
}

# Function that invokes an S3 utility to wait until the manifest file (0_manifest.xml) shows
# up in the S3 bucket's /Done folder; the ETL pipeline moves the manifest file once it has
# completed processing of the /Incoming RIF (.csv) files.
wait_for_manifest_done(){
  echo "waiting for manifest Done"
  cd ${BFD_SYNTHEA_AUTO_LOCATION}

  # wait until the 0_manifest.xml file exists in the /Done folder for all environments being prcoessed; this
  # op is synchronous meaning that we'll check/wait on one environemnt at a time. Since we have an 'all or nothing'
  # processing model, the extra time to do this synchronously really doesn't matter.
  source .venv/bin/activate
  for s3_bucket in "${S3_BUCKETS[@]}"; do
    python3 ./s3_utilities.py "${BFD_SYNTHEA_OUTPUT_LOCATION}" "wait_for_manifest_done" "${s3_bucket}"
  done
  deactivate
}


# Args:
# 1: bene id start (inclusive, taken from previous end state properties / synthea properties file)
# 2: bene id end (exclusive, taken from new output end state properties)
# 3: file system location of synthea folder
# 4: which environments to check, should be a single comma separated string consisting of test,sbx,prod or any combo of the three (example "test,sbx,prod" or "test")
do_load_validation(){
  cd ${BFD_SYNTHEA_AUTO_LOCATION}
  # restore the copy of the RIF (.csv) files back to the synthea output directory
  cp ${BFD_SYNTHEA_OUTPUT_LOCATION}/backup/*.csv ${BFD_SYNTHEA_OUTPUT_LOCATION}

  # now perform the validation of the run.
  END_BENE_ID=$(cat ${BFD_SYNTHEA_OUTPUT_LOCATION}/${BFD_END_STATE_PROPERTIES} |grep bene_id_start |sed 's/.*=//')
  echo "END_BENE_ID=${END_BENE_ID}"

  source .venv/bin/activate
  python3 validate-synthea-load.py "${BEG_BENE_ID}" "${END_BENE_ID}"  "${TARGET_SYNTHEA_DIR}" "${UNIQUE_ENVS_PARAM}"
  deactivate
}

# Args:
# 1: bene id start (inclusive, taken from previous end state properties / synthea properties file)
# 2: bene id end (exclusive, taken from new output end state properties)
# 3: file system location to write the characteristics file
# 4: which environment to check, should be a single value from the list of [test prd-sbx prod]
#
# script will check the number of lines written to the characteristics.csv file; if only the header row (row #1)
# then we'll exit out.
gen_characteristics_file(){
  cd ${BFD_SYNTHEA_AUTO_LOCATION}
  source .venv/bin/activate
  python3 generate-characteristics-file.py "${BEG_BENE_ID}" "${END_BENE_ID}"  "${BFD_SYNTHEA_AUTO_LOCATION}" "${UNIQUE_ENVS_PARAM}"

  # check the generated output file; must have more than just the header line
  line_cnt=$(cat ${BFD_SYNTHEA_AUTO_LOCATION}/${BFD_CHARACTERISTICS_FILE_NAME} |wc -l)
  if [[ "$line_cnt" -gt 1 ]]; then
    BFD_CHARACTERISTICS="${BFD_SYNTHEA_AUTO_LOCATION}/${BFD_CHARACTERISTICS_FILE_NAME}"
    echo "${BFD_SYNTHEA_OUTPUT_LOCATION}/${BFD_CHARACTERISTICS_FILE_NAME} successfully discovered"
  else
    echo "failed to read meaningful data from: ${BFD_SYNTHEA_OUTPUT_LOCATION}/${BFD_CHARACTERISTICS_FILE_NAME}"
  fi
  deactivate
}

# Function to upload the new end state properties file to an S3 bucket; it also extracts
# the newest bene_id_start variable for use in a later function/operation.
upload_characteristics_file_to_s3(){
  echo "upload ${BFD_CHARACTERISTICS_FILE_NAME} file to S3"
  cd ${BFD_SYNTHEA_AUTO_LOCATION}
  source .venv/bin/activate
  python3 ./s3_utilities.py "${BFD_SYNTHEA_AUTO_LOCATION}" "upload_characteristics"
  deactivate
}

# Function to upload the new end state properties file to an S3 bucket; it also extracts
# the newest bene_id_start variable for use in a later function/operation.
upload_props_file_to_s3(){
  echo "upload end_state.properties file to S3"
  cd ${BFD_SYNTHEA_AUTO_LOCATION}
  source .venv/bin/activate
  python3 ./s3_utilities.py "${BFD_SYNTHEA_OUTPUT_LOCATION}" "upload_prop" "${BFD_SYNTHEA_AUTO_LOCATION}"
  deactivate
}


#----------------- GO! ------------------#
# genearal fail-safe to perform cleanup of any directories and files germane to executing
# this shell script.
clean_up

# invoke function to clone the Synthea repo and build it.
install_synthea_from_git

# invoke function to clone the BFD repo.
install_bfd_from_git

# invoke function to create a python virtual environment.
activate_py_env

# invoke function to download proprietary Mitre mapping files.
download_mapping_files_from_s3

# invoke function to download proprietary Mitre shell script files.
download_script_files_from_s3

# invoke function to download (if available) a previous run's end_state.properties file.
download_props_file_from_s3

# invoke function to invoke BFD .py script that verifies that:
#  1) we have all the files necessary to perform a synthea generation run.
#  2) executes a synthea generation run
prepare_and_run_synthea

# Invoke a function to upload the generated RIF files to the appropriate BFD
# ETL pipeline S3 /Incoming bucket, where the ETL process will pick them up and
# load the synthetic data into database.
upload_synthea_results_to_s3

# Invoke a functionn to wait on / check the appropriate BFD ETL pipeline S3 bucket for
# the 0_manifest file to appear in the S3 bucket's /Done folder.
wait_for_manifest_done

# Invoke a function that executes a .py script that performs validation of data for
# the just executed ETL pipeline load.
if ! $SKIP_VALIDATION; then
  do_load_validation
fi

# Invoke function that executes a .py script that generates a new synthea characteristics
# file and uploads it to S3.
if [[ -n ${BEG_BENE_ID} && -n ${END_BENE_ID} ]]; then
  gen_characteristics_file
else
  error_exit "end state BENE_ID variables unset...exiting"
fi

# Invoke a function to upload the new end_state.properties file and the new characteristics.csv file
if [[ -n ${BEG_BENE_ID} && -n ${BFD_CHARACTERISTICS} ]]; then
  upload_props_file_to_s3
else
  error_exit "end state BEG_BENE_ID or BFD_CHARACTERISTICS variables unset...exiting"
fi
echo
echo "============================================="
echo "BFD Synthea Generation completed SUCCESFULLY!"
echo "============================================="
# return SUCCESS
exit 0;
