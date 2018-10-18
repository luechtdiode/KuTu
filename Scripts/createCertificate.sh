# see = "https://lightbend.github.io/ssl-config/CertificateGeneration.html"

# Please change for each productive installation
export PW="veRy-u@nSe(u6Re"
export ISSUER_NAME="kutuapp"
export ISSUER_ORG=""
export ISSUER_ORGUNIT=""
export HOSTNAME="kutuapp"
export HOST_ORG=""
export HOST_ORGUNIT=""
export CITY="Basel-Stadt"
export STATE="BS"
export COUNTRY="CH"

# Create a self signed key pair root CA certificate.
keytool -genkeypair -v \
  -alias $ISSUER_NAME \
  -dname "CN=$ISSUER_NAME, OU=$ISSUER_ORGUNIT, O=$ISSUER_ORG, L=$CITY, ST=$STATE, C=$COUNTRY" \
  -keystore $ISSUER_NAME.jks \
  -keypass:env PW \
  -storepass:env PW \
  -keyalg RSA \
  -keysize 4096 \
  -ext KeyUsage:critical="keyCertSign" \
  -ext BasicConstraints:critical="ca:true" \
  -validity 9999

# Export the exampleCA public certificate as exampleca.crt so that it can be used in trust stores.
keytool -export -v -alias $ISSUER_NAME -file $ISSUER_NAME.crt -keypass:env PW -storepass:env PW -keystore $ISSUER_NAME.jks -rfc

# Create a server certificate, tied to example.com
keytool -genkeypair -v \
  -alias $HOSTNAME \
  -dname "CN=$HOSTNAME, OU=$HOST_ORGUNIT, O=$HOST_ORG, L=$CITY, ST=$STATE, C=$COUNTRY" \
  -keystore $HOSTNAME.jks \
  -keypass:env PW \
  -storepass:env PW \
  -keyalg RSA \
  -keysize 2048 \
  -validity 385
  
# Create a certificate signing request for example.com
keytool -certreq -v -alias $HOSTNAME -keypass:env PW -storepass:env PW -keystore $HOSTNAME.jks -file $HOSTNAME.csr

# Tell exampleCA to sign the example.com certificate. Note the extension is on the request, not the
# original certificate.
# Technically, keyUsage should be digitalSignature for DHE or ECDHE, keyEncipherment for RSA.
keytool -gencert -v \
  -alias $ISSUER_NAME \
  -keypass:env PW \
  -storepass:env PW \
  -keystore $ISSUER_NAME.jks \
  -infile $HOSTNAME.csr \
  -outfile $HOSTNAME.crt \
  -ext KeyUsage:critical="digitalSignature,keyEncipherment" \
  -ext EKU="serverAuth" \
  -ext SAN="DNS:$HOSTNAME" \
  -rfc
  
# Tell example.com.jks it can trust exampleca as a signer.  
# the answer yes => ja in german environments -could differ according to the current locale
keytool -import -v -alias $ISSUER_NAME -file $ISSUER_NAME.crt -keystore $HOSTNAME.jks -storetype JKS -storepass:env PW << EOF
ja
EOF

# Import the signed certificate back into example.com.jks
keytool -import -v -alias $HOSTNAME -file $HOSTNAME.crt -keystore $HOSTNAME.jks -storetype JKS -storepass:env PW

# List out the contents of example.com.jks just to confirm it.  
# If you are using Play as a TLS termination point, this is the key store you should present as the server.
keytool -list -v -keystore $HOSTNAME.jks -storepass:env PW

# copy the generated $HOSTNAME.jks to the classpath's root