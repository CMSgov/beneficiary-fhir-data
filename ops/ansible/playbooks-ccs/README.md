# __BFD OPS__

This folder currently hosts DevOps related code (Ansible, Packer, Terraform, etc). I suspect this organization can be revisited once the monorepo is in place. Until then, things could live here. 

## ANSIBLE

Project and Directory Structure
- We are using the suggested directory structure provided by the Ansible community: https://docs.ansible.com/ansible/latest/user_guide/playbooks_best_practices.html#directory-layout

### Jenkins Playbooks 
##### To Do
 1. Research ssh pipelining issue, currently turned off across entire playbook. 
 2. Configure Splunk Agent 

#### New Jenkins Installation
__Name: build_jenkins.yml__<br>
__Summary:__ This playbook and associated roles configures a CCS Gold Image with specific prerequsities and then apache, jenkins and BFD specific configuration. 

##### High Level Usage Instructions
 - Requirements: 
   - Local Environment 
     - CCS VPN Connection with proper profile. 
    - AWS CLI authentication setup for the BFD AWS Account with admin privs. 
    - Tools: Packer, Ansible
   - A Gold Image ID provided by the CCS (GDIT)
   - A previously created EFS mount (this is completed with the Terraform `mgmt-stateful` module) 
   - Build the Jenkins AMI from within the `ops/ansible/playbooks-ccs` directory of the ops code by running the following command to build AND configure the Jenkins instance and volume for the first time. 
     - *packer build -var 'source_ami=ami-0f2d8f925de453e46' -var 'subnet_id=subnet-092c2a68bd18b34d1' ../../packer/build_jenkins.json *
   - Note the AMI ID that was created. You'll update the terraform variables at a later stage to deploy this.
   
##### Post Jenkins Build Steps 
Visit Jenkins URL: https://builds.bfd-mgmt.cmscloud.local/jenkins and use credentials located in vault to login and configure the following:
 - Global Tool Chains Config - Configure Docker
     - name: docker
     - location: /var/lib 

- Global Security Config - Enable Slave -> Master Access Control

#### Update Jenkins Installation 

__Name: update_jenkins.yml__<br>
__Summary:__ This playbook and associated roles configures a CCS Gold Image with specific prerequsities and then apache, jenkins and BFD specific configuration. It essential applies all roles from the build_jenkins.yml playbook except for the configuration aspects which are stored on a persistent EFS volume and attached to this instance. 

##### High Level Usage Instructions
 - Requirements: 
   - Local Environment 
     - CCS VPN Connection with proper profile. 
    - AWS CLI authentication setup for the BFD AWS Account with admin privs. 
    - Tools: Packer, Ansible
   - A Gold Image ID provided by the CCS (GDIT)
 - __example command__: from within the ops/ansible/playbooks-ccs directory and replacing the ami and subnet values that meet your deployment needs. 

*packer build -var 'source_ami=ami-12345678' -var 'subnet_id=subnet-0987654321' update_jenkins.json*
