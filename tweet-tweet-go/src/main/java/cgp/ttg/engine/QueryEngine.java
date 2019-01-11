package cgp.ttg.engine;

import cgp.ttg.webservice.ResultEntity;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.*;

public class QueryEngine {

    public static final int URL_SCORE = 2;
    public static final float IS_QUOTE_SCORE = 0.5f;
    public static final float IS_RETWEET_SCORE = 0.5f;

    public static ArrayList<ResultEntity> match(String stringQuery) {
        return match(stringQuery, 150);
    }

    public static ArrayList<ResultEntity> match(String stringQuery, int n) {
        ArrayList<ResultEntity> results = null;

        try {
            var path = Paths.get(Indexer.INDEX_PATH);
            var dr = FSDirectory.open(path);
            var indexReader = DirectoryReader.open(dr);
            var indexSearcher = new IndexSearcher(indexReader);
            var analyzer = Indexer.getCustomAnalyzer();

            // Parse query
            var queryParser = new QueryParser(Indexer.Fields.TEXT, analyzer);
            var textQuery = queryParser.parse(stringQuery);


            // Search documents
            var sort = new Sort(
                    new SortedNumericSortField(Indexer.Fields.DATE, SortField.Type.LONG, true)
            );
            var topDocs = indexSearcher.search(textQuery, n, sort, true, true);
            var scoreDocs = topDocs.scoreDocs;

            var unfilteredResults = new ArrayList<ResultEntity>();
            var docs = new ArrayList<Document>();


            // Retrieve actual documents and calculate parameters used in re-scoring
            System.err.println("RETRIEVING DOCUMENTS");
            var doc = new Document();
            var maxLength = 0l;
            var maxRetFav = 0l;
            var maxScore = topDocs.getMaxScore();
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

            System.err.println("RESCORING AND RERANKING");
            // Re-score & Re-rank
            for (var i = 0; i < scoreDocs.length; i++) {
                doc = docs.get(i);
                var resultEntity = processDoc(doc, i, scoreDocs[i].score, maxScore, maxLength, maxRetFav);
                scoreDocs[i].score = resultEntity.score;
                unfilteredResults.add(resultEntity);
            }

            Arrays.sort(scoreDocs, (a, b) -> Float.compare(b.score, a.score));
            unfilteredResults.sort((a, b) -> Float.compare(b.score, a.score));

            System.err.println("FINDING NEAR-DUPLICATES");
//          Near-duplicates detection
            var nearDuplicates = new ArrayList<ResultEntity>();
            results = new ArrayList<>();
            var isDuplicate = new ArrayList<>(Collections.nCopies(unfilteredResults.size(), false));
            for (int i = 0; i < unfilteredResults.size() - 1; i++) {
                if (isDuplicate.get(i)) { continue; }
                var res1 = unfilteredResults.get(i);
                var ndd1 = new NDD(res1.text, generateShingles(res1.text, analyzer) , 10);
                for (int j = i + 1; j < unfilteredResults.size(); j++) {
                    var res2 = unfilteredResults.get(j);
                    var ndd2 = new NDD(res2.text, generateShingles(res2.text, analyzer), 10);
                    if (ndd1.computeSimilarity(ndd2) > 0.75) {
                        if (res1.rank >= res2.rank) {
                            isDuplicate.set(j, true);
                        } else {
                            isDuplicate.set(i, true);
                            break;
                        }
                    }
                }
            }

            System.err.println("FINALIZATION");
            for (int i = 0; i < unfilteredResults.size(); i++) {
                if (isDuplicate.get(i)) {
                    nearDuplicates.add(unfilteredResults.get(i));
                } else {
                    results.add(unfilteredResults.get(i));
                }
            }

            // Debug
            System.err.println("RESULTS");
            printDocuments(results, indexReader, scoreDocs, maxRetFav);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return results;
    }

    private static void printDocuments(ArrayList<ResultEntity> result, DirectoryReader indexReader, ScoreDoc[] scoreDocs, long maxRetFav) throws IOException {
        Document doc;
        for (var i = result.size() - 1; i >= 0; i--) {
            var scoreDoc = scoreDocs[i];
            doc = indexReader.document(scoreDoc.doc);
            var author = doc.getField(Indexer.Fields.USER).stringValue();
            var retweetCount = doc.getField(Indexer.Fields.RETWEET_COUNT).numericValue().longValue();
            var favoriteCount = doc.getField(Indexer.Fields.FAVORITE_COUNT).numericValue().longValue();
            var date = new Date(doc.getField(Indexer.Fields.DATE).numericValue().longValue() * 1000);
            var text = doc.getField(Indexer.Fields.TEXT).stringValue();
            System.out.println("#" + (i + 1) + " " + author + " (" + "R: " + retweetCount +
                    "; F: " + favoriteCount + ") " + date + "\n" +
                    text + "\n" + result.get(i).scoreRepr() + "\n");
        }
        System.out.println("\nMax Retweet+Fav = " + maxRetFav);
    }

    private static ResultEntity processDoc(Document doc, int rank, float score, float maxScore, long maxLength, long maxRetFav) {
        var tweetId = doc.getField(Indexer.Fields.TWEET_ID).stringValue();
        var author = doc.getField(Indexer.Fields.USER).stringValue();
        var userFollowers = doc.getField(Indexer.Fields.USER_FOLLOWERS).numericValue().longValue();
        var userFollowing = doc.getField(Indexer.Fields.USER_FOLLOWING).numericValue().longValue();
        var date = new Date(doc.getField(Indexer.Fields.DATE).numericValue().longValue() * 1000);
        var text = doc.getField(Indexer.Fields.TEXT).stringValue();
        var urlScore = "true".equals(doc.getField(Indexer.Fields.HAS_URLS).stringValue()) ? URL_SCORE : 0;
        var lengthScore = 1.0f * doc.getField(Indexer.Fields.TEXT).stringValue().length() / maxLength;
        var retweetCount = doc.getField(Indexer.Fields.RETWEET_COUNT).numericValue().longValue();
        var favoriteCount = doc.getField(Indexer.Fields.FAVORITE_COUNT).numericValue().longValue();
        var retfavCount = favoriteCount + retweetCount;

        var baseScore = 2 * score / maxScore;
        var frScore = (1.0f * userFollowers / (userFollowers + userFollowing));
        var frul = frScore + urlScore + lengthScore;
        var retweetScore = 1.0f * retfavCount / maxRetFav;
        var qrScore = ("true".equals(doc.getField(Indexer.Fields.IS_QUOTE).stringValue()) ? IS_QUOTE_SCORE : 0)
                + ("true".equals(doc.getField(Indexer.Fields.IS_RETWEET).stringValue()) ? IS_RETWEET_SCORE : 0);
        var finalScore = baseScore + frul + retweetScore + qrScore;



        return new ResultEntity(
                rank, tweetId, author, retweetCount, favoriteCount, date, text, finalScore,
                baseScore, frScore, urlScore, lengthScore, retweetScore, qrScore
        );

    }

    public static List<String> generateShingles(String text, Analyzer analyzer) {
        try {
            var result = new ArrayList<String>();
            var tokens = new ArrayList<String>();
            var tokenStream = (analyzer.tokenStream(Indexer.Fields.TEXT, new StringReader(text)));
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                tokens.add(charTermAttribute.toString());
            }
            tokenStream.close();

            for (int i = 1; i < tokens.size(); i++) {
                result.add(tokens.get(i - 1) + tokens.get(i));
            }
            return result;
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }
}
