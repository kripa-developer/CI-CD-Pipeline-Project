pipeline {
    agent any

    environment {
        REGISTRY        = "docker.io"
        DOCKERHUB_CREDS = credentials('dockerhub-credentials')     // Jenkins credential ID
        IMAGE_NAME      = "yourdockerhubuser/demo-app"
        IMAGE_TAG       = "${env.GIT_COMMIT.take(7)}"
        KUBECONFIG_CRED = credentials('kubeconfig-file')            // Jenkins "Secret file" credential
        K8S_NAMESPACE   = "demo"
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Unit Test') {
            steps {
                sh 'mvn -B clean test'
            }
        }

        stage('Package') {
            steps {
                sh 'mvn -B package -DskipTests'
                archiveArtifacts artifacts: 'target/demo-app.jar', fingerprint: true
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG} -t ${REGISTRY}/${IMAGE_NAME}:latest ."
            }
        }

        stage('Push Docker Image') {
            steps {
                sh """
                    echo \$DOCKERHUB_CREDS_PSW | docker login ${REGISTRY} -u \$DOCKERHUB_CREDS_USR --password-stdin
                    docker push ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
                    docker push ${REGISTRY}/${IMAGE_NAME}:latest
                """
            }
        }

        stage('Deploy to Kubernetes (Rolling Update)') {
            steps {
                withEnv(["KUBECONFIG=${KUBECONFIG_CRED}"]) {
                    sh """
                        kubectl set image deployment/demo-app \
                          demo-app=${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG} \
                          -n ${K8S_NAMESPACE} --record

                        kubectl rollout status deployment/demo-app -n ${K8S_NAMESPACE} --timeout=180s
                    """
                }
            }
        }
    }

    post {
        failure {
            withEnv(["KUBECONFIG=${KUBECONFIG_CRED}"]) {
                sh "kubectl rollout undo deployment/demo-app -n ${K8S_NAMESPACE} || true"
            }
        }
        always {
            sh 'docker logout ${REGISTRY} || true'
        }
    }
}
