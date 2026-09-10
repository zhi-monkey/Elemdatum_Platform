pipeline {
  agent {
    node {
      label 'maven-nodejs'
    }

  }
  stages {
    stage('clone code') {
      agent none
      steps {
        container('base') {
          git(url: 'http://210.30.97.174:31000/ms-dev/mineai.git', branch: 'main', changelog: true, poll: false, credentialsId: 'gitlab')
        }

      }
    }

    stage('unit test (pass)') {
      agent none
      steps {
        container('maven') {
          sh 'echo `pass`'
        }

      }
    }

    stage('default-2') {
      parallel {
        stage('maven package') {
          agent none
          steps {
            container('maven') {
              sh '''mvn install
mvn -Dmaven.test.skip=true clean package
ls -lr'''
              sh '''curl -T mineai-gateway/target/mineai-gateway.jar -u generic-1659254649120:96f6ab5058b859e3d4c0c43ad7cbf532b78024c4 "https://adv-dlut-generic.pkg.coding.net/mineAI/generic/mineai-gateway.jar?version=SNAPSHOT-$BUILD_NUMBER"
curl -T mineai-data-manager/target/mineai-data-manager.jar -u generic-1659254649120:96f6ab5058b859e3d4c0c43ad7cbf532b78024c4 "https://adv-dlut-generic.pkg.coding.net/mineAI/generic/mineai-data-manager.jar?version=SNAPSHOT-$BUILD_NUMBER"
curl -T mineai-controller-manager/target/mineai-controller-manager.jar -u generic-1659254649120:96f6ab5058b859e3d4c0c43ad7cbf532b78024c4 "https://adv-dlut-generic.pkg.coding.net/mineAI/generic/mineai-controller-manager.jar?version=SNAPSHOT-$BUILD_NUMBER"
curl -T mineai-model-manager/target/mineai-model-manager.jar -u generic-1659254649120:96f6ab5058b859e3d4c0c43ad7cbf532b78024c4 "https://adv-dlut-generic.pkg.coding.net/mineAI/generic/mineai-model-manager.jar?version=SNAPSHOT-$BUILD_NUMBER"
curl -T mineai-monitor-accessor/target/mineai-monitor-accessor.jar -u generic-1659254649120:96f6ab5058b859e3d4c0c43ad7cbf532b78024c4 "https://adv-dlut-generic.pkg.coding.net/mineAI/generic/mineai-monitor-accessor.jar?version=SNAPSHOT-$BUILD_NUMBER"
curl -T mineai-system/target/mineai-system.jar -u generic-1659254649120:96f6ab5058b859e3d4c0c43ad7cbf532b78024c4 "https://adv-dlut-generic.pkg.coding.net/mineAI/generic/mineai-system.jar?version=SNAPSHOT-$BUILD_NUMBER"
curl -T mineai-worker-manager/target/mineai-worker-manager.jar -u generic-1659254649120:96f6ab5058b859e3d4c0c43ad7cbf532b78024c4 "https://adv-dlut-generic.pkg.coding.net/mineAI/generic/mineai-worker-manager.jar?version=SNAPSHOT-$BUILD_NUMBER"'''
            }

          }
        }

        stage('vite build') {
          agent none
          steps {
            container('nodejs') {
              sh 'cd mineai-ui'
              sh 'npm config set registry https://registry.npm.taobao.org/'
              sh 'yarn config set registry https://registry.npm.taobao.org/'
              sh 'yarn config set cache-folder /root/.yarn'
              sh '''cd mineai-ui
npm config set registry https://registry.npm.taobao.org
npm config set disturl https://npm.taobao.org/dist
yarn install --registry=https://registry.npm.taobao.org
yarn run build
cd ../'''
            }

          }
        }

      }
    }

    stage('build image && push image') {
      parallel {
        stage('build image - controller manager') {
          agent none
          steps {
            container('maven') {
              withCredentials([usernamePassword(credentialsId : 'aliyun-docker-registry' ,passwordVariable : 'DOCKER_PASSWORD' ,usernameVariable : 'DOCKER_USERNAME' ,)]) {
                sh 'echo "$DOCKER_PASSWORD" | docker login $REGISTRY -u "$DOCKER_USERNAME" --password-stdin'
              }

              withEnv(["APP_NAME=mineai-controller-manager"]) {
                sh 'docker build -t ${APP_NAME}:latest -f ${APP_NAME}/Dockerfile ./${APP_NAME}/'
                sh 'docker tag ${APP_NAME}:latest $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:SNAPSHOT-$BUILD_NUMBER'
                sh 'docker push $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:SNAPSHOT-$BUILD_NUMBER'
                sh 'docker tag ${APP_NAME}:latest $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:latest'
                sh 'docker push $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:latest'
              }

            }

          }
        }

        stage('build image - data manager') {
          agent none
          steps {
            container('maven') {
              withCredentials([usernamePassword(credentialsId : 'aliyun-docker-registry' ,passwordVariable : 'DOCKER_PASSWORD' ,usernameVariable : 'DOCKER_USERNAME' ,)]) {
                sh 'echo "$DOCKER_PASSWORD" | docker login $REGISTRY -u "$DOCKER_USERNAME" --password-stdin'
              }

              withEnv(["APP_NAME=mineai-data-manager"]) {
                sh 'docker build -t ${APP_NAME}:latest -f ${APP_NAME}/Dockerfile ./${APP_NAME}/'
                sh 'docker tag ${APP_NAME}:latest $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:SNAPSHOT-$BUILD_NUMBER'
                sh 'docker push $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:SNAPSHOT-$BUILD_NUMBER'
                sh 'docker tag ${APP_NAME}:latest $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:latest'
                sh 'docker push $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:latest'
              }

            }

          }
        }

        stage('build image - gateway') {
          agent none
          steps {
            container('maven') {
              withCredentials([usernamePassword(credentialsId : 'aliyun-docker-registry' ,passwordVariable : 'DOCKER_PASSWORD' ,usernameVariable : 'DOCKER_USERNAME' ,)]) {
                sh 'echo "$DOCKER_PASSWORD" | docker login $REGISTRY -u "$DOCKER_USERNAME" --password-stdin'
              }

              withEnv(["APP_NAME=mineai-gateway"]) {
                sh 'docker build -t ${APP_NAME}:latest -f ${APP_NAME}/Dockerfile ./${APP_NAME}/'
                sh 'docker tag ${APP_NAME}:latest $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:SNAPSHOT-$BUILD_NUMBER'
                sh 'docker push $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:SNAPSHOT-$BUILD_NUMBER'
                sh 'docker tag ${APP_NAME}:latest $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:latest'
                sh 'docker push $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:latest'
              }

            }

          }
        }

        stage('build image - model manager') {
          agent none
          steps {
            container('maven') {
              withCredentials([usernamePassword(credentialsId : 'aliyun-docker-registry' ,passwordVariable : 'DOCKER_PASSWORD' ,usernameVariable : 'DOCKER_USERNAME' ,)]) {
                sh 'echo "$DOCKER_PASSWORD" | docker login $REGISTRY -u "$DOCKER_USERNAME" --password-stdin'
              }

              withEnv(["APP_NAME=mineai-model-manager"]) {
                sh 'docker build -t ${APP_NAME}:latest -f ${APP_NAME}/Dockerfile ./${APP_NAME}/'
                sh 'docker tag ${APP_NAME}:latest $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:SNAPSHOT-$BUILD_NUMBER'
                sh 'docker push $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:SNAPSHOT-$BUILD_NUMBER'
                sh 'docker tag ${APP_NAME}:latest $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:latest'
                sh 'docker push $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:latest'
              }

            }

          }
        }

        stage('build image - monitor accessor') {
          agent none
          steps {
            container('maven') {
              withCredentials([usernamePassword(credentialsId : 'aliyun-docker-registry' ,passwordVariable : 'DOCKER_PASSWORD' ,usernameVariable : 'DOCKER_USERNAME' ,)]) {
                sh 'echo "$DOCKER_PASSWORD" | docker login $REGISTRY -u "$DOCKER_USERNAME" --password-stdin'
              }

              withEnv(["APP_NAME=mineai-monitor-accessor"]) {
                sh 'docker build -t ${APP_NAME}:latest -f ${APP_NAME}/Dockerfile ./${APP_NAME}/'
                sh 'docker tag ${APP_NAME}:latest $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:SNAPSHOT-$BUILD_NUMBER'
                sh 'docker push $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:SNAPSHOT-$BUILD_NUMBER'
                sh 'docker tag ${APP_NAME}:latest $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:latest'
                sh 'docker push $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:latest'
              }

            }

          }
        }

        stage('build image - system') {
          agent none
          steps {
            container('maven') {
              withCredentials([usernamePassword(credentialsId : 'aliyun-docker-registry' ,passwordVariable : 'DOCKER_PASSWORD' ,usernameVariable : 'DOCKER_USERNAME' ,)]) {
                sh 'echo "$DOCKER_PASSWORD" | docker login $REGISTRY -u "$DOCKER_USERNAME" --password-stdin'
              }

              withEnv(["APP_NAME=mineai-system"]) {
                sh 'docker build -t ${APP_NAME}:latest -f ${APP_NAME}/Dockerfile ./${APP_NAME}/'
                sh 'docker tag ${APP_NAME}:latest $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:SNAPSHOT-$BUILD_NUMBER'
                sh 'docker push $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:SNAPSHOT-$BUILD_NUMBER'
                sh 'docker tag ${APP_NAME}:latest $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:latest'
                sh 'docker push $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:latest'
              }

            }

          }
        }

        stage('build image - worker manager') {
          agent none
          steps {
            container('maven') {
              withCredentials([usernamePassword(credentialsId : 'aliyun-docker-registry' ,passwordVariable : 'DOCKER_PASSWORD' ,usernameVariable : 'DOCKER_USERNAME' ,)]) {
                sh 'echo "$DOCKER_PASSWORD" | docker login $REGISTRY -u "$DOCKER_USERNAME" --password-stdin'
              }

              withEnv(["APP_NAME=mineai-worker-manager"]) {
                sh 'docker build -t ${APP_NAME}:latest -f ${APP_NAME}/Dockerfile ./${APP_NAME}/'
                sh 'docker tag ${APP_NAME}:latest $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:SNAPSHOT-$BUILD_NUMBER'
                sh 'docker push $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:SNAPSHOT-$BUILD_NUMBER'
                sh 'docker tag ${APP_NAME}:latest $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:latest'
                sh 'docker push $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:latest'
              }

            }

          }
        }

        stage('build image - ui') {
          agent none
          steps {
            container('maven') {
              withCredentials([usernamePassword(credentialsId : 'aliyun-docker-registry' ,passwordVariable : 'DOCKER_PASSWORD' ,usernameVariable : 'DOCKER_USERNAME' ,)]) {
                sh 'echo "$DOCKER_PASSWORD" | docker login $REGISTRY -u "$DOCKER_USERNAME" --password-stdin'
              }

              withEnv(["APP_NAME=mineai-ui"]) {
                sh 'docker build -t ${APP_NAME}:latest -f ${APP_NAME}/Dockerfile ./${APP_NAME}/'
                sh 'docker tag ${APP_NAME}:latest $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:SNAPSHOT-$BUILD_NUMBER'
                sh 'docker push $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:SNAPSHOT-$BUILD_NUMBER'
                sh 'docker tag ${APP_NAME}:latest $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:latest'
                sh 'docker push $REGISTRY/$DOCKERHUB_NAMESPACE/${APP_NAME}:latest'
              }

            }

          }
        }

      }
    }

    stage('deploy to staging') {
      agent none
      steps {
        input(message: '是否部署到staging环境？', submitter: '')
        container('maven') {
          withCredentials([
                                                                                                                                                                                      kubeconfigFile(
                                                                                                                                                                                                                    credentialsId: env.KUBECONFIG_CREDENTIAL_ID,
                                                                                                                                                                                                                    variable: 'KUBECONFIG')
                                                                                                                                                                                                                  ]) {
                sh 'envsubst < mineai-filebrowser/configMap.yml | kubectl apply -f -'
                sh 'envsubst < mineai-filebrowser/pvc.yml | kubectl apply -f -'
                sh 'envsubst < mineai-filebrowser/deploy.yml | kubectl apply -f -'
                sh 'envsubst < mineai-controller-manager/deploy.yml | kubectl apply -f -'
                sh 'envsubst < mineai-data-manager/deploy.yml | kubectl apply -f -'
                sh 'envsubst < mineai-gateway/deploy.yml | kubectl apply -f -'
                sh 'envsubst < mineai-model-manager/deploy.yml | kubectl apply -f -'
                sh 'envsubst < mineai-monitor-accessor/deploy.yml | kubectl apply -f -'
                sh 'envsubst < mineai-system/deploy.yml | kubectl apply -f -'
                sh 'envsubst < mineai-ui/deploy.yml | kubectl apply -f -'
                sh 'envsubst < mineai-worker-manager/deploy.yml | kubectl apply -f -'
                sh 'envsubst < mineai-zlmediakit/configMap.yml | kubectl apply -f -'
                sh 'envsubst < mineai-zlmediakit/deploy.yml | kubectl apply -f -'
              }

            }

          }
        }

      }
      environment {
        KUBECONFIG_CREDENTIAL_ID = 'admin-deploy'
        REGISTRY = 'registry.cn-hangzhou.aliyuncs.com'
        DOCKERHUB_NAMESPACE = 'mineai'
        NACOS_ENV = 'STAGAI_ID'
        K8S_NAMESPACE = 'mine-ai'
      }
    }