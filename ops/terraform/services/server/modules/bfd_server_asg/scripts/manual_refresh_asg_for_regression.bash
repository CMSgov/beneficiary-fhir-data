#!/usr/bin/env bash
#
# Manually manipulate ASG in support of subsequent regression suite during deployment:
#   1. remove ASG warm pool instances (avoid scaling use of preceding launch template version instances)
#   2. scale up ASG to 2X desired capacity (enforcing use of new launch template instances)
#   3. scale down ASG to specified desired capacity (enforcing removal of old launch template instances) 
#
# Usage: ./manual_refresh_asg_for_regression.bash { asg_name } [ timeout ]
#   asg_name: REQUIRED - The name of the AutoScalingGroup to manipulate.
#   timeout : OPTIONAL - minutes to wait for each scaling operation (default 20)
#
# Requirements:
#   - awscli
#   - jq
#
# Examples:
#   ./manual_refresh_asg_for_regression.bash bfd-2558-prod-sbx-fhir
#     >> manually refresh ASG specified with empty warm pool most recent instance_refresh state
#   ./manual_refresh_asg_for_regression.bash bfd-2558-prod-sbx-fhir 30
#     >> manually refresh ASG specified with empty warm pool most recent instance_refresh state
#        and allow a 30 minute timeout for each scaling cycle
#

# Minimal Query Pattern for response from describe-auto-scaling-groups
asg_query="$(cat <<'EOF'
  { AsgName: .AutoScalingGroups[0].AutoScalingGroupName,
    LaunchVersion: .AutoScalingGroups[0].LaunchTemplate.Version,
    MinSize: .AutoScalingGroups[0].MinSize,
    MaxSize: .AutoScalingGroups[0].MaxSize,
    Desired: .AutoScalingGroups[0].DesiredCapacity,
    WarmPoolSize: .AutoScalingGroups[0].WarmPoolSize,
    Instances: .AutoScalingGroups[0].Instances }
EOF
)"

# extract Min Capacity
getAsgMinCap() {
    json=${1}
    echo "${json}"|jq -r '.MinSize // 1'
}

# extract Max Capacity
getAsgMaxCap() {
    json=${1}
    echo "${json}"|jq -r '.MaxSize // 1'
}

# extract Desired Capacity
getAsgDesiredCap() {
    json=${1}
    echo "${json}"|jq -r '.Desired // 1'
}

# extract Active Instance Count
getAsgHotCount() {
    json=${1}
    echo "${json}"|jq -r '.Instances|length'
}

# extract Active Instance Count
getAsgReadyVersionCount() {
    json=${1}
    ltversion=${2}
    direction=${3}
    if [ "${direction}" == "UP" ]
    then
        echo "${json}"|jq -r --arg ltver ${ltversion} '[.Instances[]|select(.LaunchTemplate.Version==$ltver)|select(.LifecycleState == "InService")]|length'
    else
        echo "${json}"|jq -r --arg ltver ${ltversion} '[.Instances[]|select(.LaunchTemplate.Version!=$ltver)|select(.LifecycleState == "InService")]|length'
    fi
}

# extract Warm Pool Capacity
getAsgWarmSize() {
    json=${1}
    echo "${json}"|jq -r '.WarmPoolSize // 0'
}

# extract LaunchVersion
getAsgLaunchVer() {
    json=${1}
    echo "${json}"|jq -r '.LaunchVersion // 0'
}

