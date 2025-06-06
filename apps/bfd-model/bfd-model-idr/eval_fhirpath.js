const fs = require('fs');
const fhirpath = require('fhirpath');
const context = require('fhirpath/fhir-context/r4');

if (process.argv.length < 4) {
  process.exit(1);
}

const resourceInput = process.argv[2];
const expression = process.argv[3];
let resource;
try {
  if (fs.existsSync(resourceInput)) {
    resource = JSON.parse(fs.readFileSync(resourceInput, 'utf8'));
  } else {
    resource = JSON.parse(resourceInput);
  }
} catch (err) {
  console.error('Error loading or parsing resource JSON:', err.message);
  process.exit(1);
}
try {
  const result = fhirpath.evaluate(resource, expression, {
    resolveInternalTypes: false,
    root: resource
  }, context);
  process.stdout.write(JSON.stringify(result))
} catch (err) {
  console.error(`Error evaluating FHIRPath expression: "${expression}"\n`, err.message);
  process.exit(1);
}
