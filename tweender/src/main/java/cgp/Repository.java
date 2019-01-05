package cgp;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Repository {
    static String HOST = "jdbc:postgresql://157.230.25.215:5432/progetto_ir";

    public static Connection getConnection() throws Exception {
        Connection c = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection(HOST, "matteo", "correct horse battery staple");
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
            var tweet = new Tweet(rs.getString("json"), rs.getString("user_id"));
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

        if (rs.next()) {
            return new TwitterUser(rs.getString("json"), rs.getString("topic"));
        }

        return null;
    }


}
