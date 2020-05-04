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
  - name: docker-cmds
    image: docker:1.12.6
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
    image: docker:19.03.1-dind
    command:
    - docker:dind docker
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
                        sh 'docker images'
                    }
                }
            }
        }
    }
}
