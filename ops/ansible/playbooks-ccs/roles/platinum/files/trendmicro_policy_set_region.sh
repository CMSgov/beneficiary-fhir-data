#!/bin/sh
# File:   /root/.trendmicro_policy_set_region.sh
# Description:  This script reads the metadata from the instance and resets the shared service agents based on region

echo "Starting TrendMicro configuration"

if [ ! -f /tmp/tmcount ]; then
    echo "1" > /tmp/tmcount
fi

tmcount=$(cat /tmp/tmcount)

DSA_BASE_DIR="/var/opt/ds_agent/diag"

get_diag_bundle () (
    echo $(find $DSA_BASE_DIR -name "*.zip" -type f -printf '%p\n' |  sort -n | tail -1)
)

gen_diag_bundle () (
    # Zip file does not get created immediately, as such it will return null, in most cases
    # TODO(?): Replace get_diag_bundle call with function to parse dsa_control -d output for filename via regex logic?
    sudo /opt/ds_agent/dsa_control -d &>/dev/null
    echo $(get_diag_bundle)
    return
)

dsm_bundle () (
    diagFile=$(get_diag_bundle)
    if [ -z "$diagFile" ]; then
        diagFile=$(gen_diag_bundle)
    else
        lastModified=$(date -r $diagFile +%s)
        currTime=$(date +%s)
        diffInSeconds=$((currTime - lastModified))
        #86400 seconds = 24hours
        if [ $diffInSeconds -gt 86400 ]; then
            diagFile=$(gen_diag_bundle)
        fi
    fi
    echo $diagFile
)

dsa_control_wrapper () (
    output=$($1)

    # Success code, return early
    if [[ "$output" == *"HTTP Status: 200 - OK"* ]]; then
        return 0
    fi

    # Error codes, handle known errors with custom codes
    if [[ "$output" == *"HTTP Status: 400 - OK"* ]]; then
        return 100
    fi
    if [[ "$output" == *"HTTP Status: 403 - Forbidden - reset agent first"* ]]; then
        return 101
    fi

    # Generic failure
    return 1
)

# Determine cloud vendor
cloud_vendor='unknown'
if [ -f /sys/hypervisor/uuid ] && [ "$(head -c 3 /sys/hypervisor/uuid | tr '[:upper:]' '[:lower:]')" == ec2 ]; then
    cloud_vendor='aws'
elif [ -f /sys/devices/virtual/dmi/id/product_uuid ] && [ "$(head -c 3 /sys/devices/virtual/dmi/id/product_uuid | tr '[:upper:]' '[:lower:]')" == ec2 ]; then
    cloud_vendor='aws'
elif [ -f /sys/devices/virtual/dmi/id/sys_vendor ] && [ "$(tr '[:upper:]' '[:lower:]' < /sys/devices/virtual/dmi/id/sys_vendor)" == 'microsoft corporation' ]; then
  cloud_vendor='azure'
fi

ELAPSED=0
until lsof -i:4118 </dev/null >/dev/null 2>&1 && /opt/ds_agent/dsa_control -r ; do
    echo 'Waiting for Trend DSM Agent';
    sleep 5;

    if [ $ELAPSED -eq 60 ]; then
      break;
    fi

   (( ELAPSED++ ))
done

configured='false'

