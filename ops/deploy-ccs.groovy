#!/usr/bin/env groovy

/**
 * <p>
 * This script will be run by Jenkins when building this repository. Specifically, it
 * contains methods that will handle deploying the applications to the environment, such
 * as {@link #deploy(String,String,BuildResult)}.
 * </p>
 */


/**
 * Deploys to the specified environment.
 *
 * @param envId the ID of the environment to deploy to
 * @param platinumAmiId the ID of the base "platinum" AMI to use for new instances/AMIs
 * @param appsBuildResult the {@link BuildResult} containing the paths to the app binaries that were built
 * @throws RuntimeException An exception will be bubbled up if the deploy tooling returns a non-zero exit code.
 */
def deploy(String envId, String platinumAmiId, BuildResult appsBuildResult) {
	echo 'Deploy to the CCS environment is not yet implemented.'
	throw new UnsupportedOperationException('Deploy to the CCS environment is not yet implemented.')
}

return this
