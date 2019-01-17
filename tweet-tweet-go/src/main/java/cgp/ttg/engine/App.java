package cgp.ttg.engine;

import me.tongfei.progressbar.ProgressBar;
import java.util.*;

public class App {


    public static void main(String[] args) throws Exception {
        var in = new Scanner(System.in);
//        doIndexing();
        doQuery(in);
    }

    private static void doQuery(Scanner in) {
        var topic = "none";
        var user = UserProfile.getProfile("custom");
        while (true) {
            try {
                System.out.println("Escimi la query...");
                var query = in.nextLine();

                if ("".equals(query)) {
                    System.exit(0);
                } else if (query.startsWith("topic")) {
                    topic = query.split(" ")[1];
                    System.out.println("Topic selezionato: " + topic);
                } else if (query.startsWith("user")) {
                    user = UserProfile.getProfile(query.split(" ")[1]);
                    topic = "none";
                    System.out.println("User selezionato: " + user.getId());
                } else {
                    if ("none".equals(topic)) {
                        QueryEngine.match(query, 150, false, true, true, null, null, null, null);
                    } else {
                        QueryEngine.match(query, 150, false, true, true, null, null, topic, user);
                    }
                    System.out.println("User: " + user.getId() + "; Topic: " + topic);
                    System.out.println("\n\n");
                }
            } catch (Exception e) { }
        }
    }

    private static void doIndexing() throws Exception {
        for (var topic : UserProfile.getTopics()) {
            var users = Repository.selectTopNUsersByTopic(5000, topic);
            System.err.println("Topic: " + topic);
            for (var user : ProgressBar.wrap(users, "Users")) {
                var userTweets = Repository.selectUserTweets(user);
                Indexer.createIndex(userTweets);
            }

        }
    }

}
