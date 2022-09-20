import json
from typing import Any, Dict, List


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

    def load_json_safe(json_str: str) -> Dict[str, Any]:
        try:
            return json.loads(json_str)
        except json.JSONDecodeError:
            return None
        except TypeError:
            return None

    raw_messages = [load_json_safe(response.body) for response in responses]
    # SQS messages can come in a lot of forms, for this service's use-case we are expecting either
    # messages from an SNS subscription or directly via the cli or the SQS web UI. In the SNS case,
    # the actual, full SQS message will include additional metadata alongside the actual message.
    # The actual message will be string-escaped JSON under the "Message" key, hence why that key's
    # value is deserialized from JSON if it exists. If it doesn't exist, we assume the message is
    # the _entire_ SQS message
    filtered_messages = [
        load_json_safe(message.get("Message")) or message
        for message in raw_messages
        if message is not None
    ]

    return filtered_messages


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
