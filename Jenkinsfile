@Library('hmcts-pipeline-library') _

pipeline {
    agent { label 'jenkins-agent' }

    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
    }

    environment {
        DOCKER_REGISTRY  = 'hmcts.azurecr.io'
        IMAGE_BACKEND    = "${DOCKER_REGISTRY}/task-management-backend"
        IMAGE_FRONTEND   = "${DOCKER_REGISTRY}/task-management-frontend"
        IMAGE_TAG        = "${env.BUILD_NUMBER}-${env.GIT_COMMIT[0..6]}"
        AKS_CLUSTER      = credentials('aks-cluster-name')
        AKS_RESOURCE_GROUP = credentials('aks-resource-group')
        DYNATRACE_API_TOKEN = credentials('dynatrace-api-token')
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Backend: Build & Test') {
            steps {
                dir('backend') {
                    sh './gradlew clean test jacocoTestReport --no-daemon'
                }
            }
            post {
                always {
                    junit 'backend/build/test-results/**/*.xml'
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'backend/build/reports/jacoco/test/html',
                        reportFiles: 'index.html',
                        reportName: 'Backend Coverage Report'
                    ])
                }
            }
        }

        stage('Frontend: Install & Test') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npm test -- --ci --coverage'
                }
            }
            post {
                always {
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'frontend/coverage/lcov-report',
                        reportFiles: 'index.html',
                        reportName: 'Frontend Coverage Report'
                    ])
                }
            }
        }

        stage('Security Scan') {
            parallel {
                stage('OWASP Dependency Check') {
                    steps {
                        dir('backend') {
                            sh './gradlew dependencyCheckAnalyze --no-daemon'
                        }
                    }
                }
                stage('npm Audit') {
                    steps {
                        dir('frontend') {
                            sh 'npm audit --audit-level=high'
                        }
                    }
                }
            }
        }

        stage('Docker Build') {
            when { branch pattern: 'main|develop|release/.*', comparator: 'REGEXP' }
            parallel {
                stage('Build Backend Image') {
                    steps {
                        sh """
                            docker build -t ${IMAGE_BACKEND}:${IMAGE_TAG} -t ${IMAGE_BACKEND}:latest ./backend
                        """
                    }
                }
                stage('Build Frontend Image') {
                    steps {
                        sh """
                            docker build -t ${IMAGE_FRONTEND}:${IMAGE_TAG} -t ${IMAGE_FRONTEND}:latest ./frontend
                        """
                    }
                }
            }
        }

        stage('Push to ACR') {
            when { branch pattern: 'main|develop|release/.*', comparator: 'REGEXP' }
            steps {
                withCredentials([usernamePassword(credentialsId: 'acr-credentials',
                        usernameVariable: 'ACR_USER', passwordVariable: 'ACR_PASS')]) {
                    sh """
                        echo "${ACR_PASS}" | docker login ${DOCKER_REGISTRY} -u "${ACR_USER}" --password-stdin
                        docker push ${IMAGE_BACKEND}:${IMAGE_TAG}
                        docker push ${IMAGE_BACKEND}:latest
                        docker push ${IMAGE_FRONTEND}:${IMAGE_TAG}
                        docker push ${IMAGE_FRONTEND}:latest
                    """
                }
            }
        }

        stage('Terraform Plan') {
            when { branch 'main' }
            steps {
                dir('infrastructure') {
                    withCredentials([azureServicePrincipal('azure-sp')]) {
                        sh '''
                            terraform init
                            terraform plan -out=tfplan \
                              -var="environment=prod" \
                              -var="subscription_id=${AZURE_SUBSCRIPTION_ID}"
                        '''
                    }
                }
            }
        }

        stage('Terraform Apply') {
            when { branch 'main' }
            input { message "Apply Terraform changes to production?" }
            steps {
                dir('infrastructure') {
                    withCredentials([azureServicePrincipal('azure-sp')]) {
                        sh 'terraform apply -auto-approve tfplan'
                    }
                }
            }
        }

        stage('Deploy to AKS') {
            when { branch 'main' }
            steps {
                withCredentials([azureServicePrincipal('azure-sp')]) {
                    sh """
                        az aks get-credentials --resource-group ${AKS_RESOURCE_GROUP} \
                            --name ${AKS_CLUSTER} --overwrite-existing

                        # Update image tags
                        kubectl set image deployment/task-backend \
                            task-backend=${IMAGE_BACKEND}:${IMAGE_TAG}
                        kubectl set image deployment/task-frontend \
                            task-frontend=${IMAGE_FRONTEND}:${IMAGE_TAG}

                        # Wait for rollout
                        kubectl rollout status deployment/task-backend --timeout=5m
                        kubectl rollout status deployment/task-frontend --timeout=5m
                    """
                }
            }
        }

        stage('Dynatrace Deployment Event') {
            when { branch 'main' }
            steps {
                sh """
                    curl -X POST "https://\${DYNATRACE_ENV_URL}/api/v1/events" \
                        -H "Authorization: Api-Token \${DYNATRACE_API_TOKEN}" \
                        -H "Content-Type: application/json" \
                        -d '{
                            "eventType": "CUSTOM_DEPLOYMENT",
                            "attachRules": {"tagRule": [{"meTypes": ["SERVICE"], "tags": [{"context": "CONTEXTLESS", "key": "task-management"}]}]},
                            "deploymentName": "Task Management Service",
                            "deploymentVersion": "${IMAGE_TAG}",
                            "deploymentProject": "HMCTS",
                            "source": "Jenkins"
                        }'
                """
            }
        }
    }

    post {
        success {
            echo "Pipeline succeeded for build ${IMAGE_TAG}"
        }
        failure {
            echo "Pipeline failed for build ${IMAGE_TAG}"
        }
        always {
            cleanWs()
        }
    }
}
