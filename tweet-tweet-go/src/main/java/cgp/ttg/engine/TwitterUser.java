package cgp.ttg.engine;

import org.json.JSONObject;

public class TwitterUser {
    public String userId;
    public String name;
    public long followersCount;
    public long friendsCount;
    public String topic;
    public String location;
    public String description;
    public boolean verified;
    public long listedCount;
    public long favouritesCount;
    public long statusesCount;

    public TwitterUser(String userId, String name, long followersCount, long friendsCount, String topic, String location, String description, boolean verified, long listedCount, long favouritesCount, long statusesCount) {
        this.userId = userId;
        this.name = name;
        this.followersCount = followersCount;
        this.friendsCount = friendsCount;
        this.topic = topic;
        this.location = location;
        this.description = description;
        this.verified = verified;
        this.listedCount = listedCount;
        this.favouritesCount = favouritesCount;
        this.statusesCount = statusesCount;
    }

    public TwitterUser(String json, String topic) {
    	var obj = new JSONObject(json);
        this.userId = obj.getString("id_str");
        this.name = obj.getString("screen_name");
        this.followersCount = obj.getLong("followers_count");
        this.friendsCount = obj.getLong("friends_count");
        this.topic = topic;
        this.location = obj.getString("location");
        this.description = obj.getString("description");
        this.verified = obj.getBoolean("verified");
        this.listedCount = obj.getLong("listed_count");
        this.favouritesCount = obj.getLong("favourites_count");
        this.statusesCount = obj.getLong("statuses_count");
    }
}
