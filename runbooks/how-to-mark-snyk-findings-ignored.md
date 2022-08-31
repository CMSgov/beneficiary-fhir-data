Follow this runbook to mark a SNYK finding that has been determined not to be a vulnerability or that is considered an
acceptable risk as 'Ignored' in the [SNYK dashboard](https://app.snyk.io/org/bluebutton-fd-oeda).

1. SNYK findings can be ignored when two lead engineers from either the BFD team or CMS agree on the justification.
   Have a discussion about why the finding does not apply to BFD or why the risk of the finding is acceptable.

2. Post a message in the [bfd](https://cmsgov.slack.com/archives/C010WDXAZFZ) slack channel summarizing the findings
   that will be marked 'Ignored' and the justification. Make adjustments as appropriate based on any feedback
   provided.

3. Draft a concise description of the justification for marking the finding as 'Ignored'.

4. From the [SNYK dashboard](https://app.snyk.io/org/bluebutton-fd-oeda) click the 'Ignore' button for the finding and
   provide the justification as well as the usernames of the engineers that consulted on the justification. Select
   the appropriate categorization:
   1. 'Ignore Permanently' should be used for findings that are not planned to ever be addressed.
   2. 'Ignore Temporarily' should be used for findings that are planned to be addressed in the next PI at the latest.
      When selecting this option the JIRA ticket number for the remediation must be provided in the description.
      The JIRA ticket should include a link to the finding in the SNYK dashboard and any additional information that is
      available as an aid to the resolution. For Maven dependency findings, reference the 'Additional Information'
      section of the [Resolving SNK-bot PR](how-to-mark-snyk-findings-ignored.md) for populating the JIRA ticket.

