FROM public.ecr.aws/lambda/python:3.8 as base

FROM public.ecr.aws/lambda/python:3.8 as prereqs
RUN yum install git gcc python3-devel -y

# Build required python packages
FROM prereqs as python
COPY requirements.txt .
RUN pip3 install -r requirements.txt

# Copy function code
FROM base as node
COPY --from=python /var/lang/lib/python3.8/site-packages /var/lang/lib/python3.8/site-packages
COPY --from=python /var/lang/bin /var/lang/bin
COPY . ${LAMBDA_TASK_ROOT}
ENV PATH="/var/lang/lib/python3.8/site-packages:${PATH}"
WORKDIR "${LAMBDA_TASK_ROOT}/services/server-load/node"
CMD $LAMBDA_TASK_ROOT/services/server-load/node/node.handler
