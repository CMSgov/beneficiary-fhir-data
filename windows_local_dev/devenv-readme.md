Development Environment Setup
=============================

Thinking of contributing to this project? Great! This document provides some help on getting a development environment setup for that work.

## Getting Started

### Windows: Cygwin

Cygwin provides a Unix environment for Windows systems. It should be installed as follows:

1. Create a directory to install Cygwin to. `C:\cygwin64` is traditional.
1. Download the Cygwin installer from <https://cygwin.com/setup-x86_64.exe> and save it to that directory, e.g. `C:\cygwin64\setup-x86_64.exe`.
1. Open a non-administrator Windows/DOS command prompt from the Start menu.
1. Run the following command to download and install Cygwin and a couple of basic extra packages (adjust the `cygwin64` directory entries to match the one you chose earlier):
    
    ```
    > setup-x86_64.exe --no-admin --quiet-mode --arch x86_64 --site http://mirror.cs.vt.edu/pub/cygwin/cygwin/ --only-site --root C:\cygwin64 --local-package-dir C:\cygwin64\packages-local --packages lynx --no-desktop
    ```
    
1. Wait for that installation to complete.

Once Cygwin is installed, it can be launched by launching the **Cygwin64 Terminal** application that will now be in the Start menu.

#### Cygwin: Package Management

Astonishingly, Cygwin doesn't really provide a decent package manager out of the box (in Unix systems, "package managers" are OS-provided utilities for installing available software). Instead, most folks either use the provided `setup-x86_64.exe` or a third-party utility like `[apt-cyg](https://github.com/transcode-open/apt-cyg)`. This guide recommends the use of `apt-cyg`, even though it doesn't support package upgrades and is unmaintained, as `setup-x86_64.exe` has a whole bunch of its own issues, such as an inability to update itself. Nonetheless, feel free to use whatever makes you happiest -- you'll just have to figure out yourself how to install all of the packages listed here as being installed via '`apt-cyg install ...`'.

Launch a Cygwin terminal and install `apt-cyg` by running  the following:

    $ lynx -source rawgit.com/transcode-open/apt-cyg/master/apt-cyg > apt-cyg
    $ install apt-cyg /bin

Once installed, `apt-cyg` can be used to install packages by running '`apt-cyg install <package>..`'. For example, to install the silly `fortune` utility:

    $ apt-cyg install fortune-mod
    $ fortune
    A debugged program is one for which you have not yet found the conditions
    that make it fail.
                    -- Jerry Ogdin

#### Cygwin: Permissions Fix

Things seem to go badly sideways in Cygwin if it tries to use ACL permissions. To avoid that, adjust the `/cygdrive` mount in `/etc/fstab` to include the `acl` and `exec` options, e.g.:

    none /cygdrive cygdrive binary,posix=0,user,noacl,exec 0 0

