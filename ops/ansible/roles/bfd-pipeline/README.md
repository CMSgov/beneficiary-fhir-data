Blue Button Data Pipeline
=========================

This Ansible role can be used to install and configure the [bluebutton-data-pipeline](https://github.com/HHSIDEAlab/bluebutton-data-pipeline) application on a system.

Requirements
------------

On the Ansible management system, this role has no dependencies beyond Ansible itself. On the system being managed, it requires RHEL.

To modify and test this role, though, a number of things will need to be installed and configured. See the instructions here for information: [Blue Button Sandbox: Development Environment](https://github.com/HHSIDEAlab/bluebutton-sandbox#development-environment).

Role Variables
--------------

This role is highly configurable, though it tries to provide reasonable defaults where possible. Here are the variables that must be defined by users:

    data_pipeline_appjar_name: bfd-pipeline-app-1.0.0-SNAPSHOT-capsule-fat.jar
    data_pipeline_appjar_localpath: /home/karlmdavis/workspaces/cms/beneficiary-fhir-data.git/apps/bfd-pipeline/bfd-pipeline-app/target
    data_pipeline_s3_bucket: name-of-the-s3-bucket-with-the-data-to-process
    data_pipeline_hicn_hash_iterations: 42  # NIST recommends at least 1000
    data_pipeline_hicn_hash_pepper: '6E6F747468657265616C706570706572'  # Hex-encoded "nottherealpepper".
    data_pipeline_db_url: 'jdbc:postgresql://mydbserver.example.com:5432/mydb'
    data_pipeline_db_username: karlmdavis
    data_pipeline_db_password: 'notverysecureeither'

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
            name: karlmdavis.bluebutton_data_pipeline
          vars:
            data_pipeline_appjar_name: bfd-pipeline-app-1.0.0-SNAPSHOT-capsule-fat.jar
            data_pipeline_appjar_localpath: /home/karlmdavis/workspaces/cms/beneficiary-fhir-data.git/apps/bfd-pipeline/bfd-pipeline-app/target
            data_pipeline_s3_bucket: name-of-the-s3-bucket-with-the-data-to-process
            data_pipeline_hicn_hash_iterations: "{{ vault_data_pipeline_hicn_hash_iterations }}"
            data_pipeline_hicn_hash_pepper: "{{ vault_data_pipeline_hicn_hash_pepper }}"
            data_pipeline_db_url: 'jdbc:postgresql://mydbserver.example.com:5432/mydb'
            data_pipeline_db_username: "{{ vault_data_pipeline_db_username }}"
            data_pipeline_db_password: "{{ vault_data_pipeline_db_password }}"

## License

This project is in the worldwide [public domain](LICENSE.md). As stated in [CONTRIBUTING](CONTRIBUTING.md):

> This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
>
> All contributions to this project will be released under the CC0 dedication. By submitting a pull request, you are agreeing to comply with this waiver of copyright interest.

