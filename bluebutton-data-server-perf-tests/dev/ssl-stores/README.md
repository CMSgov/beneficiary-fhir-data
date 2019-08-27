# Converting a PEM file to a Java Keystore file
The keytool does not provide a way to import a certificate plus a private key
from a single combined PEM file.  An openssl DER conversion will only convert the
certificate portion and not the key.  This will result in a "bad certificate"
error when used in java code.  The following is an example of what **will
not work**: 

    $ openssl x509 -outform der -in keypair.pem -out keypair.der
    $ keytool -import -alias test -file keypair.der -keystore client.keystore
    $ keytool -list -v client.keystore

      Keystore type: JKS
      Keystore provider: SUN
      
      Your keystore contains 1 entry
      
      Alias name: test
      Creation date: Nov 6, 2017
      Entry type: trustedCertEntry
      <snip>

A way that will work is to first convert the certificate/private key pair into
a pkcs12 keystore then use the keytool to import the pkcs12 keystore into a 
java key store file:

    $ openssl pkcs12 -export -inkey keypair.pem -in keypair.pem -name test -out test.p12
    $ keytool -importkeystore -srckeystore test.p12 -srcstoretype pkcs12 -destkeystore client.keystore
    $ keytool -list -v client.keystore

      Keystore type: JKS
      Keystore provider: SUN

      Your keystore contains 1 entry
      
      Alias name: test
      Creation date: Nov 6, 2017
      Entry type: PrivateKeyEntry
      Certificate chain length: 1
      Certificate[1]:
      <snip>