# eliminate warm pool instances and await completion
manual_rm_warm_pool() {
    asgname="${1}";
    timeout=${2:-15}

    CONTINUE=1
    HALTMSG="SUCCESS"
    LOOPTIME=0
    SLEEPSEC=20
    WAIT_TIMEOUT_SEC=$(( ${timeout} * 60 ))

    # initiate warm pool removal
    aws autoscaling delete-warm-pool --auto-scaling-group-name ${asg_name} --force-delete
    rc=$?
    if [ ${rc} -ne 0 ]
    then
        CONTINUE=0
        HALTMSG="ERROR: ${rc} returned by aws autoscaling delete-warm-pool, exiting"
    fi
    # sleep two minutes for async activity
    sleep 120
    # wait until warm pool is empty
    while [ ${CONTINUE} ]
    do
        sleep ${SLEEPSEC}
        LOOPTIME=$(( ${LOOPTIME} + ${SLEEPSEC} ))
        asgStateJson=$(aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names ${asgname} --output json | jq "${asg_query}")
        newWarmSize=$(getAsgWarmSize "${asgStateJson}")
        if [ ${newWarmSize} -gt 0 ]
        then
            if [ ${LOOPTIME} -ge ${WAIT_TIMEOUT_SEC} ]
            then
                CONTINUE=0
                HALTMSG="ERROR: WARM POOL REMOVAL TIMEOUT of ${timeout} mins REACHED" && break;
            else
                next;
            fi
        else
            CONTINUE=0 && break;
        fi
    done
    echo "${HALTMSG}"
}

# manually scale ASG by setting desired capacity and await completion
manual_scale_asg() {
    asgname="${1}";
    reqsize=${2};
    direction="${3}";
    ltversion=${4};
    timeout=${5:-20};
    scaleUpCount=0;

    asgStateJson=$(aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names ${asgname} --output json | jq "${asg_query}")
    initHotSize=$(getAsgHotCount "${asgJson}")
    if [ "${direction}" == "UP" ]
    then
        scaleUpCount=$(( ${reqsize} - ${initHotSize} ))
    fi

    CONTINUE=1
    HALTMSG=""
    LOOPTIME=0
    SLEEPSEC=60
    WAIT_TIMEOUT_MIN=${timeout}

    ## DBGFILE=".debug.${asgname}.${direction}.${ltversion}.${RANDOM}.log"
    DBGFILE="/dev/null"
    echo "STARTING $(date)" >> $DBGFILE

    # Request new desired capacity
    aws autoscaling set-desired-capacity --auto-scaling-group-name ${asgname} --desired-capacity ${reqsize}
    rc=$?
    if [ ${rc} -ne 0 ]
    then
        CONTINUE=0
        HALTMSG="ERROR: ${rc} returned by aws autoscaling set-desired-capacity, exiting"
    fi

    # await completion of requested scaling
    while [ ${CONTINUE} ]
    do
        # sleep at beginning of loop supports async nature of ASG changes
        sleep ${SLEEPSEC}
        LOOPTIME=$(( ${LOOPTIME} + 1 ))

        # after sleeping check ASG state
        asgStateJson=$(aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names ${asgname} --output json | jq "${asg_query}")
        desCapNow=$(getAsgHotCount "${asgStateJson}")
        readyCount=$(getAsgReadyVersionCount "${asgStateJson}" "${ltversion}" "${direction}" )

        # if desired capacity not reached, immediately go back to sleep
        echo "LOOPCOUNT ${LOOPTIME}" >> $DBGFILE
        echo "State ${asgStateJson}" >> $DBGFILE
        if [ ${desCapNow} -ne ${reqsize} ]
        then
            echo "NEXT for ${desCapNow} ne ${reqsize}" >> $DBGFILE
        else
            # requested count equals desired count if we get past first check
            # verify readyCount matches target (UP: scaleUpCount, DOWN: 0)
            if [ "${direction}" == "UP" ] 
            then
                echo "    direction ${direction} readyCount ${readyCount} vs scaleUpCount ${scaleUpCount}" >> $DBGFILE
                if [ ${readyCount} -eq ${scaleUpCount} ]
                then
                    HALTMSG="SUCCESS"
                    CONTINUE=0 && break;
                else
                    echo "NEXT UP for ${readyCount} eq ${scaleUpCount}" >> $DBGFILE
                fi
            else ## direction == DOWN
                echo "    direction ${direction} readyCount ${readyCount} vs 0" >> $DBGFILE
                if [ ${readyCount} -eq 0 ]
                then
                    HALTMSG="SUCCESS"
                    CONTINUE=0 && break;
                else
                    echo "NEXT DOWN for ${readyCount} eq 0" >> $DBGFILE
                fi
            fi

        fi

        # Reaching this point indicates Scale Request is not yet complete
        # Check timeout 
        if [ ${LOOPTIME} -ge ${WAIT_TIMEOUT_MIN} ]
        then
            CONTINUE=0;
            HALTMSG="ERROR: SCALE ${direction} TIMEOUT ${timeout} mins REACHED"
        fi
    done
    echo "${HALTMSG}"
}

