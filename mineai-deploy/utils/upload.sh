#!/bin/bash

cd $PATH_IMAGES || exit

files=$(find *.tar)
for file in $files; do
  # quay.io@jetstack@cert-manager-cainjector_v1.7.1.tar
  imageName=${file/_/:}
  # quay.io@jetstack@cert-manager-cainjector:v1.7.1.tar
  imageName=${imageName//@/\/}
  # quay.io/jetstack/cert-manager-cainjector:v1.7.1.tar
  imageName=${imageName/.tar/}
  # quay.io/jetstack/cert-manager-cainjector:v1.7.1
  prefix=${imageName%/*}
  # quay.io/jetstack
  prefix=${prefix#*/}
  # jetstack
  # 统计`/`出现的次数，如果未出现则采用默认项目名
  count=$(echo $imageName | grep -c /)
  if [ $count -eq 0 ]; then
    echo "Using default prefix"
    prefix=$REGISTRY_DEFAULT_PROJECT
  fi
  echo "Creating Harbor project"
  curl -u "${REGISTRY_USER}:${REGISTRY_PASSWD}" -X POST -H "Content-Type: application/json" "https://${REGISTRY_URL}/api/v2.0/projects" -d "{ \"project_name\": \"${prefix}\", \"public\": true}" -k
  echo "Loading $file"
  docker load -i $file
  echo "Tagging ${REGISTRY_URL}/$prefix/${imageName##*/}"
  docker tag $imageName ${REGISTRY_URL}/$prefix/${imageName##*/}
  echo "Pushing ${REGISTRY_URL}/$prefix/${imageName##*/}"
  docker push ${REGISTRY_URL}/$prefix/${imageName##*/}
done
