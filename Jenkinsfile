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

stage("Validate configuration") {
    def config = [
            nativeImage:     "docker-regis.iex.ec/$repositoryName:$buildInfo.imageTag",
            imageName:       repositoryName,
            imageTag:        buildInfo.imageTag,
            sconifyArgsPath: './docker/sconify.args',
            sconifyVersion:  '5.7.0-wal'
    ]

    if (config == null) {
        error('Build requires proper config')
    }
    for (key in ["imageName", "imageTag", "nativeImage", "sconifyArgsPath"]) {
        if (config[key] == null || config[key] == '') {
            error "Build parameters require $key"
        }
    }
    for (key in config.keySet()) {
        if (!["imageName", "imageTag", "nativeImage", "sconifyArgsPath", "sconifyVersion"].contains(key)) {
            error "Unknown key $key does not belong to [imageName, imageTag, nativeImage, sconifyArgsPath, sconifyVersion]"
        }
    }
    imageName       = config.imageName
    imageTag        = config.imageTag
    nativeImage     = config.nativeImage
    sconifyArgsPath = config.sconifyArgsPath

    sconifyVersion = config.sconifyVersion
    if (sconifyVersion == null || sconifyVersion == '') {
        echo 'Falling back to default sconify tool version'
        sconifyVersion = DEFAULT_SCONIFY_VERSION
    }

    echo "imageName       $imageName"
    echo "imageTag        $imageTag"
    echo "nativeImage     $nativeImage"
    echo "sconifyArgsPath $sconifyArgsPath"
    echo "sconifyVersion  $sconifyVersion"
}

stage('Trigger "unlocked" TEE debug image build') {
    sconeSigning(
            IMG_FROM: nativeImage,
            IMG_TO:   "docker-regis.iex.ec/$imageName-unlocked:$imageTag-sconify-$sconifyVersion-debug",
            SCRIPT_CONFIG: sconifyArgsPath,
            SCONE_IMG_NAME: 'sconecuratedimages/iexec-sconify-image',
            SCONE_IMG_VERS: sconifyVersion,
            FLAVOR: 'DEBUG'
    )
}