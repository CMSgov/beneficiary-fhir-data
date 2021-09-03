#!/bin/bash

wget --directory-prefix synthetic-data \
    $SYNTHETIC_DATA_LOCATION/0_manifest.xml \
    $SYNTHETIC_DATA_LOCATION/synthetic-beneficiary-1999.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-beneficiary-2000.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-beneficiary-2014.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-carrier-1999-1999.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-carrier-1999-2000.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-carrier-1999-2001.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-carrier-2000-2000.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-carrier-2000-2001.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-carrier-2000-2002.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-carrier-2014-2014.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-carrier-2014-2015.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-carrier-2014-2016.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-inpatient-1999-1999.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-inpatient-1999-2000.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-inpatient-1999-2001.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-inpatient-2000-2000.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-inpatient-2000-2001.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-inpatient-2000-2002.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-inpatient-2014-2014.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-inpatient-2014-2015.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-inpatient-2014-2016.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-outpatient-1999-1999.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-outpatient-2000-1999.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-outpatient-2001-1999.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-outpatient-2002-2000.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-outpatient-2014-2014.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-outpatient-2015-2014.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-outpatient-2016-2014.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-pde-2014.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-pde-2015.rif \
    $SYNTHETIC_DATA_LOCATION/synthetic-pde-2016.rif 

