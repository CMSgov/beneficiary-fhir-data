from diagrams import Cluster, Diagram, Node
from diagrams.aws.compute import EC2, Lambda, ApplicationAutoScaling
from diagrams.aws.database import Aurora
from diagrams.aws.network import ELB, PrivateSubnet, VPC
from diagrams.aws.storage import S3

#######################################################
# Setup Some Input Variables for Easier Customization #
#######################################################

legend_text = (
"legend: \l"
"AZs - BFD does not exercise the assignment of resources per AZ. \l"
"Colocation of pipeline/migrator workloads reside with master aurora node \l"
"AutoScaling Group Cluster is actually nested inside each application AZ.\l"
"Due to limitations of diagram app, ASG is not displayed. Note Instances in App Layer are in ASG.\l" #FIX_ME
"--------------\l"
"Data Sources\l"
"CCW Pipeline Instance is triggered via lambda script when RIF files arrive in s3 bucket.\l"
"RDA Pipeline runs 24x7\l"
)
title = "BFD FHIR Service - Production VPC"
outformat = "svg"
filename = "BFD FHIR Service - Production VPC"
filenamegraph = "BFD FHIR Service - Production VPC.gv"
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
    with Cluster(label='bfd-prod-vpc'):
        s3 = S3("bfd-prod-etl")
        lamb = Lambda("bfd-prod-pipeline-scheduler")
        with Cluster("DMZ Layer"):
            with Cluster("bfd-prod-az(1,2,3)-dmz"):
                with Cluster("Public Subnet"):
                    elb = ELB("Prod ELB")
        with Cluster("Application Layer"):
            with Cluster("brd-prod-az1-app"):
                with Cluster("Private Subnet"):
                    server1 = EC2("bfd-prod-fhir-ap1")
            with Cluster("brd-prod-az2-app"):
                with Cluster("Private Subnet"):
                    server2 = EC2("bfd-prod-fhir-ap2")
            with Cluster("brd-prod-az3-app"):
                with Cluster("Private Subnet"):
                    server3 = EC2("bfd-prod-fhir-ap3")
        with Cluster("Data Layer"):
            with Cluster("bfd-prod-az1-data"):
                with Cluster("Private Subnet"):
                    db1 = Aurora("bfd-prod-replica1")
            with Cluster("bfd-prod-az2-data"):
                with Cluster("Private Subnet"):
                    db2 = Aurora("bfd-prod-replica2")
                    db_writer = Aurora("bfd-prod-master")
                    ccw = EC2("CCW bfd-prod-etl1")
                    rda = EC2("RDA bfd-prod-etl1")
                    migrator = EC2("DB Migrator")
            with Cluster("bfd-prod-az3-data"):
                with Cluster("Private Subnet"):
                    db3 = Aurora("bfd-prod-replica3")
            
    elb >> server1 >> db1 >> db_writer
    elb >> server2  >> db2 >> db_writer 
    elb >> server3  >> db3 >> db_writer
    db_writer << ccw << lamb << s3
    db_writer << rda
    db_writer << migrator
diag
