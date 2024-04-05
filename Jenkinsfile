pipeline {
  environment {
    devRegistry = 'ghcr.io/datakaveri/lip-dev'
    deplRegistry = 'ghcr.io/datakaveri/lip-depl'
    registryUri = 'https://ghcr.io'
    registryCredential = 'datakaveri-ghcr'
    GIT_HASH = GIT_COMMIT.take(7)
  }

  agent { 
    node {
      label 'slave1' 
    }
  }

  stages {
    stage('Build images') {
      steps {
        script {
          devImage = docker.build(devRegistry, "-f vertx/docker/dev.dockerfile vertx/")
          deplImage = docker.build(deplRegistry, "-f vertx/docker/depl.dockerfile vertx/")
        }
      }
    }
    stage('Push Images') {
      when {
        allOf {
          anyOf {
            changeset "docker/**"
            changeset "docs/**"
            changeset "pom.xml"
            changeset "src/main/**"
            triggeredBy cause: 'UserIdCause'
          }
          expression {
            return env.GIT_BRANCH == 'origin/5.5.0';
          }
        }
      }
      steps {
        script {
          docker.withRegistry( registryUri, registryCredential ) {
            devImage.push("5.5.0-${env.GIT_HASH}")
            deplImage.push("5.5.0-${env.GIT_HASH}")
          }
        }
      }
    }
  }
}