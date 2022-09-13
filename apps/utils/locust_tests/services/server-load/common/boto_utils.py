import json
from typing import Dict, List, Optional


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


def check_queue(
    queue, timeout: int = 1, message_filter: Optional[Dict[str, str]] = None
) -> List[Dict[str, str]]:
    """Checks a given SQS queue for messages. Optionally, a message filter can be provided that will
    be used to filter out queue messages with inner JSON messages that do not have keys with values
    matching the given filter

    Args:
        queue: A boto3 SQS queue
        timeout (int, optional): Amount of time to poll for messages in queue. Defaults to 1
        message_filter (Optional[Dict[str, str]], optional): Inner JSON message filter. Messages
        must having matching keys and values to be retrieved from queue. Defaults to None

    Returns:
        List[Dict[str, str]]: _description_
    """
    # TODO: Properly type hint 'queue'
    response: List[Dict[str, str]] = queue.receive_messages(
        AttributeNames=["SenderId", "SentTimestamp"],
        WaitTimeSeconds=timeout,
    )

    if not message_filter:
        return response

    def filter_by_message(queue_msg_dict: Dict[str, str]) -> bool:
        inner_message = json.loads(queue_msg_dict["Message"])
        return all(
            k in inner_message and inner_message[k] == message_filter[k] for k in message_filter
        )

    return list(filter(filter_by_message, response))
