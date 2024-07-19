from flask import Flask

app = Flask(__name__)

@app.route('/')
def home():
    return 'Welcome!'

@app.route('/search')
def search():
    return 'Restaurant1!'

if __name__ == '__main__':
    app.run(port=8080)