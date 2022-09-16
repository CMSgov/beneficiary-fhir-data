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


def check_queue(
    queue, timeout: int = 1, message_filters: Optional[List[Dict[str, str]]] = None
) -> List[Dict[str, str]]:
    """Checks a given SQS queue for messages. Optionally, a list of message filters can be provided
    that will be used to filter out queue messages with inner JSON messages that do not have keys
    with values matching the given filter

    Args:
        queue: A boto3 SQS queue
        timeout (int, optional): Amount of time to poll for messages in queue. Defaults to 1
        message_filters (Optional[List[Dict[str, str]]], optional): Inner JSON message filter.
        Messages must having matching keys and values to be retrieved from queue. Defaults to None

    Returns:
        List[Dict[str, str]]: The list of inner queue messages that passed the filter, if applicable
    """
    # TODO: Properly type hint 'queue'
    responses: List[Dict[str, str]] = queue.receive_messages(
        AttributeNames=["SenderId", "SentTimestamp"],
        WaitTimeSeconds=timeout,
    )

    raw_messages = [response.get("Message") or response.get("Body") for response in responses]
    messages = [json.loads(raw_message) for raw_message in raw_messages]

    if not message_filters:
        return messages

    return list(filter(filter_message_by_keys, messages))
