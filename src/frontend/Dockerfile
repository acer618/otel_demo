FROM    docker-images.yelpcorp.com/jammy_yelp:latest

# python and uwsgi deps
RUN     apt-get update \
        && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
            dumb-init \
	    python3-venv \
            python3 \
        && apt-get clean

WORKDIR /usr/src/app/
RUN     python3 -m venv ./venv 

# Enable venv
ENV PATH="./venv/bin:$PATH"

COPY ./src/frontend/requirements.txt .
RUN pip install -r requirements.txt

COPY ./src/frontend/ /usr/src/app

EXPOSE 9990
CMD [ "python", "app.py" ]
