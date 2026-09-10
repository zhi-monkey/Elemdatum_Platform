#!/bin/bash

BASE_PATH='./update'

# Add execute permission
chmod +x -R utils

# Load all env variables from `.env` file
source utils/load-env.sh <.env

# Read old version tag and save new version
read -r TAG_TIPS <$BASE_PATH/history
read -r -p $'Please input image version tag:\n' -e -i "$TAG_TIPS" TAG
echo $TAG >$BASE_PATH/history
export TAG

base_dir=${PWD##*/}

echo '======Downloading Docker Images============'
tr -d '\r' <$BASE_PATH/images.txt | envsubst | sudo -E utils/download.sh

cmd_upload_apply="
source utils/load-env.sh <.env;
export TAG=$TAG;
echo '======Uploading Docker Images==============';
sudo -E utils/upload.sh;
sudo rm -f $PATH_IMAGES/*;
echo '======Applying Kubernetes Manifests========';
tr -d '\r' <$BASE_PATH/manifests.txt | utils/apply-manifests.sh;
"

if [[ "$REMOTE" = "false" ]]; then
  eval $cmd_upload_apply

elif [[ "$REMOTE" = "true" ]]; then
  # Copy folder to remote
  echo 'Please login remote:'
  scp -p$REMOTE_PORT -r ../$base_dir ${REMOTE_USER}@${REMOTE_HOST}:$REMOTE_PATH

  # Clear local
  sudo rm -f $PATH_IMAGES/*

  # Upload images and apply
  ssh -t ${REMOTE_USER}@${REMOTE_HOST} "cd $REMOTE_PATH/$base_dir; $cmd_upload_apply"
else
  echo 'Error: REMOTE in .env file can only set as true/false.'
  exit 1

fi
