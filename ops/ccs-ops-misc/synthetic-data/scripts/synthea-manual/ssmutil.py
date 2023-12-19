#
# Simple util to import for getting things out of SSM.
#
# Requires boto3 installed
#

import urllib
import boto3
from botocore.config import Config

boto_config = Config(region_name="us-east-1")
ssm_client = boto3.client("ssm", config=boto_config)
rds_client = boto3.client("rds", config=boto_config)

def get_ssm_db_string(environment):
    """
    Gets the database connection string for the given
    environment using RDS to query the cluster and gets
    the username/password from ssm.
    
    Environment should be one of: test prod-sbx prod
    """
    try:
        cluster_id = get_ssm_parameter(
            f"/bfd/{environment}/common/nonsensitive/rds_cluster_identifier"
        )
        username = get_ssm_parameter(
            f"/bfd/{environment}/server/sensitive/db/username", with_decrypt=True
        )
        raw_password = get_ssm_parameter(
            f"/bfd/{environment}/server/sensitive/db/password", with_decrypt=True
        )
    except ValueError as exc:
        print("Failed getting SSM DB params: " + str(exc))
        return
        
    try:
        db_uri = get_rds_db_uri(cluster_id)
    except ValueError as exc:
        print("Failed getting SSM DB uri: " + str(exc))
        return
    
    password = urllib.parse.quote(raw_password)
    return f"postgres://{username}:{password}@{db_uri}:5432/fhirdb"

def get_ssm_parameter(name: str, with_decrypt: bool = False) -> str:
    """
    Gets the ssm parameter with the given name from the ssm store.
    """
    response = ssm_client.get_parameter(Name=name, WithDecryption=with_decrypt)
    
    try:
        return response["Parameter"]["Value"]
    except KeyError as exc:
        raise ValueError(f'SSM parameter "{name}" not found or empty') from exc

def get_rds_db_uri(cluster_id: str) -> str:
    """
    Uses the rds connection and cluster id to get the database url for that cluster.
    """
    response = rds_client.describe_db_clusters(DBClusterIdentifier=cluster_id)

    try:
        return response["DBClusters"][0]["ReaderEndpoint"]
    except KeyError as exc:
        raise ValueError(f'DB URI not found for cluster ID "{cluster_id}"') from exc
