#!/usr/bin/env groovy

/**
 * <p>
 * This script will be run by Jenkins when building this repository. Specifically, calling
 * the <code>build()</code> method will build the Java applications: Data Pipeline, Data
 * Server, etc.
 * </p>
 */

/**
 * Models the results of a call to {@link #build}: contains the paths to the artifacts that were built or fetched.
 */
class AppResults implements Serializable {
    String dbMigratorZip
    String dataPipelineZip
    String dataServerLauncher
    String dataServerWar
}

def setupCodeArtifactEnvironment() {
    env.CA_DOMAIN     = "bfd-mgmt"
    env.CA_NAMESPACE  = "gov.cms.bfd"
    env.CA_REPOSITORY = "bfd-mgmt"

    awsAuth.assumeRole()
    withCredentials([string(credentialsId: 'bfd-aws-account-id', variable: 'AWS_ACCOUNT_ID')]) {
        // Set the auth token for aws code artifact
        env.CODEARTIFACT_AUTH_TOKEN = sh(
            returnStdout: true,
            script: '''
aws codeartifact get-authorization-token --domain bfd-mgmt \
 --domain-owner "$AWS_ACCOUNT_ID" \
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
}

def fetch() {
    env.BFD_RELEASE = sh(returnStdout: true, script: "yq '.project.version' ${workspace}/apps/pom.xml").trim()
    setupCodeArtifactEnvironment()
    archives = [
        'bfd-db-migrator':     "bfd-db-migrator-${env.BFD_RELEASE}.zip",
        'bfd-pipeline-app':    "bfd-pipeline-app-${env.BFD_RELEASE}.zip",
        'bfd-server-launcher': "bfd-server-launcher-${env.BFD_RELEASE}.zip",
        'bfd-server-war':      "bfd-server-war-${env.BFD_RELEASE}.war"
    ]

    // Create a "dist" directory at the root workspace level
    env.DIST_DIR = "${workspace}/dist"
    sh 'mkdir -p "$DIST_DIR"'

    dir(env.DIST_DIR) {
        archives.each{k,v->
            withCredentials([string(credentialsId: 'bfd-aws-account-id', variable: 'AWS_ACCOUNT_ID')]) {
                withEnv(["PACKAGE_NAME=${k}", "PACKAGE_ASSET=${v}"]) {
                    sh '''aws codeartifact get-package-version-asset \
--domain-owner "$AWS_ACCOUNT_ID" \
--domain "$CA_DOMAIN" \
--repository "$CA_REPOSITORY" \
--asset "$PACKAGE_ASSET" \
--package-version "$BFD_RELEASE" \
--package "$PACKAGE_NAME" \
--namespace "$CA_NAMESPACE" \
--format maven \
--region "$AWS_REGION" \
"${DIST_DIR}/${PACKAGE_ASSET}"
'''
                }
            }
        }
    }

    return new AppResults(
        dbMigratorZip:      "${env.DIST_DIR}/${archives['bfd-db-migrator']}",
        dataPipelineZip:    "${env.DIST_DIR}/${archives['bfd-pipeline-app']}",
        dataServerLauncher: "${env.DIST_DIR}/${archives['bfd-server-launcher']}",
        dataServerWar:      "${env.DIST_DIR}/${archives['bfd-server-war']}"
    )
}

/**
 * Builds the Java applications and utilities in this directory: Data Pipeline, Data Server, etc.
 *
 * @param verboseMaven when `false`, maven runs with `--quiet` and `--batch-mode` flags
 * @param runTests when `true`, runs unit and integration tests, default is `false`
 * @return An {@link AppResults} instance containing the paths to the artifacts that were built.
 * @throws Exception An exception will be bubbled up if the Maven build fails.
 */
def build(boolean verboseMaven, boolean runTests = false) {
    setupCodeArtifactEnvironment()
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
                   <releases>
                   <enabled>false</enabled>
                   </releases>
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
        withEnv(['MAVEN_OPTS=-Xms1024m -Xmx1024m']) {
            if (runTests) {
                sh "mvn ${quietFlags} --threads 1C -Dmaven.javadoc.skip=true -Dcheckstyle.skip -Dmaven.build.cache.enabled=false -Dmaven.jacoco.skip=false clean install"
            } else {
                sh "mvn ${quietFlags} --threads 1C --update-snapshots -DskipITs -DskipTests -Dmaven.javadoc.skip=true -Dmaven.build.cache.enabled=false clean verify"
            }
        }
    }

    return new AppResults(
        dbMigratorZip:      sh(returnStdout: true, script: """find "${workspace}/apps/bfd-db-migrator/target" -type f -name bfd-db-migrator-*.zip""").trim(),
        dataPipelineZip:    sh(returnStdout: true, script: """find "${workspace}/apps/bfd-pipeline/bfd-pipeline-app/target" -type f -name bfd-pipeline-app-*.zip""").trim(),
        dataServerLauncher: sh(returnStdout: true, script: """find "${workspace}/apps/bfd-server/bfd-server-launcher/target" -type f -name bfd-server-launcher-*.zip""").trim(),
        dataServerWar:      sh(returnStdout: true, script: """find "${workspace}/apps/bfd-server/bfd-server-war/target" -type f -name bfd-server-war-*.war""").trim()
    )
}

return this
