Development Environment Setup
=============================

Thinking of contributing to this project or some of the other Java-based Blue Button projects? Great! This document provides some help on getting a development environment setup for that work.

## Automation FTW!

First off, if you're on one of the following platforms, we provide the [devenv-install.py](./devenv-install.py) script, which automates most of the work for you. It supports:

* Windows, with [Cygwin](https://www.cygwin.com/)

It can be run as follows:

1. Install Python 3.
    * On Windows, this can be done using [apt-cyg](https://github.com/transcode-open/apt-cyg), as follows:
    
        ```
        $ apt-cyg install python3 python3-setuptools
        ```
    
1. Run the script:

    ````
    $ ./devenv-install.py
    ````

What does it do for you? Great question! It will create a `~/workspaces/tools/` directory and then download and install (as a user) the following into there for you:

* An [Oracle Java 8 JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Eclipse Mars](https://eclipse.org/mars/)
    * These Eclipse Plugins:
        * [m2e-apt](https://developer.jboss.org/en/tools/blog/2012/05/20/annotation-processing-support-in-m2e-or-m2e-apt-100-is-out?_sscc=t): Allows Maven projects in Eclipse to easily leverage [the JDK's Annotation Processing framework](http://docs.oracle.com/javase/7/docs/technotes/guides/apt/).
* [Apache Maven](https://maven.apache.org/)

If you're not using one of those supported platforms, or would prefer to setup things manually, you'll want to download and install the items listed above yourself.

## Git Large File Storage

You'll also need to manually (boo!) download and install [Git Large File Storage](https://git-lfs.github.com/), which is used by some of the projects to store the large amounts of sample data needed in tests. Once installed, you need to run the following command once on your system:

    $ git lfs install

If you've already cloned any of our repos, you'll also want to run the following command in each of them to download and checkout any LFS files (future clones should do this automagically for you, now that it's installed):

    $ git lfs pull

## Eclipse Preferences

If you're using Eclipse for development, you'll want to configure its preferences, as follows:

1. Open **Window > Preferences**.
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
