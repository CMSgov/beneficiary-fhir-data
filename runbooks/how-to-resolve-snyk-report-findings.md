Follow this runbook for each SNYK finding that appears in the
[SNYK dashboard](https://app.snyk.io/org/bluebutton-fd-oeda) that does not have an associated SNYK bot PR automatically
created in github, for those findings that *do* have a SNYK-bot PR follow the
[Resolving SNKY-bot PRs](how-to-resolve-snyk-bot-prs.md).

1. Is this a licensing finding? If yes, the finding can be marked as 'Ignored' by following the
   [Ignore SNYK Findings Runbook](how-to-mark-snyk-findings-ignored.md).

2. For all other findings, including infrastructure findings and Maven dependency updates that do not have a SNYK bot PR:
    - Create a JIRA ticket or modify the AC of an existing ticket to capture the task of remediating the finding.
    - For critical severity findings, the ticket is considered a sprint buster and should be scheduled in the
      current sprint.
    - For non-critical findings, the ticket should be scheduled no later than the next PI.

NOTE: As of this writing (Aug 2022), the BFD SNYK dashboard is not configured to report SNYK Code findings so those are
not considered in this runbook. If BFD adopts SNYK Code, this runbook should be updated to reflect the process for
resolving SNYK Code findings.