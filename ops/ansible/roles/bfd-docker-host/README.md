Ansible Role for BFD Server
===========================

This Ansible role can be used to install and configure the bfd-docker-host AMI.

Requirements
------------

On the Ansible management system, this role has no dependencies beyond Ansible itself. On the system being managed, it requires RHEL.

To modify and test this role, though, a number of things will need to be installed and configured. See the instructions here for information: [Blue Button Sandbox: Development Environment](https://github.com/HHSIDEAlab/bluebutton-sandbox#development-environment).

Tags
----

This role utilizes [Ansible Tags](https://docs.ansible.com/ansible/latest/user_guide/playbooks_tags.html) so that the same role can be used during different build or deployment stages. For CCS we use pre-ami and post-ami for immutable ec2 instances. If you execute this role without specifying tags Ansible assumes all tags are to be run. 


Role Variables
--------------

This role is not presently configurable, though that is likely to change once the bfd-docker-host AMI is utilized.

See [defaults/main.yml](./defaults/main.yml) for the list of optional/defaulted variables and their default values.

Dependencies
------------

This role does not have any runtime dependencies on other Ansible roles.

Example Playbook
----------------

See the test script in [.travis/test_base.yml](./.travis/test_base.yml) and the test case variables in [.travis/vars/](./.travis/vars/) for examples of how to apply this role in Ansible plays.

Running the Tests
-----------------

This role includes a test framework that tests everything out using Docker, locally.
Those tests can be run, as follows:

    $ ops/ansible/roles/bfd-docker-host/test/run-tests.sh
