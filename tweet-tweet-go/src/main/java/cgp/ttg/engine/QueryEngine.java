package cgp.ttg.engine;

import cgp.ttg.webservice.ResultEntity;
import cgp.ttg.webservice.TweetResultEntity;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.*;

public class QueryEngine {

    public static final float URL_SCORE = 1.0f/3;
    public static final float IS_QUOTE_SCORE = -0.5f;
    public static final float IS_RETWEET_SCORE = -0.5f;
    public static final float LUCENE_MULT = 3.0f;
    public static final float LUCENE_MULT_PERS = 3.0f;
    public static final float FOLLOWER_MULT = 1.0f;
    public static final float RETWEET_MULT = 1.0f;
    public static final float LENGTH_MULT = 0.5f;


    public static ResultEntity match(String stringQuery, int n, boolean full, boolean weightUrl, boolean filterDuplicates, Date since, Date to, String topic, UserProfile userProfile) {
        ArrayList<TweetResultEntity> match;
        var isPersonalized = userProfile != null && topic != null && !"".equals(topic);

        try {
            var path = Paths.get(Indexer.INDEX_PATH);
            var dr = FSDirectory.open(path);
            var indexReader = DirectoryReader.open(dr);
            var indexSearcher = new IndexSearcher(indexReader);
            var analyzer = Indexer.getCustomAnalyzer();
            indexSearcher.setSimilarity(new ClassicSimilarity());

            // Parse query
            var queryParser = new QueryParser(Indexer.Fields.TEXT, analyzer);
            queryParser.setDefaultOperator(QueryParser.Operator.AND);
            var textQuery = queryParser.parse(stringQuery);
            var queryBuilder = new BooleanQuery.Builder()
                    .add(textQuery, BooleanClause.Occur.MUST);

            for (var term : stringQuery.split(" ")) {
                queryBuilder.add(
                        new TermQuery(new Term(Indexer.Fields.HASHTAG, term)),
                        BooleanClause.Occur.SHOULD
                );
            }

            if (since != null && to != null) {
                queryBuilder.add(
                        LongPoint.newRangeQuery(Indexer.Fields.DATE, since.getTime() / 1000, to.getTime() / 1000),
                        BooleanClause.Occur.MUST
                );
            } else if (since != null) {
                queryBuilder.add(
                        LongPoint.newRangeQuery(Indexer.Fields.DATE, since.getTime() / 1000, Long.MAX_VALUE),
                        BooleanClause.Occur.MUST
                );
            } else if (to != null) {
                queryBuilder.add(
                        LongPoint.newRangeQuery(Indexer.Fields.DATE, 0, to.getTime() / 1000),
                        BooleanClause.Occur.MUST
                );
            }

            // Personalize query
            if (isPersonalized) {
                for (var term : userProfile.topicProfile(topic)) {
                    queryBuilder.add(new BoostQuery(
                            new TermQuery(new Term(Indexer.Fields.TEXT, term)), 3
                    ), BooleanClause.Occur.SHOULD);
                }
            }

            Query finalQuery = queryBuilder.build();

            // Search documents
            TopDocs topDocs;
            var sort = new Sort(
                    new SortedNumericSortField(Indexer.Fields.DATE, SortField.Type.LONG, true)
            );
            if (!full) {
                topDocs = indexSearcher.search(finalQuery, n, sort, true, true);
            } else {
                topDocs = indexSearcher.search(finalQuery, n);
            }
            var scoreDocs = topDocs.scoreDocs;

            var unfilteredResults = new ArrayList<TweetResultEntity>();
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
            var lucene_mult = isPersonalized ? LUCENE_MULT_PERS : LUCENE_MULT;
            for (var i = 0; i < scoreDocs.length; i++) {
                doc = docs.get(i);
                var resultEntity = processDoc(doc, i, scoreDocs[i].score, maxScore, maxLength, maxRetFav, weightUrl, lucene_mult);
                scoreDocs[i].score = resultEntity.score;
                unfilteredResults.add(resultEntity);
            }

            Arrays.sort(scoreDocs, (a, b) -> Float.compare(b.score, a.score));
            unfilteredResults.sort((a, b) -> Float.compare(b.score, a.score));

            System.err.println("FINDING NEAR-DUPLICATES");
//          Near-duplicates detection
            match = new ArrayList<>();
            var isDuplicate = new ArrayList<>(Collections.nCopies(unfilteredResults.size(), false));
            var nDuplicates = 0;

            for (int i = 0; i < unfilteredResults.size() - 1; i++) {
                if (isDuplicate.get(i)) continue;

                var res1 = unfilteredResults.get(i);
//                var ndd1 = new NDD(res1.text, generateShingles(res1.text, analyzer), 10);
                var tokenSet1 = new TreeSet<>(generateShingles(res1.text, analyzer));

                for (int j = i + 1; j < unfilteredResults.size(); j++) {
                    if (isDuplicate.get(j)) continue;

                    var res2 = unfilteredResults.get(j);
//                    var ndd2 = new NDD(res2.text, generateShingles(res2.text, analyzer), 10);
                    var tokenSet2 = new TreeSet<>(generateShingles(res2.text, analyzer));

//                    if (ndd1.computeSimilarity(ndd2) > 0.75) {
                    if (NDD.overlapCoefficient(tokenSet1, tokenSet2) > 0.8) {
                        nDuplicates++;
                        if (res1.score >= res2.score) {
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
                if (!isDuplicate.get(i)) {
                    match.add(unfilteredResults.get(i));
                } else if (!filterDuplicates) {
                    var tweetResultEntity = unfilteredResults.get(i);
                    tweetResultEntity.isDuplicate = true;
                    match.add(tweetResultEntity);
                }
            }

            // Debug
            System.err.println("RESULTS");
            printDocuments(match, maxRetFav);
            return new ResultEntity(match, nDuplicates);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static TweetResultEntity processDoc(Document doc, int rank, float score, float maxScore, long maxLength, long maxRetFav, boolean weightUrl, float lucene_mult) {
        var tweetId = doc.getField(Indexer.Fields.TWEET_ID).stringValue();
        var author = doc.getField(Indexer.Fields.USER).stringValue();
        var userFollowers = doc.getField(Indexer.Fields.USER_FOLLOWERS).numericValue().longValue();
        var userFollowing = doc.getField(Indexer.Fields.USER_FOLLOWING).numericValue().longValue();
        var date = new Date(doc.getField(Indexer.Fields.DATE).numericValue().longValue() * 1000);
        var text = doc.getField(Indexer.Fields.TEXT).stringValue();
        var urlScore = weightUrl && "true".equals(doc.getField(Indexer.Fields.HAS_URLS).stringValue()) ? URL_SCORE : 0;
        var lengthScore = LENGTH_MULT * doc.getField(Indexer.Fields.TEXT).stringValue().length() / maxLength;
        var retweetCount = doc.getField(Indexer.Fields.RETWEET_COUNT).numericValue().longValue();
        var favoriteCount = doc.getField(Indexer.Fields.FAVORITE_COUNT).numericValue().longValue();
        var retfavCount = favoriteCount + retweetCount;

        var baseScore = lucene_mult * score / maxScore;
        var frScore = FOLLOWER_MULT * (1.0f * userFollowers / (userFollowers + userFollowing));
        var frul = frScore + urlScore + lengthScore;
        var retweetScore = RETWEET_MULT * retfavCount / maxRetFav;
        var qrScore = ("true".equals(doc.getField(Indexer.Fields.IS_QUOTE).stringValue()) ? IS_QUOTE_SCORE : 0)
                + ("true".equals(doc.getField(Indexer.Fields.IS_RETWEET).stringValue()) ? IS_RETWEET_SCORE : 0);
        var finalScore = baseScore + frul + retweetScore + qrScore;


        return new TweetResultEntity(
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void printDocuments(ArrayList<TweetResultEntity> result, long maxRetFav) throws IOException {
        Document doc;
        for (var i = result.size() - 1; i >= 0; i--) {
            var tweet = result.get(i);
            System.out.println("#" + (i + 1) + " " + tweet.author + " (" + "R: " + tweet.retweetCount +
                    "; F: " + tweet.favoriteCount + ") " + tweet.date + "\n" +
                    tweet.text + "\n" + tweet.scoreRepr() + "\n" + tweet.tweetId + "\n");
        }
        System.out.println("\nMax Retweet+Fav = " + maxRetFav);
    }
}
