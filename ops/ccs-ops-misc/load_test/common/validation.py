'''Validate tests against target SLIs'''
import logging
import time
import gevent
from locust.runners import STATE_STOPPING, STATE_STOPPED, STATE_CLEANUP, WorkerRunner

# List of SLA categories and values
SLA_COVERAGE = "SLA_COVERAGE"
SLA_PATIENT = "SLA_PATIENT"
SLA_EOB_WITH_SINCE = "SLA_EOB_WITH_SINCE"
SLA_EOB_WITHOUT_SINCE = "SLA_EOB_WITHOUT_SINCE"
SLA_V1_BASELINE = "SLA_V1_BASELINE"
SLA_V2_BASELINE = "SLA_V2_BASELINE"

## Values are for 50%, 95%, 99%, and the failsafe limit in order, in milliseconds
## TODO: Pull these values from production metrics (i.e. New Relic)
slas = {
    SLA_COVERAGE : [10, 100, 250, 500],
    SLA_PATIENT : [1000, 3000, 5000, 8000],
    SLA_EOB_WITH_SINCE : [100, 250, 1000, 3000],
    SLA_EOB_WITHOUT_SINCE : [500, 1000, 3000, 6000],
    # The following values were selected for 5 concurrent users against
    # a target c5xlarge AWS instance running bfd-server
    SLA_V1_BASELINE : [10, 325, 550, 10000],
    # The following values were selected for 3 concurrent users against
    # a target c5xlarge AWS instance running bfd-server
    SLA_V2_BASELINE : [10, 325, 550, 10000]
}

def check_global_fail(environment, fail_time_ms):
    '''Checks if the test response time is too long (in the event the database is being
    overwhelmed) and if so, we stop the test.
    '''
    while not environment.runner.state in [STATE_STOPPING, STATE_STOPPED, STATE_CLEANUP]:
        time.sleep(1)
        if environment.stats.total.avg_response_time > fail_time_ms:
            logging.getLogger().warning('WARNING: Test aborted due to triggering test failsafe '
                '(average response time ratio > %sms)', fail_time_ms)
            environment.runner.quit()
            return

def setup_failsafe_event(environment, sla_category_name):
    '''Adds a listener that will add a repeating check for the global failsafe response time in
    order to stop the test if the event the environment/box under test is overwhelmed and at risk
    of crashing.
    '''
    logging.getLogger().info("Setting up failsafe event")
    if not isinstance(environment.runner, WorkerRunner):
        fail_time_ms = slas[sla_category_name][3]
        gevent.spawn(check_global_fail, environment, fail_time_ms)

def check_sla_validation(environment, sla_category_name):
    '''Checks the SLA numbers for various percentiles based on the given sla category name. This
    function is ignored unless it is the main test thread or a non-distributed test.
    '''
    logger = logging.getLogger()
    if not isinstance(environment.runner, WorkerRunner):

        logger.info('Checking SLAs...')
        sla_50 = slas[sla_category_name][0]
        sla_95 = slas[sla_category_name][1]
        sla_99 = slas[sla_category_name][2]

        if environment.stats.total.fail_ratio > 0:
            logger.info('Test failed due to request failure ratio > 0%')
            environment.process_exit_code = 1
        elif environment.stats.total.get_response_time_percentile(0.50) > sla_50:
            logger.info('Test failed due to 50th percentile response time > %d ms', sla_50)
            environment.process_exit_code = 1
        elif environment.stats.total.get_response_time_percentile(0.95) > sla_95:
            logger.info('Test failed due to 95th percentile response time > %d ms', sla_95)
            environment.process_exit_code = 1
        elif environment.stats.total.get_response_time_percentile(0.99) > sla_99:
            logger.info('Test failed due to 99th percentile response time > %d ms', sla_99)
            environment.process_exit_code = 1
        else:
            logger.info("SLAs within acceptable bounds")
