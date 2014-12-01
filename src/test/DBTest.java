package test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.SQLException;
import java.util.Scanner;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import util.api.TwitterApiClient;
import util.blob.TwitterAccount;
import util.db.TwitterDB;
import util.writer.TwitterDatabaseWriter;

public class DBTest {
	private static Scanner loginReader;
	
	public static void main(String[] args) throws FileNotFoundException, TwitterException, SQLException {
		loginReader = new Scanner(new FileReader("/Users/tuvialerea/code/git/illegalbot/twitter.login"));
		Twitter tw = setupTwitterAccountTokens();
		TwitterApiClient.init();
		TwitterAccount ta = new TwitterAccount("kingjames");
		TwitterAccount ta2 = new TwitterAccount("barakobama");
		TwitterDB.init();
		new TwitterDatabaseWriter(ta).start();
		new TwitterDatabaseWriter(ta2).start();
	}
	
    /**
     * Sets up a single Twitter account.
     * @param keys the login keys for the twitter account, includes name and type
     * @return the Twitter object
     */
    private static Twitter twitterSetup(String[] keys) {
        Twitter twitter = new TwitterFactory().getInstance();
        twitter.setOAuthConsumer(keys[2], keys[3]);
        twitter4j.auth.AccessToken accessToken = new twitter4j.auth.AccessToken(keys[4], keys[5]);
        twitter.setOAuthAccessToken(accessToken);
        return twitter;
    }
    
    /**
     * Sets up the twitter account OAuth Tokens
     */
    private static Twitter setupTwitterAccountTokens() {
    	loginReader.nextLine();
    	while(loginReader.hasNextLine()) {
    		String line = loginReader.nextLine();
    		String[] parts = line.split(",");
    		return twitterSetup(parts);
    	}
    	return null;
    }
}
