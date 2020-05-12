<!--

--- PR Hygiene Checklist ---

1. Make sure the changeset can be reviewed, keep it's scope and size succinct 
2. Make sure your branch is from your fork and has a meaningful name
3. Update the PR title: `BLUEBUTTON-99999 Add Awesomeness`
4. Edit the text below - do not leave placeholders in the text.
4.1. Remove sections that you don't feel apply
5. Add any other details that will be helpful for the reviewers: details description, screenshots, etc
6. Request a review from someone/multiple someones
7. <optional> Review your changes yourself and write up any comments / concerns as if you were reviewing someone else's code.
-->

### Change Details

<!-- Add detailed discussion of changes here: -->
<!-- This is likely a summary, or the complete contents, of your commit messages -->

Currently, on BFD developement environment, bfd server is started on a port randomly picked in range 8000:9999 to avoid port conflict when multiple server instances are up and running on same host, there is some use case where a fix port is expected, e.g. contributing local docker compose services definitions expects bfd on port 9954, a change is proposed to allow both use cases benefit from the random port reservation scheme, the change is: to expose min port and max port of the port range as maven command line parameters and with min port defaults to 8000, and max port defaults to 9999, this way, for use case that expect fixed port, server can be started from maven command line with -Dmin_port=8000 -Dmax_port=9999, no change is needed for existing use cases that prefer random ports reserve, for fixed port use case, such as contributing local docker compose definition, it only need to call maven command line with -Dmin_port=9954 -Dmax_port=9999.

### Acceptance Validation

<!-- What should reviewers look for to determine completeness -->

<!-- Insert screenshots if applicable (drag images here) -->

After the change, all bfd tests including integration tests will run as is, meanwhile fixed port use case will run in paralelle without port conflicts, and no tweaking needed on bluebutton side or bfd side.


### Feedback Requested

<!-- What type of feedback you want from your reviewers? -->

### External References

<!-- For example: replace xxx with the JIRA ticket number: -->

- [BLUEBUTTON-xxx](https://jira.cms.gov/browse/BLUEBUTTON-xxx)
 
 [BLUEBUTTON-12](https://jira.cms.gov/browse/BB2-12)
 

### Security Implications

<!-- Does the change deal with PII/PHI at all? What should reviewers look for in
terms of security concerns? -->

- [ ] new software dependencies

<!-- If yes, list the new dependencies and briefly note any relevant security impacts -->

- [ ] altered security controls

<!-- If yes, what security controls or supporting software are affected? -->

- [ ] new data stored or transmitted

<!-- If yes, what new data are we storing or transmitting? Is the data considered PII/PHI? -->

- [ ] security checklist is completed for this change

<!-- If yes, provide a link to the security checklist in Confluence here. -->

- [ ] requires more information or team discussion to evaluate security implications
<!-- Use this to indicate you're unsure how this change may impact system security and would like to solicit the team's feedback. Optionally, provide background information regarding your questions and concerns. -->

