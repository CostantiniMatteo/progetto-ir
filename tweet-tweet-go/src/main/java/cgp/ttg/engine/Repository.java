package cgp.ttg.engine;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;

public class Repository {
   static String HOST = "jdbc:postgresql://157.230.25.215:5432/progetto_ir";
    // static String HOST = "jdbc:postgresql://localhost:5432/progetto_ir";
    static String USER = "matteo";
   static String PASSWORD = "correct horse battery staple";
    // static String PASSWORD = "";

    public static Connection getConnection() throws Exception {
        Connection c = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection(HOST, USER, PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }

        return c;
    }


    public static ArrayList<TwitterUser> selectTopNUsersPerTopic(int n) throws Exception {
        String query = "select *\n" +
                "from (select user_id, followers, topic, processed, json,row_number()\n" +
                "over (partition by topic order by followers desc) as rownum\n" +
                "from twitter_user) tmp where rownum <= " + n + ";";

        return queryUsers(query);
    }

    public static ArrayList<TwitterUser> selectTopNUsersByTopic(int n, String topic) throws Exception {
        String query = "select *\n" +
                "from twitter_user where topic = '" + topic + "' " +
                "order by followers desc limit(" + n + ");";

        return queryUsers(query);
    }

    private static ArrayList<TwitterUser> queryUsers(String query) throws Exception {
        var c = getConnection();
        var stmt = c.createStatement();
        var rs = stmt.executeQuery(query);

        var result = new ArrayList<TwitterUser>();
        while (rs.next()) {
            var user = new TwitterUser(rs.getString("json"), rs.getString("topic"));
            result.add(user);
        }
        rs.close();
        stmt.close();
        c.close();

        return result;
    }


    public static ArrayList<Tweet> selectUserTweets(TwitterUser user) throws Exception {
        String query = "select * from tweets where user_id = '" + user.name + "';";

        var c = getConnection();
        var stmt = c.createStatement();
        var rs = stmt.executeQuery(query);

        var result = new ArrayList<Tweet>();
        while (rs.next()) {
            var tweet = new Tweet(rs.getString("json"), user);
            result.add(tweet);
        }
        rs.close();
        stmt.close();
        c.close();

        return result;
    }

    public static TwitterUser findUserByScreenName(String name) throws Exception {
        String query = "select * from twitter_user where user_id = '" + name + "';";

        var c = getConnection();
        var stmt = c.createStatement();
        var rs = stmt.executeQuery(query);

        TwitterUser user = null;
        if (rs.next()) {
            user = new TwitterUser(rs.getString("json"), rs.getString("topic"));
        }
        rs.close();
        stmt.close();
        c.close();

        return user;
    }

    public static Tweet findTweetByTweetId(String tweetId) {
        String query = "select * from tweets where tweet_id = " + tweetId + ";";

        Tweet tweet = null;
        try {
            var c = getConnection();
            var stmt = c.createStatement();
            var rs = stmt.executeQuery(query);


            if (rs.next()) {
                tweet = new Tweet(rs.getString("json"),
                        findUserByScreenName(rs.getString("user_id")));
            }
            rs.close();
            stmt.close();
            c.close();

        } catch (Exception e) { }

        return tweet;
    }


}
