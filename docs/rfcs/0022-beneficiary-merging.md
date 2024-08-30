# RFC Proposal

* RFC Proposal ID: `0022-beneficiary-merging`
* Start Date: 2024-08-20
* RFC PR: [beneficiary-fhir-data/rfcs#0022](https://github.com/CMSgov/beneficiary-fhir-data/pull/2413)
* JIRA Ticket(s):
    * [BFD-3585](https://jira.cms.gov/browse/BFD-3585)

In a small number of cases, a given individual may be linked to multiple beneficiary records.
CCW and its upstream systems will attempt to track any changes to a beneficiary's personal information and link it to a single
beneficiary record, but limitations of these systems occasionally cause the information to be applied to multiple records.

When this happens, we receive a cross-reference ID (`xref_id`) which will indicate there are multiple beneficiary records that refer to the same person.
Additionally, we receive another field called the cross-reference switch (`xref_sw`) which will tell us which beneficiary
record is the most current.
The value of `xref_sw` can be either `Y` (yes) or `N` (no), with a value of `Y` indicating that this beneficiary has been merged into another and a value of `N` indicating that this is the most current record.

Additionally, there may be situations where a cross-reference relationship is created in error.
When this occurs, there will be a 'kill credit' code set to `1` indicating that the cross-reference relationship is invalid and should be ignored.
When the relationship _is_ valid, the code will either be `null` or `2`.

If these cross-referenced records had no claims attached, we could simply ignore them, but unfortunately that is not the case for a sizable portion of them.

This is a fairly rare scenario, with ~400,000 out of the ~65,000,000 beneficiaries having a cross-reference ID (**Note:** it seems we are still missing quite a lot of xref IDs so it's possible that this number will change).

## Status

* Status: Proposed <!-- (Proposed/Approved/Rejected/Implemented) -->
* Implementation JIRA Ticket(s):

## Table of Contents

- [RFC Proposal](#rfc-proposal)
  - [Status](#status)
  - [Table of Contents](#table-of-contents)
  - [Causes](#causes)
  - [Scenarios](#scenarios)
    - [Scenario 1: Happy path](#scenario-1-happy-path)
    - [Scenario 2: Null MBI that is cross-referenced](#scenario-2-null-mbi-that-is-cross-referenced)
    - [Scenario 3: Null MBI that is not cross-referenced](#scenario-3-null-mbi-that-is-not-cross-referenced)
    - [Scenario 4: Duplicate MBIs that are cross-referenced](#scenario-4-duplicate-mbis-that-are-cross-referenced)
    - [Scenario 5: Duplicate MBIs that are not cross-referenced](#scenario-5-duplicate-mbis-that-are-not-cross-referenced)
    - [Scenario 6: Cross-referenced beneficiaries with different MBIs](#scenario-6-cross-referenced-beneficiaries-with-different-mbis)
    - [Scenario 7: Invalided cross-reference relationship](#scenario-7-invalided-cross-reference-relationship)
  - [Potential Solutions - Patient Endpoint](#potential-solutions---patient-endpoint)
    - [General Solutions for handling Patient resources when a merge occurs](#general-solutions-for-handling-patient-resources-when-a-merge-occurs)
      - [Option 1 - Links (Replacement)](#option-1---links-replacement)
        - [Using \_include to include linked items](#using-_include-to-include-linked-items)
      - [Option 2 - Links (See Also)](#option-2---links-see-also)
      - [Option 3 - Automatic Replacement](#option-3---automatic-replacement)
      - [Option 4 - Freezing](#option-4---freezing)
    - [Reconciling beneficiary records when searching by contract](#reconciling-beneficiary-records-when-searching-by-contract)
      - [Option 1 - Links](#option-1---links)
      - [Option 2 - Filtering](#option-2---filtering)
    - [Missing historical identifiers when searching by contract](#missing-historical-identifiers-when-searching-by-contract)
  - [Tradeoffs](#tradeoffs)

## Causes

The scenarios which cause these merged beneficiary records to be created are somewhat unclear.
In general, CCW attempts to link all upstream beneficiary data for an individual to a single beneficiary ID, but this process can behave erroneously due to limitations in the logic or bad data coming from upstream.

One situation that we know does cause a cross-reference number to be generated is when a beneficiary's HICN changes under specific circumstances.
The following information is paraphrased from the EDB data dictionary:

> The first nine characters of a HICN are known as the account number and the last one or two characters are known as the BIC (Beneficiary Identification Code).
> If the account number portion of a beneficiary's HICN changes, that will generate a cross reference ID.
>
> The BIC shows the relationship between the beneficiary and the Social Security wage earner.
> A beneficiary's BIC can change based on marital status or Social Security enrollment status.
> EDB has a concept of an "equatable" BIC change.
> When an equatable BIC change occurs, a cross reference ID is generated. 
> A BIC change from 'B' (wife) to 'D' (widow) is an example of an equatable BIC change.
> Non-equatable BIC changes only happen under erroneous conditions.

It's not explicitly stated, but I expect a cross-reference ID _should_ also be generated for non-equatable BIC changes and I have observed some instances of this.

## Scenarios

### Scenario 1: Happy path

| bene_id (FHIR ID) | mbi_num     | xref_id | xref_sw | kill_credit |
| ----------------- | ----------- | ------- | ------- | ----------- |
| 1                 | 1S00EU8FF04 | null    | null    | null        |

This is the normal scenario - in most cases a beneficiary shouldn't have a cross-reference ID.

Additionally, a beneficiary can have multiple MBIs that are properly attributed in BFD today.
These appear as historical identifiers on the patient resource.

| bene_id (FHIR ID) | mbi_num     | xref_id | xref_sw | kill_credit |
| ----------------- | ----------- | ------- | ------- | ----------- |
| 1                 | 1S00EU8FF03 | null    | null    | null        |
| 1                 | 1S00EU8FF04 | null    | null    | null        |

### Scenario 2: Null MBI that is cross-referenced

| bene_id (FHIR ID) | mbi_num     | xref_id | xref_sw | kill_credit |
| ----------------- | ----------- | ------- | ------- | ----------- |
| 2                 | 1S00EU8FF05 | 1       | N       | null        |
| 3                 | null        | 1       | Y       | null        |

There is a missing MBI for an older version of beneficiary record, but we can find the beneficiary's MBI from the newer cross-referenced record.

### Scenario 3: Null MBI that is not cross-referenced

| bene_id (FHIR ID) | mbi_num | xref_id | xref_sw | kill_credit |
| ----------------- | ------- | ------- | ------- | ----------- |
| 4                 | null    | null    | null    | null        |

This beneficiary is missing an MBI, but does not have a cross-reference ID. There's no way for us to find the missing data that it's tied to.

### Scenario 4: Duplicate MBIs that are cross-referenced

| bene_id  (FHIR ID) | mbi_num     | xref_id | xref_sw | kill_credit |
| ------------------ | ----------- | ------- | ------- | ----------- |
| 5                  | 1S00EU8FF06 | 2       | Y       | null        |
| 6                  | 1S00EU8FF06 | 2       | N       | null        |

These records are cross-referenced, so they refer to the same beneficiary. Currently, BFD will return an error in this scenario.

### Scenario 5: Duplicate MBIs that are not cross-referenced

| bene_id (FHIR ID) | mbi_num     | xref_id | xref_sw | kill_credit |
| ----------------- | ----------- | ------- | ------- | ----------- |
| 7                 | 1S00EU8FF07 | null    | null    | null        |
| 8                 | 1S00EU8FF07 | null    | null    | null        |

This is likely a case of two beneficiaries that should be cross-referenced. This is a very rare scenario (only a few dozen). CCW is looking into this to see if they can provide us with cross-reference IDs for the remaining cases.

### Scenario 6: Cross-referenced beneficiaries with different MBIs

| bene_id (FHIR ID) | mbi_num     | xref_id | xref_sw | kill_credit |
| ----------------- | ----------- | ------- | ------- | ----------- |
| 9                 | 1S00EU8FF08 | 3       | Y       | null        |
| 10                | 1S00EU8FF09 | 3       | N       | null        |

This beneficiary's MBI changed from 1S00EU8FF08 to 1S00EU8FF09. A single beneficiary's MBI can change over time, but usually the new MBI is associated with the existing beneficiary record.
In this case, the MBI change was associated with a different record instead of the existing one. Without a fix for the merged beneficiaries, the older MBI will not be shown in the list of historical MBIs.

It's worth noting that sometimes a historical MBI will be linked to both the cross-referenced record and the new record, so we may have to add some logic to ensure we're not returning duplicate identifiers during a merge operation.

### Scenario 7: Invalided cross-reference relationship

| bene_id (FHIR ID) | mbi_num     | xref_id | xref_sw | kill_credit |
| ----------------- | ----------- | ------- | ------- | ----------- |
| 11                | 1S00EU8FF11 | 3       | Y       | null        |
| 12                | 1S00EU8FF12 | 3       | N       | 1           |

Here the `kill_credit` flag is set to `1`, indicating that the `xref_id` is no longer valid and should be treated as though it does not exist.

## Potential Solutions - Patient Endpoint

### General Solutions for handling Patient resources when a merge occurs

#### Option 1 - Links (Replacement)

When a beneficiary merge occurs, the Patient response will contain a field that lists any additional Patient resources that are cross-referenced.
A resource that contains a link marked as `replaced-by` means the resource refers to an older version of the beneficiary.
A resource that contains a link marked as `replaces` means the current resource is the most up-to-date, but older versions also exist.
A non-current Patient record will also have its `active` field set to `false` (**Note:** we don't currently set the `active` field at all). 

Using the following data:

| bene_id (FHIR ID) | mbi_num     | xref_id | xref_sw | kill_credit |
| ----------------- | ----------- | ------- | ------- | ----------- |
| 5                 | 1S00EU8FF08 | 2       | Y       | null        |
| 6                 | 1S00EU8FF09 | 2       | N       | null        |

Patient 6 replaces patient 5

```json
{
  "resourceType": "Bundle",
  "id": "720151cf-dfc9-4516-ab39-2925100c7429",
  "meta": {
    "lastUpdated": "2024-08-09T12:44:27.371-07:00"
  },
  "type": "searchset",
  "total": 1,
  "link": [
    {
      "relation": "self",
      "url": "https://prod.bfd.cms.gov/v2/fhir/Patient?_format=json&_id=6"
    }
  ],
  "entry": [
    {
      "resource": {
        "resourceType": "Patient",
        "id": "6",
        "link": [
          {
            "other": {
              "reference": "Patient/5"
            },
            "type": "replaces"
          }
        ]
      }
    }
  ]
}
```

Patient 5 is replaced by patient 6

```json
{
  "resourceType": "Bundle",
  "id": "720151cf-dfc9-4516-ab39-2925100c7429",
  "meta": {
    "lastUpdated": "2024-08-09T12:44:27.371-07:00"
  },
  "type": "searchset",
  "total": 1,
  "link": [
    {
      "relation": "self",
      "url": "https://prod.bfd.cms.gov/v2/fhir/Patient?_format=json&_id=5"
    }
  ],
  "entry": [
    {
      "resource": {
        "resourceType": "Patient",
        "id": "5",
        "link": [
          {
            "other": {
              "reference": "Patient/6"
            },
            "type": "replaced-by"
          }
        ]
      }
    }
  ]
}
```

##### Using _include to include linked items

To fetch all linked items in a single call, the `_include` parameter can be set to `patient:link`.

```json
{
  "resourceType": "Bundle",
  "id": "720151cf-dfc9-4516-ab39-2925100c7429",
  "meta": {
    "lastUpdated": "2024-08-09T12:44:27.371-07:00"
  },
  "type": "searchset",
  "total": 2,
  "link": [
    {
      "relation": "self",
      "url": "https://prod.bfd.cms.gov/v2/fhir/Patient?_format=json&_id=6&_include=patient:link"
    }
  ],
  "entry": [
    {
      "resource": {
        "resourceType": "Patient",
        "id": "6",
        "link": [
          {
            "other": {
              "reference": "Patient/5"
            },
            "type": "replaces"
          }
        ]
      }
    },
    {
      "resource": {
        "resourceType": "Patient",
        "id": "5",
        "link": [
          {
            "other": {
              "reference": "Patient/6"
            },
            "type": "replaced-by"
          }
        ]
      }
    }
  ]
}
```

#### Option 2 - Links (See Also)

Using the following data:

| bene_id (FHIR ID) | mbi_num     | xref_id | xref_sw | kill_credit |
| ----------------- | ----------- | ------- | ------- | ----------- |
| 5                 | 1S00EU8FF08 | 2       | Y       | null        |
| 6                 | 1S00EU8FF09 | 2       | N       | null        |

Searching by id (bene_id) will return only the matched record.
Searching by identifier (MBI) will return both resources.
In both cases, the response will contain a `link` field marked as `seealso`, which links to the other resource.

As opposed to the `replaced-by`/`replaces` solution, this does not attempt to convey identity, which is helpful in the event that an un-merge occurs.

The `_include` operation can be supported here just like in the previous option.

We will still need an alternative way to convey which resource is the most current one.
This may need to be accomplished with an extension.

Searching for id=5 yields:

```json
{
  "resourceType": "Bundle",
  "id": "720151cf-dfc9-4516-ab39-2925100c7429",
  "meta": {
    "lastUpdated": "2024-08-09T12:44:27.371-07:00"
  },
  "type": "searchset",
  "total": 1,
  "link": [
    {
      "relation": "self",
      "url": "https://prod.bfd.cms.gov/v2/fhir/Patient?_format=json&_id=5"
    }
  ],
  "entry": [
    {
      "resource": {
        "resourceType": "Patient",
        "id": "5",
        "link": [
          {
            "other": {
              "reference": "Patient/6"
            },
            "type": "seealso"
          }
        ]
      }
    }
  ]
}
```

Searching for id=6 yields:

```json
{
  "resourceType": "Bundle",
  "id": "720151cf-dfc9-4516-ab39-2925100c7429",
  "meta": {
    "lastUpdated": "2024-08-09T12:44:27.371-07:00"
  },
  "type": "searchset",
  "total": 1,
  "link": [
    {
      "relation": "self",
      "url": "https://prod.bfd.cms.gov/v2/fhir/Patient?_format=json&_id=6"
    }
  ],
  "entry": [
    {
      "resource": {
        "resourceType": "Patient",
        "id": "6",
        "link": [
          {
            "other": {
              "reference": "Patient/5"
            },
            "type": "seealso"
          }
        ]
      }
    }
  ]
}
```

#### Option 3 - Automatic Replacement

Using the following data:

| bene_id (FHIR ID) | mbi_num     | xref_id | xref_sw | kill_credit |
| ----------------- | ----------- | ------- | ------- | ----------- |
| 5                 | 1S00EU8FF08 | 2       | Y       | null        |
| 6                 | 1S00EU8FF09 | 2       | N       | null        |

Searching for beneficiary 5 will return data for beneficiary 6 with beneficiary 5's MBI included as a historical identifier.

```json
{
  "resourceType": "Bundle",
  "id": "720151cf-dfc9-4516-ab39-2925100c7429",
  "meta": {
    "lastUpdated": "2024-08-09T12:44:27.371-07:00"
  },
  "type": "searchset",
  "total": 1,
  "link": [
    {
      "relation": "self",
      "url": "https://prod.bfd.cms.gov/v2/fhir/Patient?_format=json&_id=5"
    }
  ],
  "entry": [
    {
      "resource": {
        "resourceType": "Patient",
        "id": "6",
        "meta": {
          "lastUpdated": "2024-08-09T12:44:27.152-07:00",
          "profile": [
            "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Patient"
          ]
        },
        "identifier": [
          {
            "type": {
              "coding": [
                  {
                  "extension": [
                    {
                      "url": "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
                      "valueCoding": {
                          "system": "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
                          "code": "current",
                          "display": "Current"
                      }
                    }
                  ],
                  "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                  "code": "MC",
                  "display": "Patient's Medicare number"
                }
              ]
            },
            "system": "http://hl7.org/fhir/sid/us-mbi",
            "value": "1S00EU8FF09",
          },
          {
            "type": {
              "coding": [
                {
                  "extension": [
                    {
                      "url": "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
                      "valueCoding": {
                          "system": "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
                          "code": "historic",
                          "display": "Historic"
                      }
                    }
                  ],
                  "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                  "code": "MC",
                  "display": "Patient's Medicare number"
                }
              ]
            },
            "system": "http://hl7.org/fhir/sid/us-mbi",
            "value": "1S00EU8FF08"
          }
        ], 
      }
    }
  ]
}
```

Searching for beneficiary 6 will behave the same way.

```json
{
  "resourceType": "Bundle",
  "id": "720151cf-dfc9-4516-ab39-2925100c7429",
  "meta": {
    "lastUpdated": "2024-08-09T12:44:27.371-07:00"
  },
  "type": "searchset",
  "total": 1,
  "link": [
    {
      "relation": "self",
      "url": "https://prod.bfd.cms.gov/v2/fhir/Patient?_format=json&_id=6"
    }
  ],
  "entry": [
    {
      "resource": {
        "resourceType": "Patient",
        "id": "6",
        "meta": {
          "lastUpdated": "2024-08-09T12:44:27.152-07:00",
          "profile": [
            "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Patient"
          ]
        },
         "identifier": [
          {
            "type": {
              "coding": [
                  {
                  "extension": [
                    {
                      "url": "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
                      "valueCoding": {
                          "system": "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
                          "code": "current",
                          "display": "Current"
                      }
                    }
                  ],
                  "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                  "code": "MC",
                  "display": "Patient's Medicare number"
                }
              ]
            },
            "system": "http://hl7.org/fhir/sid/us-mbi",
            "value": "1S00EU8FF09",
          },
          {
            "type": {
              "coding": [
                {
                  "extension": [
                    {
                      "url": "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
                      "valueCoding": {
                          "system": "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
                          "code": "historic",
                          "display": "Historic"
                      }
                    }
                  ],
                  "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                  "code": "MC",
                  "display": "Patient's Medicare number"
                }
              ]
            },
            "system": "http://hl7.org/fhir/sid/us-mbi",
            "value": "1S00EU8FF08"
          }
        ], 
      }
    }
  ]
}
```

#### Option 4 - Freezing

We could "freeze" the beneficiary ID so that we always use the same ID even if a newer version is later created.
This would prevent consumers from having to handle difficulties related to the bene_id changing over time.

This would require storing a field in the database to indicate which cross reference ID is the "frozen" one. All records without a cross reference ID can initially have their frozen status set to `true` since this is the ID we are already sharing

If we have a single, non-cross-referenced beneficiary:

| bene_id  (FHIR ID) | mbi_num     | xref_id | xref_sw | frozen | kill_credit |
| ------------------ | ----------- | ------- | ------- | ------ | ----------- |
| 5                  | 1S00EU8FF08 | null    | null    | true   | null        |


and we later receive a new record to merge:

| bene_id (FHIR ID) | mbi_num     | xref_id | xref_sw | frozen | kill_credit |
| ----------------- | ----------- | ------- | ------- | ------ | ----------- |
| 5                 | 1S00EU8FF08 | 2       | Y       | true   | null        |
| 6                 | 1S00EU8FF09 | 2       | N       | false  | null        |

We can consider the new MBI (1S00EU8FF09) the current identifier, but still return 5 as the bene_id so that consumer won't have to know about the bene_id changing.

```json
{
  "resourceType": "Bundle",
  "id": "720151cf-dfc9-4516-ab39-2925100c7429",
  "meta": {
    "lastUpdated": "2024-08-09T12:44:27.371-07:00"
  },
  "type": "searchset",
  "total": 1,
  "link": [
    {
      "relation": "self",
      "url": "https://prod.bfd.cms.gov/v2/fhir/Patient?_format=json&_id=5"
    }
  ],
  "entry": [
    {
      "resource": {
        "resourceType": "Patient",
        "id": "5",
        "meta": {
          "lastUpdated": "2024-08-09T12:44:27.152-07:00",
          "profile": [
            "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Patient"
          ]
        },
         "identifier": [
          {
            "type": {
              "coding": [
                  {
                  "extension": [
                    {
                      "url": "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
                      "valueCoding": {
                          "system": "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
                          "code": "current",
                          "display": "Current"
                      }
                    }
                  ],
                  "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                  "code": "MC",
                  "display": "Patient's Medicare number"
                }
              ]
            },
            "system": "http://hl7.org/fhir/sid/us-mbi",
            "value": "1S00EU8FF09",
          },
          {
            "type": {
              "coding": [
                {
                  "extension": [
                    {
                      "url": "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
                      "valueCoding": {
                          "system": "https://bluebutton.cms.gov/resources/codesystem/identifier-currency",
                          "code": "historic",
                          "display": "Historic"
                      }
                    }
                  ],
                  "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                  "code": "MC",
                  "display": "Patient's Medicare number"
                }
              ]
            },
            "system": "http://hl7.org/fhir/sid/us-mbi",
            "value": "1S00EU8FF08"
          }
        ], 
      }
    }
  ]
}
```

Searching for bene_id 6 would behave as though that ID doesn't exist at all.

```json
{
  "resourceType": "Bundle",
  "id": "b1c08916-7cea-4c55-b7c9-5a03e59dd48b",
  "meta": {
    "lastUpdated": "2024-08-09T12:44:27.371-07:00"
  },
  "type": "searchset",
  "total": 0,
  "link": [
    {
      "relation": "self",
      "url": "https://prod.bfd.cms.gov/v2/fhir/Patient?_format=json&_id=6"
    }
  ]
}
```

The main challenge with this approach is handling existing data since we're currently returning the merged beneficiaries as though they're distinct records.
We would likely need a one-time process to reset any data being stored downstream.

### Reconciling beneficiary records when searching by contract

#### Option 1 - Links

If we have the following data:

| bene_id (FHIR ID) | mbi_num     | xref_id | xref_sw | kill_credit | ptdcntrct01 | rfrnc_yr |
| ----------------- | ----------- | ------- | ------- | ----------- | ----------- | -------- |
| 2                 | 1S00EU8FF05 | 1       | N       | null        | Z1234       | 2019     |
| 3                 | null        | 1       | Y       | null        | S4607       | 2018     |

We can use the linking solution mentioned above in this scenario as well.
Instead of automatically finding the most recent version, we return the record as is and require the caller to request the cross-referenced resource by following the link.

```json
{
  "resourceType": "Bundle",
  "id": "720151cf-dfc9-4516-ab39-2925100c7429",
  "meta": {
    "lastUpdated": "2024-08-09T12:44:27.371-07:00"
  },
  "type": "searchset",
  "total": 1,
  "active": false,
  
  "link": [
    {
      "relation": "self",
      "url": "https://prod.bfd.cms.gov/v2/fhir/Patient?_has:Coverage.extension=https://bluebutton.cms.gov/resources/variables/ptdcntrct01|S4607&_has:Coverage.rfrncyr=2018"
    },
    {
      "other": {
        "reference": "Patient/2"
      },
      "type": "seealso" // or replaces/replaced-by if we go with that option
     }
    ],
    "entry": [
    {
      "resource": {
        "resourceType": "Patient",
        "id": "3",
        "meta": {
          "lastUpdated": "2024-08-09T12:44:27.152-07:00",
          "profile": [
            "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Patient"
          ]
        }
      }
    }
  ]
}
```

#### Option 2 - Filtering

If a beneficiary record is found in the search, but it has a newer version available, only the newest version will be returned.

If we have the following data:

| bene_id (FHIR ID) | mbi_num     | xref_id | xref_sw | kill_credit | ptdcntrct01 | rfrnc_yr |
| ----------------- | ----------- | ------- | ------- | ----------- | ----------- | -------- |
| 2                 | 1S00EU8FF05 | 1       | Y       | null        | Z1234       | 2019     |
| 3                 | null        | 1       | N       | null        | S4607       | 2018     |

and the caller searches for `/v2/fhir/Patient?_has:Coverage.extension=https://bluebutton.cms.gov/resources/variables/ptdcntrct01|S4607&_has:Coverage.rfrncyr=2018`, normally beneficiary 3 would be returned. However, because beneficiary 3 has been merged into beneficiary 2, beneficiary 2 will be returned instead.

```json
{
  "resourceType": "Bundle",
  "id": "720151cf-dfc9-4516-ab39-2925100c7429",
  "meta": {
    "lastUpdated": "2024-08-09T12:44:27.371-07:00"
  },
  "type": "searchset",
  "total": 1,
  "link": [
    {
      "relation": "self",
      "url": "https://prod.bfd.cms.gov/v2/fhir/Patient?_has:Coverage.extension=https://bluebutton.cms.gov/resources/variables/ptdcntrct01|S4607&_has:Coverage.rfrncyr=2018"
    }
  ],
  "entry": [
    {
      "resource": {
        "resourceType": "Patient",
        "id": "2",
        "meta": {
          "lastUpdated": "2024-08-09T12:44:27.152-07:00",
          "profile": [
            "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Patient"
          ]
        }
      }
    }
  ]
}
```

### Missing historical identifiers when searching by contract

Currently, we do not return historical identifiers when searching by contract.
This means that any consumers of this data who attempt to use MBI as a unique identifier for bulk patient data are not able to correctly link together all of that beneficiary's data if their MBI changes.

This issue is distinct from the merge operations that we've been discussing so far, but it has enough of an overlap that it might make sense to address both issues simultaneously.

With historical identifiers included and of of the proposed changes above included, consumers will be able to see all identifiers associated with the current and past beneficiary records.

## Tradeoffs

These potential solutions fall under a spectrum of tradeoffs between "correctness" and level of effort for consumers.
The links-based approach is the most correct approach from a FHIR perspective, but requires the most effort from our peering partners and potentially from downstream users as well.
Using links with `seealso` rather than `replaces`/`replaced-by` makes the relationship between current and previous records less explicit, but provides more flexibility for handling un-merge operations (based on kill-credit) since we're no longer making a statement on identity between links.
