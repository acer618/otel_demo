FROM    docker-images.yelpcorp.com/jammy_yelp:latest

# python and uwsgi deps
RUN     apt-get update \
        && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
            dumb-init \
	    python3-venv \
            python3 \
	    iputils-ping \
        && apt-get clean

WORKDIR /usr/src/app/
RUN     python3 -m venv ./venv 

# Enable venv
ENV PATH="./venv/bin:$PATH"

COPY ./src/business_search/requirements.txt .
RUN pip install -r requirements.txt

COPY ./src/business_search/ /usr/src/app

#EXPOSE ${BUSINESS_SEARCH_PORT}
EXPOSE 9082 
CMD [ "python", "app.py" ]
