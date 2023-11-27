#!/usr/bin/env bash
# This script was provided by CMS SecOps as a hotpatch to (hopefully) upgrade the Trend Micro Deep Security Agent, which
# is currently causing instances to fail their healthchecks.
ACTIVATIONURL='dsm://internal-dsm-prod-elb-us-east-1-1932432501.us-east-1.elb.amazonaws.com:4120/'
MANAGERURL='https://internal-dsm-prod-elb-us-east-1-1932432501.us-east-1.elb.amazonaws.com:4119'
CURLOPTIONS='--silent --tlsv1.2'
linuxPlatform='';
isRPM='';

if [[ $(/usr/bin/id -u) -ne 0 ]]; then
    echo You are not running as the root user.  Please try again with root privileges.;
    logger -t You are not running as the root user.  Please try again with root privileges.;
    exit 1;
fi;

# remove the existing agent
rpm -ev $(rpm -qa ds_agent) || true

# stop all existing retry methods
killall .trendmicro_policy_set_region.sh || true
rm -rfv /etc/cron.d/trend_micro_registration

sleep 10

rm -f /tmp/PlatformDetection
rm -f /tmp/agent.*

if ! type curl >/dev/null 2>&1; then
    echo "Please install CURL before running this script."
    logger -t Please install CURL before running this script
    exit 1
fi

curl $MANAGERURL/software/deploymentscript/platform/linuxdetectscriptv1/ -o /tmp/PlatformDetection "$CURLOPTIONS" --insecure
curlRet=$?

if [[ $curlRet == 0 && -s /tmp/PlatformDetection ]]; then
    . /tmp/PlatformDetection
elif [[ $curlRet -eq 60 ]]; then
    echo "TLS certificate validation for the agent package download has failed. Please check that your Deep Security Manager TLS certificate is signed by a trusted root certificate authority. For more information, search for \"deployment scripts\" in the Deep Security Help Center."
    logger -t TLS certificate validation for the agent package download has failed. Please check that your Deep Security Manager TLS certificate is signed by a trusted root certificate authority. For more information, search for \"deployment scripts\" in the Deep Security Help Center.
    exit 1;
else
    echo "Failed to download the agent installation support script."
    logger -t Failed to download the Deep Security Agent installation support script
    exit 1
fi

platform_detect
if [[ -z "${linuxPlatform}" ]] || [[ -z "${isRPM}" ]]; then
    echo Unsupported platform is detected
    logger -t Unsupported platform is detected
    exit 1
fi

echo Downloading agent package...
if [[ $isRPM == 1 ]]; then package='agent.rpm'
    else package='agent.deb'
fi
curl -H "Agent-Version-Control: on" $MANAGERURL/software/agent/"${runningPlatform}""${majorVersion}"/"${archType}"/$package?tenantID= -o /tmp/$package "$CURLOPTIONS" --insecure
curlRet=$?
isPackageDownloaded='No'
if [ $curlRet -eq 0 ];then
    if [[ $isRPM == 1 && -s /tmp/agent.rpm ]]; then
        file /tmp/agent.rpm | grep "RPM";
        if [ $? -eq 0 ]; then
            isPackageDownloaded='RPM'
        fi
    elif [[ -s /tmp/agent.deb ]]; then
        file /tmp/agent.deb | grep "Debian";
        if [ $? -eq 0 ]; then
            isPackageDownloaded='DEB'
        fi
    fi
fi

echo Installing agent package...
rc=1
if [[ ${isPackageDownloaded} = 'RPM' ]]; then
    rpm -ihv /tmp/agent.rpm
    rc=$?
elif [[ ${isPackageDownloaded} = 'DEB' ]]; then
    dpkg -i /tmp/agent.deb
    rc=$?
else
    echo Failed to download the agent package. Please make sure the package is imported in the Deep Security Manager
    logger -t Failed to download the agent package. Please make sure the package is imported in the Deep Security Manager
    exit 1
fi
if [[ ${rc} != 0 ]]; then
    echo Failed to install the agent package
    logger -t Failed to install the agent package
    exit 1
fi

echo Install the agent package successfully

sleep 15
/opt/ds_agent/dsa_control -r
/opt/ds_agent/dsa_control -a $ACTIVATIONURL "policyid:513"
# /opt/ds_agent/dsa_control -a dsm://internal-dsm-prod-elb-us-east-1-1932432501.us-east-1.elb.amazonaws.com:4120/ "policyid:513"
