FROM amazon/cloudwatch-agent as agent
COPY config.json /etc/cwagentconfig

FROM python:3.9-slim as base
COPY . /app
WORKDIR /app
RUN dir
RUN pip install -r requirements.txt
WORKDIR /app/services/server-load/controller
CMD ["python3", "controller.py"]
