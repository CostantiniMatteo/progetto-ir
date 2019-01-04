package cgp;

import java.util.List;

public class Tweet {
    public long createdAt;
    public String tweetId;
    public String text;
    public List<String> hashtags;
    public List<String> urls;
    public List<String> userMentions;
    public List<String> mediaUrls;
    public String replyToId;
    public String geo;
    public String coordinates;
    public boolean isQuote;
    public long retweetCount;
    public long favoriteCount;
}
