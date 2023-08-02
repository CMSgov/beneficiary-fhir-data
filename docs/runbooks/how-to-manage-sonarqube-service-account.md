# How to Manage SonarQube Service Account

- [How to Manage SonarQube Service Account](#how-to-manage-sonarqube-service-account)
  - [Prerequisites](#prerequisites)
  - [Instructions](#instructions)
    - [How to Login and Rotate the SonarQube Access Account Credentials](#how-to-login-and-rotate-the-sonarqube-access-account-credentials)

<!-- ## Glossary -->

<!-- ## FAQ -->

## Prerequisites

- Access to the BFD AWS Account
- IAM permissions to decrypt sensitive SSM parameters
- An installation of the AWS CLI that is configured properly for access to the BFD/CMS AWS account
- An installation of the `ansible` CLI
- An installation of the `terraform` CLI
  - Using a tool like `tfenv` allows for multiple installations of Terraform and automatic version
    management
- Your `EDITOR` environment variable set to a proper editor
  - You can set this variable in your `.bashrc` (if you're using `bash`) or `.zshrc` (if you're
    using `zsh`) like so: `export EDITOR=<your editor executable here>`. Other shells may have a
    different syntax for setting environment variables or a different configuration file
- This repository, `beneficiary-fhir-data`, pulled down locally
- An active connection to the cms.gov VPN

## Instructions 

### How to Login and Rotate the SonarQube Access Account Credentials

> It is recommended that you read the following `README`s for more information on the SSM
> configuration scheme used by BFD before continuing:
>
> - [`mgmt`'s `base_config` `README`](../ops/terraform/env/mgmt/base_config/README.md)
> - [`base` service `README`](../ops/terraform/services/base/README.md)

1. In your terminal, navigate to the root of your local copy of the `beneficiary-fhir-data`
   repository using `cd`
2. In your terminal, relative to the root of this repository, `cd` to the directory associated with
   the `mgmt` Terraform module:

   ```bash
   cd ops/terraform/env/mgmt
   ```

3. Initialize the Terraform state locally:

   ```bash
   terraform init
   ```

4. Once initialized, view the Terraform plan and verify that Terraform is able to load state for all
   of the resources managed by the `mgmt` module and that no changes are necessary:

   ```bash
   terraform plan
   ```

5. Navigate into the `base_config` module's directory:

   ```bash
   cd base_config
   ```

6. Ensure you are authenticated with AWS and are able to run AWS CLI commands

7. View the encrypted yaml `mgmt.eyaml` using the `read-and-decrypt-eyaml.sh` script using the
   commands below. This will decrypt the encrypted `mgmt.eyaml` file and display the contents in your command line output

   ```bash
   chmod +x scripts/read-and-decrypt-eyaml.sh
   scripts/read-and-decrypt-eyaml.sh mgmt
   ```

8. You will see the following keys shown below, `....`
   Each key represents the following:

   1. `service_account_access_id` represents the sonarqube service account username to login in with
   2. `service_account_access_password` represents the sonarqube access password to log in via UI or API
3. `service_account_access_key` represents the sonarqube access key that is rotates every 90 days to manage the servie account and resolve any downstream BFD SonarQube users' credential issues.

   ```yml
   /bfd/mgmt/common/sensitive/service_accounts/sonar/service_account_access_id: "some.id"
   /bfd/mgmt/common/sensitive/service_accounts/sonar/service_account_access_password: "some.password"
   /bfd/mgmt/common/sensitive/service_accounts/sonar/service_account_access_key: "some.key"
   ```

9. Log in to SonarQube by copying and pasting the `service_account_access_id` and `service_account_access_password` from the `read-and-decrypt-eyaml.sh` output and enter them at the [Log in](https://sonarqube.cloud.cms.gov/sessions/new) page

10. View the security keys and generate a new 90 day access key via the [security dashboard](https://sonarqube.cloud.cms.gov/account/security) - follow the [instructions](https://docs.sonarsource.com/sonarqube/latest/user-guide/user-account/generating-and-using-tokens/). Copy the newly generated access key.

11. Open the encrypted yaml `mgmt.eyaml` for editing using the `edit-eyaml.sh` script using the
   commands below. This will decrypt the encrypted `mgmt.eyaml` file and open it in your defined
   `EDITOR`. The script will wait until the file is _closed_ by your editor, at which point it will
   re-encrypt `mgmt.eyaml` with your changes and save it

   ```bash
   chmod +x scripts/edit-eyaml.sh
   scripts/edit-eyaml.sh mgmt
   ```

9. Following the format outlined in step #8, update the `service_account_access_key` value 
10. Close the file. This should immediately update the encrypted `mgmt.eyaml` with your new changes
11. Return to the `mgmt` module:

    ```bash
    cd ..
    ```

12. Plan the changes to the Terraform state and verify that there are only _additions_ to the state
    and that these additions correspond to the new SSM parameters defined in step #9:

    ```bash
    terraform plan
    ```

13. Open a new Pull Request with the changes to all configuration in the associated branch
14. Once approved, the changes to `mgmt` can be applied:

    1. From the root of the repository, `cd` into the `mgmt` module:

       ```bash
       cd ops/terraform/env/mgmt
       ```

    2. Apply the changes to configuration ensuring that there are no unexpected changes:

       ```bash
       terraform apply
       ```
15. In the SonarQube [security dashboard](https://sonarqube.cloud.cms.gov/account/security), go and revoke the expired/expiring access key
