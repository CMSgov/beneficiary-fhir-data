mvn --quiet clean install

mvn --quiet exec:java \
-Dexec.mainClass="gov.cms.bfd.data.npi.utility.App" \
-Dexec.args="./src/main/resources/"