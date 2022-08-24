FROM public.ecr.aws/lambda/python:3.8 as base
RUN  yum install git gcc python3-devel -y

FROM base as python
# Install the function's dependencies using file requirements.txt
# from your project folder.
COPY requirements.txt .
RUN  pip3 install -r requirements.txt
ENV  PATH="/var/lang/lib/python3.8/site-packages:${PATH}"

FROM python as broker
# Copy function code
COPY . ${LAMBDA_TASK_ROOT}
RUN cp "${LAMBDA_TASK_ROOT}/lambda/server-load/broker.py" "${LAMBDA_TASK_ROOT}/app.py"
CMD [ "app.handler" ]

FROM python as controller
# Copy function code
COPY . ${LAMBDA_TASK_ROOT}
RUN cp "${LAMBDA_TASK_ROOT}/lambda/server-load/controller.py" "${LAMBDA_TASK_ROOT}/app.py"
CMD [ "app.handler" ]

FROM python as node
# Copy function code
COPY . ${LAMBDA_TASK_ROOT}
RUN cp "${LAMBDA_TASK_ROOT}/lambda/server-load/node.py" "${LAMBDA_TASK_ROOT}/app.py"
# Set the CMD to your handler (could also be done as a parameter override outside of the Dockerfile)
CMD [ "app.handler" ]
