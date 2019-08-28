Blue Button API Sandbox Ansible Repo
------------------------------------

This repository contains the Ansible provisioning, roles, etc. used to setup and manage the Blue Button API's backend systems.

## Development Environment

In order to use and/or modify this repository, a number of tools need to be installed.

### Python

This project requires Python 2.7. It can be installed as follows on Ubuntu:

    $ sudo apt-get install python

Or on Cygwin:

    $ apt-cyg install python

### virtualenv

This project has some dependencies that have to be installed via `pip` (as opposed to `apt-get`). Accordingly, it's strongly recommended that you make use of a [Python virtual environment](http://docs.python-guide.org/en/latest/dev/virtualenvs/) to manage those dependencies.

If it isn't already installed, install the `virtualenv` package and the development tools that will be needed to build the Python packages we'll be grabbing. On Ubuntu, this is best done via:

    $ sudo apt-get install python-virtualenv
    $ sudo apt-get install build-essentials

Or on Cygwin:

    $ apt-cyg install python-setuptools
    $ python -m ensurepip
    $ pip install virtualenv
    $ apt-cyg install python-devel libpq-devel gcc-g++

Next, create a virtual environment for this project and install the project's dependencies into it:

    $ cd bluebutton-ansible-playbooks-data.git
    $ virtualenv -p /usr/bin/python2.7 venv
    $ source venv/bin/activate
    $ pip install --upgrade setuptools
    $ pip install --requirement requirements.txt

The `source` command above will need to be run every time you open a new terminal to work on this project.

Be sure to update the `requirements.frozen.txt` file after `pip install`ing a new dependency for this project:

    $ pip freeze > requirements.frozen.txt

### Cygwin SSL: GDIT Employees

Many organizations use an intercepting HTTP/S proxy to inspect all traffic over their networks. GDIT is one such organization, and so if you are attempting to use Cygwin on such a system, you will first need to do the following:

1. Export the proxy CA certificate using Chrome:
    a. Open Chrome.
    b. Browse to a site that gets intercepted by the proxy. For example: <https://galaxy.ansible.com/>
    c. Click the SSL lock icon in the address bar and select **Certificate**.
    d. Switch to the **Certification Path** tab
    e. Export the "HQ100" certificate:
        1. Select the root entry (i.e. "HQ100-ROOTCA") and click **View Certificate**.
        2. Switch to the **Details** tab and click **Copy to File...**.
        3. In the **Certificate Export Wizard**...
            a. Click **Next** to proceed to the **Export File Format** screen.
            b. Select **Base-64 encoded X.509** and click **Next** to proceed to the **File to Export** screen.
            c. Save the file as `Downloads\gdit-mitm-ca-certificate-hq100.cer` and click **Next**.
            d. Click **Finish** and then click **OK** however many times to get back to the **Certification Path** dialog.
    e. Export the "HQ200" certificate:
        1. Select the second entry (i.e. "n-HQ200-CAISS01") and click **View Certificate**.
        2. Switch to the **Details** tab and click **Copy to File...**.
        3. In the **Certificate Export Wizard**...
            a. Click **Next** to proceed to the **Export File Format** screen.
            b. Select **Base-64 encoded X.509** and click **Next** to proceed to the **File to Export** screen.
            c. Save the file as `Downloads\gdit-mitm-ca-certificate-hq200.cer` and click **Next**.
            d. Click **Finish** and then click **OK** however many times to close all those dialogs.
2. Import the proxy CA certificate into Cygwin:
    a. Open a Cygwin terminal.
    b. Run the following:
        
        ```
        $ cp /cygdrive/c/Users/<your-username>/Downloads/gdit-mitm-ca-certificate-*.cer /etc/pki/ca-trust/source/anchors/
        $ update-ca-trust
        ```
        
That should do it.

### Ansible Roles

Run the following command to download and install the roles required by this project into `~/.ansible/roles/`:

    $ ansible-galaxy remove karlmdavis.bluebutton_data_pipeline \
        && ansible-galaxy remove karlmdavis.bluebutton_data_server \
        && ansible-galaxy install -r install_roles.yml

### Ansible Vault Password

The security-sensitive values used in these playbooks (e.g. usernames, passwords, etc.) are encrypted using [Ansible Vault](http://docs.ansible.com/ansible/playbooks_vault.html). In order to view these values or run the plays you will need a copy of the project's `vault.password` file. Please this file in the root of the project, and ensure that it is only readable by your user account. **Never** commit it to source control! (Git is configured to ignore it via [.gitignore](./.gitignore).)

### SSH

These playbooks rely on SSH host aliases, which must be configured in your `~/.ssh/config` file. See how this file is created for Jenkins in the `roles/builds_jenkins/templates/ssh_config.j2` template. You'll need to speak with HealthAPT system admins to get the account, SSH access, and keys that will be needed for this to work.

TODO: fix above paragraph to account for running play

To run that play locally on a Linux system:

    $ ./ansible-playbook-wrapper bootstrap.yml --extra-vars "proxy_required=false ssh_config_dest=$HOME/.ssh/config ssh_config_uid=$(id --user) ssh_config_gid=$(id --group)"

On Cygwin:

    $ ./ansible-playbook-wrapper bootstrap.yml --extra-vars "proxy_required=false ssh_config_dest=$HOME/.ssh/config ssh_config_uid=$(id --user) ssh_config_gid=$(id --group) root_uid=$(id --user) root_gid=$(id --group)"

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

    $ ./aws-mfa-refresh.sh --source-profile=bluebutton_backend --mfa-serial-number arn:aws:iam::11111111:mfa/myuser

## Running the Playbooks

The playbooks can be run, as follows:

    $ ./ansible-playbook-wrapper backend.yml --limit=bluebutton-healthapt-lss-builds:env_test --extra-vars "data_pipeline_version=0.1.0-SNAPSHOT data_server_version=1.0.0-SNAPSHOT"

The `extra-vars` in that command may need to be adjusted:

* `data_pipeline_version`: The version of the `gov.cms.bfd:bfd-pipeline-app` artifact to deploy.
* `data_server_version`: The version of the `gov.cms.bfd:bfd-server-war` artifact to deploy.

## License

This project is in the worldwide [public domain](LICENSE.md). As stated in [CONTRIBUTING](CONTRIBUTING.md):

> This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
>
> All contributions to this project will be released under the CC0 dedication. By submitting a pull request, you are agreeing to comply with this waiver of copyright interest.
