# pylint: disable=pointless-statement, expression-not-assigned
# pyright: reportUnusedExpression=false
from typing import Any, Optional

from diagrams import Cluster, Diagram, Edge
from diagrams.aws.compute import LambdaFunction
from diagrams.aws.integration import Eventbridge, SimpleNotificationServiceSnsTopic
from diagrams.aws.management import Cloudwatch, CloudwatchAlarm
from diagrams.saas.chat import Slack

graph_attrs = {"direction": "RL"}


def _solid_cluster(
    label: Optional[str] = None,
    direction: Optional[str] = None,
    graph_attr: Optional[dict[Any, Any]] = None,
) -> Cluster:
    return Cluster(
        label=label or "",
        direction=direction or "RL",
        graph_attr=graph_attr | {"style": "solid"} if graph_attr else {"style": "solid"},
    )


with Diagram("bfd-2728", graph_attr=graph_attrs, outformat="png", show=False):
    with _solid_cluster(label="server Terraservice"):
        metrics = Cloudwatch("Log metrics for 500s")
        with _solid_cluster(label="bfd_server_error_alerts Terraform module"):
            err_alarm = CloudwatchAlarm("Alarm for any 500")
            sns_topic = SimpleNotificationServiceSnsTopic("Scheduler Lambda Topic")
            scheduler_lambda = LambdaFunction("Scheduler Lambda")
            event_bridge = Eventbridge("EventBridge Scheduler")
            with _solid_cluster(
                label="Alerter Lambda", direction="TB", graph_attr={"rankdir": "TB"}
            ):
                log_insights = Cloudwatch("Log Insights\naccess.json")
                insights_slack_lambda = LambdaFunction("Alerter Lambda\nFunction")

    metrics >> err_alarm
    err_alarm >> Edge(label="OK -> ALARM") >> sns_topic
    err_alarm >> Edge(label="ALARM -> OK") >> sns_topic
    sns_topic >> Edge(label="Event indicating\nOK -> ALARM") >> scheduler_lambda
    sns_topic >> Edge(label="Event indicating\nALARM -> OK") >> scheduler_lambda
    (
        scheduler_lambda
        >> Edge(label="rate every 5 mins\n+ start in 10 sec\n+ remove any orphan scheds")
        >> event_bridge
    )
    (
        scheduler_lambda
        >> Edge(label="remove rate every 5 mins\n+ start in 10 sec\n+ remove any orphan scheds")
        >> event_bridge
    )
    (
        log_insights
        >> Edge(label="past 5:30 mins of 500s", reverse=True, forward=False)
        >> insights_slack_lambda
    )
    (event_bridge >> Edge(label="rate(5 minutes),\none-time sched") >> insights_slack_lambda)
    (
        insights_slack_lambda
        >> Edge(
            label=(
                "if 500s found:\nmsg with # of 500s per-partner, per-op\n+ link to log insights"
                " query"
            )
        )
        >> Slack("#bfd-internal-alerts")
    )
