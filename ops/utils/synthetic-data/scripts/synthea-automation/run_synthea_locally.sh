#!/usr/bin/env bash
set -eo pipefail

# global variables
CLEANUP="True" # defaults to removing generated files on error, interupt, etc.

help() {
  echo
  echo "run_synthea_locally.sh:"
  echo "----------------------  ------------------------------------------------------------------------"
  echo "--num, -n               : number of beneficiaries to generate (default 100)"
  echo "--build_root, -b        : root directory for Synthea build (default /tmp)"
  echo "--target_env, -t        : comma-separated string, combination of: 'prod', 'test', 'prod-sbx' (default test)"
  echo "--synthea_jar, -j       : boolean (true/false) indicating to use Synthea Release jar file (default true)"
  echo "--cleanup, -c           : boolean (true/false) whether to perform 'pre' and 'post' file cleanup (default true)"
  echo "--num_future_months, -f : number of months into the future claims can have their claim dates set to (default 0)"
  echo "--target_contract, -r  : indicates a partD contract to tie all generated benes to if --use_target_contract is set to true"
  echo "--use_target_contract, -u : If set to true, indicates to tie all generated items to a single partD contract specified by --target_contract, must be 5 characters (default Y9999)"

  exit 1;
}

# setup vars with defaults; override from ars as appropriate
# root directory of BFD git branch
BFD_ROOT_DIR=${PWD}
# root directory for checkout of Synthea git master
BUILD_ROOT_DIR="/tmp"
# comma-separated string denoting which BFD db environments to populate with synthetic data
TARGET_ENV="test"
# number of 'synthetic' beneficiaries to create
NUM_GENERATED_BENES=100
# boolean indicating if Synthea will use its Release .jar file (true) or if we need to build Synthea
SKIP_SYNTHEA_BUILD="true"
# boolean if we should generate future claim data
GENERATE_FUTURE="false"
# num of months into future for future claim lines
NUM_FUTURE_MONTHS=0
# partD contract to target, if USE_TARGET_CONTRACT is set to true
TARGET_CONTRACT="Y9999"
# whether to use the target contract when generating data
USE_TARGET_CONTRACT="false"

# setup for args we'll handle
# shellcheck disable=SC2034
args=$(getopt -l "num:build_root:target_env:synthea_jar:synthea_validate:cleanup:help" -o "n:b:t:j:v:c:h" -- "$@")

# parse the args
num_regex="^[0-9]+$"
# parse the args
while [ $# -ge 1 ]; do
    case "$1" in
        --)
            # No more options left.
            shift
            break
            ;;
        -n|--num)
              if ! [[ $2 =~ $num_regex ]] ; then
                echo "ERROR, non-numeric specified ($2)...exiting" >&2; exit 1
              fi
              NUM_GENERATED_BENES="$2"
              shift
              ;;
        -b|--build_root)
              BUILD_ROOT_DIR="$2"
              if [[ ! -d "${BUILD_ROOT_DIR}" ]] ; then
                echo "ERROR, Synthea build directory does not exist: ${BUILD_ROOT_DIR}" >&2; exit 1
              fi
              shift
              ;;
        -t|--target_env)
              TARGET_ENV="$2"
              shift
              ;;
        -j|--synthea_jar)
              SKIP_SYNTHEA_BUILD=$(echo "$2" | tr '[:upper:]' '[:lower:]')
              if [[ "${SKIP_SYNTHEA_BUILD}" != "true" && "${SKIP_SYNTHEA_BUILD}" != "false" ]]; then
                echo "ERROR, Invalid boolean value for using Synthea jar: ${SKIP_SYNTHEA_BUILD}" >&2; exit 1
              fi
              shift
              ;;
        -c|--cleanup)
              CLEANUP=$(echo "$2" | tr '[:upper:]' '[:lower:]')
              if [[ "${CLEANUP}" != "true" && "${CLEANUP}" != "false" ]]; then
                echo "ERROR, Invalid boolean value for setting CLEANUP flag: ${CLEANUP}" >&2; exit 1
              fi
              shift
              ;;
        -f|--num_future_months)
            if ! [[ $2 =~ $num_regex ]] ; then
              echo "ERROR, non-numeric future month value specified ($2)...exiting" >&2; exit 1
            fi
            NUM_FUTURE_MONTHS="$2"
            if [[ $2 -gt 0 ]] ; then
                GENERATE_FUTURE="true"
            fi
            shift
            ;;
        -r|--target_contract)
            TARGET_CONTRACT="$2"
            shift
            ;;
        -u|--use_target_contract)
            USE_TARGET_CONTRACT=$(echo "$2" | tr '[:upper:]' '[:lower:]')
            if [[ "${USE_TARGET_CONTRACT}" != "true" && "${USE_TARGET_CONTRACT}" != "false" ]]; then
              echo "ERROR, Invalid boolean value for using target contract: ${USE_TARGET_CONTRACT}" >&2; exit 1
            fi
            shift
            ;;
        -h|--help)
              help
              ;;
    esac
    shift
