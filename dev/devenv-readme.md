Development Environment Setup
=============================

Thinking of contributing to this project or some of the other Java-based Blue Button projects? Great! This document provides some help on getting a development environment setup for that work.

## Getting Started

### Windows: Cygwin

TODO: document install of cygwin, apt-cyg, etc.

Things seem to go badly sideways in Cygwin if it tries to use ACL permissions. To avoid that, adjust the `/cygdrive` mount in `/etc/fstab` to include the `acl` and `exec` options, e.g.:

    none /cygdrive cygdrive binary,posix=0,user,noacl,exec 0 0

After changing that, be sure to completely close and restart all Cygwin processes. (Here's the reference that helped me solve this problem: <https://stackoverflow.com/questions/5828037/cygwin-sets-file-permission-to-000/7082542#7082542>.)

Install the dependencies required by the `devenv-install.py` script:

    $ apt-cyg install python3 python3-lxml python3-setuptools unzip cabextract

__Note:__ If apt-cyg is having problems connecting to your cygwin mirror this may be due to an [incorrect HOSTTYPE setting](#hosttype).

Install and configure Git:

    $ apt-cyg install git
    $ git config --global user.email "myemail@example.com"
    $ git config --global user.name "My Full Name"

## Automation FTW!

First off, if you're on one of the following platforms, we provide the [devenv-install.py](./devenv-install.py) script, which automates most of the work for you. It supports:

* Windows, with [Cygwin](https://www.cygwin.com/)

It can be run as follows from a Bash prompt:

    $ ./devenv-install.py

What does it do for you? Great question! It will create a `~/workspaces/tools/` directory and then download and install (as a user) the following into there for you:

* An [Oracle Java 8 JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Apache Maven](https://maven.apache.org/)
* [Latest Eclipse Version](https://www.eclipse.org/downloads/)
    * As of 10/20/2017, Eclipse Mars and Oxygen are known to work with our projects.
* Eclipse __m2e-apt__ Plugin
    * The m2e-apt plugin allows Maven projects in Eclipse to easily leverage [the JDK's Annotation Processing framework](http://docs.oracle.com/javase/7/docs/technotes/guides/apt/).
    * Manual installation of the m2e-apt plugin can be achieved by using the [Drag To Install](http://marketplace.eclipse.org/content/m2e-apt) option.  To install from within Eclipse IDE open **Help > Eclipse Marketplace...** and search for **m2e-apt** using the **Eclipse Marketplace** find dialog box.  Click the **Install** button for the plugin and restart Eclipse when prompted. 
    * [Usage Instructions for m2e-apt](https://developer.jboss.org/en/tools/blog/2012/05/20/annotation-processing-support-in-m2e-or-m2e-apt-100-is-out?_sscc=t)

If you're not using one of those supported platforms, or would prefer to setup things manually, you'll want to download and install the items listed above yourself.

## Cygwin Configuration

### Associating .sh files with Cygwin

Shell scripts that end in .sh should be associated with the Cygwin shell you have chosen to use.  Follow these steps to change the association for your installation:

1. Open an elevated Command Prompt by finding the **Command Prompt** application in the Start menu, right-clicking it, and selecting **Run as administrator**.
1. Create an empty/unmapped file type for `.sh` script files:
    
    ```
    > assoc .sh=unix_shell_script
    ```
    
1. Map the file type created in the previous step to a launch command:
    
    ```
    > ftype unix_shell_script="C:\cygwin64\bin\bash.exe" %1 %*
    ```
    

<a name="hosttype"></a>
### HOSTTYPE Configuration

If apt-cyg is having problems connecting to a Cygwin mirror your HOSTTYPE configuration may be the problem.  Verify your HOSTTYPE does not have additional decoration(i.e. x86_64-cygwin) and only contains the system architecture(i.e. x86_64) that you are attempting to install on.

```
# Example incorrect setting
$ echo $HOSTTYPE
x86_64-cygwin

# Example correct setting
$ echo $HOSTTYPE
x86_64

`HOSTTYPE` can be overridden on the command line or set in your shell configuration (i.e. `~/.bashrc`, `~/.cshrc`, etc).  To override `HOSTTYPE` on the command line use the following construct:

```
# for csh/tcsh
> setenv HOSTTYPE x86_64 && apt-cyg <command>

# for bash
$ HOSTTYPE=x86_64 apt-cyg <command>
```

## AWS Configuration

### AWS Credentials

Many of the automated tests associated with the Blue Button framework use AWS resources.  Before running a build using Maven or importing projects into your Eclipse IDE, which will run a build automatically, please ensure the appropriate accounts and credentials are configured within your environment.  **This is necessary to prevent incurring unwanted charges on the wrong AWS account**.

Below are links to detailed instructions on configuring your AWS credentials for your environment:

  * [Configuration and Credential Files](http://docs.aws.amazon.com/cli/latest/userguide/cli-config-files.html)

## Github Configuration

### Github SSH

You will need to configure an SSH credential in order to clone the Blue Button repositories.  Instructions are thoroughly documented on Github but for convenience here are the relevant links:

  * [Connecting to Github with SSH](https://help.github.com/articles/connecting-to-github-with-ssh/)
  * [Generating a new SSH key and adding it to the ssh-agent](https://help.github.com/articles/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent/)
  * [Adding a new SSH key to your GitHub account](https://help.github.com/articles/adding-a-new-ssh-key-to-your-github-account/)
  * [Testing your SSH connection](https://help.github.com/articles/testing-your-ssh-connection/)

## Maven Configuration

### Maven `toolchains.xml`

Right now, the script does not create the Maven `toolchains.xml` file (though it can and should). As a workaround, create it manually yourself, as `~/.m2/toolchains.xml`, with content similar to the following (adjust the paths to match your system):

```
<?xml version="1.0" encoding="UTF8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>1.8</version>
      <vendor>oracle</vendor>
      <!-- Change the id value to match the jdk version on your system --> 
      <id>jdk-8u31-linux-x64</id>
    </provides>
    <configuration>
      <!-- Change the jdkHome value to point to the jdk location on your system --> 
      <jdkHome>/home/myusername/workspaces/tools/jdk-8u31-linux-x64</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

### Skipping Tests

The default install goal for the maven build will run the integration tests.  If you do not want to run them as some do use AWS resources use the following command line when executing the build:

```
$ mvn clean install -DskipITs
```

## Eclipse Configuration

The following instructions are to be executed from within the Eclipse IDE application to ensure proper configuration.

### Eclipse JDK

Verify Eclipse is using the correct JDK.

1. Open **Window > Preferences**.
1. Select **Java > Installed JREs**.
1. If your JDK does not appear in the **Installed JREs** table add it by clicking the **Add** button, select **Standard VM** and locate your installation using the **Directory...** button.
1. Ensure your JDK is selected in the **Installed JREs** table by checking the checkbox next to the JDK you wish to use.

### Eclipse Preferences

If you're using Eclipse for development, you'll want to configure its preferences, as follows:

1. Open **Window > Preferences**.
1. Select **Maven**.
    1. Enable **Download Artifact Sources**.
    1. Enable **Download Artifact JavaDoc**.
1. Select **Maven > Annotation Processing**.
    1. Enable the **Automatically configure JDT APT** option.
1. Select **Java > Code Style > Code Templates**.
    1. Click **Import...** and select this project's [eclipse-codetemplates.xml](./eclipse-codetemplates.xml) file.
        * This configures the file, class, method, etc. comments on new items such that they match the existing style used in these projects.
    1. Enable the **Automatically add comments for new methods and types** option.
1. Select **Java > Code Style > Formatter**.
    1. Click **Import...** and select this project's [eclipse-formatter.xml](./eclipse-formatter.xml) file.
        * This configures the Eclipse autoformatter (`ctrl+shift+f`) to match the existing style used in these projects.
1. Select **Java > Editor > Save Actions**.
    1. Enable the **Perform the selected actions on save** option.
    1. Enable the **Format source code** option.
1. Click **OK**.

### Importing Maven Projects into Eclipse

If you have already cloned Blue Button repositories to your system they can easily be added to your Eclipse workspace using the **Import** feature.

1. Open **File > Import...**.
1. Select **Existing Maven Projects**.
1. Specify a **Root Directory** using the **Browse...** button or by typing in a path.
1. Select the pom files you want to import from the **Projects** table.
1. Click **Finish**.
1. The projects and packages you selected will now appear in the **Project Explorer** window.
