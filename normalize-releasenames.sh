#!/usr/bin/env bash

normalizeReleaseName() {
  osqualifier=$1
  extension=$2
  for installfile in target/${osqualifier}/*.${extension}
  do
    if [[ -f $installfile ]]
    then
      newinstallfile=$(echo "$installfile" | sed "s/.$extension/-$osqualifier.$extension/")
      echo $newinstallfile
      mv "$installfile" "$newinstallfile"
    else
      echo "$installfile not found (skip)"
    fi
  done
}

normalizeReleaseName macOS-x86_64 pkg
normalizeReleaseName macOS-aarch64 pkg
normalizeReleaseName Linux deb
normalizeReleaseName Win64 msi
