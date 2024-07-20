from flask import Flask
import requests

from py_zipkin.zipkin import zipkin_span
from py_zipkin.transport import BaseTransportHandler
from py_zipkin.zipkin import create_http_headers_for_new_span

business_search_port = 8081

app = Flask(__name__)

@app.route('/')
def home():
    return 'Welcome!'


def get_business_list():
    url = "http://localhost:8081/api/businesses"
    response = requests.get(
                    headers=create_http_headers_for_new_span(),
                    url=url
                )
    print(response)
    if response.status_code == 200:
        return response.text
    else:
        return "Error: Unable to fetch the business list"

@app.route('/search')
def search():
    with zipkin_span(
        service_name='business_search', 
        span_name='GET /search',
        transport_handler=HttpTransport(),
        sample_rate=100.0,
    ):
        return get_business_list()   
     


class HttpTransport(BaseTransportHandler):

    def get_max_payload_bytes(self):
        return None

    def send(self, encoded_span):
        # The collector expects a thrift-encoded list of spans.
        pass
"""         requests.post(
            'http://localhost:9411/api/v1/spans',
            data=encoded_span,
            headers={'Content-Type': 'application/x-thrift'}, 
            )
"""
        

if __name__ == '__main__':
    app.run(port=8080)