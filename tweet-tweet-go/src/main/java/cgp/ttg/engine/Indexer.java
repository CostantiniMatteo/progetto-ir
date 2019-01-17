package cgp.ttg.engine;

import me.tongfei.progressbar.ProgressBar;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class Indexer {
    public static final String INDEX_PATH = "index";
    public static final String USER_INDEX_PATH = "user_index";
    public static final FieldType tweetField = new FieldType() {{
        setTokenized(true);
        setStored(true);
        setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        setStoreTermVectors(true);
    }};

    public class Fields {
        public static final String TWEET_ID = "tweetId";
        public static final String DATE = "date";
        public static final String TEXT = "text";
        public static final String IS_REPLY = "isReply";
        public static final String IS_QUOTE = "isQuote";
        public static final String IS_RETWEET = "isRetweet";
        public static final String HAS_URLS = "hasUrls";
        public static final String HAS_MEDIA = "hasMedia";
        public static final String RETWEET_COUNT = "retweetCount";
        public static final String FAVORITE_COUNT = "favoriteCount";
        public static final String LANG = "lang";
        public static final String HASHTAG = "hashtag";
        public static final String USER = "user";
        public static final String USER_FOLLOWERS = "userFollowers";
        public static final String USER_FOLLOWING = "userFollowing";
        public static final String USER_VERIFIED = "userVerified";
    }


    public static Document tweetToDocument(Tweet tweet) {
        var doc = new Document();

        doc.add(new StringField(Fields.TWEET_ID, tweet.tweetId, Field.Store.YES));
        doc.add(new StringField(Fields.USER, tweet.user.name, Field.Store.YES));
        doc.add(new LongPoint(Fields.DATE, tweet.createdAt));
        doc.add(new StoredField(Fields.DATE, tweet.createdAt));
        doc.add(new SortedNumericDocValuesField(Fields.DATE, tweet.createdAt));
//        doc.add(new TextField(Fields.TEXT, tweet.text, Field.Store.YES));
        doc.add(new Field(Fields.TEXT, tweet.text, tweetField));
        doc.add(new StringField(Fields.IS_REPLY, tweet.replyToId != null ? "true" : "false", Field.Store.YES));
        doc.add(new StringField(Fields.IS_QUOTE, tweet.isQuote ? "true" : "false", Field.Store.YES));
        doc.add(new StringField(Fields.IS_RETWEET, tweet.isRetweet ? "true" : "false", Field.Store.YES));
        doc.add(new StringField(Fields.HAS_URLS, tweet.urls.size() > 0 ? "true" : "false", Field.Store.YES));
        doc.add(new StringField(Fields.HAS_MEDIA, tweet.media.size() > 0 ? "true" : "false", Field.Store.YES));
        doc.add(new StringField(Fields.USER_VERIFIED, tweet.user.verified ? "true" : "false", Field.Store.YES));
        doc.add(new LongPoint(Fields.RETWEET_COUNT, tweet.retweetCount));
        doc.add(new StoredField(Fields.RETWEET_COUNT, tweet.retweetCount));
        doc.add(new LongPoint(Fields.FAVORITE_COUNT, tweet.favoriteCount));
        doc.add(new StoredField(Fields.FAVORITE_COUNT, tweet.favoriteCount));
        doc.add(new LongPoint(Fields.USER_FOLLOWERS, tweet.user.followersCount));
        doc.add(new StoredField(Fields.USER_FOLLOWERS, tweet.user.followersCount));
        doc.add(new LongPoint(Fields.USER_FOLLOWING, tweet.user.friendsCount));
        doc.add(new StoredField(Fields.USER_FOLLOWING, tweet.user.friendsCount));
        doc.add(new StringField(Fields.LANG, tweet.lang, Field.Store.YES));
        for (var hashtag : tweet.hashtags) {
            doc.add(new StringField(Fields.HASHTAG, hashtag, Field.Store.YES));
//            doc.add(new TextField(Fields.HASHTAG, hashtag, Field.Store.YES));
        }

        return doc;
    }


    public static void createIndex(List<Tweet> tweets) {
        createIndex(tweets, INDEX_PATH, true);
    }

    public static void createIndex(List<Tweet> tweets, String indexPath, boolean append) {
        var path = Paths.get(indexPath);
        try {
            var directory = FSDirectory.open(path);
            var analyzer = getCustomAnalyzer();
            var cfg = new IndexWriterConfig(analyzer);
            cfg.setSimilarity(new ClassicSimilarity());
            var indexWriter = new IndexWriter(directory, cfg);

            // Empty index before writing so that we don't add documents to an already existing index
            if (!append) {
                indexWriter.deleteAll();
                indexWriter.commit();
            }

            for (var tweet : tweets) {
                if ("en".equals(tweet.lang)) {
                    indexWriter.addDocument(tweetToDocument(tweet));
                }
            }
            indexWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Analyzer getCustomAnalyzer() {
        try {
            var analyzer = CustomAnalyzer.builder()
                    .addCharFilter(
                            "patternreplace",
                            "pattern",
                            "((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)",
                            "replacement",
                            " "
                    )
                    .withTokenizer("standard")
                    .addTokenFilter("lowercase")
                    .addTokenFilter("stop")
                    .addTokenFilter("porterstem")
                    .build();
            return analyzer;
        } catch (IOException e) {
            return null;
        }

    }


}
