"""This module contains various utility functions shared between the node and controller that are
related to boto3.
"""

import json
import re
from typing import TYPE_CHECKING, Any

if TYPE_CHECKING:
    from types_boto3_rds import RDSClient
    from types_boto3_sqs.service_resource import Queue
    from types_boto3_ssm import SSMClient

else:
    RDSClient = object
    Queue = object
    SSMClient = object


def get_ssm_parameter(ssm_client: SSMClient, name: str, with_decrypt: bool = False) -> str:
    """Retrieve the value of the given SSM parameter optionally decrypting it if specified.

    Args:
        ssm_client: An instance of boto3's SSM client
        name (str): The name of the SSM parameter to retrieve
        with_decrypt (bool, optional): Whether or not to decrypt the retrieved SSM paraemeter.
        Defaults to False.

    Raises:
        ValueError: Raised if the parameter was not found

    Returns:
        str: The value of the SSM parameter
    """
    # TODO: Properly type hint 'ssm_client'
    response = ssm_client.get_parameter(Name=name, WithDecryption=with_decrypt)

    try:
        return response["Parameter"]["Value"]  # type: ignore
    except KeyError as exc:
        raise ValueError(f'SSM parameter "{name}" not found or empty') from exc


def get_rds_db_uri(rds_client: RDSClient, cluster_id: str) -> str:
    """Retrieve the RDS database URI for a given cluster ID's reader endpoint.

    Args:
        rds_client: An instance of boto3's RDS client
        cluster_id (str): The cluster ID to get the reader endpoint URI from

    Raises:
        ValueError: Raised if no reader endpoint was found for the given cluster ID

    Returns:
        str: The URI for the cluster's reader endpoint
    """
    # TODO: Properly type hint 'rds_client'
    response = rds_client.describe_db_clusters(DBClusterIdentifier=cluster_id)

    try:
        return response["DBClusters"][0]["ReaderEndpoint"]  # type: ignore
    except KeyError as exc:
        raise ValueError(f'DB URI not found for cluster ID "{cluster_id}"') from exc


def check_queue(queue: Queue, timeout: int = 1) -> list[dict[str, str]]:
    """Check a given SQS queue for messages and returns the inner JSON message from each.

    Args:
        queue: A boto3 SQS queue
        timeout (int, optional): Amount of time to poll for messages in queue. Defaults to 1

    Returns:
        List[Dict[str, str]]: The list of inner queue messages
    """
    responses = queue.receive_messages(
        AttributeNames=["SenderId", "SentTimestamp"],
        WaitTimeSeconds=timeout,
    )

    def load_json_safe(json_str: str) -> dict[str, Any] | None:
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
    return [
        load_json_safe(message.get("Message") or "") or message
        for message in raw_messages
        if message is not None
    ]


def get_warm_pool_count(message: dict[str, str]) -> int:
    """Retrieve the desired warm pool instance count from a given SQS scaling message.

    Args:
        message (Dict[str, str]): An SQS message generated from a scaling event

    Raises:
        ValueError: Raised if message does not have a Cause key
        ValueError: Raise if message's Cause does not indicate scaling

    Returns:
        int: The number of desired instances to scale to
    """
    try:
        scaling_cause = message["Cause"]
    except KeyError as exc:
        raise ValueError(f'Message {message} does not contain a "Cause" key') from exc

    count_search = re.search(
        r"increasing the capacity from \d+ to (\d+)", scaling_cause, re.IGNORECASE
    )
    if not count_search:
        raise ValueError(f"Cause in message {message} does not indicate any scaling occurred")

    return int(count_search.group(1))
