
# Welcome!

We want to ensure a welcoming environment for all of our projects. Our staff follow the [18F Code of Conduct](https://github.com/18F/code-of-conduct/blob/master/code-of-conduct.md) and all contributors should do the same.

Although this is a public repo, contributing to the BFD is for CMS-approved contributors only, not outside contributors.

Contributing to the BFD
---

### Background

BFD exists to enable the CMS Enterprise to drive innovation in data sharing so that beneficiaries and their healthcare partners have the data they need to make informed decisions about their healthcare.

We provide a comprehensive, performant, and trustworthy platform to transform the way that the CMS enterprise shares and uses data by providing [FHIR](https://www.hl7.org/fhir/)-formatted data, including beneficiary demographic, enrollment, and claims data.

Review the [README](https://github.com/CMSgov/beneficiary-fhir-data/blob/master/README.md) for additional information on BFD. 


### Contributing changes

Contributions to the BFD are welcome from any party inside CMS.
Small changes like "good first issues" can be submitted for consideration without any additional process. This [LGTM resource](https://lgtm.com/projects/g/CMSgov/beneficiary-fhir-data/?mode=list) provides candidates for "good first issues". 

Any [substantive change must go though an RFC process](#proposing-substantive-changes) before work on the change itself can start.

Any code changes should be properly commented and accompanied by appropriate unit and integration test as per [DASG Engineering Standards](https://github.com/CMSgov/cms-oeda-dasg/blob/master/policies/engineering_standards.md).

![Workflow diagram for making a contribution to BFD](docs/assets/BFDContributionDiagram.png "Making a Contribution to BFD")

**FAQ**

Q: What kind of changes don't require an RFC?

A: In general bug fixes and small changes that do not affect behavior or meaning. If you're unsure please quickly ask in the [#bfd](https://cmsgov.slack.com/archives/C010WDXAZFZ) channel or in the Scrum of Scrums meeting. If your PR involves a substantial change, it will be rejected and you will be asked to go through the RFC process.

Q: How do I know what “first issues” are up for grabs? 

A: First issues are tracked on the BFD’s Jira board with the "Good_First_Issue" label: <https://jira.cms.gov/issues/?jql=project%20%3D%20BFD%20AND%20status%20%3D%20Open%20AND%20labels%20%3D%20Good_First_Issue>

### Proposing substantive changes

Substantive changes need to go through a design process involving the core team.
Opening an RFC provides a path for this inclusion and process.
Start an RFC by copying the file [`rfcs/0000-template.md`](rfcs/0000-template.md) to `rfcs/0000-<my idea>.md` and fill in the details. 
Open a PR using the RFC template [submit a pull request](#opening-a-pr).
The RFC will remain open for a 2 week period, at the end of which a go/no-go meeting will be held.
If approved by at least two core team members and there are no outstanding reservations, the RFC will be accepted.
Once accepted the author of the RFC and their team can scope the work within their regular process. Link or reference the RFC in the related JIRA ticket.
The core team will respond with design feedback, and the author should be prepared to revise it in response.

**FAQ**

Q: What qualifies as a substantive change?

A: There is no strict definition, however examples of substantive changes are:

1. Any change to or addition of an API endpoint (either URL or response) that is not a bug fix.
2. Changes that affect the ingestion of data into the BFD (the ETL process). 
3. Changes that significantly alter the structure of the codebase.


Q: What if I'm not sure if I need an RFC?

A: Reach out to the [#bfd](https://cmsgov.slack.com/archives/C010WDXAZFZ) channel or ask in the Scrum of Scrums meeting and see what the BFD team thinks.


Q: How should I prepare for an RFC?

A: Bring the idea to the [#bfd](https://cmsgov.slack.com/archives/C010WDXAZFZ) channel or the Scrum of Scrums meeting and talk it over with the core team.


Q: What if my RFC is not accepted?

A: It will be closed, but can be reopened if it is updated to address the items that prevented it's acceptance.


Q: What if my team doesn’t have the resources to implement our accepted RFC? 

A: Anyone can coordinate with you and the core team to take over the work. 

### Getting started

Going to work on this project? Great! There are currently two documented methods for getting a local environment up and running to get you setup for development. 

[Getting started on BFD via Docker](README.md) 

#### Opening A PR

To contribute work back to the BFD your branch must be pushed to the `CMSgov/beneficiary-fhir-data` repository.
```
git push origin <your username>/<your feature name>
# To make sure "origin" points to CMSgov/beneficiary-fhir-data run
# git remote -v
# if a different remote points to CMSgov/beneficiary-fhir-data
# replace "origin" with that remotes name in the origional command
```
In order to obtain permission to do this contact a github administrator or reach out on [#bfd](https://cmsgov.slack.com/archives/C010WDXAZFZ).
Once pushed, [open a pull request](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request) and use this [PR template](https://github.com/CMSgov/cms-oeda-dasg) found in .github

Please fill out each section of the body of the PR or quickly justify why the section does not apply to this particular change.
Reviewers will automatically be suggested or requested based on the projects CODEOWNERS file, feel free to add other reviewers if desired.
Once all automated checks pass and two reviewers have approved your pull request, a code owner from the core team will do a final review and make the decision to merge it in or not.

If you have any questions feel free to reach out on the [#bfd](https://cmsgov.slack.com/archives/C010WDXAZFZ) channel in CMS slack!

**FAQ**

Q: What if the core team rejects my PR?

A: The BFD core team will commit to never rejecting a PR without providing a path forward. The developer who put up the PR should review any feedback, and discuss with their product manager the scope of the work that is now outstanding.
