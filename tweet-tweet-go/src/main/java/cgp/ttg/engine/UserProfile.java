package cgp.ttg.engine;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.search.spell.LuceneDictionary;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class UserProfile {
    private static final String USER_PROFILES_PATH = "tweet-tweet-go/src/main/resources/";
    public static String[] userIds = new String[] { "user1", "user2" };
    public static String[] topics = new String[] { "sport", "music", "tech", "cs", "politics", "cinema", "food", "science", "cars", "finance" };
    private static HashMap<String, HashMap<String, List<String>>> userDocumentsByTopic = initUserDocumentsByTopic();
    private static HashMap<String, UserProfile> users = new HashMap<>() {{
        put("custom", new UserProfile("custom", new HashMap<>()));
    }};


    private String id;
    private HashMap<String, List<Tweet>> documents;
    private HashMap<String, Set<String>> profile;

    class TermTfidfPair {
        String term;
        float tfidf;

        TermTfidfPair(String term, float tdidf) {
            this.term = term;
            this.tfidf = tdidf;
        }
    }

    public static UserProfile getProfile(String userId) {
        var user = users.getOrDefault(userId, null);
        if (user == null) {
            user = initUser(userId);
            users.put(userId, user);
        }
        return user;
    }

    public UserProfile(String id, HashMap<String, List<Tweet>> documents) {
        this.id = id;
        this.profile = new HashMap<>();
        this.documents = documents;
        initProfile();
    }

    public void updateProfile(String topic, Tweet document) {
        this.documents.get(topic).add(document);
        initProfile();
    }

    public String getId() {
        return id;
    }

    public Set<String> topicProfile(String topic) {
        return this.profile.getOrDefault(topic, new HashSet<>());
    }

    private void initProfile() {
        this.profile = new HashMap<>();

        var similarity = new ClassicSimilarity();
        try {
            for (String topic : this.documents.keySet()) {
                var topicTweets = this.documents.get(topic);
                Indexer.createIndex(topicTweets, Indexer.USER_INDEX_PATH, false);
                List<TermTfidfPair> termTfidfList = new ArrayList<>();

                var userIndexReader = DirectoryReader.open(FSDirectory.open(Paths.get(Indexer.USER_INDEX_PATH)));
                var indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(Indexer.INDEX_PATH)));
                var ld = new LuceneDictionary(userIndexReader, Indexer.Fields.TEXT);
                var iterator = ld.getEntryIterator();
                BytesRef byteRef = null;
                while ((byteRef = iterator.next()) != null) {
                    var term = byteRef.utf8ToString();
                    var idf = similarity.idf(indexReader.docFreq(new Term(Indexer.Fields.TEXT, term)), indexReader.numDocs());
                    var tf = similarity.tf(userIndexReader.totalTermFreq(new Term(Indexer.Fields.TEXT, term)));
                    var tfidf = tf * idf;
                    termTfidfList.add(new TermTfidfPair(term, tfidf));
                }

                termTfidfList.sort((a, b) -> Float.compare(b.tfidf, a.tfidf));
//                if (termTfidfList.size() > 20) {
//                    termTfidfList = termTfidfList.subList(0, 20);
//                }
                this.profile.put(topic, termTfidfList.stream().map((i) -> i.term).collect(Collectors.toSet()));
            }
        } catch (Exception e) { }
    }

    private static UserProfile initUser(String userId) {
        var tweetIdsByTopic = userDocumentsByTopic.get(userId);
        var tweetsMap = new HashMap<String, List<Tweet>>();
        List tweets;
        for (var topic : tweetIdsByTopic.keySet()) {
            tweets = tweetIdsByTopic.get(topic).stream().map(Repository::findTweetByTweetId).collect(Collectors.toList());
            tweetsMap.put(topic, tweets);
        }
        return new UserProfile(userId, tweetsMap);
    }

    private static HashMap<String, HashMap<String, List<String>>> initUserDocumentsByTopic() {
        var result = new HashMap<String, HashMap<String, List<String>>>();

        for (String user : UserProfile.userIds) {
            var userMap = new HashMap<String, List<String>>();

            File file = null;
            try {
                file = new File(USER_PROFILES_PATH + user);
            } catch (Exception e) { e.printStackTrace(); }
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    var topic = line;
                    var tweets = new ArrayList<String>();
                    for (var tweet : br.readLine().split(" ")) {
                        tweets.add(tweet);
                    }
                    userMap.put(topic, tweets);
                }
            } catch (Exception e) {
                // Properly handled /s
                e.printStackTrace();
            }
            result.put(user, userMap);
        }

        return result;
    }
}