After changing that, be sure to completely close and restart all Cygwin processes. (Here's the reference that helped me solve this problem: <https://stackoverflow.com/questions/5828037/cygwin-sets-file-permission-to-000/7082542#7082542>.)

#### Cygwin: Other Prerequisites

Install the dependencies required by the `devenv-install.py` script:

    $ apt-cyg install python3 python3-lxml python3-setuptools unzip cabextract wget

__Note:__ If apt-cyg is having problems connecting to your cygwin mirror this may be due to an [incorrect HOSTTYPE setting](#hosttype).

Install and configure the other general development utilities that will be needed:

    $ apt-cyg install git
    $ git config --global user.email "myemail@example.com"
    $ git config --global user.name "My Full Name"

#### Cygwin: Shell Script File Association

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
#### Cygwin: HOSTTYPE Configuration

If apt-cyg is having problems connecting to a Cygwin mirror your HOSTTYPE configuration may be the problem.  Verify your HOSTTYPE does not have additional decoration(i.e. x86_64-cygwin) and only contains the system architecture(i.e. x86_64) that you are attempting to install on.

```
# Example incorrect setting
$ echo $HOSTTYPE
x86_64-cygwin

# Example correct setting
$ echo $HOSTTYPE
x86_64
```

`HOSTTYPE` can be overridden on the command line or set in your shell configuration (i.e. `~/.bashrc`, `~/.cshrc`, etc).  To override `HOSTTYPE` on the command line use the following construct:

```
# for csh/tcsh
> setenv HOSTTYPE x86_64 && apt-cyg <command>

# for bash
$ HOSTTYPE=x86_64 apt-cyg <command>
```

## Automation FTW!

First off, if you're on one of the following platforms, we provide the [devenv-install.py](./devenv-install.py) script, which automates most of the work for you. It supports:

* Windows, with [Cygwin](https://www.cygwin.com/)

It can be run as follows from a Bash prompt:

    $ wget https://github.com/CMSgov/beneficiary-fhir-data/raw/master/dev/devenv-install.py
    $ chmod a+x ./devenv-install.py
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

## AWS Configuration

### AWS Credentials

Many of the automated tests associated with the Blue Button framework use AWS resources.  Before running a build using Maven or importing projects into your Eclipse IDE, which will run a build automatically, please ensure the appropriate accounts and credentials are configured within your environment.  **This is necessary to prevent incurring unwanted charges on the wrong AWS account**.

Below are links to detailed instructions on configuring your AWS credentials for your environment:

  * [Configuration and Credential Files](http://docs.aws.amazon.com/cli/latest/userguide/cli-config-files.html)

## Github Configuration

You will need to configure an SSH credential in order to clone the Blue Button repositories.  Instructions are thoroughly documented on Github but for convenience here are the relevant links:

  * [Connecting to Github with SSH](https://help.github.com/articles/connecting-to-github-with-ssh/)
  * [Generating a new SSH key and adding it to the ssh-agent](https://help.github.com/articles/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent/)
  * [Adding a new SSH key to your GitHub account](https://help.github.com/articles/adding-a-new-ssh-key-to-your-github-account/)
  * [Testing your SSH connection](https://help.github.com/articles/testing-your-ssh-connection/)

## Cloning the Repository

Clone the repository:

    $ mkdir -p ~/workspaces/bfd/
    $ git clone git@github.com:CMSgov/beneficiary-fhir-data.git ~/workspaces/bfd/beneficiary-fhir-data.git

## Building the Applications via Maven

The application code is in the `apps/` directory.

Running the Maven build up through the `install` phase will compile the code,
  run the unit tests, run the integration tests,
  and cache the build artifacts locally in `~/.m2/repository/`.
Run it, as follows:

    $ cd ~/workspaces/bfd/beneficiary-fhir-data.git/
    $ cd apps/
    $ mvn clean install

The first build will take longer as it fetches dependencies,
  but subsequent builds should take about 5 minutes to complete.

### Skipping Tests

If you do not want to run the integration tests (which add several minutes to the build and do use AWS resources), add the `-DskipITs` flag to the build:

```
$ mvn clean install -DskipITs
```

### Testing Against PostgreSQL

By default, the integration tests will all run against an in-memory DB called HSQL,
  which gets created on the fly and cleaned up as soon as the build completes.
This is done mostly to save time, as HSQL is hilariously fast.
It's also _close enough_ to PostgreSQL that anything that works with HSQL
  should also work with PostgreSQL.

Nevertheless, we use PostgreSQL in production and some changes **do** need
  to be tested against it -- particularly those involving DB schema and/or JPA changes.
To stand up a temporary PostgreSQL DB and run the ITs against it, do the following:

    $ docker-compose --file dev/docker-compose.yml up --detach
    $ mvn clean verify "-Dits.db.url=jdbc:postgresql://$(docker-compose --file dev/docker-compose.yml port postgresql 5432)/bfd?user=bfd&password=InsecureLocalDev"
    $ docker-compose --file dev/docker-compose.yml down

Builds against PostgreSQL are slower, and generally take about 10 minutes to complete (as opposed to 5).

## Eclipse Configuration

The following instructions are to be executed from within the Eclipse IDE application to ensure proper configuration.

### Eclipse JDK

Verify Eclipse is using the correct Java 8 JDK.

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
    1. Click **Import...** and select this project's [eclipse-codetemplates.xml](docs/assets/eclipse-codetemplates.xml) file.
        * This configures the file, class, method, etc. comments on new items such that they match the existing style used in these projects.
    1. Enable the **Automatically add comments for new methods and types** option.
1. Select **Java > Code Style > Formatter**.
    1. Click **Import...** and select this project's [eclipse-java-google-style.xml](docs/assets/eclipse-java-google-style.xml) file.
        * This configures the Eclipse autoformatter (`ctrl+shift+f`) to (mostly) match the one used by the autoformatter that is applied during Maven builds.
        * The [eclipse-java-google-style.xml](docs/assets/eclipse-java-google-style.xml) file was originally acquired from here: <https://github.com/google/styleguide/blob/gh-pages/eclipse-java-google-style.xml>.
1. Select **Java > Editor > Save Actions**.
    1. Enable the **Perform the selected actions on save** option.
    1. Enable the **Format source code** option.
1. Click **OK**.

### Importing Maven Projects into Eclipse

The repository can easily be added to your Eclipse workspace using the **Import** feature.

1. Open **File > Import...**.
1. Select **Existing Maven Projects**.
1. Specify the **Root Directory** using the **Browse...** button or by typing in a path ex: `~/workspaces/bfd/beneficiary-fhir-data.git`.
1. Verify that it found the projects in the **Projects** table.
1. Click **Finish**.
1. The projects and packages you selected will now appear in the **Project Explorer** window.
