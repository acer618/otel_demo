from flask import Flask, request, jsonify

from py_zipkin.zipkin import zipkin_span
from py_zipkin.util import ZipkinAttrs
from py_zipkin.transport import BaseTransportHandler
from py_zipkin.zipkin import create_http_headers_for_new_span

import requests

business_search_port = 9082

app = Flask(__name__)

@app.route('/')
def home():
    return 'Welcome!'


def get_business_list():
    return jsonify([
        {
            'name': 'Business 1',
            'location': 'Location 1',
        },
        {
            'name': 'Business 2',
            'location': 'Location 2',
        },
    ])  

@app.route('/businesses')
def businesses():
    headers = request.headers
    print(headers)  # Print incoming request headers
    zipkin_attrs = ZipkinAttrs(
        trace_id=headers.get("X-B3-Traceid"),
        #span_id=generate_random_64bit_string(),
        span_id=headers.get("X-B3-Spanid"),
        parent_span_id=headers.get("X-B3-Parent-Spanid"),
        flags='0',
        is_sampled=headers.get("X-B3-Sampled"),
    )
    with zipkin_span(
        service_name='business_search', 
        span_name='GET /businesses',
        transport_handler=HttpTransport(),
        zipkin_attrs=zipkin_attrs,
    ):
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
    app.run(port=business_search_port)
