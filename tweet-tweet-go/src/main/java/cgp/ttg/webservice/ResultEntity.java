package cgp.ttg.webservice;

import java.util.List;

public class ResultEntity {
    List<TweetResultEntity> results;
    int nDuplicates;

    public ResultEntity(List<TweetResultEntity> results, int nDuplicates) {
        this.results = results;
        this.nDuplicates = nDuplicates;
    }
}
