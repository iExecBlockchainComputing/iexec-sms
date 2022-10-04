@Library('global-jenkins-library@2.2.0') _

String repositoryName = 'iexec-sms'

buildJavaProject(
        buildInfo: getBuildInfo(),
        integrationTestsEnvVars: [],
        shouldPublishJars: true,
        shouldPublishDockerImages: true,
        dockerfileDir: 'build/resources/main',
        dockerfileFilename: 'Dockerfile.untrusted',
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
        sconifyVersion:  '5.7.0-wal'
)
