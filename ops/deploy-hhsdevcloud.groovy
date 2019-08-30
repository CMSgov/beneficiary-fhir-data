#!/usr/bin/env groovy

/**
 * <p>
 * This script will be run by Jenkins when building this repository. Specifically, it
 * contains methods that will handle deploying the applications to the environment, such
 * as {@link #deploy(BuildResult)}.
 * </p>
 */


/**
 * Deploys to the hhsdevcloud/"old sandbox" environment.
 *
 * @param appBuildResults the {@link AppBuildResults} containing the paths to the app binaries that were built
 * @throws RuntimeException An exception will be bubbled up if the deploy tooling returns a non-zero exit code.
 */
def deploy(AppBuildResults appBuildResults) {
	echo 'Deploy to the hhsdevcloud environment is not yet implemented.'
}

return this
