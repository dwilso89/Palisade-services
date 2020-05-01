/*
 * Copyright 2020 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


podTemplate(yaml: '''
apiVersion: v1
kind: Pod
metadata:
    name: dind
spec:
  affinity:
    nodeAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 1
        preference:
          matchExpressions:
          - key: palisade-node-name
            operator: In
            values: 
            - node1
            - node2
            - node3
  containers:
  - name: docker-cmds
    image: docker:1.12.6
    imagePullPolicy: IfNotPresent
    command:
    - sleep
    args:
    - 99d
    resources:
        requests:
            cpu: 10m
            memory: 256Mi    
    env:
      - name: DOCKER_HOST
        value: tcp://localhost:2375
        
  - name: hadolint
    image: hadolint/hadolint:latest-debian@sha256:15016b18964c5e623bd2677661a0be3c00ffa85ef3129b11acf814000872861e
    imagePullPolicy: Always
    command:
        - cat
    tty: true
    
  - name: maven
    image: 779921734503.dkr.ecr.eu-west-1.amazonaws.com/docker-jnlp-slave-image:INFRA
    imagePullPolicy: IfNotPresent
    command: ['cat']
    tty: true
    env:
    - name: TILLER_NAMESPACE
      value: tiller
    - name: HELM_HOST
      value: :44134
    volumeMounts:
      - mountPath: /var/run
        name: docker-sock
  volumes:
    - name: docker-graph-storage
      emptyDir: {}
    - name: docker-sock
      hostPath:
         path: /var/run
''') {
    node(POD_LABEL) {
        stage('Bootstrap') {
            echo sh(script: 'env|sort', returnStdout: true)
        }
        stage('Unit Tests, Checkstyle and Install') {
            //Repositories must get built in their own directory, they can be 'cd' back into later on
            dir ('Palisade-services') {
                git url: 'https://github.com/gchq/Palisade-services.git'
                sh "git fetch origin develop"
                // CHANGE_BRANCH will be null unless you are building a PR, in which case it'll become your original branch name, i.e pal-xxx
                // If CHANGE_BRANCH is null, git will then try to build BRANCH_NAME which is pal-xxx, and if the branch doesnt exist it will default back to develop
                sh "git checkout ${env.CHANGE_BRANCH} || git checkout ${env.BRANCH_NAME} || git checkout develop"
                container('docker-cmds') {
                    configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                        sh 'mvn -s $MAVEN_SETTINGS install'
                    }
                }
            }
        }
        stage('Integration Tests') {
            dir ('Palisade-integration-tests') {
            git url: 'https://github.com/gchq/Palisade-integration-tests.git'
            sh "git fetch origin develop"
            // CHANGE_BRANCH will be null unless you are building a PR, in which case it'll become your original branch name, i.e pal-xxx
            // If CHANGE_BRANCH is null, git will then try to build BRANCH_NAME which is pal-xxx, and if the branch doesnt exist it will default back to develop
            sh "git checkout ${env.CHANGE_BRANCH} || git checkout ${env.BRANCH_NAME} || git checkout develop"
                container('docker-cmds') {
                    configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                        sh 'mvn -s $MAVEN_SETTINGS install'
                    }
                }
            }
        }
        stage('SonarQube analysis') {
            dir ('Palisade-services') {
                container('docker-cmds') {
                    withCredentials([string(credentialsId: "${env.SQ_WEB_HOOK}", variable: 'SONARQUBE_WEBHOOK'),
                                     string(credentialsId: "${env.SQ_KEY_STORE_PASS}", variable: 'KEYSTORE_PASS'),
                                     file(credentialsId: "${env.SQ_KEY_STORE}", variable: 'KEYSTORE')]) {
                        configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                            withSonarQubeEnv(installationName: 'sonar') {
                                sh 'mvn -s $MAVEN_SETTINGS org.sonarsource.scanner.maven:sonar-maven-plugin:3.7.0.1746:sonar -Dsonar.projectKey="Palisade-Services-${BRANCH_NAME}" -Dsonar.projectName="Palisade-Services-${BRANCH_NAME}" -Dsonar.webhooks.project=$SONARQUBE_WEBHOOK -Djavax.net.ssl.trustStore=$KEYSTORE -Djavax.net.ssl.trustStorePassword=$KEYSTORE_PASS'
                            }
                        }
                    }
                }
            }
        }
        stage('Hadolinting') {
            dir ('Palisade-services') {
                container('hadolint') {
                    sh 'hadolint */Dockerfile'
                }
            }
        }

        stage('Maven deploy') {
            dir ('Palisade-services') {
                git url: 'https://github.com/gchq/Palisade-integration-tests.git'
                sh "git fetch origin develop"
                // CHANGE_BRANCH will be null unless you are building a PR, in which case it'll become your original branch name, i.e pal-xxx
                // If CHANGE_BRANCH is null, git will then try to build BRANCH_NAME which is pal-xxx, and if the branch doesnt exist it will default back to develop
                sh "git checkout ${env.CHANGE_BRANCH} || git checkout ${env.BRANCH_NAME} || git checkout develop"
                container('maven') {
                    configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                        if (("${env.BRANCH_NAME}" == "develop") ||
                                ("${env.BRANCH_NAME}" == "master")) {
                            sh 'palisade-login'
                            //now extract the public IP addresses that this will be open on
                            sh 'extract-addresses'
                            sh 'mvn -s $MAVEN_SETTINGS deploy -Dmaven.test.skip=true'
                            sh 'helm upgrade --install palisade . --set traefik.install=true,dashboard.install=true   --set global.repository=${ECR_REGISTRY}  --set global.hostname=${EGRESS_ELB} --set global.localMount.enabled=false,global.localMount.volumeHandle=${VOLUME_HANDLE} --namespace dev'
                        } else {
                            sh "echo - no deploy"
                        }
                    }
                }
            }
        }
    }
    // No need to occupy a node
    stage("SonarQube Quality Gate") {
        timeout(time: 1, unit: 'HOURS') {
            // Just in case something goes wrong, pipeline will be killed after a timeout
            def qg = waitForQualityGate()
            // Reuse taskId previously collected by withSonarQubeEnv
            if (qg.status != 'OK') {
                error "Pipeline aborted due to SonarQube quality gate failure: ${qg.status}"
            }
        }
    }
}
