from flask import Flask, render_template, request, abort, make_response, redirect, url_for, jsonify
from flask_cors import *
import requests
import json


app = Flask(__name__, static_url_path='')
CORS(app)
app.config['CORS_HEADERS'] = 'Content-Type'


BASE_URL = "http://localhost:8080/"
EMBED_URL = "https://publish.twitter.com/oembed?url=https://twitter.com/Interior/status/"

@app.route('/', methods = ['GET', 'POST'])
@cross_origin()
def home():

    if 'q' in request.args:
        params = request.args.to_dict(flat=True)
        print(params)

        r = requests.get(BASE_URL + "search", params=params)
        print(r)
        r_json = r.json()
        return jsonify(r_json["results"])

    else:
        r = requests.get(BASE_URL + "users")
        print(r.json())
        return render_template('index.html', users=r.json())

@app.route('/tweet/html', methods = ['GET'])
def get_tweet_html():
    t_id = request.args.get("tweetId")
    print(t_id)
    r = requests.get(EMBED_URL + str(t_id))

    if r.status_code != 200:
        print(t_id)
        return ('', 204)

    return jsonify(r.json())






if __name__ == '__main__':
    app.run(debug=True)
