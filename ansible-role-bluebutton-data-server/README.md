Ansible Role for Blue Button FHIR Data Server
=============================================

This Ansible role can be used to install and configure the [bluebutton-server](https://github.com/HHSIDEAlab/bluebutton-server) application on a system.

Requirements
------------

On the Ansible management system, this role has no dependencies beyond Ansible itself. On the system being managed, it requires RHEL.

To modify and test this role, though, a number of things will need to be installed and configured. See the instructions here for information: [Blue Button Sandbox: Development Environment](https://github.com/HHSIDEAlab/bluebutton-sandbox#development-environment).

Role Variables
--------------

This role is highly configurable, though it tries to provide reasonable defaults where possible.

Variables that must be defined to use the role:

* `data_server_artifacts_mode`: Set to "`s3`" if the WAR and app server should be copied from an AWS S3 bucket or "`local`" if they should be copied from a path local to the Ansible management host.
* `data_server_artifacts_s3_bucket`: The name of the S3 bucket containing the artifacts to be deployed. Must be set if `data_server_artifacts_mode` is "`s3`".
* `data_server_appserver_installer_name`: The name of the app server installation bundle to be deployed.
* `data_server_appserver_name`: The name of the directory that will be unzipped by the app server installation bundle, e.g. "`wildfly-8.1.0.Final`".
* `data_server_appserver_local_dir`: The local directory for the app server installation bundle to be deployed. Must be set if `data_server_artifacts_mode` is "`local`".
* `data_server_war_name`: The name of the WAR file to be deployed (as saved in either S3 or a local directory).
* `data_server_war_local_dir`: The local directory of the WAR file to be deployed. Must be set if `data_server_artifacts_mode` is "`local`".
* `data_server_ssl_server_genkeypair_args`: The arguments to pass to the `keytool` command when generating a server keypair.
* `data_server_ssl_client_cas`: A list of the SSL client certificate authorities that will be given access to the server.
    * `alias`: The alias to assign to the SSL client certificate authority in the server's Java trust store.
    * `certificate`: The contents of the public certificate authority for the SSL client to authorize.
* `data_server_ssl_client_certificates`: A list of the (non-CA) SSL client certificates that will be given access to the server.
    * `alias`: The alias to assign to the SSL client certificate in the server's Java trust store.
    * `certificate`: The contents of the public certificate for the SSL client to authorize.
* `data_server_db_url`: The JDBC URL to connect to.
* `data_server_db_username`: The DB username to connect as.
* `data_server_db_password`: The DB user's password to connect with.

See [defaults/main.yml](./defaults/main.yml) for the list of optional/defaulted variables and their default values.

Dependencies
------------

This role does not have any runtime dependencies on other Ansible roles.

Example Playbook
----------------

See the test script in [.travis/test_base.yml](./.travis/test_base.yml) and the test case variables in [.travis/vars/](./.travis/vars/) for examples of how to apply this role in Ansible plays.

## License

This project is in the worldwide [public domain](LICENSE.md). As stated in [CONTRIBUTING](CONTRIBUTING.md):

> This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
>
> All contributions to this project will be released under the CC0 dedication. By submitting a pull request, you are agreeing to comply with this waiver of copyright interest.

