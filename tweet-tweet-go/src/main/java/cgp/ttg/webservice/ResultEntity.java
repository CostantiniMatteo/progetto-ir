package cgp.ttg.webservice;

import java.util.List;

public class ResultEntity {
    public List<TweetResultEntity> results;
    public List<TweetResultEntity> duplicates;

    public ResultEntity(List<TweetResultEntity> results, List<TweetResultEntity> duplicates) {
        this.results = results;
        this.duplicates = duplicates;
    }
}
