import argparse
import glob
import re
import subprocess
from typing import List, Tuple

import yaml


def get_fhir_resource_files(base_dir: str, recently_changed: bool) -> List[str]:
    if recently_changed is True:
        git_call = subprocess.run(['git', 'diff', '--name-only', 'master...'], check=True, capture_output=True)
        grep_call = subprocess.run(['grep', base_dir], input=git_call.stdout, capture_output=True)
        unique_call = subprocess.run(['uniq'], input=grep_call.stdout, capture_output=True)
        output = unique_call.stdout.decode('utf-8')
        if len(output) == 0:
            return []
        return output.strip().split('\n')
    else:
        return glob.glob(f'{base_dir}/*.json')


def any_filters_match(patterns: List[str], error: str) -> bool:
    return any(re.match(regex, error) for regex in patterns)


def get_global_filter(ignore_list: dict) -> List[str]:
    global_filters = []

    if 'global_filter' in ignore_list and isinstance(ignore_list['global_filter'], dict):
        global_filter = ignore_list['global_filter']

        if 'error_patterns' in global_filter and isinstance(global_filter['error_patterns'], list):
            global_filters = global_filter['error_patterns']

    return global_filters


def get_file_filter(ignore_list: dict, file_path: str) -> List[str]:
    filters = []

    if type(ignore_list) is dict:
        if 'file_filter' in ignore_list and ignore_list['file_filter'] is not None:
            for file_filter in ignore_list['file_filter']:
                if re.search(file_filter['file_pattern'], file_path):
                    if 'error_patterns' in file_filter and isinstance(file_filter['error_patterns'], list):
                        filters = filters + file_filter['error_patterns']

    return filters


def filter_errors(ignore_list: dict, errors_per_file: dict) -> dict:
    global_filters = get_global_filter(ignore_list)

    filtered_errors_per_file = {}

    for file_name in errors_per_file.keys():
        file_filters = get_file_filter(ignore_list, file_name)

        for error in errors_per_file[file_name]:
            if not any_filters_match(global_filters, error) and not any_filters_match(file_filters, error):
                filtered_errors_per_file.setdefault(file_name, []).append(error)

    return filtered_errors_per_file


def validate_resources(version: str, ignore_list: dict, files: List[str]) -> dict:
    java_commands = ['java', '-Xmx3G', '-Xms2G', '-jar', 'validator_cli.jar']

    for file_name in files:
        java_commands.append(file_name)

    java_commands.append('-version')
    java_commands.append(version)

    java_call = subprocess.run(java_commands, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    output = java_call.stdout.decode('utf-8')
    error_output = java_call.stderr.decode('utf-8')
    if java_call.returncode != 0:
        if output == '':
            print('Validation failed, but no output was generated.')
        elif error_output != '':
            print(error_output)
            print('There was an issue processing the request.')
        exit(1)
    output_lines = output.split('\n')

    file_name = "loading_output"
    errors_per_file = {}

    for line in output_lines:
        if line.startswith('-- '):
            file_search = re.search('-- ([^\\s]*) -*', line)

            if file_search:
                file_name = file_search.group(1)
                errors_per_file[file_name] = []

        stripped_line = line.strip()

        if stripped_line.lower().startswith('error @'):
            errors_per_file[file_name].append(stripped_line)

    return filter_errors(ignore_list, errors_per_file)


class RunConfig(object):
    def __init__(self):
        self.target_dir = ''
        self.version = ''


def validate_resource_dir(run_config: RunConfig, ignore_list: dict, recently_changed: bool) -> Tuple[int, dict]:
    print('Checking directory {}'.format(run_config.target_dir))
    files = get_fhir_resource_files(run_config.target_dir, recently_changed)
    files.sort()
    file_count = len(files)

    if file_count > 0:
        print('Validating {} resources'.format(file_count))

        invalid_resources = validate_resources(run_config.version, ignore_list, files)
        for resource in invalid_resources:
            if len(resource) == 0:
                del invalid_resources[resource]
    else:
        print('No resources to validate')
        invalid_resources = {}

    return file_count, invalid_resources


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--ignorefile',
                        default='validations_ignorelist.yml',
                        help='path to the ignore list yaml file', )
    parser.add_argument('-r', '--recent', action='store_const', const=True, help="Recent changes")
    args = parser.parse_args()

    try:
        ignore_list = yaml.safe_load(open(args.ignorefile))
        print('Ignore list found, using to filter results')
    except FileNotFoundError:
        print('Could not find ignore list file, running without filters')
        ignore_list = {'ignore_list': {}}

    if ignore_list is not None and 'ignore_list' in ignore_list:
        filters = ignore_list['ignore_list']
    else:
        filters = {'ignore_list': {}}

    v1_config = RunConfig()
    v1_config.target_dir = 'apps/bfd-server/bfd-server-war/src/test/resources/endpoint-responses/v1'
    v1_config.version = '3.0'

    v2_config = RunConfig()
    v2_config.target_dir = 'apps/bfd-server/bfd-server-war/src/test/resources/endpoint-responses/v2'
    v2_config.version = '4.0'

    v1_count, v1_invalid_resources = validate_resource_dir(v1_config, filters, args.recent)
    v2_count, v2_invalid_resources = validate_resource_dir(v2_config, filters, args.recent)
    total_count = v1_count + v2_count
    invalid_resources = dict(v1_invalid_resources, **v2_invalid_resources)

    if invalid_resources:
        total_errors = sum(len(invalid_resources[key]) for key in invalid_resources)
        print('Results: {} invalid resources ({} total errors)'.format(len(invalid_resources), total_errors))
        for file_name in invalid_resources:
            print(f'  - {file_name}')
            for error in invalid_resources[file_name]:
                print(f'    {error}')
        exit(1)
    elif total_count > 0:
        print('Results: {} resources validated'.format(v1_count + v2_count))
    else:
        print('Results: No resources were validated')


if __name__ == "__main__":
    main()
