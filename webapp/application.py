from flask import Flask, render_template, request, abort, make_response, redirect, url_for
import requests
import json


app = Flask(__name__, static_url_path='')

BASE_URL = "http://localhost:8080/"
EMBED_URL = "https://publish.twitter.com/oembed?url=https://twitter.com/Interior/status/"

@app.route('/', methods = ['GET', 'POST'])
def home():

    if 'q' in request.args:
        r = requests.get(BASE_URL + "search", params={'q': request.args.get('q')})
        r_json = r.json()
        tweetIds = [d['tweetId'] for d in r_json]
        tweet_html = []
        for tweetId in tweetIds:
            try:
                r_tweet = requests.get(EMBED_URL + tweetId).json()
                tweet_html += [r_tweet['html']]
            except:
                pass

        return render_template('index.html', tweet_html=tweet_html)

    else:

        return render_template('index.html')


if __name__ == '__main__':
    app.run(debug=True)
