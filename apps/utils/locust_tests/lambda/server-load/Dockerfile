FROM public.ecr.aws/lambda/python:3.8 as base

FROM public.ecr.aws/lambda/python:3.8 as prereqs
RUN  yum install git gcc python3-devel -y

FROM prereqs as python
# Install the function's dependencies using file requirements.txt
# from your project folder.
COPY requirements.txt .
RUN  pip3 install -r requirements.txt

# Copy function code
FROM base as node
COPY --from=python /var/lang/lib/python3.8/site-packages /var/lang/lib/python3.8/site-packages
COPY --from=python /var/lang/bin /var/lang/bin
COPY . ${LAMBDA_TASK_ROOT}
# Copy function code
ENV  PATH="/var/lang/lib/python3.8/site-packages:${PATH}"
RUN cp "${LAMBDA_TASK_ROOT}/lambda/server-load/node.py" "${LAMBDA_TASK_ROOT}/app.py"
CMD [ "app.handler" ]
