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
  containers:
  - name: hadolint
    image: hadolint/hadolint:latest-debian@sha256:15016b18964c5e623bd2677661a0be3c00ffa85ef3129b11acf814000872861e
    imagePullPolicy: Always
    command:
        - cat
    tty: true  
    
  - name: docker-cmds
    image: 779921734503.dkr.ecr.eu-west-1.amazonaws.com/jnlp-did:INFRA
    imagePullPolicy: IfNotPresent
    resources: 
      requests: 
        cpu: 10m 
        memory: 256Mi     
    command:
    - sleep
    args:
    - 99d
    env:
      - name: DOCKER_HOST
        value: tcp://localhost:2375
        
        
    
  - name: dind-daemon
    image: docker:1.12.6-dind
    resources:
      requests:
        cpu: 20m
        memory: 512Mi
    securityContext:
      privileged: true
    volumeMounts:
      - name: docker-graph-storage
        mountPath: /var/lib/docker

  volumes:
    - name: docker-graph-storage
      emptyDir: {}
''') {
    node(POD_LABEL) {
        def GIT_BRANCH_NAME

        stage('Bootstrap') {
            if (env.CHANGE_BRANCH) {
                GIT_BRANCH_NAME=env.CHANGE_BRANCH
            } else {
                GIT_BRANCH_NAME=env.BRANCH_NAME
            }
            echo sh(script: 'env | sort', returnStdout: true)
        }

        stage('Prerequisites') {
            // If this branch name exists in the repo for a mvn dependency
            // Install that version, rather than pulling from nexus
            dir ('Palisade-common') {
                git url: 'https://github.com/gchq/Palisade-common.git'
                if (sh(script: "git checkout ${GIT_BRANCH_NAME}", returnStatus: true) == 0) {
                    container('docker-cmds') {
                        configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                            sh 'mvn -s $MAVEN_SETTINGS install -P quick'
                        }
                    }
                }
            }
            dir ('Palisade-readers') {
                git url: 'https://github.com/gchq/Palisade-readers.git'
                if (sh(script: "git checkout ${GIT_BRANCH_NAME}", returnStatus: true) == 0) {
                    container('docker-cmds') {
                        configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                            sh 'mvn -s $MAVEN_SETTINGS install -P quick'
                        }
                    }
                }
            }
        }

        stage('Install, Unit Tests, Checkstyle') {
            dir ('Palisade-services') {
                git url: 'https://github.com/gchq/Palisade-services.git'
                sh "git checkout ${GIT_BRANCH_NAME}"
                container('docker-cmds') {
                    configFileProvider([configFile(fileId: "${env.CONFIG_FILE}", variable: 'MAVEN_SETTINGS')]) {
                        sh 'mvn -s $MAVEN_SETTINGS install'
                    }
                }
            }
            echo sh(script: 'env | sort', returnStdout: true)
        }
    }
}
