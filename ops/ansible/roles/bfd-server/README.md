Ansible Role for BFD Server
===========================

This Ansible role can be used to install and configure the [bfd-server](../../../../bfd-server) application on a system.

Requirements
------------

On the Ansible management system, this role has no dependencies beyond Ansible itself. On the system being managed, it requires RHEL.

To modify and test this role, though, a number of things will need to be installed and configured. See the instructions here for information: [Blue Button Sandbox: Development Environment](https://github.com/HHSIDEAlab/bluebutton-sandbox#development-environment).

Tags
----

This role utilizes [Ansible Tags](https://docs.ansible.com/ansible/latest/user_guide/playbooks_tags.html) so that the same role can be used during different build or deployment stages. For CCS we use pre-ami and post-ami for immutable ec2 instances. If you execute this role without specifying tags Ansible assumes all tags are to be run. 

Post Role Execution
-------------------

Due to this role being used for pre and post configuration tasks, a restart of the Pipeline Service is required, something like this should work:

-  name: Trigger Pipeline Service Restart handler
   command: /bin/true
   notify: 
      - Restart Pipeline Service

Role Variables
--------------

This role is highly configurable, though it tries to provide reasonable defaults where possible.

Variables that must be defined to use the role:

* `data_server_container`: The path (on the management system) to the app server installation bundle to be deployed.
* `data_server_container_name`: The name of the directory that will be unzipped by the app server installation bundle, e.g. "`wildfly-8.1.0.Final`".
* `data_server_war`: The path (on the management system) to the WAR file to be deployed.
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
