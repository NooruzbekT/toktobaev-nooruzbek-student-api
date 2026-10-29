// Jenkins declarative pipeline for the Student API by Nooruzbek Toktobaev
// Stages: Checkout -> Build -> Test -> Docker Build -> Verify.
// A failing stage (compile error, failing test) stops the pipeline: later stages are skipped.
pipeline {
    agent any

    tools {
        jdk 'jdk21'
    }

    environment {
        STUDENT_NAME = 'Nooruzbek Toktobaev'
        IMAGE_NAME   = 'toktobaev-nooruzbek-student-api'
        IMAGE_TAG    = 'v1.1-toktobaev-nooruzbek'
    }

    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: 'Git branch to build')
    }

    // GitHub cannot reach a Jenkins on localhost with a webhook, so Jenkins polls GitHub every ~2 minutes
    triggers {
        pollSCM('H/2 * * * *')
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps {
                // Explicit branch name (instead of the job's "*/${BRANCH}" pattern):
                // SCM polling does not expand parameters, so it needs the real branch recorded here
                checkout scmGit(
                    branches: [[name: "*/${params.BRANCH}"]],
                    userRemoteConfigs: [[url: 'https://github.com/NooruzbekT/toktobaev-nooruzbek-student-api.git',
                                         credentialsId: 'github-token']])
                echo "Build by ${STUDENT_NAME} (branch ${params.BRANCH})"
                sh 'git log -1 --format="Commit %h by %an: %s"'
            }
        }

        stage('Build') {
            steps {
                echo "Compiling Student API of ${STUDENT_NAME} with Gradle"
                sh 'chmod +x gradlew'
                sh './gradlew clean assemble --no-daemon'
            }
        }

        stage('Test') {
            steps {
                echo "Running automated tests for ${STUDENT_NAME}"
                sh './gradlew test --no-daemon'
            }
            post {
                always {
                    junit allowEmptyResults: false, testResults: 'build/test-results/test/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                echo "Building image ${IMAGE_NAME}:${IMAGE_TAG} for ${STUDENT_NAME}"
                sh 'docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .'
            }
        }

        stage('Verify') {
            steps {
                sh 'docker image inspect ${IMAGE_NAME}:${IMAGE_TAG} --format "Image {{.RepoTags}} created {{.Created}} size {{.Size}} bytes"'
                sh 'docker images | grep ${IMAGE_NAME}'
                echo "Verify OK: image ${IMAGE_NAME}:${IMAGE_TAG} exists (student: ${STUDENT_NAME})"
            }
        }
    }

    post {
        success {
            echo "SUCCESS: pipeline of ${STUDENT_NAME} finished, image ${IMAGE_NAME}:${IMAGE_TAG} is ready"
        }
        failure {
            echo "FAILURE: pipeline of ${STUDENT_NAME} failed, see the stage log above"
        }
    }
}
