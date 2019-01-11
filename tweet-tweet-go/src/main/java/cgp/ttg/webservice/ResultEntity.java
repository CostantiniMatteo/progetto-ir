package cgp.ttg.webservice;

import java.util.Date;

public class ResultEntity {
    public long rank;
    public String tweetId;
    public String author;
    public long retweetCount;
    public long favoriteCount;
    public Date date;
    public String text;
    public float score;
    public float luceneScore;
    public float frScore;
    public float urlScore;
    public float lengthScore;
    public float retweetScore;
    public float qrScore;

    public ResultEntity(long rank, String tweetId, String author, long retweetCount, long favoriteCount, Date date, String text, float score, float luceneScore, float frScore, float urlScore, float lengthScore, float retweetScore, float qrScore) {
        this.rank = rank;
        this.tweetId = tweetId;
        this.author = author;
        this.retweetCount = retweetCount;
        this.favoriteCount = favoriteCount;
        this.date = date;
        this.text = text;
        this.score = score;
        this.luceneScore = luceneScore;
        this.frScore = frScore;
        this.urlScore = urlScore;
        this.lengthScore = lengthScore;
        this.retweetScore = retweetScore;
        this.qrScore = qrScore;
    }

    public String scoreRepr() {
        return score + " = Lu" + luceneScore + " + FR" + frScore +
                " + U" + urlScore + " + Le" + lengthScore + " + R" +
                retweetScore + " + Q" + qrScore;
    }
}
