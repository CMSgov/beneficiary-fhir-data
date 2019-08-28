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

This project requires Python 2.7. It can be installed as follows:

    $ sudo apt-get install python

The following packages are also required by some of the Python modules that will be used:

    $ sudo apt-get install libpq-dev

### virtualenv

This project has some dependencies that have to be installed via `pip` (as opposed to `apt-get`). Accordingly, it's strongly recommended that you make use of a [Python virtual environment](http://docs.python-guide.org/en/latest/dev/virtualenvs/) to manage those dependencies.

If it isn't already installed, install the `virtualenv` package. On Ubuntu, this is best done via:

    $ sudo apt-get install python-virtualenv

Next, create a virtual environment for this project and install the project's dependencies into it:

    $ cd bluebutton-ansible-playbooks-data-sandbox.git
    $ virtualenv -p /usr/bin/python2.7 venv
    $ source venv/bin/activate
    $ pip install --upgrade setuptools
    $ pip install --requirement requirements.txt

The `source` command above will need to be run every time you open a new terminal to work on this project.

Be sure to update the `requirements.frozen.txt` file after `pip install`ing a new dependency for this project:

    $ pip freeze > requirements.frozen.txt

### Ansible Roles

Run the following command to download and install the roles required by this project into `~/.ansible/roles/`:

    $ ansible-galaxy install --role-file=install_roles.yml --force

### AWS Credentials

Per <http://blogs.aws.amazon.com/security/post/Tx3D6U6WSFGOK2H/A-New-and-Standardized-Way-to-Manage-Credentials-in-the-AWS-SDKs>, create `~/.aws/credentials` and populate it with your AWS access key and secret (obtained from IAM):

    [default]
    # These AWS keys are for the john.smith@cms.hhs.gov account.
    aws_access_key_id = secret
    aws_secret_access_key = supersecret

Ensure that the EC2 key to be used is loaded into SSH Agent:

    $ ssh-add foo.pem

### User Identity

These plays need to know the identity of the user/system running them. Copy the identity template:

    $ cp group_vars/all/management_user.yml.template group_vars/all/management_user.yml

And then edit the resulting `group_vars/all/management_user.yml` file to fill in the required fields.

### Running the Playbooks

The playbooks can be run, as follows:

    $ ansible-playbook-wrapper sandbox.yml --extra-vars "ec2_key_name=<key-name> maven_repo=<local-repo-path> bluebutton_server_version=<version> wildfly_version=<version> backend_etl_version=<version>"

This project has an unfortunately large amount of variables that must be specified for each run. Each of the `extra-vars` should be set, as follows:

* `deploy_id_custom`: Optional. An ID for the deployment to be run, which will be used to tag AMIs, etc. in AWS. By default, a UTC timestamp is used.
* `ec2_key_name`: The name of the SSH key (as it's labeled in EC2) that all newly-created EC2 instances should be associated with.
* `maven_repo`: Path to the local Maven repository directory, from which the deployment resources will be pulled.
* `bluebutton_server_version`: The version of the Blue Button Data Server artifact (`gov.cms.bfd:bfd-server-war:war` artifact (and related artifacts) to deploy as the Blue Button backend FHIR server.
* `wildfly_version`: The version of the `org.wildfly:wildfly-dist:tar.gz` artifact to deploy and use to host the Blue Button backend FHIR server.
* `backend_etl_version`: The version of the Blue Button Data Pipeline artifact (`gov.cms.bfd:bfd-pipeline-app:capsule-fat:jar`) to deploy.

## Interacting with the Deployed Environment

### Querying the Data Server

Ad-hoc queries can be run against the Blue Button backend Data Server, as follows:

    $ curl --silent --insecure --cert-type pem --cert files/client-test-keypair.pem "https://fhir.backend.bluebutton.hhsdevcloud.us/baseDstu3/ExplanationOfBenefit?patient=3960&_format=application%2Fjson%2Bfhir"

### Ansible Inventory

Unlike most Ansible playbooks, this project does not use either a static inventory file or a scripted dynamic inventory. Instead, the inventory is generated at runtime, by the plays themselves.

## License

This project is in the worldwide [public domain](LICENSE.md). As stated in [CONTRIBUTING](CONTRIBUTING.md):

> This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
>
> All contributions to this project will be released under the CC0 dedication. By submitting a pull request, you are agreeing to comply with this waiver of copyright interest.

