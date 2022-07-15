"""Contains implementation for UserInitAwareLoadShape"""

import logging
from typing import Optional, Tuple

from locust import LoadTestShape
from locust.env import Environment


class UserInitAwareLoadShape(LoadTestShape):
    """LoadTestShape class that provides a "load shape" that compensates for the time it
    takes to initialize users. This includes compensating for "ramp-up" time when the number
    of users is greater than the specified spawn rate (i.e. 100 users at a spawn rate of 10), as
    well as compensating for the time it takes to load data globally or per-user (but not per-task).
    """

    def __init__(self):
        super().__init__()
        self.environment: Environment
        self.target_users: int
        self.target_spawn_rate: int
        self.target_runtime: int

        self.logger = logging.getLogger()
        self.time_active = False
        self.has_ticked = False

    def __first_tick(self) -> bool:
        if not self.runner:
            return False

        self.logger.info(
            'Locust reports that "users" and "spawn-rate" are ignored -- this warning can be'
            " disregarded as the current load shape class, UserInitAwareLoadShape, uses those"
            " options."
        )

        self.environment = self.runner.environment
        if not self.environment.parsed_options:
            self.logger.error(
                "Locust's parsed options are not available -- is Locust running as a library?"
            )
            return False

        self.target_users = self.environment.parsed_options.num_users or 1
        self.target_spawn_rate = self.environment.parsed_options.spawn_rate or 1
        self.target_runtime = self.environment.parsed_options.tests_runtime
        if not self.target_runtime:
            self.logger.info("--tests-runtime not specified, tests will run indefinitely.")

        return True

    def tick(self) -> Optional[Tuple[int, float]]:
        if not self.has_ticked:
            self.has_ticked = True
            is_env_setup = self.__first_tick()

            if not is_env_setup:
                return None

        users = self.get_current_user_count()
        if self.target_users == users:
            if not self.time_active:
                self.logger.info(
                    "The %d requested users have finished spawning,"
                    " %d second test runtime limit is now active",
                    self.target_users,
                    self.target_runtime,
                )
                self.reset_time()
                self.time_active = True

            if self.target_runtime and self.get_run_time() > self.target_runtime:
                return None

        self.logger.debug("Current runtime: %f", self.get_run_time())
        self.logger.debug("Current user count: %d", self.get_current_user_count())

        return (self.target_users, self.target_spawn_rate)
