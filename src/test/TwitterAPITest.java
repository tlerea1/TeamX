package test;


import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collection;
import java.util.Scanner;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import util.api.TwitterApiClient;
import util.blob.Tweet;

public class TwitterAPITest {
	
	private static Scanner loginReader;
	
	public static void main(String[] args) throws FileNotFoundException, TwitterException {
		loginReader = new Scanner(new FileReader("/Users/tuvialerea/code/git/illegalbot/twitter.login"));
		TwitterApiClient.init();
		Collection<Tweet> tweets = TwitterApiClient.getTweets("kingjames");
		Tweet t = ((Tweet) tweets.toArray()[1]);
		System.out.println(t.getTweet());
		Collection<String> retweets = TwitterApiClient.getRetweeters(t);
		for (String s: retweets) {
			System.out.println(s);
		}
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
    	while(loginReader.hasNextLine()) {
    		String line = loginReader.nextLine();
    		String[] parts = line.split(",");
    		return twitterSetup(parts);
    	}
    	return null;
    }
}
