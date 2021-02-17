FROM python:3-alpine

WORKDIR /usr/share/app
COPY requirements.txt .
RUN apk update && python -m pip install --upgrade pip && pip install -r requirements.txt 
ENTRYPOINT ["python", "redis-client.py"]

