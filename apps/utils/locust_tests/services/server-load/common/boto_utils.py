import json
from typing import Dict, List, Optional

from common.message_filters import filter_message_by_keys


def get_ssm_parameter(ssm_client, name: str, with_decrypt: bool = False) -> str:
    # TODO: Properly type hint 'ssm_client'
    response = ssm_client.get_parameter(Name=name, WithDecryption=with_decrypt)

    try:
        return response["Parameter"]["Value"]
    except KeyError as exc:
        raise ValueError(f'SSM parameter "{name}" not found or empty') from exc


def get_rds_db_uri(rds_client, cluster_id: str) -> str:
    # TODO: Properly type hint 'rds_client'
    response = rds_client.describe_db_clusters(DBClusterIdentifier=cluster_id)

    try:
        return response["DBClusters"][0]["ReaderEndpoint"]
    except KeyError as exc:
        raise ValueError(f'DB URI not found for cluster ID "{cluster_id}"') from exc


def check_queue(queue, timeout: int = 1) -> List[Dict[str, str]]:
    """Checks a given SQS queue for messages and returns the inner JSON message from each

    Args:
        queue: A boto3 SQS queue
        timeout (int, optional): Amount of time to poll for messages in queue. Defaults to 1

    Returns:
        List[Dict[str, str]]: The list of inner queue messages
    """
    # TODO: Properly type hint 'queue'
    responses: List[Dict[str, str]] = queue.receive_messages(
        AttributeNames=["SenderId", "SentTimestamp"],
        WaitTimeSeconds=timeout,
    )

    raw_messages = [response.get("Message") or response.get("Body") for response in responses]
    messages = [json.loads(raw_message) for raw_message in raw_messages]

    return messages


def get_warm_pool_count(autoscaling_client, asg_name: str) -> int:
    """Returns the count of instances that are currently in the given ASG's warm pool

    Args:
        autoscaling_client: Boto3 "autoscaling" client
        asg_name (str): Name of the autoscaling group to check

    Returns:
        int: The count of instances available in the warm pool
    """
    response = autoscaling_client.describe_warm_pool(AutoScalingGroupName=asg_name)

    return len(response["Instances"])
