#!/usr/bin/env bash
if [ -z ${1+x} ]; then echo "Missing [Passphrase]"; exit; else echo "Passprase [****]"; fi
if [ -z ${2+x} ]; then echo "Missing [Mail]"; exit; else echo "Mail [${2}]"; fi
if [ -z ${3+x} ]; then echo "Missing [Name]"; exit; else echo "Name [${3}]"; fi
cat >keyfile <<EOF
     %echo Generating a basic OpenPGP key
     Key-Type: RSA
     Key-Length: 2048
     Subkey-Type: ELG-E
     Subkey-Length: 1024
     Name-Real: $3
     Name-Comment: keep it simple
     Name-Email: $2
     Expire-Date: 0
     Passphrase: $1
     %pubring gpg.pub
     %secring gpg.sec
     %key created
     %echo done
EOF
gpg --batch --gen-key keyfile
gpg --import gpg.pub
rm -rf foo