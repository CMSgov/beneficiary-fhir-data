'''Populate the Beneficiaries table from the api_history table'''
import sys

from awsglue.context import GlueContext
from awsglue.dynamicframe import DynamicFrame
from awsglue.job import Job
from awsglue.utils import getResolvedOptions
from pyspark.context import SparkContext
from pyspark.sql import functions as SqlFuncs

args = getResolvedOptions(sys.argv, ["JOB_NAME"])
sc = SparkContext()
glueContext = GlueContext(sc)
spark = glueContext.spark_session
job = Job(glueContext)
job.init(args["JOB_NAME"], args)

args = getResolvedOptions(sys.argv,
                          ['JOB_NAME',
                           'sourceDatabase',
                           'sourceTable',
                           'targetDatabase',
                           'targetTable'])

print("sourceDatabase is set to: ", args['sourceDatabase'])
print("   sourceTable is set to: ", args['sourceTable'])
print("targetDatabase is set to: ", args['targetDatabase'])
print("   targetTable is set to: ", args['targetTable'])


SourceDyf = glueContext.create_dynamic_frame.from_catalog(database=args['sourceDatabase'],
    table_name=args['sourceTable'], transformation_ctx="SourceDyf",)

# With bookmarks enabled, we have to make sure that there is data to be processed
if SourceDyf.count() > 0:
    TransformedDf = (
        SourceDyf.toDF()
        .filter(SqlFuncs.col("`mdc_http_access_response_status`") == "200")
        .withColumn("bene_id", SqlFuncs.expr("""explode(transform(split(`mdc_bene_id`,","), x -> bigint(x)))"""))
        .withColumn("timestamp", SqlFuncs.to_timestamp(SqlFuncs.col("timestamp")))
        .select(
            SqlFuncs.col("bene_id"),
            SqlFuncs.col("timestamp"),
            SqlFuncs.col("`mdc_http_access_request_clientssl_dn`").alias("clientssl_dn"),
            SqlFuncs.col("`mdc_http_access_request_operation`").alias("operation"),
            SqlFuncs.col("`mdc_http_access_request_uri`").alias("uri"),
            SqlFuncs.col("`mdc_http_access_request_query_string`").alias("query_string"),
            SqlFuncs.col("year"),
            SqlFuncs.col("month"),
            SqlFuncs.col("day")
        )
    )

    OutputDyf = DynamicFrame.fromDF(TransformedDf, glueContext, "OutputDyf")

    glueContext.write_dynamic_frame.from_catalog(
        frame=OutputDyf,
        database=args['targetDatabase'],
        table_name=args['targetTable'],
        additional_options={
            "updateBehavior": "UPDATE_IN_DATABASE",
            "partitionKeys": ["year", "month", "day"],
            "enableUpdateCatalog": True,
        },
        transformation_ctx="WriteNode",
    )

job.commit()
