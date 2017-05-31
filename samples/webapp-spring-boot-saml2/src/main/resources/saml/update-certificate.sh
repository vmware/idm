#!/bin/bash
set -e

# Use this script to update the certificate of your Identity Provider (i.e. VMware Identity Manager) in the keystore
# './src/main/resources/saml/samlKeystore.jks' file
# Run:
# $ ./src/main/resources/saml/update-certificate.sh


IDP_HOST=dev.vmwareidentity.asia
IDP_PORT=443
CERTIFICATE_FILE=vmware-idm.cert
KEYSTORE_FILE=`dirname $0`/samlKeystore.jks
KEYSTORE_PASSWORD=secret
KEYSTORE_ALIAS=vmwareidm

openssl s_client -host ${IDP_HOST} -port ${IDP_PORT} -prexit -showcerts </dev/null | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > ${CERTIFICATE_FILE}
keytool -delete -alias ${KEYSTORE_ALIAS} -keystore ${KEYSTORE_FILE} -storepass ${KEYSTORE_PASSWORD}
keytool -import -alias ${KEYSTORE_ALIAS} -file ${CERTIFICATE_FILE} -keystore ${KEYSTORE_FILE} -storepass ${KEYSTORE_PASSWORD} -noprompt

rm -rf ${CERTIFICATE_FILE}
