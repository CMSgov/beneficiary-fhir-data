import argparse
import glob
import re
import subprocess
import yaml


def get_fhir_resource_files(recently_changed):
    target_directory = 'apps/bfd-server/bfd-server-war/src/test/resources/endpoint-responses/v2'
    if recently_changed is True:
        git_call = subprocess.run(['git', 'diff', '--name-only', 'master...'], check=True, capture_output=True)
        grep_call = subprocess.run(['grep', target_directory], input=git_call.stdout, capture_output=True)
        unique_call = subprocess.run(['uniq'], input=grep_call.stdout, capture_output=True)
        output = unique_call.stdout.decode('utf-8')
        return output.strip().split('\n')
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
    java_call = subprocess.run(
        ['java', '-Xmx3G', '-Xms2G', '-jar', 'validator_cli.jar', '', file_path, '-version', '4.0'],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE)
    output = java_call.stdout.decode('utf-8')
    error_output = java_call.stderr.decode('utf-8')
    if java_call.returncode != 0:
        if output == '':
            print('Validation of \'{}\' failed, but no output was generated.'.format(file_path))
        elif error_output != '':
            print(error_output)
            print('There was an issue processing the request.')
            exit(1)
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
    file_count = len(files)
    print('Validating {} resources (This should take about {} minutes)...'.format(file_count, file_count))
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
        total_errors = sum(len(invalid_resources[key]) for key in invalid_resources)
        print('There were {} invalid resources ({} total errors)'.format(len(invalid_resources), total_errors))
        for file_name in invalid_resources:
            print(f'  - {file_name}')
            for error in invalid_resources[file_name]:
                print(f'    {error}')
        exit(1)
    else:
        print('All resources validated.')


if __name__ == "__main__":
    main()
