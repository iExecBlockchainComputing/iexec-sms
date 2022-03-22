@Library('global-jenkins-library@feature/external-iexec-oci') _
buildJavaProject(
        buildInfo: getBuildInfo(),
        integrationTestsEnvVars: [],
        shouldPublishJars: true,
        shouldPublishDockerImages: true,
        dockerfileDir: 'build/resources/main',
        dockerfileFilename: 'Dockerfile.untrusted',
        preDevelopVisibility: 'iex.ec',
        developVisibility: 'iex.ec',
        preProductionVisibility: 'docker.io',
        productionVisibility: 'docker.io')
