package cgp.ttg.engine;

import me.tongfei.progressbar.ProgressBar;

import java.util.*;
import java.util.stream.Collectors;

public class App {


    public static void main(String[] args) throws Exception {
        var in = new Scanner(System.in);
//        doIndexing();
        doQuery(in, null);
    }

    private static void doQuery(Scanner in, UserProfile u) {
        while (true) {
            System.out.println("Escimi la query...");
            var query = in.nextLine();
            if (!"".equals(query)) {
                QueryEngine.match(query, 150, false, true, true, null, null, null, null);
                System.out.println("\n\n");
            } else {
                break;
            }
        }
    }

    private static void doIndexing() throws Exception {
        for (var topic : UserProfile.topics) {
            var users = Repository.selectTopNUsersByTopic(100, topic);
            var tweets = new LinkedList<Tweet>();
            for (var user : ProgressBar.wrap(users, "Users")) {
                var userTweets = Repository.selectUserTweets(user);
                tweets.addAll(userTweets);
            }
            Indexer.createIndex(tweets);
        }
    }

}
