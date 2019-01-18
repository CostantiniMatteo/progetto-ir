from flask import Flask, render_template, request, abort, make_response, redirect, url_for, jsonify
from flask_cors import *
import requests
import json


app = Flask(__name__, static_url_path='')
CORS(app)
app.config['CORS_HEADERS'] = 'Content-Type'


BASE_URL = "http://localhost:8080/"

@app.route('/', methods = ['GET', 'POST'])
@cross_origin()
def home():

    if 'q' in request.args:
        params = request.args.to_dict(flat=True)
        print(params)

        r = requests.get(BASE_URL + "search", params=params)
        print(r)
        r_json = r.json()
        return jsonify(r_json)

    else:
        r = requests.get(BASE_URL + "users")
        print(r.json())
        return render_template('index.html', users=r.json())



@app.route('/update-profile', methods = ['GET'])
def update_profile():
    r = requests.post(BASE_URL + "update-profile/" + request.args.get("topic") + "/" + request.args.get("tweetId"))
    print(r)
    return ''



if __name__ == '__main__':
    app.run(debug=True)
