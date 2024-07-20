from flask import Flask, request, jsonify

from py_zipkin.zipkin import zipkin_span
from py_zipkin.transport import BaseTransportHandler
from py_zipkin.zipkin import create_http_headers_for_new_span


business_search_port = 8082

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
    with zipkin_span(
        service_name='business_search', 
        span_name='GET /businesses',
        transport_handler=HttpTransport(),
    ):
        print(request.headers)  # Print incoming request headers
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
    app.run(port=business_search_port)