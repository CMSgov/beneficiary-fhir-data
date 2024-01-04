export BFD_ENV="3085-test"
export BFD_ROOT=/home/randall/code/beneficiary-fhir-data
aws ssm get-parameter --name "/bfd/$BFD_ENV/server/sensitive/test_client_key" \
   --region "us-east-1" \
   --with-decryption | jq -r '.Parameter.Value' > $HOME/bfd-test-cert.pem
aws ssm get-parameter --name "/bfd/$BFD_ENV/server/sensitive/test_client_cert" \
   --region "us-east-1" \
   --with-decryption | jq -r '.Parameter.Value' >> $HOME/bfd-test-cert.pem
export CLIENT_CERT_PATH=$HOME/bfd-test-cert.pem
export DB_CLUSTER_ID=$(aws ssm get-parameter --name "/bfd/$BFD_ENV/common/nonsensitive/rds_cluster_identifier" \
                 --region "us-east-1" | jq -r '.Parameter.Value')
export DB_USERNAME=$(aws ssm get-parameter --name "/bfd/$BFD_ENV/server/sensitive/data_server_db_username" \
                 --with-decryption \
                 --region "us-east-1" | jq -r '.Parameter.Value')
export DB_RAW_PASSWORD=$(aws ssm get-parameter --name "/bfd/$BFD_ENV/server/sensitive/data_server_db_password" \
                 --with-decryption \
                 --region "us-east-1" | jq -r '.Parameter.Value')
export DB_PASSWORD=$(printf %s "$DB_RAW_PASSWORD" | jq -sRr @uri)
export DB_READER_URI=$(aws rds describe-db-clusters --db-cluster-identifier "$DB_CLUSTER_ID" \
                 --region "us-east-1" | jq -r '.DBClusters[0].ReaderEndpoint')
export DATABASE_CONSTR="postgres://$DB_USERNAME:$DB_PASSWORD@$DB_READER_URI:5432/fhirdb"

locust -f v2/regression_suite.py \
  --users=100 \
  --host="https://10.235.22.6:443" \
  --spawn-rate=5 \
  --spawned-runtime="10m" \
  --client-cert-path="$CLIENT_CERT_PATH" \
  --database-connection-string="$DATABASE_CONSTR" \
  --headless