done

echo "Runnning script with args:"
echo "=================================================="
echo "BFD environment(s)           : ${TARGET_ENV}"
echo "Num beneficiaries to create  : ${NUM_GENERATED_BENES}"
echo "Synthea build directory      : ${BUILD_ROOT_DIR}"
echo "Use Synthea release jar      : ${SKIP_SYNTHEA_BUILD}"
echo "Perform file cleanup         : ${CLEANUP}"
echo "Generate future months       : ${NUM_FUTURE_MONTHS}"
echo "Generate future files        : ${GENERATE_FUTURE}"
echo "Using single target contract : ${USE_TARGET_CONTRACT}"
if [[ "${USE_TARGET_CONTRACT}" == "true" ]]; then
  echo "Target contract              : ${TARGET_CONTRACT}"
fi
echo ""

# the root will probably be passed in by Jenkins (maybe /opt?)...using /opt/dev for now
TARGET_SYNTHEA_DIR=${BUILD_ROOT_DIR}/synthea

# we'll need to keep track of 'begin' and 'end' bene_id values necessary to perform
# various Synthea generation and validation tasks.
BEG_BENE_ID=

# Need to support up to 3 concurrent environments (prod, prod-sbx, test) for a given run;
# each environment uses a distinct S3 bucket; lower-case the TARGET_ENV string to ensure
# deterministic outcome(s)
TARGET_ENV=$(echo "$TARGET_ENV" | tr '[:upper:]' '[:lower:]')
declare -A S3_BUCKETS

# various S3 buckets used by the ETL pipeline
PROD_S3_BUCKET=
TEST_S3_BUCKET=
PROD_SBX_S3_BUCKET=

# save IFS context (in case bash doesn't...)
oIFS=${IFS}
# Set comma as delimiter
IFS=','
# Read / split the string into an array using comma as delimiter
read -a arr <<< "${TARGET_ENV}"

# use associate array to ensure uniqueness (i.e., only one instance of ea env);
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
	  echo "Unrecognized environment, ignoring: ${nam}"
	  ;;
  esac
done

# create single string that can be passed to validation .py scripts; this is a comma-separated
# list of target deployment tags (i.e., prod,test)
UNIQUE_ENVS_PARAM=$(echo "${!S3_BUCKETS[*]}")
if [ -z "${UNIQUE_ENVS_PARAM}" ] ; then
  echo "ERROR, no valid environment specified!"
  help
fi
echo "enviroments to pass to .py validation scripts: ${UNIQUE_ENVS_PARAM}"
# restore IFS to original state
IFS=${oIFS}

# these names are immtuable
SYNTHEA_JAR_FILE="synthea-with-dependencies.jar"
SYNTHEA_GIT_REPO="https://github.com/synthetichealth/synthea.git"
SYNTHEA_LATEST_JAR="https://github.com/synthetichealth/synthea/releases/download/master-branch-latest/${SYNTHEA_JAR_FILE}"

# assorted variables used by the script.
BFD_SYNTHEA_AUTO_LOCATION="${BFD_ROOT_DIR}/ops/ccs-ops-misc/synthetic-data/scripts/synthea-automation"

# filename to maintain the ends state of a synthea run
BFD_END_STATE_PROPERTIES="end_state.properties"
# file that is a copy of the end_state.properties file from a
# previous synthea generation run.
BFD_END_STATE_PROPERTIES_ORIG="${BFD_END_STATE_PROPERTIES}_orig"
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
    # put bfd back together as we found it...mainly useful if this
    # script is run outside of Jenkins (i.e., by a developer) and
    # we want the git status to reflect no changes.
    rm -f "${BFD_SYNTHEA_AUTO_LOCATION}/${BFD_END_STATE_PROPERTIES}"
    rm -f "${BFD_SYNTHEA_AUTO_LOCATION}/${BFD_END_STATE_PROPERTIES_ORIG}"
    rm -fR "${BFD_SYNTHEA_AUTO_LOCATION}/__pycache__/"
    chmod 644 "${BFD_SYNTHEA_AUTO_LOCATION}/prepare-and-run-synthea.py"
    chmod 644 "${BFD_SYNTHEA_AUTO_LOCATION}/s3_utilities.py"
  fi
}

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
    echo "installing synthea from git repo"
    git clone "${SYNTHEA_GIT_REPO}" "${TARGET_SYNTHEA_DIR}"
    cd "${TARGET_SYNTHEA_DIR}"

  if [ "$SKIP_SYNTHEA_BUILD" ]; then
    echo "installing pre-built synthea release jar"
    curl -LkSs "${SYNTHEA_LATEST_JAR}" -o "./${SYNTHEA_JAR_FILE}"
  else
    # mitre synthea build has sporadic build failures
    ./gradlew clean check
  fi
}

