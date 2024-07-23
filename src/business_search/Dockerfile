FROM    docker-images.yelpcorp.com/jammy_yelp:latest

# python and uwsgi deps
RUN     apt-get update \
        && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
            dumb-init \
#            libcap2 \
#            libxml2 \
#            libyaml-0-2 \
#            python3-distutils \
	    python3-venv \
            python3 \
        && apt-get clean

WORKDIR /usr/src/app/
RUN     python3 -m venv ./venv 

# Enable venv
ENV PATH="/usr/src/app/venv/bin:$PATH"

COPY ./requirements.txt /usr/src/app/requirements.txt
RUN pip install -r requirements.txt

COPY . .

ENTRYPOINT [ "python", "app.py" ]
