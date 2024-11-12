# __BFD OPS__

This folder currently hosts Ansible plays and tasks. 

## ANSIBLE

Project and Directory Structure
- We are using the suggested directory structure provided by the Ansible community: https://docs.ansible.com/ansible/latest/user_guide/playbooks_best_practices.html#directory-layout

## Jenkins Playbooks 

### New Jenkins Installation
__Name: build_jenkins.yml__<br>
__Summary:__ This playbook and associated roles configures a CCS Gold Image with specific prerequsities and then apache, jenkins and BFD specific configuration. 

#### High Level Usage Instructions
 - Requirements: 
   - Local Environment 
     - CCS VPN Connection with proper profile. 
    - AWS CLI authentication setup for the BFD AWS Account with admin privs. 
    - Tools: Packer, Ansible
   - A Gold Image ID provided by the CCS (GDIT)
   - A previously created EFS mount (this is completed with the Terraform `mgmt-stateful` module) 
   - Subnet ID - changes with mgmt (us-east-1a) or mgmt-test (us-east-1c)
   __STOP the current jenkins service on the live instance.__
   - Build the Jenkins AMI from within the `ops/ansible/playbooks-ccs` directory of the ops code by running the following command to build AND configure the Jenkins instance and volume for the first time. 
     - `packer build -var 'source_ami=ami-0f2d8f925de453e46' -var 'subnet_id=subnet-092c2a68bd18b34d1' - var 'env=mgmt' ../../packer/build_jenkins.json`
   - Note the AMI ID that was created. You'll update the terraform variables at a later stage to deploy this.

### Update Jenkins Installation 

__Name: update_jenkins.yml__<br>
__Summary:__ This playbook and associated roles configures a CCS Gold Image with specific prerequsities and then apache, jenkins and BFD specific configuration. It essential applies all roles from the build_jenkins.yml playbook except for the configuration aspects which are stored on a persistent EFS volume and attached to this instance. 

#### High Level Usage Instructions
 - Requirements: 
   - Local Environment 
     - CCS VPN Connection with proper profile. 
    - AWS CLI authentication setup for the BFD AWS Account with admin privs. 
    - Tools: Packer, Ansible
   - A Gold Image ID provided by the CCS (GDIT)
   - Subnet ID changes with mgmt (us-east-1a) or mgmt-test (us-east-1c)
   __STOP the current jenkins service on the live instance.__
 - __example command__: from within the `ops/ansible/playbooks-ccs` directory and replacing the ami and subnet values that meet your deployment needs. 

- `packer build -var 'source_ami=ami-12345678' -var 'subnet_id=subnet-0987654321' - var 'env=mgmt' update_jenkins.json`

### Jenkins Deployment
- Update the terraform vars in Keybase (/infrastructure/secrets/terraform-vars) and supply the new AMI form the previous build or update packer job. 
- Move into the `ops/terraform/env/mgmt, mgmt-test/stateless`
- Run the following command: 
- `terraform apply -var-file=/keybase/team/oeda_bfs/infrastructure/secrets/terraform-vars/mgmt-test/jenkins/terraform.tfvars`
- You will see a new ASG and launch template being created. 


