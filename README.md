Blue Button API Sandbox Ansible Repo
------------------------------------

This repository contains the Ansible provisioning, roles, etc. used to setup and manage the Blue Button API's backend systems.

## Development Environment

In order to use and/or modify this repository, a number of tools need to be installed.

### Python

This project requires Python 2.7. It can be installed as follows:

    $ sudo apt-get install python

### virtualenv

This project has some dependencies that have to be installed via `pip` (as opposed to `apt-get`). Accordingly, it's strongly recommended that you make use of a [Python virtual environment](http://docs.python-guide.org/en/latest/dev/virtualenvs/) to manage those dependencies.

If it isn't already installed, install the `virtualenv` package. On Ubuntu, this is best done via:

    $ sudo apt-get install python-virtualenv

Next, create a virtual environment for this project and install the project's dependencies into it:

    $ cd bluebutton-ansible-role-data-pipeline.git
    $ virtualenv -p /usr/bin/python2.7 venv
    $ source venv/bin/activate
    $ pip install -r requirements.txt

The `source` command above will need to be run every time you open a new terminal to work on this project.

Be sure to update the `requirements.txt` file after `pip install`ing a new dependency for this project:

    $ pip freeze > requirements.txt

### Ansible Roles

Run the following command to download and install the roles required by this project into `~/.ansible/roles/`:

    $ ansible-galaxy install -r install_roles.yml

### SSH

These playbooks rely on SSH host aliases, which must be configured in your `~/.ssh/config` file. Here's an example with fake IPs and user names (ask a HealthAPT sysadmin for the correct values):

```
Host bluebutton-healthapt-prod-etl
  HostName 1.2.3.4
  User myusername
  IdentityFile ~/workspaces/cms/healthapt-aws-sshkey.pem

Host bluebutton-healthapt-prod-fhir
  HostName 1.2.3.5
  User myusername
  IdentityFile ~/workspaces/cms/healthapt-aws-sshkey.pem
```

## Ansible Vault Password

The security-sensitive values used in these playbooks (e.g. usernames, passwords, etc.) are encrypted using [Ansible Vault](http://docs.ansible.com/ansible/playbooks_vault.html). In order to view these values or run the plays you will need a copy of the project's `vault.password` file. Please this file in the root of the project, and ensure that it is only readable by your user account. **Never** commit it to source control! (Git is configured to ignore it via [.gitignore](./.gitignore).)

If you don't have this file, you will receive errors like the following when trying to run the playbooks:

    TODO

## Running the Playbooks

The playbooks can be run, as follows:

    $ ansible-playbook backend.yml --inventory-file=hosts-production

## License

This project is in the worldwide [public domain](LICENSE.md). As stated in [CONTRIBUTING](CONTRIBUTING.md):

> This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
>
> All contributions to this project will be released under the CC0 dedication. By submitting a pull request, you are agreeing to comply with this waiver of copyright interest.

