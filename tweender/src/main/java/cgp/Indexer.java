package cgp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Indexer {
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

    public static void createIndex() throws Exception {
        throw new Exception();
    }

    public static List<TwitterUser> selectTopNUsersPerTopic(int n) throws Exception {
        String query = "select *\n" +
                "from (select user_id, followers, topic, processed, row_number()\n" +
                "over (partition by topic order by followers desc) as rownum\n" +
                "from twitter_user) tmp where rownum <= " + n + ";";

        Connection c = getConnection();
        Statement stmt = c.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        ArrayList result = new ArrayList<TwitterUser>();
        while (rs.next()) {
            TwitterUser user = new TwitterUser();
        }
        rs.close();
        stmt.close();
        c.close();

        return result;
    }


}