# Function to ensure the BFD repository has proper permissions; this shell script
# and associated python scripts do not need to be built, but will need read/execute.
install_bfd_from_git(){
  # make sure the scripts are executable
  chmod 744 "${BFD_SYNTHEA_AUTO_LOCATION}/prepare-and-run-synthea.py"
  chmod 744 "${BFD_SYNTHEA_AUTO_LOCATION}/s3_utilities.py"
}

# Utility function to create a python virtual environment that will be used during the
# course of executing the various python scripts used by this shell script.
activate_py_env(){
  echo "activating python env in: ${BFD_SYNTHEA_AUTO_LOCATION}"
  cd "${BFD_SYNTHEA_AUTO_LOCATION}"
  python3 -m venv .venv
  source .venv/bin/activate
  # we'll need the following python packages
  pip3 install awscli boto3 psycopg2
  deactivate
}

# Function to download proprietary Mitre synthea mapping files, necessary
# for generating synthetic beneficiaries and claims.
download_mapping_files_from_s3(){
  echo "download mapping files from S3 to: ${MAPPING_FILES_LOCATION}"
  cd "${BFD_SYNTHEA_AUTO_LOCATION}"
  source .venv/bin/activate
  python3 ./s3_utilities.py "${MAPPING_FILES_LOCATION}" "download_file"
  deactivate
}

# Function that invokes a python S3 utility to download synthea script file.
download_script_file_from_s3(){
  echo "download script files from S3 from: ${TARGET_SYNTHEA_DIR}"
  cd "${BFD_SYNTHEA_AUTO_LOCATION}"
  source .venv/bin/activate
  python3 ./s3_utilities.py "${TARGET_SYNTHEA_DIR}" "download_script"
  # make sure the script is executable
  chmod +x "${TARGET_SYNTHEA_DIR}/national_bfd.sh"
  deactivate
}

# Function that invokes a python S3 utility to download the synthea end_state.properties file.
download_props_file_from_s3(){
  echo "download BFD ${BFD_END_STATE_PROPERTIES} file from S3 to: ${BFD_SYNTHEA_AUTO_LOCATION}"
  cd "${BFD_SYNTHEA_AUTO_LOCATION}"
  source .venv/bin/activate
  python3 s3_utilities.py "./" "download_prop"
  chmod 644 "${BFD_END_STATE_PROPERTIES}"
  # Make a copy of the downloaded end state properties file.
  cat "./${BFD_END_STATE_PROPERTIES}" > "${BFD_END_STATE_PROPERTIES_ORIG}"

  # extract the bene_id_start variable from the downloaded end state properties file.
  # It will be used in a later function/operation.
  BEG_BENE_ID=$(cat "./${BFD_END_STATE_PROPERTIES_ORIG}" |grep bene_id_start |sed 's/.*=//')
  echo "BEG_BENE_ID=${BEG_BENE_ID}"
  deactivate
}

# Args:
# 1: end state properties file path
# 2: location of synthea git repo
# 3: number of beneficiaries to be generated
# 4: environment to load/validate; either: test, prod-sbx, prod or comma-delimited string containing any of them
# 5: target contract to use, if 6 is set to true
# 6: true/false whether to associate all generated items with a single contract, using arg5
#
prepare_and_run_synthea(){
  cd "${BFD_SYNTHEA_AUTO_LOCATION}"
  source .venv/bin/activate
  python3 prepare-and-run-synthea.py "${BFD_END_STATE_PROPERTIES}" "${TARGET_SYNTHEA_DIR}" "${NUM_GENERATED_BENES}" "${NUM_FUTURE_MONTHS}" "${TARGET_CONTRACT}" "${USE_TARGET_CONTRACT}"
  deactivate
}

# Function that splits the synthea future lines into their own loads; only
# does anything if future lines were generated.
#
#Args:
# 1: location of synthea git repo
#
split_future_loads(){
  if [ "${GENERATE_FUTURE}" == 'true' ]; then
      cd "${BFD_SYNTHEA_AUTO_LOCATION}"
      source .venv/bin/activate
      python3 split-future-claims.py "${TARGET_SYNTHEA_DIR}"
      deactivate
  fi
}

