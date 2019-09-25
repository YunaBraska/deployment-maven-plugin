#!/usr/bin/env bash
gpg --batch --yes --delete-secret-keys $(gpg --no-default-keyring --secret-keyring ./gpg.sec --keyring ./gpg.pub --list-keys | grep "      " | head -n1 | xargs)
gpg --batch --yes --delete-keys $(gpg --no-default-keyring --secret-keyring ./gpg.sec --keyring ./gpg.pub --list-secret-keys | grep "      " | head -n1 | xargs)
rm -rf gpg.pub keyfile