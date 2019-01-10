package cgp.progettoir.engine;

import cgp.progettoir.webservice.ResultEntity;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.store.FSDirectory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class QueryEngine {

    public static final int URL_SCORE = 2;

    public static ArrayList<ResultEntity> match(String stringQuery) {
        return match(stringQuery, 100);
    }

    public static ArrayList<ResultEntity> match(String stringQuery, int n) {
        ArrayList<ResultEntity> result = null;

        try {
            var path = Paths.get(Indexer.INDEX_PATH);
            var dr = FSDirectory.open(path);
            var indexReader = DirectoryReader.open(dr);
            var indexSearcher = new IndexSearcher(indexReader);
            var analyzer = new EnglishAnalyzer();


            var queryParser = new QueryParser(Indexer.Fields.TEXT, analyzer);
            var textQuery = queryParser.parse(stringQuery);

            var sort = new Sort(
                    new SortedNumericSortField(Indexer.Fields.DATE, SortField.Type.LONG, true)
            );
            var topDocs = indexSearcher.search(textQuery, n, sort, true, true);
            var scoreDocs = topDocs.scoreDocs;
            result = new ArrayList<>(scoreDocs.length);
            var docs = new ArrayList<Document>(scoreDocs.length);


            var doc = new Document();
            var maxLength = 0;
            var maxRetFav = 0l;
            var maxRetrieved = topDocs.getMaxScore();
            for (var scoreDoc : scoreDocs) {
                doc = indexReader.document(scoreDoc.doc);
                var docLength = doc.getField(Indexer.Fields.TEXT).stringValue().length();
                var retfavCount = doc.getField(Indexer.Fields.FAVORITE_COUNT).numericValue().longValue() +
                        doc.getField(Indexer.Fields.RETWEET_COUNT).numericValue().longValue();
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

                var tweetId = doc.getField(Indexer.Fields.TWEET_ID).stringValue();
                var author = doc.getField(Indexer.Fields.USER).stringValue();
                var userFollowers = doc.getField(Indexer.Fields.USER_FOLLOWERS).numericValue().longValue();
                var userFollowing = doc.getField(Indexer.Fields.USER_FOLLOWING).numericValue().longValue();
                var date = new Date(doc.getField(Indexer.Fields.DATE).numericValue().longValue() * 1000);
                var text = doc.getField(Indexer.Fields.TEXT).stringValue();
                var urlScore = "true".equals(doc.getField(Indexer.Fields.HAS_URLS).stringValue()) ? URL_SCORE : 0;
                var lengthScore = doc.getField(Indexer.Fields.TEXT).stringValue().length() / maxLength;
                var retweetCount = doc.getField(Indexer.Fields.RETWEET_COUNT).numericValue().longValue();
                var favoriteCount = doc.getField(Indexer.Fields.FAVORITE_COUNT).numericValue().longValue();
                var retfavCount = favoriteCount + retweetCount;

                var baseScore = 2 * scoreDocs[i].score / maxRetrieved;
                var frScore = (userFollowers / (userFollowers + userFollowing));
                var frul = frScore + urlScore + lengthScore;
                var retweetScore = retfavCount / maxRetFav;
                var finalScore = baseScore + frul + retweetScore;
                scoreDocs[i].score = finalScore;

                result.add(new ResultEntity(
                        i, tweetId, author, retweetCount, favoriteCount, date, text, finalScore,
                        baseScore, frScore, urlScore, lengthScore, retweetScore
                ));
            }

            Arrays.sort(scoreDocs, (a, b) -> Float.compare(b.score, a.score));
//            result.sort((a, b) -> Float.compare(b.score, a.score));


            for (var i = scoreDocs.length - 1; i >= 0; i--) {
                var scoreDoc = scoreDocs[i];
                doc = indexReader.document(scoreDoc.doc);
                var author = doc.getField(Indexer.Fields.USER).stringValue();
                var retweetCount = doc.getField(Indexer.Fields.RETWEET_COUNT).numericValue().longValue();
                var favoriteCount = doc.getField(Indexer.Fields.FAVORITE_COUNT).numericValue().longValue();
                var date = new Date(doc.getField(Indexer.Fields.DATE).numericValue().longValue() * 1000);
                var text = doc.getField(Indexer.Fields.TEXT).stringValue();
                System.out.println("#" + (i + 1) + " " + author + " (" + "R: " + retweetCount +
                        "; F: " + favoriteCount + ") " + date + "\n" + text + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return result;
    }
}
