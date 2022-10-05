@Library('global-jenkins-library@2.2.0') _

String repositoryName = 'iexec-sms'

buildInfo = getBuildInfo()

buildJavaProject(
        buildInfo: buildInfo,
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

stage('Trigger TEE debug image build') {
    def nativeImage     = "docker-regis.iex.ec/$repositoryName:$buildInfo.imageTag"
    def imageName       = repositoryName
    def imageTag        = buildInfo.imageTag
    def sconifyArgsPath = './docker/sconify.args'
    def sconifyVersion  = '5.7.0-wal'

    sconeSigning(
            IMG_FROM: nativeImage,
            IMG_TO:   "docker-regis.iex.ec/$imageName-unlocked:$imageTag-sconify-$sconifyVersion-debug",
            SCRIPT_CONFIG: sconifyArgsPath,
            SCONE_IMG_NAME: 'sconecuratedimages/iexec-sconify-image',
            SCONE_IMG_VERS: sconifyVersion,
            FLAVOR: 'DEBUG'
    )
}
