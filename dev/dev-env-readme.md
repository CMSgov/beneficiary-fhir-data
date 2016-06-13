Development Environment Setup
=============================

Thinking of contributing to this project? Great! This document provides some help on getting a development environment setup for that work.

## Common/Shared Instructions

Most of the setup and configuration for this project is the same as for the other Java-based Blue Button projects. Accordingly, please be sure to follow all of the instructions documented here, first: [bluebutton-parent-pom: Development Environment Setup](https://github.com/HHSIDEAlab/bluebutton-parent-pom/blob/devenv-instructions/dev/devenv-readme.md).

## Git LFS

This project uses [git-lfs](https://git-lfs.github.com/) to manage the large DE-SynPUF archives needed in the source tree. Install it, as follows:

    $ curl -s https://packagecloud.io/install/repositories/github/git-lfs/script.deb.sh | sudo bash
    $ sudo apt-get install git-lfs
    $ git lfs install

If you'd already cloned this repo before installing Git LFS, run the following to grab the LFS files:

    $ git lfs pull

However, if you'd installed Git LFS *before* cloning, you should already be good to go.

## Build Dependencies

This project depends on the [HAPI FHIR](https://github.com/jamesagnew/hapi-fhir) project. Releases of that project are available in the Maven Central repository, which generally makes things pretty simple: our Maven builds will pick up theirs.

Unfortunately, this project will sometimes need to depend on an interim/snapshot build of HAPI FHIR. When that's the case, developers will first need to locally checkout and `mvn install` that interim version themselves, manually. To keep this simpler, a fork of HAPI FHIR is maintained in the [HHSIDEAlab/hapi-fhir](https://github.com/HHSIDEAlab/hapi-fhir) repository on GitHub, which will always point to whatever version of HAPI FHIR this one depends on. You can checkout and build that fork, as follows:

    $ git clone https://github.com/HHSIDEAlab/hapi-fhir.git hhsidealab-hapi-fhir.git
    $ cd hhsidealab-hapi-fhir.git
    $ mvn clean install -DskipITs=true -DskipTests=true

Once the build is done, the HAPI FHIR artifacts will be placed into your user's local Maven repository (`~/.m2/repository`), available for use by this project or others.
