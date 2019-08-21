# __BFD OPS__

This folder currently hosts DevOps related code (Ansible, Packer, Terraform, etc). I suspect this organization can be revisited once the monorepo is in place. Until then, things could live here. 

## ANSIBLE

Project and Directory Structure
- We are using the suggested directory structure provided by the Ansible community: https://docs.ansible.com/ansible/latest/user_guide/playbooks_best_practices.html#directory-layout

### Jenkins Playbooks 
##### To Do
 1. Research ssh pipelining issue, currently turned off across entire playbook. 
 2. Install Cloudwatch agent and configure.
 3. Configure Splunk Agent 
 4. Setup SSH Access, currently using bfd-jenkins key. Located in team keybase. 
   - option 1: https://aws.amazon.com/blogs/compute/new-using-amazon-ec2-instance-connect-for-ssh-access-to-your-ec2-instances/
 5. Automate via groovy the setup of docker name and location in global tools settings.
 6. Consider moving to Jenkins in Docker. 

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
   - A previously created and formatted EBS volume
     - Create EBS Volume within AWS (console or cli)
       - Suggested configuration
         - Name: Same as used for the variable {{ bfd-jenkins-ebs_name }} value in the playbook. 
         - Type: General Purpose SSD (gp2)
         - Size: >= 1000G 
         - AZ: us-east-1(a-c)
         - Encryption: Yes, use MGMT CMK
         - Device: /dev/sdf 
         - Filesystem: xfs 
   - Build the Jenkins AMI from within the Ansible/ directory of the ops code (i.e. bfd-ops) by running the following command to build AND configure the Jenkins instance and volume for the first time. 
     - *packer build -var 'source_ami=ami-12345678' -var 'subnet_id=subnet-0987654321' update_jenkins.json*
   - Note the AMI ID that was created. You'll update the terraform variables at a later stage to deploy this.
   
##### Post Installation Config 
Visit Jenkins URL: https://builds.bfd-mgmt.cmscloud.local/jenkins and use credentials located in vault to login and configure the following:
 - Global Tool Chains Config - Configure Docker
     - name: docker
     - location: /var/lib 

- Global Security Config - Enable Slave -> Master Access Control

#### Update Jenkins Installation 

__Name: update_jenkins.yml__<br>
__Summary:__ This playbook and associated roles configures a CCS Gold Image with specific prerequsities and then apache, jenkins and BFD specific configuration. It essential applies all roles from the build_jenkins.yml playbook except for the configuration aspects which are stored on a persistent EBS volume and attached to this instance. 

##### High Level Usage Instructions
 - Requirements: 
   - Local Environment 
     - CCS VPN Connection with proper profile. 
    - AWS CLI authentication setup for the BFD AWS Account with admin privs. 
    - Tools: Packer, Ansible
   - A Gold Image ID provided by the CCS (GDIT)
 - __example command__: from within the ansible directory and replacing the ami and subnet values that meet your deployment needs. 

*packer build -var 'source_ami=ami-12345678' -var 'subnet_id=subnet-0987654321' update_jenkins.json*
