from diagrams import Cluster, Diagram, Node
from diagrams.aws.compute import EC2, Lambda
from diagrams.aws.database import Aurora
from diagrams.aws.network import ELB, VPCPeering
from diagrams.aws.storage import S3
from diagrams.generic.place import Datacenter

#######################################################
# Setup Some Input Variables for Easier Customization #
#######################################################

legend_text = (
"legend: \l"
"prod-sbx.bfd.cms.gov - External Facing(DMZ Subnets) requires mTLS \l"
"prod.bfd.cms.gov/test.bfd.cms.gov - Internal Facing (App Subnets) requires mTLS \l"
"AZs - BFD does not exercise the assignment of resources per AZ. \l"
"Colocation of pipeline/migrator workloads reside with writer aurora node \l"
)

title = "BFD High Level View - Infrastructure"
outformat = "svg"
filename = "bfd_infrastructure"
filenamegraph = "bfd_infrastructure.gv"
show = False
direction = "TB"

with Diagram(
    name=title,
    direction=direction,
    show=show,
    filename=filename,
    outformat=outformat,
) as diag:
    Node(label=legend_text, width="8", shape="plaintext", )
    # Cluster = Group, so this outline will group all the items nested in it automatically
    with Cluster("CMS AWS"):
        etl = S3("CCW Data Ingest")
        bb2 = VPCPeering("BB2 API")
        bcda = VPCPeering("BCDA API")
        ab2d = VPCPeering("AB2D API")
        dpc = VPCPeering("DPC API")
        rda = VPCPeering("MPSM RDA API")
        # Sub cluster for grouping inside the vpc
        with Cluster("BFD Prod VPC"):
            elb = ELB("Prod ELB")
            lamb = Lambda("bfd-prod-pipeline-scheduler")
            # Sub cluster for grouping inside the AZs
            with Cluster("AZ1"):
                server1 = EC2("FHIR Server AZ1")
                db1 = Aurora("Postgres Reader 1")
            # Sub cluster for grouping inside the AZs
            with Cluster("AZ2"):
                server2 = EC2("FHIR Server AZ2")
                db2 = Aurora("Postgres Reader 2")
            # Sub cluster for grouping inside the AZs
            with Cluster("AZ3"):
                server3 = EC2("FHIR Server AZ3")
                db3 = Aurora("Postgres Reader 3")
                db_writer = Aurora("Postgres Writer")
                migrator = EC2("DB Migrator")
                pipeline = EC2("CCW Data Pipeline")
                rda_pipeline = EC2("RDA Data Pipeline")
                
    with Cluster("CCW"):
        ccw = Datacenter("CCW ETL Process")

 ###################################################
    # FLOW OF ACTION, NETWORK, or OTHER PATH TO CHART #
###################################################
        elb >> server1 >> db1
        elb >> server2 >> db2
        elb >> server3 >> db3
    ccw >> etl >> lamb >> pipeline >> db_writer
    migrator >> db_writer
    bb2 >> elb
    bcda >> elb
    ab2d >> elb
    dpc >> elb
    rda >> rda_pipeline >> db_writer
    
diag
