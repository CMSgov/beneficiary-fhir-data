FROM public.ecr.aws/lambda/python:3.11 as base

FROM public.ecr.aws/lambda/python:3.11 as prereqs
RUN yum install git gcc python3-devel -y

# Build required python packages
FROM prereqs as python
COPY requirements.txt .
RUN pip3 install -r requirements.txt

# Copy function code
FROM base as node
COPY --from=python /var/lang/lib/python3.11/site-packages /var/lang/lib/python3.11/site-packages
COPY --from=python /var/lang/bin /var/lang/bin
COPY . "${LAMBDA_TASK_ROOT}/locust_tests"
RUN mv $LAMBDA_TASK_ROOT/locust_tests/services/server-load/* $LAMBDA_TASK_ROOT/
ENV PATH="/var/lang/lib/python3.11/site-packages:${PATH}"
ENV NODE_LOCUST_TESTS_DIR="${LAMBDA_TASK_ROOT}/locust_tests"
CMD [ "node/node.handler" ]

