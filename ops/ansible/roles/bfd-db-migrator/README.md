Ansible Role for BFD DB Migrator
=============================

This Ansible role can be used to install and configure the [bfd-db-migrator](../../../../apps/bfd-db-migrator) application on a system.

Requirements
------------

On the Ansible management system, this role has no dependencies beyond Ansible itself. On the system being managed, it requires systemd-enabled Enterprise Linux, e.g. Centos, RedHat, or Amazon Linux.

Tags
----

This role utilizes [Ansible Tags](https://docs.ansible.com/ansible/latest/user_guide/playbooks_tags.html) so that the same role can be used during different build or deployment stages. For CCS we use pre-ami and post-ami for immutable ec2 instances. If you execute this role without specifying tags Ansible assumes all tags are to be run. 

Role Variables
--------------

This role is highly configurable, though it tries to provide reasonable defaults where possible: Here are the variables that must be defined by users:

| Name                                | Description                                                                                   | Default              | Required        |
|-------------------------------------|-----------------------------------------------------------------------------------------------|----------------------|-----------------|
| db_migrator_db_password             | password for targeted database                                                                | n/a                  | yes             |
| db_migrator_db_url                  | url for targeted database, e.g. `jdbc:postgresql://mydbserver.example.com:5432/mydb`          | n/a                  | yes             |
| db_migrator_db_username             | username for targeted database                                                                | n/a                  | yes             |
| bfd_env                             | deployment env, e.g. `prod`, `prod-sbx`, `test` **required by migrator-monitor**<sup>\*</sup> | n/a                  | no<sup>\*</sup> |
| db_migrator_dir                     | primary, on-host directory for migrator-related resources                                     | /opt/bfd-db-migrator | no              |
| db_migrator_jvm_args                | arguments passed directly to the JVM                                                          | -Xmx64g              | no              |
| db_migrator_tmp_dir                 |                                                                                               | /tmp                 | no              |
| db_migrator_user                    | user to be created to run migrator and migrator-monitor service                               | bb-migrator          | no              |
| migrator_monitor_enabled            | migrator-monitor enabled for sqs message passing, **requires `bfd_env`**                      | false                | no              |
| migrator_monitor_heartbeat_interval | sleep interval between monitor heartbeats                                                     | 300                  | no              |


See [defaults/main.yml](./defaults/main.yml) for the list of defaulted variables and their default values.

Dependencies
------------

This role does not have any runtime dependencies on other Ansible roles.

Example Playbook
----------------

Here's an example of how to apply this role to the `bfd-db-migrator` host in an Ansible play:

    - hosts: bfd-db-migrator
    tasks:
        - name: Apply Role
        import_role:
            name: bfd-db-migrator
        vars:
            db_migrator_zip: "{{ lookup('env','HOME') }}/.m2/repository/gov/cms/bfd/bfd-db-migrator/1.0.0-SNAPSHOT/bfd-db-migrator-1.0.0-SNAPSHOT.zip"
            db_migrator_db_url: 'jdbc:hsqldb:mem:test'
            db_migrator_db_username: "{{ vault_db_migrator_db_username }}"
            db_migrator_db_password: "{{ vault_db_migrator_db_password }}"


Running the Tests
-----------------

This role includes a test framework that tests everything out using Docker, locally.
Those tests can be run, as follows:

    $ ops/ansible/roles/bfd-db-migrator/test/run-tests.sh
