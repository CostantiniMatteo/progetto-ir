package cgp.ttg.engine;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.search.spell.LuceneDictionary;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class UserProfile {
    public static String[] users = new String[] { "user1", "user2", "user3", "user4", "user5" };
    public static String[] topics = new String[] { "sport", "music", "tech", "cs", "politics", "cinema", "food", "science", "cars", "finance" };
    private HashMap<String, Set<String>> profile;
    private String id;

    class TermTfidfPair {
        public String term;
        public float tfidf;

        TermTfidfPair(String term, float tdidf) {
            this.term = term;
            this.tfidf = tdidf;
        }
    }

    public UserProfile(String id, HashMap<String, List<Tweet>> tweets) {
        this.id = id;
        this.profile = new HashMap<>();

        var similarity = new ClassicSimilarity();
        try {
            for (String topic : tweets.keySet()) {
                var topicTweets = tweets.get(topic);
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

    public String getId() {
        return id;
    }

    public Set<String> topicProfile(String topic) {
        return this.profile.getOrDefault(topic, new HashSet<>());
    }


    public static String[] csTweetIds = new String[] {"1048729861164912641", "1033107225856958464", "1036691894410063873", "1048122413374947328", "1045000780833378304", "1020066565948133376", "1057120980513050624", "1054771723936116737", "1019994908617211904"};
    public static String[] appleFanBoy = new String[] {"1075889856201388032", "1077173725076713472", "1059795695245451265", "1063091601369677824", "1029083728126074880", "1044338922606776325", "1076196049558126592", "1026501043142553600", "979804798609510401", "742705381336621056", "473732377123229696"};

    public static UserProfile getProfile(String user) {
        var tweetsMap = new HashMap<String, List<Tweet>>();
        List tweets = new ArrayList();
        switch (user) {
            case "user1":
                tweets = Arrays.stream(csTweetIds).map(Repository::findTweetByTweetId).collect(Collectors.toList());
                break;
            case "user2":
                tweets = Arrays.stream(appleFanBoy).map(Repository::findTweetByTweetId).collect(Collectors.toList());
                break;
        }
        tweetsMap.put("cs", tweets);
        return new UserProfile("Me", tweetsMap);
    }
}
