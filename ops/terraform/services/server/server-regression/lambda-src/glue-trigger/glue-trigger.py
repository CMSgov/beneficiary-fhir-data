import os

import boto3

crawler_name = os.environ.get("CRAWLER_NAME")

glue = boto3.client(service_name="glue", region_name="us-east-1")


def handler(event, context):
    try:
        glue.start_crawler(Name=crawler_name)
    except glue.exceptions.CrawlerRunningException:
        print(f"{crawler_name} was already running")
