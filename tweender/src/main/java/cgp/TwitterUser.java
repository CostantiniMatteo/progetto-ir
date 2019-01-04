package cgp;

public class TwitterUser {
    public String userId;
    public String name;
    public long followersCount;
    public long friendsCount;
    public String topic;
    public String location;
    public String description;
    public boolean isVerified;
    public long listedCount;
    public long favouritesCount;
    public long statusedCount;

    public TwitterUser(String userId, String name, long followersCount, long friendsCount, String topic, String location, String description, boolean isVerified, long listedCount, long favouritesCount, long statusedCount) {
        this.userId = userId;
        this.name = name;
        this.followersCount = followersCount;
        this.friendsCount = friendsCount;
        this.topic = topic;
        this.location = location;
        this.description = description;
        this.isVerified = isVerified;
        this.listedCount = listedCount;
        this.favouritesCount = favouritesCount;
        this.statusedCount = statusedCount;
    }

    public TwitterUser() {

    }
}