# Function that invokes an S3 utility to upload new generated synthetic data to an
# S3 folder where the BFD ETL pipeline will discover the synthetic RIF files and
# load them into the appropriate BFD database.
upload_synthea_results_to_s3(){

  echo "upload synthea results to S3"
  cd "${BFD_SYNTHEA_AUTO_LOCATION}"

  ## list out the future folders inside the output directory
  FOLDERS=()
  readarray -t FOLDERS < <(find "${BFD_SYNTHEA_OUTPUT_LOCATION}" -maxdepth 1 -mindepth 1 -type d -execdir basename {} ';')
  echo "${#FOLDERS[@]} folders to upload found"

  # upload the RIF (.csv) files to S3 ETL bucket(s); once per environment, passed in as arg
  source .venv/bin/activate
  for s3_bucket in "${S3_BUCKETS[@]}"; do
    echo "uploading RIF files to: ${s3_bucket}"
    python3 ./s3_utilities.py "${BFD_SYNTHEA_OUTPUT_LOCATION}" "upload_synthea_results" "${s3_bucket}"

    if [ "$GENERATE_FUTURE" == 'true' ]; then
        # for each future folder, also upload it to the environment
        curr_num=1
        for folder_name in "${FOLDERS[@]}"; do
            echo "uploading future RIF files to: ${s3_bucket} (${curr_num}/${#FOLDERS[@]})"
            curr_num=$(($curr_num+1))
            python3 ./s3_utilities.py "${BFD_SYNTHEA_OUTPUT_LOCATION}/${folder_name}" "upload_synthea_result_folder" "${s3_bucket}"
        done
    fi


  done
  deactivate
}

# Function that invokes an S3 utility to wait until the manifest file (0_manifest.xml) shows
# up in the S3 bucket's /Done folder; the ETL pipeline moves the manifest file once it has
# completed processing of the /Incoming RIF (.csv) files.
wait_for_manifest_done(){
  echo "waiting for manifest Done"
  cd "${BFD_SYNTHEA_AUTO_LOCATION}"

  # wait until the 0_manifest.xml file exists in the /Done folder for all environments being prcoessed; this
  # op is synchronous meaning that we'll check/wait on one environemnt at a time. Since we have an 'all or nothing'
  # processing model, the extra time to do this synchronously really doesn't matter.
  source .venv/bin/activate
  for s3_bucket in "${S3_BUCKETS[@]}"; do
    python3 ./s3_utilities.py "${BFD_SYNTHEA_OUTPUT_LOCATION}" "wait_for_manifest_done" "${s3_bucket}"
  done
  deactivate
}

# Function to upload the new end state properties file to an S3 bucket; it also extracts
# the newest bene_id_start variable for use in a later function/operation.
upload_props_file_to_s3(){
  echo "upload end_state.properties file to S3"
  cd "${BFD_SYNTHEA_AUTO_LOCATION}"
  source .venv/bin/activate
  python3 ./s3_utilities.py "${BFD_SYNTHEA_OUTPUT_LOCATION}" "upload_prop" "${BFD_SYNTHEA_AUTO_LOCATION}"
  deactivate
}

#----------------- GO! ------------------#
# we'll trap system interrupts and perform cleanup.
trap "clean_up" INT HUP

# general fail-safe to perform cleanup of any directories and files germane to executing
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

# invoke function to download proprietary Mitre shell script file.
download_script_file_from_s3

# invoke function to download (if available) a previous run's end_state.properties file.
download_props_file_from_s3

# invoke function to invoke BFD .py script that verifies that:
#  1) we have all the files necessary to perform a synthea generation run.
#  2) executes a synthea generation run
prepare_and_run_synthea

# invoke function to split future lines into separate, future loads; no-op
# if future lines are not generated.
split_future_loads

# Invoke a function to upload the generated RIF files to the appropriate BFD
# ETL pipeline S3 /Incoming bucket, where the ETL process will pick them up and
# load the synthetic data into database.
upload_synthea_results_to_s3

# Invoke a function to wait on / check the appropriate BFD ETL pipeline S3 bucket for
# the 0_manifest file to appear in the S3 bucket's /Done folder.
wait_for_manifest_done

# Invoke a function to upload the new end_state.properties file and the new characteristics.csv file
if [[ -n ${BEG_BENE_ID} ]]; then
  upload_props_file_to_s3
else
  error_exit "end state BEG_BENE_ID variables unset...exiting"
fi

# cleanup after ourselves...
clean_up

echo
echo "============================================="
echo "BFD Synthea Generation completed SUCCESFULLY!"
echo "============================================="
# return SUCCESS
exit 0;
