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
        r = requests.get(BASE_URL + "search", params={'q': request.args.get('q')})
        r_json = r.json()
        # tweetIds = [d['tweetId'] for d in r_json]
        # tweet_html = []
        # for tweetId in tweetIds:
        #     try:
        #         r_tweet = requests.get(EMBED_URL + tweetId).json()
        #         tweet_html += [r_tweet['html']]
        #     except:
        #         pass

        # return render_template('index.html', tweet_html=tweet_html)
        return jsonify(r_json)

    else:

        return render_template('index.html')

@app.route('/tweet/html', methods = ['GET'])
def get_tweet_html():
    t_id = request.args.get("tweetId")
    print(t_id)
    r = requests.get(EMBED_URL + str(t_id))
    print(r.json())
    return jsonify(r.json())






if __name__ == '__main__':
    app.run(debug=True)
