pipeline {
  environment {
    devRegistry = 'ghcr.io/datakaveri/rs-dev'
    deplRegistry = 'ghcr.io/datakaveri/rs-depl'
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
          echo 'Pulled - ' + env.GIT_BRANCH
          devImage = docker.build(devRegistry, "-f vertx/docker/dev.dockerfile .")
          deplImage = docker.build(deplRegistry, "-f vertx/docker/depl.dockerfile .")
        }
      }
    }
    stage('Continuous Deployment') {
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
            return env.GIT_BRANCH == 'origin/master';
          }
        }
      }
      stages {
        stage('Push Images') {
          steps {
            script {
              docker.withRegistry( registryUri, registryCredential ) {
                devImage.push("5.0.0-${env.GIT_HASH}")
                deplImage.push("5.0.0-${env.GIT_HASH}")
              }
            }
          }
        }
        stage('Docker Swarm deployment') {
          steps {
            script {
              sh "ssh azureuser@docker-swarm 'docker service update lip_lip --image ghcr.io/datakaveri/lip-depl:5.0.0-${env.GIT_HASH}'"
              sh 'sleep 10'
            }
          }
          post {
            failure {
              error "Failed to deploy image in Docker Swarm"
            }
          }          
        }
      }
      post {
        failure {
          script {
            if (env.GIT_BRANCH == 'origin/master') {
              emailext recipientProviders: [buildUser(), developers()], to: '$RS_RECIPIENTS, $DEFAULT_RECIPIENTS', subject: '$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!', body: '''$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS:
              Check console output at $BUILD_URL to view the results.'''
            }
          }
        }
      }
    }
  }
}
