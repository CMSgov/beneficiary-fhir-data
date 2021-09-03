Ansible Role for BFD Pipeline
=============================

This Ansible role can be used to install and configure the [bfd-pipeline](../../../../apps/bfd-pipeline) application on a system.

Requirements
------------

On the Ansible management system, this role has no dependencies beyond Ansible itself. On the system being managed, it requires RHEL.

To modify and test this role, though, a number of things will need to be installed and configured. See the instructions here for information: [Blue Button Sandbox: Development Environment](https://github.com/HHSIDEAlab/bluebutton-sandbox#development-environment).

Tags
----

This role utilizes [Ansible Tags](https://docs.ansible.com/ansible/latest/user_guide/playbooks_tags.html) so that the same role can be used during different build or deployment stages. For CCS we use pre-ami and post-ami for immutable ec2 instances. If you execute this role without specifying tags Ansible assumes all tags are to be run. 

Role Variables
--------------

This role is highly configurable, though it tries to provide reasonable defaults where possible. Here are the variables that must be defined by users:

    data_pipeline_zip: /home/karlmdavis/workspaces/cms/beneficiary-fhir-data.git/apps/bfd-pipeline/bfd-pipeline-app/target/bfd-pipeline-app-1.0.0-SNAPSHOT.zip
    data_pipeline_s3_bucket: name-of-the-s3-bucket-with-the-data-to-process
    data_pipeline_hicn_hash_iterations: 42  # NIST recommends at least 1000
    data_pipeline_hicn_hash_pepper: '6E6F747468657265616C706570706572'  # Hex-encoded "nottherealpepper".
    data_pipeline_db_url: 'jdbc:postgresql://mydbserver.example.com:5432/mydb'
    data_pipeline_db_username: karlmdavis
    data_pipeline_db_password: 'notverysecureeither'
    data_pipeline_rda_job_enabled: false
    data_pipeline_rda_job_interval_seconds: 300
    data_pipeline_rda_job_batch_size: 20
    data_pipeline_rda_grpc_host: myrdahost.example.com
    data_pipeline_rda_grpc_port: 443
    data_pipeline_rda_grpc_max_idle_seconds: 600

See [defaults/main.yml](./defaults/main.yml) for the list of defaulted variables and their default values.

Dependencies
------------

This role does not have any runtime dependencies on other Ansible roles.

Example Playbook
----------------

Here's an example of how to apply this role to the `etlbox` host in an Ansible play:

    - hosts: pipeline_box
      tasks:
        - include_role:
            name: bfd-pipeline
          vars:
            data_pipeline_zip: /home/karlmdavis/workspaces/cms/beneficiary-fhir-data.git/apps/bfd-pipeline/bfd-pipeline-app/target/bfd-pipeline-app-1.0.0-SNAPSHOT.zip
            data_pipeline_s3_bucket: name-of-the-s3-bucket-with-the-data-to-process
            data_pipeline_hicn_hash_iterations: "{{ vault_data_pipeline_hicn_hash_iterations }}"
            data_pipeline_hicn_hash_pepper: "{{ vault_data_pipeline_hicn_hash_pepper }}"
            data_pipeline_db_url: 'jdbc:postgresql://mydbserver.example.com:5432/mydb'
            data_pipeline_db_username: "{{ vault_data_pipeline_db_username }}"
            data_pipeline_db_password: "{{ vault_data_pipeline_db_password }}"

Running the Tests
-----------------

This role includes a test framework that tests everything out using Docker, locally.
Those tests can be run, as follows:

    $ ops/ansible/roles/bfd-pipeline/test/run-tests.sh
