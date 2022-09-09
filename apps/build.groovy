#!/usr/bin/env groovy

/**
 * <p>
 * This script will be run by Jenkins when building this repository. Specifically, calling
 * the <code>build()</code> method will build the Java applications: Data Pipeline, Data
 * Server, etc.
 * </p>
 */

/**
 * Models the results of a call to {@link #build}: contains the paths to the artifacts that were built.
 */
class AppBuildResults implements Serializable {
	String dbMigratorZip
	String dataPipelineZip
	String dataServerLauncher
	String dataServerWar
}

/**
 * Builds the Java applications and utilities in this directory: Data Pipeline, Data Server, etc.
 *
 * @param verboseMaven when `false`, maven runs with `--quiet` and `--batch-mode` flags
 * @return An {@link AppBuildResults} instance containing the paths to the artifacts that were built.
 * @throws Exception An exception will be bubbled up if the Maven build fails.
 */
def build(boolean verboseMaven) {
	withCredentials([string(credentialsId: 'bfd-aws-account-id', variable: 'ACCOUNT_ID')]) {
		// Set the auth token for aws code artifact
		env.CODEARTIFACT_AUTH_TOKEN = sh(
			returnStdout: true,
			script: '''
aws codeartifact get-authorization-token --domain bfd-mgmt \
 --domain-owner "$ACCOUNT_ID" \
 --output text --query authorizationToken
'''
		).trim()

		// Get our endpoint url for our aws code artifact
		env.CODEARTIFACT_ENDPOINT = sh(
			returnStdout: true,
			script: '''
aws codeartifact get-repository-endpoint \
--domain bfd-mgmt --repository bfd-mgmt \
--format maven --output text
'''
		).trim()
	}

	// Add the authorization token and username for our aws code artifact repository
	sh '''
cat <<EOF > ~/.m2/settings.xml
<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"
xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd\">
    <profiles>
        <profile>
            <id>bfd-mgmt-bfd-mgmt</id>
            <activation>
            <activeByDefault>true</activeByDefault>
            </activation>
            <repositories>
                <repository>
                    <id>bfd-mgmt-bfd-mgmt</id>
                    <url>${CODEARTIFACT_ENDPOINT}</url>
                </repository>
            </repositories>
        </profile>
    </profiles>
    <servers>
        <server>
        <username>aws</username>
        <password>${CODEARTIFACT_AUTH_TOKEN}</password>
        <id>bfd-mgmt-bfd-mgmt</id>
        </server>
    </servers>
</settings>
EOF
'''

	dir ('apps') {
		quietFlags = verboseMaven ? '' : '--quiet --batch-mode'
		sh "mvn ${quietFlags} --threads 1C --update-snapshots -DskipITs -DskipTests -Dmaven.javadoc.skip=true clean verify"
	}

	dir ('apps/bfd-data-fda') {
		quietFlags = verboseMaven ? '' : '--quiet --batch-mode'
		sh "mvn ${quietFlags} --threads 1C verify"
	}

	return new AppBuildResults(
		dbMigratorZip: 'apps/bfd-db-migrator/target/bfd-db-migrator-1.0.0-SNAPSHOT.zip',
		dataPipelineZip: 'apps/bfd-pipeline/bfd-pipeline-app/target/bfd-pipeline-app-1.0.0-SNAPSHOT.zip',
		dataServerLauncher: 'apps/bfd-server/bfd-server-launcher/target/bfd-server-launcher-1.0.0-SNAPSHOT.zip',
		dataServerWar: 'apps/bfd-server/bfd-server-war/target/bfd-server-war-1.0.0-SNAPSHOT.war'
	)
}

return this
