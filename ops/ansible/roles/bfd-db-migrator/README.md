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

| Name                                | Description                                                                                               | Default              | Required        |
|-------------------------------------|-----------------------------------------------------------------------------------------------------------|----------------------|-----------------|
| db_migrator_db_password             | password for targeted database                                                                            | n/a                  | yes             |
| db_migrator_db_url                  | url for targeted database, e.g. `jdbc:postgresql://mydbserver.example.com:5432/mydb`                      | n/a                  | yes             |
| db_migrator_db_username             | username for targeted database                                                                            | n/a                  | yes             |
| env                                 | deployment env, e.g. `prod`, `prod-sbx`, `test` **required by the migrator monitor**<sup>\*</sup>         | test                 | no<sup>\*</sup> |
| db_migrator_dir                     | primary, on-host directory for migrator-related resources                                                 | /opt/bfd-db-migrator | no              |
| db_migrator_tmp_dir                 | defines the `-Djava.io.tmpdir` for the migrator's jvm                                                     | /tmp                 | no              |
| db_migrator_user                    | user to be created to run migrator service                                                                | bb-migrator          | no              |
| migrator_monitor_enabled            | migrator-monitor enabled for sqs message passing, **requires `env`**                                      | false                | no              |
| migrator_monitor_heartbeat_interval | sleep interval between monitor heartbeats                                                                 | 300                  | no              |
| sqs_queue_name                      | the sqs queue to read from when monitoring the migrator **required by the migrator monitor**<sup>\*</sup> | bfd-test-migrator    | no<sup>\*</sup> |

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
            bfd_version: 2.0.0-SNAPSHOT
            db_migrator_zip: "{{ lookup('env','HOME') }}/.m2/repository/gov/cms/bfd/bfd-db-migrator/{{ bfd_version }}/bfd-db-migrator-{{ bfd_version }}.zip"
            db_migrator_db_url: 'jdbc:hsqldb:mem:test'
            db_migrator_db_username: "{{ ssm_db_migrator_db_username }}"
            db_migrator_db_password: "{{ ssm_db_migrator_db_password }}"


Running the Tests
-----------------

This role includes a test framework that tests everything out using Docker, locally.
Those tests can be run, as follows:

The tests are _optimized_ for running in GitHub Actions, and currently run in serial _after_ the java verification steps and generation of a GitHub-stored container image:

    $ ops/ansible/roles/bfd-db-migrator/test/run-tests.sh

Local development is also possible and operators will need to supply a known image tag for `ghcr.io/cmsgov/bfd-apps`. For truly local development, operators might generate this locally from the _root_ of the repository, e.g.

``` sh
mvn -f apps/ --threads 1C -DskipTests -DskipITs --Dmaven.javadoc.skip=true clean verify
docker build apps/ --file apps/Dockerfile -t ghcr.io/cmsgov/bfd-apps:some-image-tag
```

From this, the tests can be run by issuing the following:

``` sh
# running with
# optional `-e` extra variables flag for migrator monitor enablement
# optional `-k` to keep the container running after test completion
# and specifying `some-image-tag` to target the image generated above
ops/ansible/roles/bfd-db-migrator/test/run-tests.sh -e migrator_monitor_enabled=True -k some-image-tag
```
