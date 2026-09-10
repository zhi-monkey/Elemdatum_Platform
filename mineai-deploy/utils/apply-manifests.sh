#!/bin/bash

cd "$PATH_MANIFEST" || exit

# mineai-gateway.yml
while read -r manifest
do
  envsubst <$manifest | kubectl apply -f -
done
