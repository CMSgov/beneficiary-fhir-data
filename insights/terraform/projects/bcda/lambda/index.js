const AWS = require("aws-sdk");

AWS.config.apiVersions = {
    athena: '2017-05-18',
  };

const athena = new AWS.Athena();
const envs = ['prod','opensbx','test','dev'];

const get_params = (env) => {
  if (!envs.includes(env)) {
    return undefined;
  }
  return {
    QueryString: `MSCK REPAIR TABLE bcda.${env}_insights;`,
    QueryExecutionContext: {
      Catalog: 'AwsDataCatalog',
      Database: 'bcda'
    },
    ResultConfiguration: {
      OutputLocation: 's3://bfd-insights-bcda-577373831711/workgroups/bcda/'
    },
    WorkGroup: 'bcda'
  }
}

const execute_query = (env) => {
  return athena.startQueryExecution(get_params(env)).promise()
  .then((results) => {
    console.log(results);
  })
  .catch((err) => {
    console.log(`Error: ${err}`);
  })
}

const handler = (event, context, callback) => {
      Promise.all(envs.map(x => execute_query(x)))
      .then((results) => {
        console.log(`Results: ${results}`);
      })
      .catch(err => {
        console.log(`Error: ${err}`);
      })
}

exports.handler = handler;
