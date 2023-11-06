FROM python:3.9-slim as base
COPY . /app
WORKDIR /app
RUN dir
RUN mkdir -p logs
RUN pip3 install --upgrade pip
RUN pip3 install -r requirements.txt
WORKDIR /app/services/server-load/controller
CMD ["python3", "controller.py"]
