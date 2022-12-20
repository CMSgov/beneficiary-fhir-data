## To run this script:

```shell
chmod +x ./cert-checker
mv .env.sample .env
vi .env # (see ops for the correct settings)
source .env
./cert-checker.sh -e "prod-sbx.bfd.cms.gov:443"
```
