[![Build Status](https://travis-ci.org/karlmdavis/ansible-role-jenkins2.svg?branch=master)](https://travis-ci.org/karlmdavis/ansible-role-jenkins2)

Ansible Role for Jenkins 2+
===========================

This [Ansible](https://www.ansible.com/) role can be used to install and manage [Jenkins 2](https://jenkins.io/2.0/).

Requirements
------------

This role requires Ansible 2.4 or later, with either Ansible pipelining available or `setfacl` available on the system being managed (per [Becoming an Unprivileged User](http://docs.ansible.com/ansible/latest/become.html#becoming-an-unprivileged-user)).

The role currently supports Ubuntu 14.04 (Trusty) and Ubuntu 16.04 (Xenial), though contributtions for additional platform support are welcome!

Role Variables
--------------

This role supports the following variables, listed here with their default values from [defaults/main.yml](defaults/main.yml):

* `jenkins_release_line`: `'weekly'`
    * When set to `long_term_support`, the role will install the LTS releases of Jenkins.
    * When set to `weekly`, the role will install the weekly releases of Jenkins.
* `jenkins_release_update`: `true`
    * If `true`, the Jenkins package (YUM, APT, etc.) will be upgraded to the latest version when this role is run.
* `jenkins_home`: `/var/lib/jenkins`
    * The directory that (most of) Jenkins data will be stored.
    * Due to limitations of the Jenkins installer, the `jenkins` service account will still use the default as its home directory. This should really only come into play for SSH keys.
* `jenkins_port`: `8080`
    * The port that Jenkins will run on, for HTTP requests.
    * On most systems, this value will need to be over 1024, as Jenkins is not run as `root`.
* `jenkins_context_path`: `''`
    * The context path that Jenkins will be hosted at, e.g. `/foo` in `http://localhost:8080/foo`. Leave as `''` to host at root path.
* `jenkins_url_external`: `''`
    * The external URL that users will use to access Jenkins. Gets set in the Jenkins config and used in emails, webhooks, etc.
    * If this is left empty/None, the configuration will not be set and Jenkins will try to auto-discover this (which won't work correctly if it's proxied).
* `jenkins_admin_username`: (undefined)
    * If one of `jenkins_admin_username` and `jenkins_admin_password` are defined, both must be.
    * Override this variable to specify the Jenkins administrator credentials that should be used for each possible security realm.
    * If left undefined, the role will attempt to use anonymous authentication.
    * Note that the role will automatically detect if Jenkins is set to allow anonymous authentication (as is the case right after install) and handle it properly.
* `jenkins_admin_password`: (undefined)
    * If one of `jenkins_admin_username` and `jenkins_admin_password` are defined, both must be.
    * Override this variable to specify the Jenkins administrator credentials that should be used for each possible security realm.
* `jenkins_session_timeout`: `30`
    * The number of minutes before Jenkins sessions timeout, i.e. how long logins are valid for.
	* Defaults to 30 minutes.
	* Can be set to `0` to never timeout.
* `jenkins_plugins_extra`: `[]`
    * Override this variable to install additional Jenkins plugins.
    * These would be in addition to the plugins recommended by Jenkins 2's new setup wizard, which are installed automatically by this role (see `jenkins_plugins_recommended` in [defaults/main.yml](defaults/main.yml)).
* `jenkins_plugins_timeout`: `60`
    * The amount of time (in seconds) before a plugin install/update will fail. This value is passed to the timeout parameter in `jenkins_plugin` module. (See here for details: <http://docs.ansible.com/ansible/latest/jenkins_plugin_module.html#options>.)
* `jenkins_plugins_update`: `true`
    * If `true`, the Jenkins plugins will be updated when this role is run. (Note that missing plugins will always be installed.)
* `jenkins_java_args_extra`: `''`
    * Additional options that will be added to `JAVA_ARGS` for the Jenkins process, such as the JVM memory settings, e.g. `-Xmx4g`.
* `jenkins_http_proxy_server`, `jenkins_http_proxy_port`, `jenkins_http_proxy_no_proxy_hosts`: (all undefined)
    * These server the same function as the JVM's `http.proxyHost`, `http.proxyPort`, and `http.nonProxyHosts` system properties, except that the settings will be used for both HTTP and HTTPS requests.
    * Specifically, these settings will be used to configure:
        * The Jenkins JVM's `http.proxyHost`, `http.proxyPort`, `https.proxyHost`, `https.proxyPort`, and `http.nonProxyHosts` system properties, as documented on [Java Networking and Proxies](https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html).
        * The Jenkins-specific proxy settings (which some plugins, such as the [GitHub plugin](https://wiki.jenkins.io/display/JENKINS/Github+Plugin), require), as documented on [JenkinsBehindProxy](https://wiki.jenkins.io/display/JENKINS/JenkinsBehindProxy).
        * The value of `jenkins_http_proxy_no_proxy_hosts` should be a list, e.g. `['localhost', 'example.com']`.

Dependencies
------------

This role does not have direct dependencies on other Ansible roles. However, it does require that a Java JRE be available on the system path.

Example Playbook
----------------

This role can be installed, as follows:

    $ ansible-galaxy install karlmdavis.jenkins2

This role can be applied, as follows:

```yaml
- hosts: some_box
  tasks:
    - import_role:
        name: karlmdavis.ansible-jenkins2
      vars:
        jenkins_plugins_extra:
          - github-oauth
```

## Running Groovy Scripts to Configure Jenkins

After installing Jenkins, Groovy scripts can be run via Ansible to further customize Jenkins.

For example, here's how to install Jenkins and then configure Jenkins to use its `HudsonPrivateSecurityRealm`, for local Jenkins accounts:

```yaml
- hosts: some_box
  tasks:

    - import_role:
        name: karlmdavis.ansible-jenkins2
      vars:
        # Won't be required on first run, but will be on prior runs (after
        # security has been enabled, per below).
        jenkins_admin_username: test
        jenkins_admin_password: supersecret

    # Ensure that Jenkins has restarted, if it needs to.
    - meta: flush_handlers

    # Configure security to use Jenkins-local accounts.
    - name: Configure Security
      jenkins_script:
        url: "{{ jenkins_url_local }}"
        user: "{{ jenkins_dynamic_admin_username | default(omit) }}"
        password: "{{ jenkins_dynamic_admin_password | default(omit) }}"
        script: |
          // These are the basic imports that Jenkin's interactive script console
          // automatically includes.
          import jenkins.*;
          import jenkins.model.*;
          import hudson.*;
          import hudson.model.*;

          // Configure the security realm, which handles authentication.
          def securityRealm = new hudson.security.HudsonPrivateSecurityRealm(false)
          if(!securityRealm.equals(Jenkins.instance.getSecurityRealm())) {
            Jenkins.instance.setSecurityRealm(securityRealm)

            // Create a user to login with. Ensure that user is bound to the
            // system-local `jenkins` user's SSH key, to ensure that this
            // account can be used with Jenkins' CLI.
            def testUser = securityRealm.createAccount("test", "supersecret")
            testUser.addProperty(new hudson.tasks.Mailer.UserProperty("foo@example.com"));
            testUser.save()

            Jenkins.instance.save()
            println "Changed authentication."
          }

          // Configure the authorization strategy, which specifies who can do
          // what.
          def authorizationStrategy = new hudson.security.FullControlOnceLoggedInAuthorizationStrategy()
          if(!authorizationStrategy.equals(Jenkins.instance.getAuthorizationStrategy())) {
            authorizationStrategy.setAllowAnonymousRead(false)
            Jenkins.instance.setAuthorizationStrategy(authorizationStrategy)
            Jenkins.instance.save()
            println "Changed authorization."
          }
      register: shell_jenkins_security
      changed_when: "(shell_jenkins_security | success) and 'Changed' not in shell_jenkins_security.stdout"
```

Alternatively, the Groovy scripts can be stored as separate files and pulled in using a `lookup(...)`, as below:

```yaml
- hosts: some_box
  tasks:

    - import_role:
        name: karlmdavis.ansible-jenkins2
      vars:
        # Won't be required on first run, but will be on prior runs (after
        # security has been enabled, per below).
        jenkins_admin_username: test
        jenkins_admin_password: supersecret

    # Ensure that Jenkins has restarted, if it needs to.
    - meta: flush_handlers

    # Configure security to use Jenkins-local accounts.
    - name: Configure Security
      jenkins_script:
        url: "{{ jenkins_url_local }}"
        user: "{{ jenkins_dynamic_admin_username | default(omit) }}"
        password: "{{ jenkins_dynamic_admin_password | default(omit) }}"
        script: "{{ lookup('template', 'templates/jenkins_security.groovy.j2') }}"
```

License
-------

This project is in the worldwide [public domain](LICENSE.md). As stated in [CONTRIBUTING](CONTRIBUTING.md):

> This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
>
> All contributions to this project will be released under the CC0 dedication. By submitting a pull request, you are agreeing to comply with this waiver of copyright interest.

Author Information
------------------

This plugin was authored by Karl M. Davis (https://justdavis.com/karl/).

