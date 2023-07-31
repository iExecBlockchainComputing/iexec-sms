@Library('global-jenkins-library@2.6.0') _

String repositoryName = 'iexec-sms'

buildInfo = getBuildInfo()

// Override properties defined in getBuildInfo and add parameters
// Define BUILD_TEE parameter for non-PR builds (isPullRequestBuild is false)
if (!buildInfo.isPullRequestBuild) {
  properties([
    buildDiscarder(logRotator(numToKeepStr: '10')),
    parameters([booleanParam(description: 'Build TEE images', name: 'BUILD_TEE')])
  ])
}

buildJavaProject(
        buildInfo: buildInfo,
        integrationTestsEnvVars: [],
        shouldPublishJars: true,
        shouldPublishDockerImages: true,
        dockerfileDir: '.',
        buildContext: '.',
        preDevelopVisibility: 'iex.ec',
        developVisibility: 'iex.ec',
        preProductionVisibility: 'docker.io',
        productionVisibility: 'docker.io')

// BUILD_TEE parameter only exists for non-PR builds (isPullRequestBuild is false)
// If BUILD_TEE is false, TEE builds won't be executed and we return here
if (!buildInfo.isPullRequestBuild && !params.BUILD_TEE) {
  return
}

sconeBuildUnlocked(
        nativeImage:     "docker-regis.iex.ec/$repositoryName:$buildInfo.imageTag",
        imageName:       repositoryName,
        imageTag:        buildInfo.imageTag,
        sconifyArgsPath: './docker/sconify.args',
        sconifyImage:    'sconecuratedimages/iexec-sconify-image',
        sconifyVersion:  '5.7.0-wal'
)
