@Library('global-jenkins-library@2.2.1') _

String repositoryName = 'iexec-sms'

buildInfo = getBuildInfo()

buildJavaProject(
        buildInfo: buildInfo,
        integrationTestsEnvVars: [],
        shouldPublishJars: true,
        shouldPublishDockerImages: true,
        buildContext: '.',
        preDevelopVisibility: 'iex.ec',
        developVisibility: 'iex.ec',
        preProductionVisibility: 'docker.io',
        productionVisibility: 'docker.io')

sconeBuildUnlocked(
        nativeImage:     "docker-regis.iex.ec/$repositoryName:$buildInfo.imageTag",
        imageName:       repositoryName,
        imageTag:        buildInfo.imageTag,
        sconifyArgsPath: './docker/sconify.args',
        sconifyImage:    'sconecuratedimages/iexec-sconify-image',
        sconifyVersion:  '5.7.0-wal'
)
