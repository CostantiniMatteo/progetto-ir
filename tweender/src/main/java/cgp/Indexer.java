package cgp;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Indexer {


    // TODO: Che cambia tra Field.Store.YES, Field.Store.NO
    // TODO: Che vuol dire che è "indexed but not stored"?
    public static Document tweetToDocument(Tweet tweet) {
        var doc = new Document();

        doc.add(new StringField("tweetId", tweet.tweetId, Field.Store.YES));
        // TODO: Alternativa come nelle soluzioni del lab?
        doc.add(new LongPoint("date", tweet.createdAt));
//      date string formatted as yyyymmdd
//      doc_idx.add(new SortedDocValuesField("date", new BytesRef(date)));
//      doc_idx.add(new StoredField("date", date));
        doc.add(new TextField("text", tweet.text, Field.Store.YES));
        // TODO: Come gestiamo i boolean? Come cazzo si fa a creare un Field normale?
//      var isReply = new Field("boolean", tweet.replyToId != null ? "true" : "false", null);
//      var isQuote = new Field("boolean", tweet.quote ? "true" : "false", null);
//      Questo è quello che veniva suggerito su SO (nel 2012)
//      doc.add(new Field("boolean","1",Field.Store.NO,Field.Index.NOT_ANALYZED_NO_NORMS));

        // TODO: È stored?
        doc.add(new LongPoint("retweetCount", tweet.retweetCount));
        doc.add(new LongPoint("favoriteCount", tweet.favoriteCount));

        for (var hashtag : tweet.hashtags) {
            // TODO: La spelling correction viene fatta se cerco su uno StringField?
            doc.add(new StringField("hashtag", hashtag, Field.Store.YES));
            doc.add(new TextField("hashtag", hashtag, Field.Store.YES));
        }

        // TODO: Che si fa con urls ed userMentions?
        // Noi vogliamo usarle per cambiare lo score giusto?
        // e.g. se menziona un utente con molto follower, score in più
        // Però come vanno costruiti i field?

        return doc;
    }

    public static void createIndex(List<Tweet> tweets, String indexPath) {
        var path = Paths.get(indexPath);
        try {
            var dr = FSDirectory.open(path);
            var analyzer = new StandardAnalyzer();
            var writer = new IndexWriterConfig(analyzer);
            // TODO: Che cazzo è la LMDDirichletSimilarity?!
            var similarity = new LMDirichletSimilarity(2000);
            writer.setSimilarity(similarity);
            var iw = new IndexWriter(dr, writer);
            for (var tweet : tweets) {
                iw.addDocument(tweetToDocument(tweet));
            }
            iw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
