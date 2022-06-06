import sys
from awsglue.transforms import *
from awsglue.utils import getResolvedOptions
from pyspark.context import SparkContext
from awsglue.context import GlueContext
from awsglue.job import Job
from awsglue.dynamicframe import DynamicFrame
from pyspark.sql import functions as SqlFuncs
from pyspark.sql.window import Window

args = getResolvedOptions(sys.argv, ["JOB_NAME"])
sc = SparkContext()
glueContext = GlueContext(sc)
spark = glueContext.spark_session
job = Job(glueContext)
job.init(args["JOB_NAME"], args)

args = getResolvedOptions(sys.argv,
                          ['JOB_NAME',
                           'sourceTable',
                           'targetTable'])

print("sourceTable is set to: ", args['sourceTable'])
print("targetTable is set to: ", args['targetTable'])

SourceDyf = glueContext.create_dynamic_frame.from_catalog(database="bfd", table_name=args['sourceTable'], transformation_ctx="SourceDyf",)

# With bookmarks enabled, we have to make sure that there is data to be processed
if SourceDyf.count() > 0:
    TransformedDf = (
        SourceDyf.toDF()
        .filter(SqlFuncs.col("`mdc.http_access.response.status`") == "200")
        .withColumn("bene_id", SqlFuncs.expr("""explode(transform(split(`mdc.bene_id`,","), x -> bigint(x)))"""))
        .withColumn("timestamp", SqlFuncs.to_timestamp(SqlFuncs.col("timestamp")))
        .select(
            SqlFuncs.col("bene_id"),
            SqlFuncs.col("timestamp"),
            SqlFuncs.col("`mdc.http_access.request.clientssl.dn`").alias("clientssl_dn"),
            SqlFuncs.col("`mdc.http_access.request.operation`").alias("operation"),
            SqlFuncs.col("`mdc.http_access.request.uri`").alias("uri"),
            SqlFuncs.col("`mdc.http_access.request.query_string`").alias("query_string"),
            SqlFuncs.col("year"),
            SqlFuncs.col("month"),
            SqlFuncs.col("day")
        )
    )

    OutputDyf = DynamicFrame.fromDF(TransformedDf, glueContext, "OutputDyf")

    glueContext.write_dynamic_frame.from_catalog(
        frame=OutputDyf,
        database="bfd",
        table_name=args['targetTable'],
        additional_options={
            "updateBehavior": "UPDATE_IN_DATABASE",
            "partitionKeys": ["year", "month", "day"],
        },
        transformation_ctx="WriteNode",
    )

job.commit()
