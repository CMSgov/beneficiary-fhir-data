Project and Directory Structure
- We are using the suggested directory structure provided by the Ansible community: https://docs.ansible.com/ansible/latest/user_guide/playbooks_best_practices.html#directory-layout

To Do: 
1. Research ssh pipelining issue, currently turned off across entire playbook, slow. 
2. Install Cloudwatch agent and configure. 
3. Setup SSH Access, currently using bfd-jenkins key. Located in team keybase. 
  - option 1: https://aws.amazon.com/blogs/compute/new-using-amazon-ec2-instance-connect-for-ssh-access-to-your-ec2-instances/
4. Figure out why task Configure File Provider Plugin in conf_jenkins fails with: 
script failed with stacktrace: org.codehaus.groovy.control.MultipleCompilationErrorsException: startup failed:\nScript1.groovy: 121: unable to resolve class org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig \n @ line 121, column 3.\n     new org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig(\n     ^\n\nScript1.groovy: 135: unable to resolve class org.jenkinsci.plugins.configfiles.maven.MavenToolchainsConfig \n @ line 135, column 3.\n     new org.jenkinsci.plugins.configfiles.maven.MavenToolchainsConfig
