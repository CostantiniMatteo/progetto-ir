package cgp.ttg.engine;

import me.tongfei.progressbar.ProgressBar;

import java.util.*;
import java.util.stream.Collectors;

public class App {

    public static String[] csTweetIds = new String[] {"1048729861164912641", "1033107225856958464", "1036691894410063873", "1048122413374947328", "1045000780833378304", "1020066565948133376", "1057120980513050624", "1054771723936116737", "1019994908617211904"};
    public static String[] appleFanBoy = new String[] {"1075889856201388032", "1077173725076713472", "1059795695245451265", "1063091601369677824", "1029083728126074880", "1044338922606776325", "1076196049558126592", "1026501043142553600", "979804798609510401", "742705381336621056", "473732377123229696"};

    public static void main(String[] args) throws Exception {
        var in = new Scanner(System.in);

//        var blah = new HashMap<String, List<Tweet>>();
//        var tweets = Arrays.stream(appleFanBoy).map((id) -> Repository.findTweetByTweetId(id)).collect(Collectors.toList());
//        blah.put("cs", tweets);
//        var u = new UserProfile("Me", blah);
        doIndexing();
//        doQuery(in, u);
    }

    private static void doQuery(Scanner in, UserProfile u) {
        while (true) {
            System.out.println("Escimi la query...");
            var query = in.nextLine();
            if (!"".equals(query)) {
                QueryEngine.match(query, u, "cs");
                System.out.println("\n\n");
            } else {
                break;
            }
        }
    }

    private static void doIndexing() throws Exception {
        var topics = new String[]{"sport", "music", "tech", "cs", "politics", "cinema", "food", "science", "cars", "finance"};
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
