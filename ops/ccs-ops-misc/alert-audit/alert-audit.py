#!/usr/bin/env python3
import boto3
from pydantic import BaseModel
from typing import List
import logging
from datetime import datetime
import os
import re

class CWAlert(BaseModel):
    alarmName: str = ''
    alarmArn: str = ''
    alarmDescription: str = ''
    alarmConfigurationUpdatedTimestamp: datetime = ''
    okActions: list = ''
    alarmActions: list = ''
    stateValue: str = ''

def fetch_alerts() -> List[CWAlert]:
    cloudwatch = boto3.client('cloudwatch')
    alerts = cloudwatch.describe_alarms(
        AlarmTypes=['MetricAlarm'],
        ActionPrefix="arn:aws:sns:us-east-1:577373831711:bfd-prod-cloudwatch-alarms",
        )['MetricAlarms']
    
    CWAlerts: List[CWAlert] = []
    for alert in alerts:
        a = CWAlert()
        a.alarmName = alert['AlarmName']
        a.alarmArn = alert['AlarmArn']
        a.alarmDescription = alert['AlarmDescription']
        a.alarmConfigurationUpdatedTimestamp = alert['AlarmConfigurationUpdatedTimestamp']
        a.okActions = alert['OKActions']
        a.alarmActions = alert['AlarmActions']
        a.stateValue = alert['StateValue']
        
        CWAlerts.append(a)
    return CWAlerts
        
def create_valid_html(CWAlerts: List[CWAlert]):
    '''
        Creates a html report from the given alerts
    '''

    file = open(f'alert_audit_{datetime.now().strftime("%Y%m%d-%H:%M:%S")}.html', 'w')
    html = '''
        <html>
        <head>
            <style>
                body, html {
                    font-family: system-ui;
                    font-size: 1em;
                }
                table {
                    width: 100%;
                    font-size: 0.4em;
                }
                table tr th {
                    background-color: whitesmoke;
                }
                    table, td, th {
                        border:1px solid black;
                        border-collapse: collapse;
                    }
                    td, th {
                        padding: 5px;
                    }
                </style>
            </head>
            <body>
                <h1>AWS Cloudwatch Alert Audit<h1>
                <table>
        '''

    if (len(CWAlerts) > 0):
        html += '<tr>'
        html += '<th>index</th>'
        for key, value in CWAlerts[0]:
            html += f'<th>{key}</th>'
        html += '</tr>'

        index = 0
        for alert in CWAlerts:
            html += f'<td>{index}</td>'
            for key, value in alert:
                html += f'<td>{value}</td>'
            html += '</tr>'
            index += 1

        html += '</table></body></html>'

        file.write(html)
        file.close()
        
def generate_cloudwatch_alert_audit():
    '''
    Fetches all Cloudwatch Alerts based on query filters and generates HTML Report
    https://awscli.amazonaws.com/v2/documentation/api/latest/reference/cloudwatch/describe-alarms.html
    '''
    
    # fetch all alerts and create a html report
    alerts = [alert for alert in fetch_alerts()]
    create_valid_html(CWAlerts=alerts)

    print(f'Finished, Found: {len(alerts)} alerts(s)')

if __name__ == '__main__':
    generate_cloudwatch_alert_audit()
