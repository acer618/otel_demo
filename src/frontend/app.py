from flask import Flask, request
import requests

from py_zipkin import get_default_tracer
from py_zipkin.zipkin import zipkin_client_span
from py_zipkin.zipkin import zipkin_span
from py_zipkin.zipkin import Kind
from py_zipkin.zipkin import Endpoint
from py_zipkin.transport import BaseTransportHandler
from py_zipkin.request_helpers import create_http_headers

frontend_port = 8880
search_port = 9091

app = Flask(__name__)

@app.route('/')
def home():
    return 'Welcome!'


def get_business_list():
    url = "http://search:9091/api/businesses"
    with zipkin_client_span(
        service_name='frontend', 
        span_name='GET /api/businesses',
    ) as zipkin_context:
        zipkin_context.kind = Kind.CLIENT
        zipkin_context.remote_endpoint = Endpoint(service_name='search', port=search_port, ipv4='', ipv6='')
        headers=create_http_headers(zipkin_context, get_default_tracer(), False)
        print(headers)
        response = requests.get(
                        url=url,
                        headers=headers,
                    )
        
        print(response)
        if response.status_code == 200:
            return response.text
        else:
            return "Error: Unable to fetch the business list"

def get_business_list1():
    return "hi"

@app.route('/search')
def search():
    with zipkin_span(
        service_name='frontend', 
        span_name='GET /search',
        transport_handler=HttpTransport(),
        sample_rate=100.0,
    ) as zipkin_context:
        print(request.headers)
        zipkin_context.kind = Kind.SERVER
        zipkin_context.update_binary_annotations({'app.force_sample': "true"})
        return get_business_list()
     


class HttpTransport(BaseTransportHandler):

    def get_max_payload_bytes(self):
        return None

    def send(self, encoded_span):
        print(encoded_span)
        response = requests.post(
           'http://169.254.255.254:9411/api/v2/spans',
           data=encoded_span,
           headers={'Content-Type': 'application/json'}, 
        )
        print(response)

        
if __name__ == '__main__':
    app.run(port=frontend_port, host='0.0.0.0')
