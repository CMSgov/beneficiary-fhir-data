Role Name
=========

This role has a few different ways of pulling configurations, secrets, key/value pairs, etc... from the AWS SSM Parameter Store. It is a proof of concept, not a final solution.

Requirements
------------

This role requires that you have the AWS profile for connecting to the AWS SSM Parameter store setup on the machine running this role.  This role requires that you have placed data into the AWS SSM Parameters store.

Role Variables
--------------

This role requires that you specify the following variables:
* project: (name or shorthand for the project ex: bfd)
* stage: (ex: prod/dev/test/etc...)
* date: (YYYYMMDD ex: 20190905)
* path: "/{{ project }}/{{ stage }}/{{ date }}/" (Variable built from the others)

Dependencies
------------

N/A

Example Playbook
----------------

Including an example of how to use your role (for instance, with variables passed in as parameters) is always nice for users too:

    - hosts: all
      roles:
         - { role: bfd-get-parameters }

License
-------

BSD

Author Information
------------------

cthulhuplus