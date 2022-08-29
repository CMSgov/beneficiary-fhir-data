/**
 * Wrap the awscli to authenticate with AWS by assuming a specific, fully qualified role. The role
 * must be the id of an available Jenkins secret.
 *
 * In addition to the setting credentials as environment variables for AWS requests, this also
 * sets AWS_REGION, DEFAULT_AWS_REGION, and a special NEXT_AWS_AUTH. NEXT_AWS_AUTH contains the
 * the time at which ~50% of the requested session has elapsed. If this environment variable is
 * populated, the script will only re-authenticate if there is less than half the time left in the
 * current session. By default, this requests a 60 minute session and subsequent calls of this
 * method will only reauthenticate if there are fewer than 30 minutes remaining on the session. This
 * keeps both the Jenkins logs tidier and avoids excessive role assumption as recorded in cloudwatch
 * logs. This is an especially important consideration in tight loops that seek to ensure valid AWS
 * credentials before or after each iteration.
 *
 * TODO: consider transitioning away from wrapped awscli role assumption to something in groovy/java
 * TODO: implement validation safety surrounding e.g. credentialsId, sessionName
 *
 * @param args {@link Map} that optionally includes sessionDurationSeconds, sessionName, awsRegion,
 * and credentialsId
 */
void assumeRole(Map args = [:]) {
	sessionDurationSeconds = args.sessionDurationSeconds ?: 3600
	// default `sessionName` as `env.JOB_NAME` is vaguely sanitized to replace non-alphanumeric
	// chars with '-' AND deduplicates consecutive '-'. This is somewhat more restrictive than the
	// regex used in the STS API for RoleSessionName, i.e. `/[\w+=,.@:\/-]*/`
	// See https://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRole.html
	sessionName = args.sessionName ?: env.JOB_NAME.replaceAll(/[^a-zA-Z\d]/, '-').replaceAll(/[\-]+/, '-')
	credentialsId = args.credentialsId ?: 'bfd-aws-assume-role'
	awsRegion = args.awsRegion ?: 'us-east-1'

	if (env.NEXT_AWS_AUTH == null || java.time.Instant.now() > java.time.Instant.parse(env.NEXT_AWS_AUTH)) {
		echo "Authenticating..."
		withEnv(["DURATION=${sessionDurationSeconds}",
				 "SESSION_NAME=${sessionName}",
				 'AWS_ACCESS_KEY_ID=',
				 'AWS_SECRET_ACCESS_KEY=',
				 'AWS_SESSION_TOKEN=']) {
			withCredentials([string(credentialsId: credentialsId, variable: 'JENKINS_ROLE')]) {
				awsCredentials = sh(
					returnStdout: true,
					script: '''
aws sts assume-role \
  --duration-seconds "$DURATION" \
  --role-arn "$JENKINS_ROLE" \
  --role-session-name "$SESSION_NAME" \
  --output text --query Credentials
'''
				).trim().split(/\s+/)
				// Set nextAuthSeconds to renew through ~50% of original session's duration
				nextAuthSeconds = (sessionDurationSeconds / 2).longValue()
				env.NEXT_AWS_AUTH = java.time.Instant.now().plus(java.time.Duration.ofSeconds(nextAuthSeconds))
				env.AWS_REGION = awsRegion
				env.AWS_DEFAULT_REGION = awsRegion
				env.AWS_ACCESS_KEY_ID = awsCredentials[0]
				env.AWS_SECRET_ACCESS_KEY = awsCredentials[2]
				env.AWS_SESSION_TOKEN = awsCredentials[3]
			}
		}
	}
}
