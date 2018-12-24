import json, tweepy, psycopg2
from tqdm import tqdm

ACCESS_TOKEN = "2500103229-hDp4JG4zj1BaORNg9vd2Dkq8YLMtiNIYPW4QrlA"
ACCESS_SECRET = "SuwYfgKEdjbPQ5uLPCQzkp2aC4DPsnrGfnrNcizhUvDEc"
CONSUMER_KEY = "f7SdIH8WMrLE9JmHVqpsQAO7T"
CONSUMER_SECRET = "ShJhcYAD5LU48fPcPqB2Xs3i9q7Wo7v91u6MvvgXF9ycFBfyan"

THRESHOLD = 100

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


auth = tweepy.OAuthHandler(CONSUMER_KEY, CONSUMER_SECRET)
auth.set_access_token(ACCESS_TOKEN, ACCESS_SECRET)
api = tweepy.API(
    auth,
    wait_on_rate_limit=True,
    wait_on_rate_limit_notify=True,
    compression=True,
)

with open("lists.dat", "r") as lists_file:
    lists = [line.split() for line in lists_file.read().split("\n")[:-1]]
    topics = [item[0] for item in lists]

# for topic, list_name, owner in tqdm(lists):
#     res = tweepy.Cursor(
#         api.list_members, slug=list_name, owner_screen_name=owner
#     )
#     members = [
#         (user.screen_name, user.followers_count, topic, json.dumps(user._json))
#         for user in res.items()
#     ]
#     cur = conn.cursor()
#     cur.executemany(
#         "INSERT INTO twitter_user(user_id, followers, topic, json, processed) VALUES (%s, %s, %s, %s, FALSE)",
#         members,
#     )
#     cur.close()


def process_tweet(tweet):
    tweet_id = tweet.id
    screen_name = tweet.user.screen_name
    del tweet._json["user"]
    return (tweet_id, screen_name, json.dumps(tweet._json))


failed = []
for topic in topics:
    print(f"===== Topic: {topic} =====")
    # Get top N users from topic list
    cur = conn.cursor()
    cur.execute("select user_id, processed from twitter_user"
        # """
        # select user_id, processed from (select user_id, followers, topic, processed, row_number()
        #     over (partition by topic order by followers desc) as rownum
        #     from twitter_user) tmp where rownum <= {threshold};
        # """.format(threshold=THRESHOLD)
    )
    users = cur.fetchall()
    cur.close()
    i = 1; total = len(users) // 3
    for screen_name, processed in users[:total]:
        if processed: continue
        print(f"[{i}/{total}] Processing user {screen_name}.", end=' ')

        status_cursor = tweepy.Cursor(
            api.user_timeline, screen_name=screen_name, tweet_mode="extended"
        )

        try:
            tweets = [process_tweet(tweet) for tweet in status_cursor.items(3200)]
        except Exception:
            print("Failed to fetch tweets.")
            failed.append((screen_name, topic))
            continue

        cur = conn.cursor()
        try:
            cur.executemany(
                "INSERT INTO tweets(tweet_id, user_id, json) VALUES (%s, %s, %s)",
                tweets,
            )
            # import ipdb; ipdb.set_trace()
            cur.execute(
                "UPDATE twitter_user SET processed = TRUE WHERE user_id = %s;",
                (screen_name,))
            print("Done.")
        except Exception:
            print(f"Failed when communicating with database.")
            cur.execute("DELETE FROM tweets WHERE user_id = %s", (screen_name,))
        finally:
            cur.close()
            i += 1


# La query della vita:
# select * from (select user_id, followers, topic, row_number()
#    over (partition by topic order by followers desc) as rownum
#    from twitter_user) tmp where rownum <= 100;
