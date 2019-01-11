package cgp.ttg.engine;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class Tweet {
    public long createdAt;
    public TwitterUser user;
    public String tweetId;
    public String text;
    public List<String> hashtags;
    public List<String> urls;
    public List<String> userMentions;
    public List<Media> media;
    public String replyToId;
    public String geo;
    public String coordinates;
    public boolean isQuote;
    public boolean isRetweet;
    public long retweetCount;
    public long favoriteCount;
    public String lang;

    public class Media {
        public String type;
        public String url;

        public Media(String type, String url) {
            this.type = type;
            this.url = url;
        }
    }

    public Tweet(String json, TwitterUser user) {
        this.user = user;

        var obj = new JSONObject(json);
        this.createdAt = getTimestampFromDateString(obj.getString("created_at"));
        this.tweetId = obj.getString("id_str");
        this.text = obj.getString("full_text");
        this.geo = null;
        this.coordinates = null;
        this.isQuote = obj.getBoolean("is_quote_status");
        this.retweetCount = obj.getLong("retweet_count");
        this.favoriteCount = obj.getLong("favorite_count");
        this.lang = obj.getString("lang");
        this.isRetweet = obj.has("retweeted_status");
        if (this.isRetweet) {
            this.text = obj.getJSONObject("retweeted_status").getString("full_text");
        }

        var entities = obj.getJSONObject("entities");
        this.hashtags = stringListFromJsonArray(entities.getJSONArray("hashtags"), "text");
        this.urls = stringListFromJsonArray(entities.getJSONArray("urls"), "expanded_url");
        this.userMentions = stringListFromJsonArray(entities.getJSONArray("user_mentions"), "screen_name");

        this.media = new ArrayList<Media>();
        if (obj.has("extended_entities")) {
            var extendedEntities = obj.getJSONObject("extended_entities");
            var mediaJsonArray = extendedEntities.getJSONArray("media");
            for (var i = 0; i < mediaJsonArray.length(); i++) {
                var itemObj = mediaJsonArray.getJSONObject(i);
                this.media.add(new Media(itemObj.getString("type"), itemObj.getString("expanded_url")));
            }
        }

        var replyObj = obj.get("in_reply_to_status_id_str");
        this.replyToId = replyObj != JSONObject.NULL ? replyObj.toString() : null;
    }

    public static long getTimestampFromDateString(String date) {
        var simpleDateFormat = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyy");
        try {
            var dateObj = simpleDateFormat.parse(date);
            return dateObj.getTime() / 1000;
        } catch (ParseException e) {
            return 0;
        }
    }

    public static ArrayList<String> stringListFromJsonArray(JSONArray jsonArray, String field) {
        var result = new ArrayList<String>(jsonArray.length());
        for (var i = 0; i < jsonArray.length(); i++) {
            var itemObj = jsonArray.getJSONObject(i);
            result.add(itemObj.getString(field));
        }
        return result;
    }
}
