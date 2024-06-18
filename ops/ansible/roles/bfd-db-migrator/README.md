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

This role is configurable, though it tries to provide reasonable defaults where possible:

| Name                                   | Description                                               | Default              | Required |
|----------------------------------------|-----------------------------------------------------------|----------------------|----------|
| db_auth_type_override                  | override for the database username                        |                      | no       |
| db_max_connections_override            | override for the database max connections figure          |                      | no       |
| db_password_override                   | override for the database password                        |                      | no       |
| db_url_override                        | override for the database url                             |                      | no       |
| db_username_override                   | override for the database username                        |                      | no       |
| new_relic_metrics_license_key_override | override new relice license key                           |                      | no       |
| new_relic_metrics_host_override        | override new relic metrics host                           |                      | no       |
| new_relic_metrics_path_override        | override newrelic path                                    |                      | no       |
| new_relic_metrics_period_override      | overide newrelic metrics period                           |                      | no       |
| env_name_std                           | deployment env, e.g. `prod`, `prod-sbx`, `test`           | unknown-environment  | no       |
| logs                                   | logs directory                                            | `{{ ref_dir }}`      | no       |
| ref_dir                                | primary, on-host directory for migrator-related resources | /opt/bfd-db-migrator | no       |
| tmp                                    | defines the `-Djava.io.tmpdir` for the migrator's jvm     | `{{ ref_dir }}/tmp`  | no       |
| user                                   | user to be created to run migrator service                | bb-migrator          | no       |

See [defaults/main.yml](./defaults/main.yml) for the list of defaulted variables and their default values.

Dependencies
------------

This role does not have any runtime dependencies on other Ansible roles.

Example Playbook
----------------

Here's an example of how to apply this role to the `bfd-db-migrator` host in an Ansible play:

``` yaml
- hosts: bfd-db-migrator
  tasks:
    - name: Install Prerequisites
      vars:
        ansible_python_interpreter: /usr/bin/python
      yum:
        pkg:
          - procps
          - awscli
          - jq
        state: present
      become: true
    - name: Apply Role
      import_role:
        name: bfd-db-migrator
      vars:
        env_name_std: dev
        db_migrator_zip: "{{ lookup('env','HOME') }}/.m2/repository/gov/cms/bfd/bfd-db-migrator/{{ bfd_version }}/bfd-db-migrator-{{ bfd_version }}.zip"
        db_url_override: jdbc:postgresql://db:5432/fhirdb
        db_username_override: bfd
        db_password_override: bfd
```

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
ops/ansible/roles/bfd-db-migrator/test/run-tests.sh -e db_url_override=jdbc:postgresql://db:5432/alt -k some-image-tag
```
