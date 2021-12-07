@Library('jenkins-library@1.3.0') _
buildJavaProject(
        integrationTestsEnvVars: [],
        shouldPublishJars: true,
        shouldPublishDockerImages: true,
        dockerfileDir: 'build/resources/main',
        dockerfileFilename: "Dockerfile.untrusted",
        buildContext: '.',
        //dockerImageRepositoryName: '',
        preProductionVisibility: 'docker.io',
        productionVisibility: 'docker.io')
