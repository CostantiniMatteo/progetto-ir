package cgp.ttg.webservice;

import cgp.ttg.engine.QueryEngine;
import cgp.ttg.engine.Repository;
import cgp.ttg.engine.UserProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;


@RestController
public class ApplicationController {

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public ResponseEntity<ResultEntity> getJobs(
            @RequestParam(value = "q") String query,
            @RequestParam(value = "n", defaultValue = "100") int n,
            @RequestParam(value = "full", defaultValue = "false") boolean full,
            @RequestParam(value = "url", defaultValue = "true") boolean weightUrl,
            @RequestParam(value = "media", defaultValue = "true") boolean weightMedia,
            @RequestParam(value = "since", required = false) Date since,
            @RequestParam(value = "to", required = false) Date to,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "user", required = false) String user
    ) {
        var userProfile = UserProfile.getProfile(user);
        var result = QueryEngine.match(query, n, full, weightUrl, since, to, topic, userProfile);
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/topics", method = RequestMethod.GET)
    public ResponseEntity<List<String>> getTopics() {
        return ResponseEntity.ok(Arrays.asList(UserProfile.topics));
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public ResponseEntity<List<String>> getUsers() {
        return ResponseEntity.ok(Arrays.asList(UserProfile.userIds));
    }

    @RequestMapping(value = "/update-profile/{topic}/{tweetId}", method = RequestMethod.POST)
    public ResponseEntity updateProfile(@PathVariable String topic, @PathVariable String tweetId) {
        var tweet = Repository.findTweetByTweetId(tweetId);
        UserProfile.getProfile("custom").updateProfile(topic, tweet);
        return ResponseEntity.ok().build();
    }
}
