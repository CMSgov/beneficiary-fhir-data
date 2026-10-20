#!/usr/bin/env bash
set -Eeou pipefail

find_internal_elb_dns() {
	local stack_tag_value="${1:-}"
	local service_tag_value="${2:-}"

	if [[ -z "${stack_tag_value}" || -z "${service_tag_value}" ]]; then
		echo "ERROR: usage: find_internal_elb_dns <env> <service>" >&2
		return 1
	fi

	local aws_region="${AWS_REGION:-${AWS_DEFAULT_REGION:-us-east-1}}"

	local lb_arns
	lb_arns=$(aws resourcegroupstaggingapi get-resources \
		--region "${aws_region}" \
		--resource-type-filters "elasticloadbalancing:loadbalancer" \
		--tag-filters "Key=stack,Values=${stack_tag_value}" "Key=service,Values=${service_tag_value}" \
		--query 'ResourceTagMappingList[].ResourceARN' \
		--output text) || return 1

	if [[ -z "${lb_arns}" ]]; then
		return 1
	fi

	local -a elbv2_arns=()
	for arn in ${lb_arns}; do
		elbv2_arns+=("${arn}")
	done

	local dns
	dns=$(aws elbv2 describe-load-balancers \
		--region "${aws_region}" \
		--load-balancer-arns "${elbv2_arns[@]}" \
		--query "LoadBalancers[?Scheme=='internal'].DNSName | [0]" \
		--output text) || return 1

	if [[ -z "${dns}" || "${dns}" == "None" ]]; then
		echo "ERROR: found load balancer(s) tagged stack=${stack_tag_value} service=${service_tag_value}, but none have Scheme=internal." >&2
		return 1
	fi

	echo "${dns}"
}

_bfd_tmp_v1v2=$(mktemp)
_bfd_tmp_v3=$(mktemp)

find_internal_elb_dns "$BFD_ENV" server >"${_bfd_tmp_v1v2}" &
_bfd_pid_v1v2=$!

find_internal_elb_dns "$BFD_ENV" server-ng >"${_bfd_tmp_v3}" &
_bfd_pid_v3=$!

wait "${_bfd_pid_v1v2}" && _bfd_status_v1v2=0 || _bfd_status_v1v2=$?
wait "${_bfd_pid_v3}" && _bfd_status_v3=0 || _bfd_status_v3=$?

if [[ ${_bfd_status_v1v2} -eq 0 ]]; then
	BFD_V1V2_LB_DNSNAME="$(cat "$_bfd_tmp_v1v2")"
	export BFD_V1V2_LB_DNSNAME
else
	unset BFD_V1V2_LB_DNSNAME
fi

if [[ ${_bfd_status_v3} -eq 0 ]]; then
	BFD_V3_LB_DNSNAME="$(cat "$_bfd_tmp_v3")"
	export BFD_V3_LB_DNSNAME
else
	unset BFD_V3_LB_DNSNAME
fi

rm -f "${_bfd_tmp_v1v2}" "${_bfd_tmp_v3}"
unset _bfd_tmp_v1v2 _bfd_tmp_v3 _bfd_pid_v1v2 _bfd_pid_v3 _bfd_status_v1v2 _bfd_status_v3
