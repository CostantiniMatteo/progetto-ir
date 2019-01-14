package cgp.ttg.engine;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;
import org.apache.lucene.search.spell.LuceneDictionary;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class UserProfile {
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
                Indexer.createIndex(topicTweets, Indexer.USER_INDEX_PATH);
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
}
