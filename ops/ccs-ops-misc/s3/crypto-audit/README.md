# s3 crypto audit script

This script checks buckets to ensure SSE is enabled, and optionally, will check bucket objects for encryption as well.

## Quickstart

First, move into the crypto-audit directory, install requirements, and run `--help`.

```sh
python3 -m venv .venv
. .venv/bin/activate
pip3 install -r requirements.txt
chmod +x s3caudit.py
./s3caudit.py --help
```

### Logging

This script makes use of stderr for logging and streams status updates to stdout.

Log entries are grepable.

Normally, you would want to redirect log entries to a file for later review. See examples below.

### Examples

This first example:

- Checks buckets for SSE (ignoring the public test data bucket)
- If the bucket is encrypted with an AWS managed key, it will check the first 100 objects in the top 2 level paths
- If the bucket does not have SSE enabled, it will check all objects in that bucket
- And finally, it redirects (`2>`) the logs to a file on your Desktop.

The script will steam status updates to your console in a more human readable format. Note:

- ✓'s mean the object is encrypted using the bucket key
- .'s are encrypted, but not with the default bucket key
- x's are objects that are not encrypted

(_You can get the details by grepping the log file._)

```sh
$ ./s3caudit.py \
    --ignore-bucket "bfd-public-test-data" \
    --max-depth 2 \
    --max-items 100 \
    --full-check-if-sse-disabled \
    2> ~/Desktop/results.log
Checking '123456789012-foo-logs's objects  [done]
Checking '123456789012-us-east-1's objects ✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓[done]
Checking '123456789012-us-west-2's objects ✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓✓[done]
Checking 'foo-bar's objects xxxxxx✓✓xxxxxxxxxx..xxxxxxxxxxxxxxxxxxxx✓✓xxxxxxxxxxxxxxxx✓✓xxxxxxxxxxxx[done]

# Show the logs (tail -f to follow)
$ cat ~/Desktop/results.log
2022-10-10 14:38:20,816 INFO a123456789012-foo-logs sse enabled with aws managed key (AES256)
2022-10-10 14:38:20,816 INFO *** Validating 123456789012-foo-logs objects (max_depth=2, max_items=100, prefix=)
2022-10-10 14:38:21,191 CRITICAL s3://123456789012-foo-logs/foo.txt is not encrypted!
2022-10-10 14:38:21,323 CRITICAL s3://123456789012-foo-logs/bar.txt is not encrypted!

# Grep for CRITICAL issues only
$ grep CRITICAL ~/Desktop/results.log

```
