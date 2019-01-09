package cgp;

import cgp.Indexer.Fields;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class QueryEngine {
    public static void match(String stringQuery) {
        try {
            var path = Paths.get(Indexer.INDEX_PATH);
            var dr = FSDirectory.open(path);
            var indexReader = DirectoryReader.open(dr);
            var indexSearcher = new IndexSearcher(indexReader);
            var analyzer = new EnglishAnalyzer();

            var queryParser = new QueryParser(Fields.TEXT, analyzer);
            var textQuery = queryParser.parse(stringQuery);

            var sort = new Sort(
                    new SortedNumericSortField(Fields.DATE, SortField.Type.LONG, true)
            );
            var topDocs = indexSearcher.search(
                    textQuery,
                    100,
                    sort,
                    true,
                    true
            );

            var doc = new Document();
            var scoreDocs = topDocs.scoreDocs;
            var docs = new ArrayList<Document>(scoreDocs.length);
            var maxLength = 0;
            var maxRetFav = 0l;
            var maxRetrieved = topDocs.getMaxScore();
            for (var scoreDoc : scoreDocs) {
                doc = indexReader.document(scoreDoc.doc);
                var docLength = doc.getField(Fields.TEXT).stringValue().length();
                var retfavCount = doc.getField(Fields.FAVORITE_COUNT).numericValue().longValue() +
                        doc.getField(Fields.RETWEET_COUNT).numericValue().longValue();
                if (docLength > maxLength) {
                    maxLength = docLength;
                }
                if (retfavCount > maxRetFav) {
                    maxRetFav = retfavCount;
                }
                docs.add(doc);
            }

            for (var i = 0; i < scoreDocs.length; i++) {
                doc = docs.get(i);

                var userFollowers = doc.getField(Fields.USER_FOLLOWERS).numericValue().longValue();
                var userFollowing = doc.getField(Fields.USER_FOLLOWING).numericValue().longValue();
                var urlScore = "true".equals(doc.getField(Fields.HAS_URLS).stringValue()) ? 2 : 0;
                var lengthScore = doc.getField(Fields.TEXT).stringValue().length() / maxLength;
                var retfavCount = doc.getField(Fields.FAVORITE_COUNT).numericValue().longValue() +
                        doc.getField(Fields.RETWEET_COUNT).numericValue().longValue();

                var baseScore = 2 * scoreDocs[i].score / maxRetrieved;
                var frul = (userFollowers / (userFollowers + userFollowing)) + urlScore + lengthScore;
                var retweetScore = retfavCount / maxRetFav;
                var finalScore = baseScore + frul + retweetScore;
                scoreDocs[i].score = finalScore;
            }

            Arrays.sort(scoreDocs, (a, b) -> Float.compare(b.score, a.score));
            for (var i = scoreDocs.length - 1; i >= 0; i--) {
                var scoreDoc = scoreDocs[i];
                doc = indexReader.document(scoreDoc.doc);
                System.out.println(
                        "#" + (i + 1) + " " + doc.getField(Fields.USER).stringValue() + " (" +
                                "R: " + doc.getField(Fields.RETWEET_COUNT).stringValue() +
                                "; F: " + doc.getField(Fields.FAVORITE_COUNT).stringValue() + ") " +
                                new Date(doc.getField(Fields.DATE).numericValue().longValue()*1000) + "\n" +
                                doc.getField(Fields.TEXT).stringValue() + "\n"
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
