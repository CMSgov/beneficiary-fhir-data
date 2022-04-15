import argparse
import glob
import os
import re
import subprocess
import yaml


def get_fhir_resource_files(recently_changed):
    target_directory = 'apps/bfd-server/bfd-server-war/src/test/resources/endpoint-responses/v2'
    if recently_changed is True:
        return os.popen(f'git diff --name-only master... | grep {target_directory} | sort | uniq')
    else:
        return glob.glob(f'{target_directory}/*.json')


def any_filters_match(patterns, error):
    return any(re.match(regex, error) for regex in patterns)


def filter_errors(global_error_patterns, file_error_patterns, errors):
    filtered_errors = []
    for error in errors:
        if not any_filters_match(global_error_patterns, error) and not any_filters_match(file_error_patterns, error):
            filtered_errors.append(error)
    return filtered_errors


def get_file_filter(white_list, file_path):
    filters = []

    if type(white_list) is dict:
        if 'file_filter' in white_list and white_list['file_filter'] is not None:
            for file_filter in white_list['file_filter']:
                if re.search(file_filter['file_pattern'], file_path):
                    if 'error_patterns' in file_filter and type(file_filter['error_patterns']) is list:
                        filters = file_filter['error_patterns']

    return filters


def validate_resource(white_list, file_path):
    file_filters = get_file_filter(white_list, file_path)
    output = subprocess.run(['bash', 'mock_validator.sh', file_path, '-version 4.0'], stdout=subprocess.PIPE).stdout.decode('utf-8')
    output_lines = output.split('\n')
    errors = []

    for line in output_lines:
        stripped_line = line.strip()
        if stripped_line.lower().startswith('error @'):
            errors.append(stripped_line)

    global_filters = []

    if type(white_list) is dict:
        if 'global_filter' in white_list and type(white_list['global_filter']) is dict:
            global_filter = white_list['global_filter']

            if 'error_patterns' in global_filter and type(global_filter['error_patterns']) is list:
                global_filters = global_filter['error_patterns']

    return filter_errors(global_filters, file_filters, errors)


def validate_resources(white_list, recently_changed):
    files = get_fhir_resource_files(recently_changed)
    files.sort()
    invalid_resources = {}
    for file_path in files:
        errors = validate_resource(white_list, file_path)
        if errors:
            invalid_resources[file_path] = errors
    return invalid_resources


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('-r', '--recent', action='store_const', const=True, help="Recent changes")
    args = parser.parse_args()

    white_list = yaml.safe_load(open('validations_whitelist.yml'))

    invalid_resources = validate_resources(white_list['white_list'], args.recent)

    if invalid_resources:
        print('There were {} invalid resources'.format(len(invalid_resources)))
        for file_name in invalid_resources:
            print(f'  - {file_name}')
            for error in invalid_resources[file_name]:
                print(f'    {error}')


if __name__ == "__main__":
    main()
