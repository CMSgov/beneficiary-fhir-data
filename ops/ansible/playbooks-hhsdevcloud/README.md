Blue Button API Sandbox Ansible Repo
------------------------------------

This repository contains the Ansible provisioning, roles, etc. used to setup and manage the Blue Button API's developer sandbox.

While it can be run manually, it is expected that these Ansible playbooks will be run automatically by the Blue Button API's build server as its projects are successfully built. The playbooks will deploy the newly-built artifacts such that they host the public Blue Button sandbox. If the sandbox is already deployed and running, it attempts to manage the updates such that clients will not experience any dropped requests or downtime.

These plays use different deployment approaches, as appropriate, for the different systems being deployed:

* **(Mostly) Permanent Infrastructure** (database servers, S3 buckets, KMS keys, etc.)
    * These systems are deployed as a "typical" Ansible project: check to see if they're present and if not, fix that. Note that the logic for upgrading these systems will have to be added into the plays, as needed.
* **Load Balanced Applications** (backend FHIR server)
    * These systems are deployed using the excellent approach outlined in this article: [Why Ansible 1.8 is the new immutable deployment killer](https://t37.net/why-ansible-1-8-is-the-new-immutable-deployment-killer.html). The Ansible plays stand up a "master" EC2 instance, configure it properly, convert it to an AMI, then terminate it. The "master" AMI is then pushed incrementally out to an EC2 auto-sclaing group (attached to an EC2 load balancer).
    * The plays could be sped up quite a bit if they were enhanced to create and use "partial master" AMIs, rather than always starting from a completely clean OS image.
* **Other Non-Load Balanced Infrastructure** (backend ETL service)
    * The ETL service can't be load-balanced (as it pulls data, rather being pushed requests). In addition, it tolerates downtime as long as care is taken to ensure that it is allowed to shutdown gracefully (ensuring that processing isn't interrupted in the middle of a data set). Accordingly, the deployment of this service just gracefully stops the current ETL EC2 instance, then stands up a new one from scratch.

Note that deployment rollback is not supported for any of these services. Problems should instead be resolved by "rolling forward" to new releases that resolve whatever problems were encountered. This fits the high-velocity DevOps model being used in this project.

## Development Environment

In order to use and/or modify this repository, a number of tools need to be installed.

### Python

This project requires Python 3.7. It can be installed as follows on MacOS (via Homebrew):

    $ brew install python

Additional packages are also required by some of the Python modules that will be used. To install them on MacOS (via Homebrew):

    $ brew install libpq

To install those additional packages on RHEL 7:

    $ sudo yum install postgresql-libs

### pipenv

This project has some Python dependencies that have to be installed. We use the `pipenv` tool to install and manage those dependencies in a segregated virtual environment (so that this project's dependencies don't conflict with dependencies from other projects).

If it isn't already installed, install the `pipenv` package and the development tools that will be needed to build the Python packages we'll be grabbing. On MacOs, this is best done via:

    $ brew install pipenv

Next, initialize `pipenv` for this project:

    $ cd ops/ansible/playbooks-healthapt
    $ pipenv install --three

When you want to run something that requires the Python dependencies, prefix the command with `pipenv run`.

You can add, update, etc. the project's Python dependencies by editing the `Pipfile` in this directory and using `pipenv`, as documented here: <https://docs.pipenv.org/en/latest/>.

### Ansible Roles

Run the following command to download and install the roles required by this project into `~/.ansible/roles/`:

    $ pipenv run ansible-galaxy install --force --role-file=install_roles.yml

### AWS Credentials

Per <http://blogs.aws.amazon.com/security/post/Tx3D6U6WSFGOK2H/A-New-and-Standardized-Way-to-Manage-Credentials-in-the-AWS-SDKs>, create `~/.aws/credentials` and populate it with your AWS access key and secret (obtained from IAM):

    [default]
    # These AWS keys are for the john.smith@cms.hhs.gov account.
    aws_access_key_id = secret
    aws_secret_access_key = supersecret

Ensure that the EC2 key to be used is loaded into SSH Agent:

    $ ssh-add foo.pem

### Running the Playbooks

The playbooks can be run, as follows:

    $ cat << EOF > extra_vars.json
    {
      "management_user": {
        "cn": "John Doe",
        "email": "jdoe@cms.hhs.gov"
      },
      "ec2_key_name": "somekeyname",
      "data_pipeline_jar": "../../../apps/bfd-pipeline/bfd-pipeline-app/target/bfd-pipeline-app-1.0.0-SNAPSHOT-capsule-fat.jar",
      "data_server_container": "../../../apps/bfd-server/bfd-server-war/target/bfd-server/wildfly-dist-8.1.0.Final.tar.gz",
      "data_server_container_name": "wildfly-8.1.0.Final",
      "data_server_war": "../../../apps/bfd-server/bfd-server-war/target/bfd-server-war-1.0.0-SNAPSHOT.war"
    }
    EOF
    $ pipenv run ./ansible-playbook-wrapper sandbox.yml --extra-vars "@extra_vars.json"

This project has a number of variables that must be specified for each run. Each of the `extra-vars` should be set, as follows:

* `deploy_id_custom`: Optional. An ID for the deployment to be run, which will be used to tag AMIs, etc. in AWS. By default, a UTC timestamp is used.
* `management_user`: The administrator contact that will be referenced in any notifications that are sent out.
* `ec2_key_name`: The name of the SSH key (as it's labeled in EC2) that all newly-created EC2 instances should be associated with.

## Interacting with the Deployed Environment

### Querying the Data Server

Ad-hoc queries can be run against the Blue Button backend Data Server, as follows:

    $ curl --silent --insecure --cert-type pem --cert files/client-test-keypair.pem "https://fhir.backend.bluebutton.hhsdevcloud.us/baseDstu3/ExplanationOfBenefit?patient=3960&_format=application%2Fjson%2Bfhir"

### Ansible Inventory

Unlike most Ansible playbooks, this project does not use either a static inventory file or a scripted dynamic inventory. Instead, the inventory is generated at runtime, by the plays themselves.
