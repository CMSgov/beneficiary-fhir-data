BFD Ansible Repo (for the HealthAPT environment)
------------------------------------------------

This repository contains the Ansible provisioning, roles, etc. used to setup and manage the BFD systems in the HealthAPT AWS environment.

## Development Environment

In order to use and/or modify this repository, a number of tools need to be installed.

### Python

This project requires Python 3.7. It can be installed as follows on MacOS (via Homebrew):

    $ brew install python

### pipenv

This project has some Python dependencies that have to be installed. We use the `pipenv` tool to install and manage those dependencies in a segregated virtual environment (so that this project's dependencies don't conflict with dependencies from other projects).

If it isn't already installed, install the `pipenv` package and the development tools that will be needed to build the Python packages we'll be grabbing. On MacOs, this is best done via:

    $ brew install pipenv

Next, initialize `pipenv` for this project:

    $ cd ops/ansible/playbooks-healthapt
    $ pipenv install --three

When you want to run something that requires the Python dependencies, prefix the command with `pipenv run`.

You can add, update, etc. the project's Python dependencies by editing the `Pipfile` in this directory and using `pipenv`, as documented here: <https://docs.pipenv.org/en/latest/>.

### Ansible Roles

Run the following command to download and install the roles required by this project into `~/.ansible/roles/`:

    $ pipenv run ansible-galaxy install --force --role-file=install_roles.yml

### Ansible Vault Password

The security-sensitive values used in these playbooks (e.g. usernames, passwords, etc.) are encrypted using [Ansible Vault](http://docs.ansible.com/ansible/playbooks_vault.html). In order to view these values or run the plays you will need a copy of the project's `vault.password` file. Please this file in the root of the project, and ensure that it is only readable by your user account. **Never** commit it to source control! (Git is configured to ignore it via [.gitignore](./.gitignore).)

### SSH

These playbooks rely on SSH host aliases, which must be configured in your `~/.ssh/config` file. See how this file is created for Jenkins in the `roles/builds_jenkins/templates/ssh_config.j2` template. You'll need to speak with HealthAPT system admins to get the account, SSH access, and keys that will be needed for this to work.

TODO: fix above paragraph to account for running play

To run that play locally on a Linux system:

    $ pipenv run ./ansible-playbook-wrapper bootstrap.yml --extra-vars "proxy_required=false ssh_config_dest=$HOME/.ssh/config ssh_config_uid=$(id --user) ssh_config_gid=$(id --group)"

If that play gets to, and then fails on, the "Configure Systems for Deploys - Fetch Jenkins SSH Public Key" then you're all set; that's as much of it as you need to succeed locally.

### AWS API Security Tokens

The AWS accounts used for the systems being managed are configured to require multi-factor authentication. You'll first need to ensure that you have set this up such that you can login to the AWS account's web console (speak with HealthAPT system administrators for the URL and assistance with this). Once you can login to the web console, ensure that you've created an API key for your user and configured it as a profile in `~/.aws/credentials`, e.g.:

```
# These AWS keys are for my user in the Blue Button 2.0 Backend AWS account.
[bluebutton_backend]
aws_access_key_id = foo
aws_secret_access_key = bar
```

Due to the MFA requirements, though, that access key won't be useable by itself. Instead, you'll have to configure an additional profile in `~/.aws/credentials` with a valid MFA/session token, e.g.:

```
[bluebutton_backend_mfa]
aws_secret_access_key = fizz
aws_session_token = buzz
aws_access_key_id = whoozit
```

You can Google around for how to generate this. Or, much more simply, you can use the provided `aws-mfa-refresh.sh` script to automatically generate/update it as needed (be sure to use the correct `mfa-serial-number` value, as listed in IAM for your user):

    $ pipenv run ./aws-mfa-refresh.sh --source-profile=bluebutton_backend --mfa-serial-number arn:aws:iam::11111111:mfa/myuser

## Running the Playbooks

The playbooks can be run, as follows:

    $ cat << EOF > extra_vars.json
    {
      "limit_envs": [
        "ts"
      ],
      "data_pipeline_jar": "../../../apps/bfd-pipeline/bfd-pipeline-app/target/bfd-pipeline-app-1.0.0-SNAPSHOT-capsule-fat.jar",
      "data_server_container": "../../../apps/bfd-server/bfd-server-war/target/bfd-server/wildfly-dist-8.1.0.Final.tar.gz",
      "data_server_container_name": "wildfly-8.1.0.Final",
      "data_server_war": "../../../apps/bfd-server/bfd-server-war/target/bfd-server-war-1.0.0-SNAPSHOT.war"
    }
    EOF
    $ pipenv run ./ansible-playbook-wrapper backend.yml --limit=localhost:env_test --extra-vars "@extra_vars.json"

The following in those commands may need to be adjusted:

* `limit_envs`: The short names/IDs of the environments to deploy to, as listed in `group_vars/all/main.yml`.
* `limit`: The longer names/IDs of the environments to deploy to, as listed in `hosts`.