# Call for cloud vendor specific metadata
if [ $cloud_vendor == 'aws' ]; then
    echo "Detected AWS Cloud"
    token=$(curl -sS -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
    region=$(curl -sS -H "X-aws-ec2-metadata-token: $token" http://169.254.169.254/latest/meta-data/placement/region)

    if [[ $region = "us-east-1" ]]; then
        echo "Detected AWS region us-east-1"

        command='/opt/ds_agent/dsa_control -a dsm://cep-east.commercial.cms.gov:4120/ policyid:513 --max-dsm-retries 3 --dsm-retry-interval 30'
        dsa_control_wrapper "$command"

        status=$?

        # Status 101 indicates agent needs reset
        if [[ $status -eq 101 ]]; then
          /opt/ds_agent/dsa_control -r
          dsa_control_wrapper "$command"

          status=$?
        fi

        # We need a clean exit
        if [[ $status -eq 0 ]]; then
            configured='true'
        fi

        echo "Assigned east TrendMicro generic policy"
    elif [[ $region = "us-west-2" ]]; then
        echo "Detected AWS region us-west-2"

        command='/opt/ds_agent/dsa_control -a dsm://cep-west.commercial.cms.gov:4120/ policyid:378 --max-dsm-retries 3 --dsm-retry-interval 30'
        dsa_control_wrapper "$command"

        status=$?

        # Status 101 indicates agent needs reset
        if [[ $status -eq 101 ]]; then
          /opt/ds_agent/dsa_control -r
          dsa_control_wrapper "$command"

          status=$?
        fi

        # We need a clean exit
        if [[ $status -eq 0 ]]; then
            configured='true'
        fi

        echo "Assigned west TrendMicro generic policy"
    elif [[ $region = "us-gov-west-1" ]]; then
        echo "Detected AWS region us-gov-west-1"

        command='/opt/ds_agent/dsa_control -a dsm://internal-dsm-prod-elb-1903952832.us-gov-west-1.elb.amazonaws.com:4120/ policyid:1 --max-dsm-retries 3 --dsm-retry-interval 30'
        dsa_control_wrapper "$command"

        status=$?

        # Status 101 indicates agent needs reset
        if [[ $status -eq 101 ]]; then
          /opt/ds_agent/dsa_control -r
          dsa_control_wrapper "$command"

          status=$?
        fi

        # We need a clean exit
        if [[ $status -eq 0 ]]; then
            configured='true'
        fi
        echo "Assigned GovCloud TrendMicro generic policy"
    fi
elif [ $cloud_vendor == 'azure' ]; then
    echo "Detected Azure Cloud"

    command='/opt/ds_agent/dsa_control -a dsm://cep-mag.cloud.cms.gov:4120/ policyid:10 --max-dsm-retries 3 --dsm-retry-interval 30'
    dsa_control_wrapper "$command"

    status=$?

    # Status 101 indicates agent needs reset
    if [[ $status -eq 101 ]]; then
      /opt/ds_agent/dsa_control -r
      dsa_control_wrapper "$command"

      status=$?
    fi

    # We need a clean exit
    if [[ $status -eq 0 ]]; then
        configured='true'
    fi

    echo "Assigned Azure TrendMicro generic policy"
fi

# If configuration failed, set login warning and setup cron job to attempt registration
if [ $configured == 'false' ]; then
    # 19 in tmcount = attempted 19 times, 3 minutes each attempt, totalling over one hour of retries including initial attempt
    if [ $tmcount -gt 19 ]; then
        bundle=$(dsm_bundle)
        cat << EOF > /etc/profile.d/TrendMicroAlert.sh
echo ""
echo "ALERT!!! There was an issue registering this server with TrendMicro."
echo ""
echo "Please open a Cloud Support ticket, and include the following information:"
echo ""
[ -n "$bundle" ] && echo "Attach this file: $bundle"
echo "Retry is attempted every 3 minutes, this is attempt #: $tmcount"
echo ""
echo "DSM Command: $command"
echo ""

if [ -f /etc/cmscloud-goldimage.txt ]; then
    echo "----- AMI DETAILS -----"
    grep -v '^#' /etc/cmscloud-goldimage.txt
fi
EOF
        chmod 755 /etc/profile.d/TrendMicroAlert.sh
    fi

    if [ ! -f /etc/cron.d/trend_micro_registration ]; then
        echo '*/3 * * * * root /root/.trendmicro_policy_set_region.sh' > /etc/cron.d/trend_micro_registration
    fi

    echo "TrendMicro Configuration was not successful"
    tmcount=$((tmcount+1))
    echo "$tmcount" > /tmp/tmcount
    exit 1
fi

# If configuration succeeds, clean up any artifacts from possible failed attempts
if [ $configured == 'true' ]; then
    if [ -f /etc/profile.d/TrendMicroAlert.sh ]; then
        rm -rf /etc/profile.d/TrendMicroAlert.sh
    fi
    if [ -f /etc/cron.d/trend_micro_registration ]; then
        rm -rf /etc/cron.d/trend_micro_registration
    fi
    if [ -f /tmp/tmcount ]; then
        rm -rf /tmp/tmcount
    fi
fi

echo "Finished TrendMicro Configuration"
