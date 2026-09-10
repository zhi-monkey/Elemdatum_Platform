#!/bin/bash

mkdir -p $PATH_IMAGES
cd $PATH_IMAGES || exit

while read -r row
do
  fileName=${row/:/_}
  fileName=${fileName//\//@}
  if [ -e $fileName.tar ]
  then
    echo "$row exists"
  else
    echo "Downloading $row"
    docker pull $row
    echo "Saving $row"
    docker save -o $fileName.tar $row
    chmod 777 $fileName.tar
  fi
done
