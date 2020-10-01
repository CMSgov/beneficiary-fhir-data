# BFD Request Options

This document details the request options that can be used when calling BFD.
Future versions of BFD may apply some of these options automatically,
  based on other request details (e.g. authentication/authorization).

## Optional Data

Some data fields are optional; they're only included when the request is configured to do so.

* HTTP Header: `IncludeIdentifiers`
    * Operations: all `/Patient` requests
    * Default value: `false`
    * Supported values: `false`, `true`, `hicn`, `mbi`
    * Description:
      Do **not** set this header more than once; an arbitrary value will be selected if that happens.
      When set to `mbi`, BFD will include all of the known MBIs for the requested beneficiary (unhashed).
      When set to `hicn`, BFD will include all of the known HICNs for the requested beneficiary (unhashed).
      When set to `true`, BFD will include all of the known MBIs and HICNs for the requested beneficiary (unhashed).
      When set to `false`, BFD will not include any (unhashed) MBIs or HICNs for the requested beneficiary.
* HTTP Header: `IncludeAddressFields`
    * Operations: all `/Patient` requests
    * Default value: `false`
    * Supported values: `false`, `true`
    * Description:
      When set to `true`, BFD will include all of the detailed address data for the requested beneficiary.
      When set to `false`, BFD will not include detailed address data for the requested beneficiary.
      Please note that, even when `false` county and ZIP/postal codes will still be included for the specified beneficiary.
