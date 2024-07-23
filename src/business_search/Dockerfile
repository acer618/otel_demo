FROM docker-images.yelpcorp.com/jammy_yelp:latest

RUN apt-get update \
     && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
     libpython3.9 \
     && apt-get clean

WORKDIR /usr/src/app/
COPY ./requirements.txt ./

RUN pip install --no-cache-dir -r requirements.txt

COPY ./src/business_search/ ./

EXPOSE ${BUSINESS_SEARCH_PORT}
ENTRYPOINT [ "python", "app.py" ]
