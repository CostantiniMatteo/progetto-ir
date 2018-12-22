import json, tweepy, psycopg2
from tqdm import tqdm

ACCESS_TOKEN = "2500103229-hDp4JG4zj1BaORNg9vd2Dkq8YLMtiNIYPW4QrlA"
ACCESS_SECRET = "SuwYfgKEdjbPQ5uLPCQzkp2aC4DPsnrGfnrNcizhUvDEc"
CONSUMER_KEY = "f7SdIH8WMrLE9JmHVqpsQAO7T"
CONSUMER_SECRET = "ShJhcYAD5LU48fPcPqB2Xs3i9q7Wo7v91u6MvvgXF9ycFBfyan"

auth = tweepy.OAuthHandler(CONSUMER_KEY, CONSUMER_SECRET)
auth.set_access_token(ACCESS_TOKEN, ACCESS_SECRET)

# Twitter API development use pagination for Iterating through
# timelines, user lists, direct messages, etc. To help make pagination
# easier and Tweepy has the Cursor object.

try:
    conn = psycopg2.connect(
        "dbname='progetto_ir' user='matteo' host='localhost' password=''"
    )
    conn.autocommit = True
except:
    print("I am unable to connect to the database. ¯\\_(ツ)_/¯")


api = tweepy.API(
    auth,
    wait_on_rate_limit=True,
    wait_on_rate_limit_notify=True,
    compression=True,
)

with open("lists.dat", "r") as lists_file:
    lists = [line.split() for line in lists_file.read().split("\n")[:-1]]
    topics = [item[0] for item in lists]

for topic, list_name, owner in tqdm(lists):
    res = tweepy.Cursor(
        api.list_members, slug=list_name, owner_screen_name=owner
    )
    members = [
        (user.screen_name, user.followers_count, topic, json.dumps(user._json))
        for user in res.items()
    ]
    cur = conn.cursor()
    cur.executemany(
        "INSERT INTO twitter_user(user_id, followers, topic, json) VALUES (%s, %s, %s, %s)",
        members,
    )
    cur.close()


def process_tweet(tweet):
    tweet_id = tweet.id
    screen_name = tweet.user.screen_name
    del tweet._json["user"]
    return (tweet_id, screen_name, tweet._json)


for topic in topics:
    # Get top N users from topic list
    cur = conn.cursor()
    cur.execute(
        """
        select user_id from (select user_id, followers, topic, row_number()
            over (partition by topic order by followers desc) as rownum
            from twitter_user) tmp where rownum <= 100;
        """
    )
    users = [item[0] for item in cur.fetchall()]
    cur.close()
    for screen_name in users:
        status_cursor = tweepy.Cursor(
            api.user_timeline, screen_name=screen_name, tweet_mode="extended"
        )
        tweets = [process_tweet(tweet) for tweet in status_cursor.items(3200)]
        cur = conn.cursor()
        cur.executemany(
            "INSERT INTO tweets(tweet_id, user_id, json) VALUES (%s, %s, %s)",
            tweets,
        )
        cur.close()

# La query della vita:
# select * from (select user_id, followers, topic, row_number()
#    over (partition by topic order by followers desc) as rownum
#    from twitter_user) tmp where rownum <= 100;
