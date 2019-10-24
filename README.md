MyMedicare.gov BlueButton Parent POM
====================================

This repo just contains a Maven parent POM and some other build/dev resources that are shared by the CMS/MyMedicare.gov Java projects.

## Development Environment

Going to work on this project or one of the other Blue Button Java projects? Great! You can follow the instructions in [Development Environment Setup](./dev/devenv-readme.md) to get going.

## Releases

This project uses Maven's [maven-release-plugin](http://maven.apache.org/maven-release/maven-release-plugin/) for releases, and must be manually released by a developer with permissions to [its GitHub repo](https://github.com/HHSIDEAlab/bluebutton-parent-pom) and to [OSSRH](http://central.sonatype.org/pages/ossrh-guide.html) (which is used to ensure its releases land in Maven Central).

Run the following commands to perform a release:

    $ mvn release:prepare release:perform
    $ git push --all && git push --tags

## Parameter Store

Parameter naming

The parameter names are made from the following:
* Project Name
* Environment (dev/prod/sandbox/etc)
* Date that the value was added in YYYYMMDD format
* Parameter key
Example: /weapon-x/dev/20190130/subject

Log into Jenkins and upload your AWS SSM Parameter Store json file to the Managed Files section of Manage Jenkins.

The format should look something like this:
```
{
    "fus": [
    {
        "Name": "/weapon-x/dev/20190130/fus",
        "Description": "",
        "Value": "fus",
        "Type": "SecureString",
        "Overwrite": true,
        "AllowedPattern": "",
        "Tier": "Standard"
    }],

    "ro": [
    {
        "Name": "/weapon-x/dev/20190130/ro",
        "Description": "",
        "Value": "ro",
        "Type": "SecureString",
        "Overwrite": true,
        "AllowedPattern": "",
        "Tier": "Standard"
    }],

    "dah": [
    {
        "Name": "/weapon-x/dev/20190130/dah",
        "Description": "",
        "Value": "dah",
        "Type": "SecureString",
        "Overwrite": true,
        "AllowedPattern": "",
        "Tier": "Standard"
    }]
}
```

Note the File ID as it will be required to push the parameters to the Parameter Store.

Find the job to push parameters to the AWS parameters store, paste the File ID into the job, and then run it.

Pulling Parameters requires either a BASH or Ansible script.
BASH Example:
```
/usr/local/bin/aws ssm get-parameters-by-path --with-decryption  --path "$1"
```
Where $1 is the path where the parameters live. Further parsing of the output of the above command will likely be necessary. Something like this may work:
```
eval $(/usr/local/bin/aws ssm get-parameters-by-path --with-decryption  --path "$1" | jq -r '.Parameters| .[] | "export " + .Name + "=\"" + .Value + "\""  ' | sed "s|/$1||g")
```

Ansible Example:
```
- name: Pull Parameters from AWS SSM Parameter Store
  hosts: all
  remote_user: ec2-user
  gather_facts: no
  vars:
    ansible_ssh_pipelining: no
    env: "test"
### Best if put in var file
#    project: bbrooks
#    stage: dev
#    date: 20190905
#    path: "/{{ project }}/{{ stage }}/{{ date }}/"
  vars_files:
    - "{{ variable_file }}"

  roles:
    - bfd.get-parameters
```
```
  - name: Set Facts
    set_fact:
      "{{ item.key|upper }}={{ item.value }}"
    loop: "{{ lookup('aws_ssm', '{{ path }}', region='us-east-2', shortnames=true, bypath=true, recursive=true, decrypt=True)|dict2items }}"
```
This will lookup the AWS Parameter Store path and put those values into the environment for the duration of the Ansible task.

## License

This project is in the worldwide [public domain](LICENSE.md). As stated in [CONTRIBUTING](CONTRIBUTING.md):

> This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
>
> All contributions to this project will be released under the CC0 dedication. By submitting a pull request, you are agreeing to comply with this waiver of copyright interest.
