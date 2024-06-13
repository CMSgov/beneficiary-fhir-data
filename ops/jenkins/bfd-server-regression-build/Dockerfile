FROM public.ecr.aws/lambda/python:3.11 as base
RUN  yum install git gcc python3-devel -y

FROM base as python
# Install the function's dependencies using file requirements.txt
# from your project folder.
COPY requirements.txt .
RUN  pip3 install -r requirements.txt
ENV  PATH="/var/lang/lib/python3.11/site-packages:${PATH}"

FROM python as app
# Copy function code
COPY . ${LAMBDA_TASK_ROOT}
RUN mv "${LAMBDA_TASK_ROOT}/lambda/server-regression/app.py" "${LAMBDA_TASK_ROOT}"
RUN rm -rf "${LAMBDA_TASK_ROOT}/lambda"

# Set the CMD to your handler (could also be done as a parameter override outside of the Dockerfile)
CMD [ "app.handler" ]
