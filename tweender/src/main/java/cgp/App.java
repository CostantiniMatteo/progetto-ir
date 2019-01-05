package cgp;

public class App
{
    public static void main(String[] args) throws Exception {
        System.out.println( "Hello World!" );
        var topUsers = Repository.selectTopNUsersPerTopic(5);
        System.out.println(topUsers);
        var user = Repository.findUserByScreenName("gregbretzz");
        var tweets = Repository.selectUserTweets(user);
        System.out.println(tweets);
    }

}
