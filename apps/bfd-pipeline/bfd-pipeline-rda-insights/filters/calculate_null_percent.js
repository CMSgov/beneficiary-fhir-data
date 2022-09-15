function main(argumentMap) {
    argumentMap['%nulls'] = argumentMap['num_null_'] / argumentMap['num_rows'] * 100;
    return argumentMap;
}