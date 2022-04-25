Ansible Role for BFD DB Migrator
=============================

This Ansible role can be used to install and configure the [bfd-db-migrator](../../../../apps/bfd-db-migrator) application on a system.

Requirements
------------

On the Ansible management system, this role has no dependencies beyond Ansible itself. On the system being managed, it requires systemd-enabled Enterprise Linux, e.g. Centos, RedHat, or Amazon Linux.

To modify and test this role, though, a number of things will need to be installed and configured. See the instructions here for information: [Blue Button Sandbox: Development Environment](https://github.com/HHSIDEAlab/bluebutton-sandbox#development-environment).

Tags
----

This role utilizes [Ansible Tags](https://docs.ansible.com/ansible/latest/user_guide/playbooks_tags.html) so that the same role can be used during different build or deployment stages. For CCS we use pre-ami and post-ami for immutable ec2 instances. If you execute this role without specifying tags Ansible assumes all tags are to be run. 

Role Variables
--------------

This role is highly configurable, though it tries to provide reasonable defaults where possible. Here are the variables that must be defined by users:

    db_migrator_dir: /usr/local/bfd-migrator
    db_migrator_user: bb-migrator
    db_migrator_jvm_args: -Xmx64g
    db_migrator_tmp_dir: /tmp
    db_migrator_db_url: 'jdbc:postgresql://mydbserver.example.com:5432/mydb'
    db_migrator_db_username: karlmdavis
    db_migrator_db_password: 'notverysecureeither'

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
