Project and Directory Structure
- We are using the suggested directory structure provided by the Ansible community: https://docs.ansible.com/ansible/latest/user_guide/playbooks_best_practices.html#directory-layout

To Do: 
1. Research ssh pipelining issue, currently turned off across entire playbook, slow. 
2. Install Cloudwatch agent and configure. 
3. Setup SSH Access, currently using bfd-jenkins key. Located in team keybase. 
  - option 1: https://aws.amazon.com/blogs/compute/new-using-amazon-ec2-instance-connect-for-ssh-access-to-your-ec2-instances/
4. Figure out why task Configure File Provider Plugin in conf_jenkins fails with: 
script failed with stacktrace: org.codehaus.groovy.control.MultipleCompilationErrorsException: startup failed:\nScript1.groovy: 121: unable to resolve class org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig \n @ line 121, column 3.\n     new org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig(\n     ^\n\nScript1.groovy: 135: unable to resolve class org.jenkinsci.plugins.configfiles.maven.MavenToolchainsConfig \n @ line 135, column 3.\n     new org.jenkinsci.plugins.configfiles.maven.MavenToolchainsConfig
  

*How to use the Jenkins playbooks (build_jenkins.yml and update_jenkins.yml)*
Build Jenkins (build_jenkins.yml) - First Time Deployment
 - Requirements: 
   - Local Environment 
    - AWS CLI authentication setup for the BFD AWS Account with admin privs. 
    - packer 
    - Ansible
   - A Gold Image ID provided by the CCS (GDIT)
   - A previously created and formatted EBS volume (Skip step if already created)
     - Create EBS Volume within AWS (console or cli)
       - Suggested configuration
         - Name: Same as used for the variable {{ bfd-jenkins-ebs_name }} value in the playbook. 
         - Type: General Purpose SSD (gp2)
         - Size: >= 1000G 
         - AZ: us-east-1(a-c)
         - Encryption: Yes, CMK = bfd-mgmt-cmk
         - Device: /dev/sdf 
         - Filesystem: xfs 
   - Build the Jenkins AMI (first time) From within the Ansible/ directory of the ops code (i.e. bfd-ops) run the following command to build AND configure the Jenkins instance and volume for the first time. 
     - packer build -var 'source_ami=<insert_gold_ami_id>' -var 'subnet_id=subnet-036f17fe18e59a0d9' build_jenkins.json
   - Note the AMI ID that was created and Update the terraform vars for Jenkins deployment with the new AMI. 
   
Configure Jenkins (First Time) - Manage Jenkins - To login use username and password from Vault host_vars/builds.bfd-mgmt.cmscloud.local
 - Global Tool Chains Config - Configure Docker
   name: docker
   location: /var/lib 
  

## Updating 2021 update instructions

Please use `scripts/jenkins.sh` to update existing jenkins deployments until further notice.

## To Upgrade

```bash
# move into the ops scripts directory
cd ~/projects/beneficiary-fhir-data/ops/ccs-ops-misc/scripts

# copy and source the jenkins.env from kb (get with someone for the path)
cp /path/to/jenkins.env .env
source .env

# build the ami
./jenkins.sh --build-ami

# take note of the ami from the output and deploy with
./jenkins.sh --jenkins-ami 'ami-idfromabovestep' deploy
```
