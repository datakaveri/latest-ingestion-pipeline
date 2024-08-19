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
    stage('Continuous Deployment') {
      when {
        allOf {
          anyOf {
            changeset "vertx/docker/**"
            changeset "vertx/docs/**"
            changeset "vertx/pom.xml"
            changeset "vertx/src/main/**"
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
                devImage.push("5.6.0-alpha-${env.GIT_HASH}")
                deplImage.push("5.6.0-alpha-${env.GIT_HASH}")
              }
            }
          }
        }
        stage('Docker Swarm deployment') {
          steps {
            script {
              sh "ssh azureuser@docker-swarm 'docker service update lip_lip --image ghcr.io/datakaveri/lip-depl:5.6.0-alpha-${env.GIT_HASH}'"
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