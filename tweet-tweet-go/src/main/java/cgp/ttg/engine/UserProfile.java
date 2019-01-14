package cgp.ttg.engine;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;
import org.apache.lucene.search.spell.LuceneDictionary;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class UserProfile {
    public static final Analyzer analyzer = Indexer.getCustomAnalyzer();
    public HashMap<String, Set<String>> profile;
    public String id;

    public UserProfile(String id, HashMap<String, List<Tweet>> tweets) {
        this.id = id;

        try {
            for (String topic : tweets.keySet()) {
                var topicTweets = tweets.get(topic);
                Indexer.createIndex(topicTweets, Indexer.USER_INDEX_PATH);

                var userIndexReader = DirectoryReader.open(FSDirectory.open(Paths.get(Indexer.USER_INDEX_PATH)));
                var indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(Indexer.INDEX_PATH)));
                var ld = new LuceneDictionary(indexReader, "field");
                BytesRefIterator iterator = ld.getEntryIterator();
                BytesRef byteRef = null;
                while ((byteRef = iterator.next()) != null) {
                    String term = byteRef.utf8ToString();
                }
            }
        } catch (Exception e) {

        }
    }
}
