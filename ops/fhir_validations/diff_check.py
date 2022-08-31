import argparse
import glob
import re
import subprocess

from typing import List

def parse_arguments():
    parser = argparse.ArgumentParser()
    # This is normally executed by the pipeline from the root repo directory, thus the defaults are relative to that.
    parser.add_argument('-v', '--validator',
                        default='validator_cli.jar',
                        help='path to the fhir validator jar')
    parser.add_argument('-d', '--directory',
                        default='apps/bfd-server/bfd-server-war/src/test/resources/endpoint-responses',
                        help='path to the directory containing the resource version folders')
    parser.add_argument('-i', '--ignorefile',
                        default='ops/fhir_validations/validations_ignorelist.yml',
                        help='path to the ignore list yaml file')
    parser.add_argument('-r', '--recent', action='store_const', const=True, help="Recent changes")

    return parser.parse_args()


def get_resource_changes(base_dir: str) -> List[str]:
    git_call = subprocess.run(['git', 'diff', '--name-only', 'master...'], check=True, capture_output=True)
    grep_call = subprocess.run(['grep', base_dir], input=git_call.stdout, capture_output=True)
    unique_call = subprocess.run(['uniq'], input=grep_call.stdout, capture_output=True)
    output = unique_call.stdout.decode('utf-8')
    if len(output) == 0:
        return []
    return output.strip().split('\n')


def has_ignore_changes(ignore_file: str) -> bool:
    git_call = subprocess.run(['git', 'diff', '--name-only', 'master...'], check=True, capture_output=True)
    grep_call = subprocess.run(['grep', ignore_file], input=git_call.stdout, capture_output=True)
    output = grep_call.stdout.decode('utf-8')

    if len(output) == 0:
        return False

    return True


def main():
    args = parse_arguments()

    if len(get_resource_changes(args.directory)) > 0 or has_ignore_changes(args.ignorefile):
        exit(1)

    exit(0)


if __name__ == "__main__":
    main()
