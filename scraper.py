import json, tweepy, psycopg2, sys, multiprocessing, threading
from tqdm import tqdm


ACCESS_PAIRS = [
    (
        "2500103229-hDp4JG4zj1BaORNg9vd2Dkq8YLMtiNIYPW4QrlA",
        "SuwYfgKEdjbPQ5uLPCQzkp2aC4DPsnrGfnrNcizhUvDEc",
    ),  # Costa
    (
        "1077975711607083008-FK1SkjUXlr30jAd5UVxzU4b8Y5FEOp",
        "pfGTqxH34BH8iLYpvQGhRjDqflYdtquegtrYySSoF2NWF",
    ),  # Dario
    (
        "1077970775313993729-lrCK9ZGCw34oMt8zVfmkOEnAtRfBPb",
        "Etl3o7nk81X0mf5AsNJpc6znckikpzrJ7OaI9x5zUFpWk",
    ),  # Michele
]
ACCESS_TOKEN, ACCESS_SECRET = ACCESS_PAIRS[0]
CONSUMER_KEY = "f7SdIH8WMrLE9JmHVqpsQAO7T"
CONSUMER_SECRET = "ShJhcYAD5LU48fPcPqB2Xs3i9q7Wo7v91u6MvvgXF9ycFBfyan"

THRESHOLD = 100
USER_LIMIT = 900
APP_LIMIT = 1500

# Twitter API development use pagination for Iterating through
# timelines, user lists, direct messages, etc. To help make pagination
# easier and Tweepy has the Cursor object.


def get_db_connection(db="progetto_ir", user="matteo", host="localhost", pw=""):
    try:
        conn = psycopg2.connect(
            f"dbname='{db}' user='{user}' host='{host}' password='{pw}'"
        )
        conn.autocommit = True
    except:
        print("Unable to connect to the database. ¯\\_(ツ)_/¯")
        raise Exception("Unable to connect to the database. ¯\\_(ツ)_/¯")

    return conn


def get_twitter_app_api(cons_key=CONSUMER_KEY, cons_secr=CONSUMER_SECRET):
    auth = tweepy.AppAuthHandler(cons_key, cons_secr)
    api = tweepy.API(
        auth,
        wait_on_rate_limit=True,
        wait_on_rate_limit_notify=True,
        compression=True,
    )

    return api


def get_twitter_user_api(
    cons_key=CONSUMER_KEY,
    cons_secr=CONSUMER_SECRET,
    acc_tok=ACCESS_TOKEN,
    acc_secr=ACCESS_SECRET,
):
    auth = tweepy.OAuthHandler(cons_key, cons_secr)
    auth.set_access_token(acc_tok, acc_secr)
    api = tweepy.API(
        auth,
        wait_on_rate_limit=True,
        wait_on_rate_limit_notify=True,
        compression=True,
    )

    return api


def read_lists_and_topics(filename="lists.dat"):
    with open(filename, "r") as lists_file:
        lists = [line.split() for line in lists_file.read().split("\n")[:-1]]
        topics = [item[0] for item in lists]

    return lists, topics


def process_lists(lists, conn, api):
    for topic, list_name, owner in tqdm(lists):
        res = tweepy.Cursor(
            api.list_members, slug=list_name, owner_screen_name=owner
        )
        members = [
            (
                user.screen_name,
                user.followers_count,
                topic,
                json.dumps(user._json),
            )
            for user in res.items()
        ]
        cur = conn.cursor()
        cur.executemany(
            """INSERT INTO
                twitter_user(user_id, followers, topic, json, processed)
                    VALUES (%s, %s, %s, %s, FALSE)""",
            members,
        )
        cur.close()


def process_tweet(tweet):
    tweet_id = tweet.id
    screen_name = tweet.user.screen_name
    del tweet._json["user"]
    return (tweet_id, screen_name, json.dumps(tweet._json))


def process_users(queue, conn, api, id=0):
    while not queue.empty():
        screen_name, processed = queue.get()
        if processed:
            queue.task_done()
            continue

        print(f"(id: {id}) Processing user {screen_name}.")

        status_cursor = tweepy.Cursor(
            api.user_timeline, screen_name=screen_name, tweet_mode="extended"
        )

        try:
            tweets = [
                process_tweet(tweet) for tweet in status_cursor.items(3200)
            ]
        except Exception:
            print(f"(id: {id} Failed to fetch tweets for user {screen_name}.")
            queue.task_done()
            continue

        cur = conn.cursor()
        try:
            cur.executemany(
                """INSERT INTO tweets(tweet_id, user_id, json)
                    VALUES (%s, %s, %s)""",
                tweets,
            )
            cur.execute(
                "UPDATE twitter_user SET processed = TRUE WHERE user_id = %s;",
                (screen_name,),
            )
            print(f"(id: {id}) User {screen_name} done.")
        except Exception:
            print(f"(id: {id}) Failed when communicating with database.")
            cur.execute("DELETE FROM tweets WHERE user_id = %s", (screen_name,))
        finally:
            cur.close()
            queue.task_done()


class LoggedJoinableQueue(object):
    def __init__(self, maxsize=0):
        self._jq = multiprocessing.JoinableQueue(maxsize=maxsize)
        self._done = 0
        self._done_lock = threading.Lock()

    def __getattr__(self, name):
        return getattr(self._jq, name)

    def task_done(self):
        self._jq.task_done()
        with self._done_lock:
            self._done += 1
            print(f"Item processed")
            sys.stdout.flush()


if __name__ == "__main__":
    lists, topics = read_lists_and_topics()
    apis = [get_twitter_app_api()] + [
        get_twitter_user_api(acc_tok=acc_tok, acc_secr=acc_secr)
        for acc_tok, acc_secr in ACCESS_PAIRS
    ]
    conns = [get_db_connection() for i in range(len(ACCESS_PAIRS) + 1)]

    # Needs to be called just once
    # process_lists(lists, conns[0], apis[0])

    cur = conns[0].cursor()
    cur.execute(
        # """
        # select user_id, processed
        # from (select user_id, followers, topic, processed, row_number()
        #     over (partition by topic order by followers desc) as rownum
        #     from twitter_user) tmp where rownum <= {threshold};
        # """.format(
        #     threshold=THRESHOLD
        # )
        "select user_id, processed from twitter_user where processed = false order by followers desc limit(1000);"
    )
    users = cur.fetchall()
    cur.close()

    queue = LoggedJoinableQueue(maxsize=len(users))
    for user in users:
        queue.put(user)

    assert len(conns) == len(apis)

    jobs = [
        multiprocessing.Process(
            target=process_users, args=(queue, conns[i], apis[i], i)
        )
        for i in range(0, len(apis))
    ]

    for j in jobs:
        j.start()
    queue.join()

# La query della vita:
# select * from (select user_id, followers, topic, row_number()
#    over (partition by topic order by followers desc) as rownum
#    from twitter_user) tmp where rownum <= 100;
