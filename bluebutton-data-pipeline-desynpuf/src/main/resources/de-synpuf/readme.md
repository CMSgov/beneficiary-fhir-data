DE-SynPUF Sample Data
=====================

This data was downloaded from [CMS 2008-2010 Data Entrepreneurs Synthetic Public Use File (DE-SynPUF)](https://www.cms.gov/Research-Statistics-Data-and-Systems/Downloadable-Public-Use-Files/SynPUFs/DE_Syn_PUF.html).

After downloading the sample ZIP files, they were extracted and then recompressed into a single file, as follows:

    $ mkdir de-synpuf-sample-1
    $ unzip DE1_0_\*.zip -d de-synpuf-sample-1
    $ cd de-synpuf-sample-1
    $ tar -cvjSf bluebutton-data-pipeline-sampledata/src/main/resources/de-synpuf/sample-1.tar.bz2 ./*

The resulting combined archive was then hooked in to git-lfs, as follows:

    $ git lfs track bluebutton-data-pipeline-sampledata/src/main/resources/de-synpuf/sample-1.tar.bz2
    $ git add bluebutton-data-pipeline-sampledata/src/main/resources/de-synpuf/sample-1.tar.bz2