# set script options
set -euo pipefail
shopt -s expand_aliases

# capture input args
asg_name="${1}"
refresh_timeout="${2:-20}"

# capture initial ASG state after one minute sleep (ASG actions are all async)
sleep 60;
asgJson=$(aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names ${asg_name} --output json | jq "${asg_query}")

# identify initial ASG attributes to determine next steps
currentMin=$(getAsgMinCap "${asgJson}")
currentMax=$(getAsgMaxCap "${asgJson}")
desiredCap=$(getAsgDesiredCap "${asgJson}")
initHotSize=$(getAsgHotCount "${asgJson}")
currentWarmSize=$(getAsgWarmSize "${asgJson}")
targetScaleUp=$(( ${initHotSize} * 2 ))
maxHotLaunchVer=$(echo "${asgJson}"|jq -r '[.Instances[]|.LaunchTemplate.Version|tonumber]|max')
minHotLaunchVer=$(echo "${asgJson}"|jq -r '[.Instances[]|.LaunchTemplate.Version|tonumber]|min')
cfgLaunchVer=$(getAsgLaunchVer "${asgJson}")

# provide current state output
echo "For ${asg_name}"
echo "    Current Min/Max ${currentMin} / ${currentMax}, current Desired ${desiredCap}, current Active ${initHotSize}"
echo "    Targeted Scaling Count ${targetScaleUp}, configured Launch Ver ${cfgLaunchVer}, max Active Launch Ver ${maxHotLaunchVer}"

# sanity check for mixed Launch Versions exceptional condition
if [ ${minHotLaunchVer} -ne ${maxHotLaunchVer} ]
then
    lvarray="$(echo "${asgJson}"|jq -r '[.Instances[]|.LaunchTemplate.Version|tonumber]')"
    echo "ERROR: Mix of active Launch Template versions ${lvarray} is UNSUPPORTED OPERATION" && exit -2;
fi

# Do we need to remove the Warm Pool
if [ ${currentWarmSize} -gt 0 ]
then
    echo "    Need to Terminate Warm Instances ..."
    warm_pool_removal_msg=$(manual_rm_warm_pool ${asg_name})
    if [ "${warm_pool_removal_msg}" != "SUCCESS" ]
    then 
        echo ${warm_pool_removal_msg} && exit 4;
    fi
else
    echo "    Warm Pool Size ${currentWarmSize} requires no adjustment"
fi

# Do we need to execute Scaling down/up for Regression Testing
if [ ${targetScaleUp} -le ${currentMax} ] && [ ${cfgLaunchVer} -gt ${maxHotLaunchVer} ]
then
    echo "    Need to force Refresh by scaling UP Hot Instances from ${initHotSize} to ${targetScaleUp} then back down Down"
    scale_up_msg=$(manual_scale_asg ${asg_name} ${targetScaleUp} UP ${cfgLaunchVer} ${refresh_timeout})

    if [ $(echo "${scale_up_msg}" | grep SUCCESS | wc -l) -eq 0 ]
    then
        echo ${scale_up_msg} && exit 8;
    else
        echo "    Scale UP SUCCEEDED"
        scale_down_msg=$(manual_scale_asg ${asg_name} ${initHotSize} DOWN ${cfgLaunchVer} ${refresh_timeout})
        if [ $(echo "${scale_down_msg}" | grep SUCCESS | wc -l) -eq 0 ]
        then
            echo ${scale_down_msg} && exit 16;
        fi
    fi
    echo "SUCCESS"
else
    if [ ${maxHotLaunchVer} -ge ${cfgLaunchVer} ]
    then
        echo "    Launch Versions already match, No Scaling required"
    else
        echo "    Cluster max ${currentMax} does not support Scale Up to ${targetScaleUp}"
    fi
fi 
