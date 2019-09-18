### Usage "./testscript.sh /path/to/parameters/"
### Requires parameters being pushed to SSM Parameter store
eval $(aws ssm get-parameters-by-path --with-decryption  --path "$1" | jq -r '.Parameters| .[] | "export " + .Name + "=\"" + .Value + "\""  ' | sed "s|$1||g")
echo $fus
echo $ro
echo $dah