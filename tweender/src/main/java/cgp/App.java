package cgp;

import java.util.ArrayList;
import java.util.List;

public class App
{
    public static void main(String[] args) throws Exception {
        System.out.println( "Hello World!" );
        List<TwitterUser> l = new ArrayList(Indexer.selectTopNUsersPerTopic(5));
        System.out.println(l);
    }

}
