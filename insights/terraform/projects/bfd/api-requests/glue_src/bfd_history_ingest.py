'''Ingest historical log data.'''
import sys

from awsglue.context import GlueContext
from awsglue.dynamicframe import DynamicFrame
from awsglue.job import Job
from awsglue.transforms import Unbox, Map
from awsglue.utils import getResolvedOptions
from pyspark.context import SparkContext
from pyspark.sql import functions as SqlFuncs


def format_field_names(record):
    ''' Format field names within a record to remove "." characters and lowercase. '''

    try:
        # Make a new record, because we can't iterate over the old one AND change the keys
        record_copy = {}

        for field_id in record.keys():
            record_copy[field_id.lower().replace('.', '_')] = record[field_id]

        return record_copy

    except Exception as e:
        # AWS Glue jobs are not good at providing error output from these
        # Mapping functions, which get outsourced to separate threads
        print('BFD_ERROR: {0}'.format(e))
        raise e


# Main

args = getResolvedOptions(sys.argv, ["JOB_NAME"])
sc = SparkContext()
glueContext = GlueContext(sc)
spark = glueContext.spark_session
job = Job(glueContext)
job.init(args["JOB_NAME"], args)

args = getResolvedOptions(sys.argv,
                          ['JOB_NAME',
                           'tempLocation',
                           'sourceDatabase',
                           'sourceTable',
                           'targetDatabase',
                           'targetTable'])

print("sourceDatabase is set to: ", args['sourceDatabase'])
print("   sourceTable is set to: ", args['sourceTable'])
print("targetDatabase is set to: ", args['targetDatabase'])
print("   targetTable is set to: ", args['targetTable'])

SourceDf = glueContext.create_dynamic_frame.from_catalog(
    database=args['sourceDatabase'],
    table_name=args['sourceTable'],
    transformation_ctx="SourceDf",
)

record_count = SourceDf.count()

# With bookmarks enabled, we have to make sure that there is data to be processed
if record_count > 0:
    print("Here is the schema from the source")
    SourceDf.toDF().printSchema()

    NextNode = DynamicFrame.fromDF(
        Unbox.apply(frame = SourceDf, path = "message", format="json").toDF()
        .select(SqlFuncs.col("message.*")), glueContext, "nextNode")

    RelationalizeBeneNode = NextNode.relationalize(
        'root',
        args['tempLocation']
        ).select('root')

    relationalized = DynamicFrame.fromDF(
        RelationalizeBeneNode.toDF()
        .withColumn('year', SqlFuncs.substring('timestamp', 1,4))
        .withColumn('month', SqlFuncs.substring('timestamp', 6,2))
        .withColumn('day', SqlFuncs.substring('timestamp', 9,2)),
        glueContext,
        "Relationalize"
        )

    OutputDy = Map.apply(frame = relationalized,
            f = format_field_names, transformation_ctx = 'Reformat_Field_Names')

    print("Here is the output schema:")
    OutputDy.toDF().printSchema()

    # Script generated for node Data Catalog table
    DataCatalogtable_node3 = glueContext.write_dynamic_frame.from_catalog(
        frame=OutputDy,
        database=args['targetDatabase'],
        table_name=args['targetTable'],
        additional_options={
            "enableUpdateCatalog": True,
            "updateBehavior": "UPDATE_IN_DATABASE",
            "partitionKeys": ["year", "month", "day"],
        },
        transformation_ctx="DataCatalogtable_node3",
    )

job.commit()

print("Job complete. %d records processed." % record_count)
