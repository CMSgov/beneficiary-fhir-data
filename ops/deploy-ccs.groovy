#!/usr/bin/env groovy

/**
 * <p>
 * This script will be run by Jenkins when building this repository. Specifically, it
 * contains methods that will handle deploying the applications to the environment, such
 * as {@link #deploy(String,String,BuildResult)}.
 * </p>
 */


/**
 * Models the results of a call to {@link #buildAppAmis}: contains the IDs of the AMIs that were built.
 */
class AmiIds implements Serializable {
	/**
	 * <p>
	 * The ID of the base "platinum" AMI to use for new instances/AMIs, or <code>null</code> if such an AMI does not yet exist.
	 * </p>
	 * <p>
	 * Why "platinum"? Because it's essentially a respin of the CCS environment's "gold master", with
	 * updates and additional tooling and configuration applied.
	 * </p>
	 */
	String platinumAmiId

	/**
	 * The ID of the AMI that will run the BFD Pipeline service, or <code>null</code> if such an AMI does not yet exist.
	 */
	String bfdPipelineAmiId

	/**
	 * The ID of the AMI that will run the BFD Server service, or <code>null</code> if such an AMI does not yet exist.
	 */
	String bfdServerAmiId
}

/**
 * Finds the IDs of the latest BFD AMIs (if any) in the environment.
 *
 * @return a new {@link AmiIds} instance detailing the already-existing, latest AMIs (if any) that are now available for use
 * @throws RuntimeException An exception will be bubbled up if the AMI-builder tooling returns a non-zero exit code.
 */
def findAmis() {
	echo 'AMI finding in the CCS environment is not yet implemented.'
	throw new UnsupportedOperationException('AMI finding in the CCS environment is not yet implemented.')

	// In the brave, wonderful future, though, this would do something like the following:
	// sh 'something dostuff'
	// return new AmiIds(
	// 	platinumAmiId: 'fizz, as captured/parsed from the AWS CLI or Anisble output',
	// 	bfdPipelineAmiId: 'foo, as captured/parsed from the AWS CLI or Anisble output',
	// 	bfdServerAmiId: 'bar, as captured/parsed from the AWS CLI or Anisble output'
	// )
}

/**
 * <p>
 * Builds the base "platinum" AMI to use for new instances/AMIs.
 * </p>
 * <p>
 * Why "platinum"? Because it's essentially a respin of the CCS environment's "gold master", with
 * updates and additional tooling and configuration applied.
 * </p>
 *
 * @param amiIds an {@link AmiIds} instance detailing the IDs of the AMIs that already exist
 * @return a new {@link AmiIds} instance detailing the shiny new AMI that is now available for use, and any other that were already available
 * @throws RuntimeException An exception will be bubbled up if the AMI-builder tooling returns a non-zero exit code.
 */
def buildPlatinumAmi(AmiIds amiIds) {
	echo 'Platinum AMI building in the CCS environment is not yet implemented.'
	throw new UnsupportedOperationException('Platinum AMI building in the CCS environment is not yet implemented.')

	// In the brave, wonderful future, though, this would do something like the following:
	// sh 'packer dostuff'
	// return new AmiIds(
	// 	platinumAmiId: 'foo, as captured/parsed from the Packer output',
	// 	bfdPipelineAmiId: amiIds.bfdPipelineAmiId,
	// 	bfdServerAmiId: amiIds.bfdServerAmiId
	// )
}

/**
 * Deploys/redeploys Jenkins and related systems to the management environment.
 *
 * @param amiIds an {@link AmiIds} instance detailing the IDs of the AMIs that should be used
 * @throws RuntimeException An exception will be bubbled up if the deploy tooling returns a non-zero exit code.
 */
def deployManagement(AmiIds amiIds) {
	echo 'Deploy to the CCS mgmt environment is not yet implemented.'
	throw new UnsupportedOperationException('Deploy to the CCS mgmt environment is not yet implemented.')
}

/**
 * Builds the BFD Pipeline and BFD Server AMIs.
 *
 * @param amiIds an {@link AmiIds} instance detailing the IDs of the AMIs that already exist
 * @param appBuildResults the {@link AppBuildResults} containing the paths to the app binaries that were built
 * @return a new {@link AmiIds} instance detailing the shiny new AMIs that are now available for use
 * @throws RuntimeException An exception will be bubbled up if the AMI-builder tooling returns a non-zero exit code.
 */
def buildAppAmis(AmiIds amiIds, AppBuildResults appBuildResults) {
	echo 'AMI building in the CCS environment is not yet implemented.'
	throw new UnsupportedOperationException('AMI building in the CCS environment is not yet implemented.')

	// In the brave, wonderful future, though, this would do something like the following:
	// sh 'packer dostuff'
	// return new AmiIds(
	// 	platinumAmiId: amiIds.platinumAmiId,
	// 	bfdPipelineAmiId: 'foo, as captured/parsed from the Packer output',
	// 	bfdServerAmiId: 'bar, as captured/parsed from the Packer output'
	// )
}

/**
 * Deploys to the specified environment.
 *
 * @param envId the ID of the environment to deploy to
 * @param amiIds an {@link AmiIds} instance detailing the IDs of the AMIs that should be used
 * @param appBuildResults (not used in the CCS environment; this stuff is all baked into the AMIs there, instead)
 * @throws RuntimeException An exception will be bubbled up if the deploy tooling returns a non-zero exit code.
 */
def deploy(String envId, AmiIds amiIds, AppBuildResults appBuildResults) {
	echo 'Deploy to the CCS environment is not yet implemented.'
	throw new UnsupportedOperationException('Deploy to the CCS environment is not yet implemented.')
}

return this
