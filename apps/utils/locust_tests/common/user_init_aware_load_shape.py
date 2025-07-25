"""Contains implementation for UserInitAwareLoadShape."""

import logging

from locust import LoadTestShape
from locust.env import Environment


class UserInitAwareLoadShape(LoadTestShape):
    """LoadTestShape class that provides a "load shape" that compensates for the time it
    takes to initialize users. This includes compensating for "ramp-up" time when the number
    of users is greater than the specified spawn rate (i.e. 100 users at a spawn rate of 10), as
    well as compensating for the time it takes to load data globally or per-user (but not per-task).
    """

    def __init__(self) -> None:
        super().__init__()
        self.environment: Environment
        self.target_users: int
        self.target_spawn_rate: int
        self.target_runtime: int | None

        self.logger = logging.getLogger()
        self.has_time_activated = False
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
        self.target_runtime = self.environment.parsed_options.spawned_runtime

        if self.target_runtime is None:
            self.logger.info("--spawned-runtime not specified, tests will run indefinitely.")

        if self.target_runtime == 0:
            self.logger.info(
                "--spawned-runtime is 0, test run will stop once all users have spawned."
            )

        return True

    def tick(self) -> tuple[int, float] | None:
        if not self.has_ticked:
            self.has_ticked = True
            is_env_setup = self.__first_tick()

            if not is_env_setup:
                return None

        users = self.get_current_user_count()
        if self.target_users == users:
            if not self.has_time_activated:
                if self.target_runtime:
                    self.logger.info(
                        "The %d requested users have finished spawning,"
                        " %d second test runtime limit is now active",
                        self.target_users,
                        self.target_runtime,
                    )
                elif self.target_runtime == 0:
                    self.logger.info(
                        "The %d requested users have finished spawning, the test run will now end",
                        self.target_users,
                    )
                elif self.target_runtime is None:
                    self.logger.info(
                        "The %d requested users have finished spawning,"
                        " tests will continue to run indefinitely until manually stopped",
                        self.target_users,
                    )

                self.reset_time()
                self.has_time_activated = True

            if self.target_runtime is not None and self.get_run_time() > self.target_runtime:
                return None

        self.logger.debug("Current runtime: %f", self.get_run_time())
        self.logger.debug("Current user count: %d", self.get_current_user_count())
        self.logger.debug("Has runtime reset: %s", self.has_time_activated)

        return (self.target_users, self.target_spawn_rate)
