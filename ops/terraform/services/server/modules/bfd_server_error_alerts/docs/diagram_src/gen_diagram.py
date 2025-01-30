# pylint: disable=pointless-statement, expression-not-assigned
# pyright: reportUnusedExpression=false
from typing import Any, Optional

from diagrams import Cluster, Diagram, Edge
from diagrams.aws.compute import LambdaFunction
from diagrams.aws.integration import Eventbridge
from diagrams.aws.management import Cloudwatch
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


with Diagram(
    "BFD Server Error Alerts Infastructure", graph_attr=graph_attrs, outformat="png", show=False
):
    with _solid_cluster(label="server Terraservice"):
        with _solid_cluster(label="bfd_server_error_alerts Terraform module"):
            event_bridge = Eventbridge("EventBridge Scheduler\nrate(5 minutes) Schedule")
            with _solid_cluster(
                label="Alerter Lambda", direction="TB", graph_attr={"rankdir": "TB"}
            ):
                log_insights = Cloudwatch("Log Insights\naccess.json")
                insights_slack_lambda = LambdaFunction("Alerter Lambda\nFunction")

    (
        log_insights
        >> Edge(label="past 5:30 mins of 500s", reverse=True, forward=False)
        >> insights_slack_lambda
    )
    (event_bridge >> insights_slack_lambda)
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
