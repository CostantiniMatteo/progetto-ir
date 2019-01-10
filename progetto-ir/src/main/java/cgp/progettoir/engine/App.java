package cgp.progettoir.engine;

import me.tongfei.progressbar.ProgressBar;

import java.util.LinkedList;
import java.util.Scanner;

public class App
{
    public static void main(String[] args) throws Exception {
        var in = new Scanner(System.in);

//        doIndexing();
        doQuery(in);
    }

    private static void doQuery(Scanner in) {
        while (true) {
            System.out.println("Escimi la query...");
            var query = in.nextLine();
            if (!"".equals(query)) {
                QueryEngine.match(query);
                System.out.println("\n\n");
            } else {
                break;
            }
        }
    }

    private static void doIndexing() throws Exception {
        var topics = new String[] { "sport", "music", "tech", "cs", "politics", "cinema", "food", "science", "cars", "finance" };
        for (var topic : topics) {
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
