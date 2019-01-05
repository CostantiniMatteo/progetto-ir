### See PIN-based authorization for details at
### https://dev.twitter.com/docs/auth/pin-based-authorization

import tweepy

consumer_key=<your_app_consumer_key>
consumer_secret=<your_app_consumer_secret>

auth = tweepy.OAuthHandler(consumer_key, consumer_secret)

# get access token from the user and redirect to auth URL
auth_url = auth.get_authorization_url()
print 'Authorization URL: ' + auth_url

# ask user to verify the PIN generated in broswer
verifier = raw_input('PIN: ').strip()
auth.get_access_token(verifier)
print 'ACCESS_KEY = "%s"' % auth.access_token.key
print 'ACCESS_SECRET = "%s"' % auth.access_token.secret

# authenticate and retrieve user name
auth.set_access_token(auth.access_token.key, auth.access_token.secret)
api = tweepy.API(auth)
username = api.me().name
print 'Ready to post to ' + username
