package util.api;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import util.blob.LockTwitter;
import util.blob.Tweet;

/**
 * API wraper for the twitter API
 * 
 * @author Gabe
 * 
 */
public class TwitterApiClient {

    // authenticated twitter account to make the requests
    private static CopyOnWriteArrayList<LockTwitter> twitters = new CopyOnWriteArrayList<LockTwitter>();
    // Sleep time between API calls
    private static final long SLEEP_TIME = 500;
    private static Scanner loginReader;
    private static int current = 0;
    private static Object currentLock = new Object();
    private static boolean inited = false;

    private TwitterApiClient() {
    }

    public static void init() {
        if (inited) {
            return;
        }
        setupTwitterAccountTokens();
        inited = true;
    }

    /**
     * Sets up a single Twitter account.
     * 
     * @param keys
     *            the login keys for the twitter account, includes name and type
     * @return the Twitter object
     */
    private static Twitter twitterSetup(String[] keys) {
        Twitter twitter = new TwitterFactory().getInstance();
        twitter.setOAuthConsumer(keys[2], keys[3]);
        twitter4j.auth.AccessToken accessToken = new twitter4j.auth.AccessToken(
                keys[4], keys[5]);
        twitter.setOAuthAccessToken(accessToken);
        return twitter;
    }

    /**
     * Sets up the twitter account OAuth Tokens
     */
    private static void setupTwitterAccountTokens() {
        try {
            loginReader = new Scanner(new FileReader(
                    "/home/ubuntu/illegalbot/twitter.login"));
            // loginReader = new Scanner(new
            // FileReader("/users/tuvialerea/code/git/illegalbot/twitter.login"));
            while (loginReader.hasNextLine()) {
                String line = loginReader.nextLine();
                String[] parts = line.split(",");
                Twitter twitter = twitterSetup(parts);
                twitters.add(new LockTwitter(twitter));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void changeTwitter() {
        synchronized (currentLock) {
            current++;
            if (current == twitters.size()) {
                current = 0;
            }
        }
    }

    /**
     * Requests the 200 most recent tweets by the user
     * 
     * @param username
     *            Username of the user
     * @return Collection of tweets. Up to 200 tweets
     * @throws TwitterException
     */
    public static Collection<Tweet> getTweets(String username)
            throws TwitterException {
        LockTwitter tw = twitters.get(current);
        ResponseList<Status> list = tw.getTwitter().getUserTimeline(username,
                new Paging(1, 200));
        Collection<Tweet> toReturn = new ArrayList<Tweet>();
        for (Status s : list) {
            toReturn.add(new Tweet(s.getText(), s.getRetweetCount(), s
                    .getCreatedAt().getTime(), s.getId()));
        }
        sleep();
        tw.unlock();
        changeTwitter();
        return toReturn;
    }

    /**
     * Request the followers of the current user. Gets up to 200
     * 
     * @param username
     *            Username of the user
     * @return Collection of usernames. Up to 200
     * @throws TwitterException
     */
    public static Collection<String> getFollowers(String username)
            throws TwitterException {
        LockTwitter tw = twitters.get(current);
        Collection<String> toReturn = new ArrayList<String>();
        for (User user : tw.getTwitter().getFollowersList(username, -1, 200)) {
            toReturn.add(user.getScreenName());
        }
        sleep();
        tw.unlock();
        changeTwitter();
        return toReturn;
    }

    /**
     * Request the Following of the current user. Gets up to 200
     * 
     * @param username
     *            Username of the user
     * @return Collection of usernames. Up to 200
     * @throws TwitterException
     */
    public static Collection<String> getFollowing(String username)
            throws TwitterException {
        LockTwitter tw = twitters.get(current);
        Collection<String> toReturn = new ArrayList<String>();
        for (User user : tw.getTwitter().getFriendsList(username, -1, 200)) {
            toReturn.add(user.getScreenName());
        }
        sleep();
        tw.unlock();
        changeTwitter();
        return toReturn;
    }

    /**
     * Request the number of followers of the user
     * 
     * @param username
     *            Username of the user
     * @return Number of users
     * @throws TwitterException
     */
    public static int getNumFollowers(String username) throws TwitterException {
        LockTwitter tw = twitters.get(current);
        int toReturn = tw.getTwitter().showUser(username).getFollowersCount();
        tw.unlock();
        changeTwitter();
        return toReturn;
    }

    /**
     * Request the number of followers of the user
     * 
     * @param username
     *            Username of the user
     * @return Number of users
     * @throws TwitterException
     */
    public static int getNumFollowing(String username) throws TwitterException {
        LockTwitter tw = twitters.get(current);
        int toReturn = tw.getTwitter().showUser(username).getFriendsCount();
        tw.unlock();
        changeTwitter();
        return toReturn;
    }

    /**
     * Returns the Twitter Id for the given screenname
     * 
     * @param username
     *            the screenname to lookup
     * @return the Twitter Id
     * @throws TwitterException
     *             if connection fails
     */
    public static long getId(String username) throws TwitterException {
        LockTwitter tw = twitters.get(current);
        long toReturn = tw.getTwitter().showUser(username).getId();
        tw.unlock();
        changeTwitter();
        return toReturn;
    }

    /**
     * NOT IMPLEMENTED
     * 
     * @param username
     * @return
     */
    public String getDescription(String username) {
        // TODO
        return null;
    }

    /**
     * NOT IMPLEMENTED
     * 
     * @param username
     * @return
     */
    public String getLocation(String username) {
        // TODO
        return null;
    }

    /**
     * Get the first 100 screen names the retweeted the given tweet
     * 
     * @param tweet
     *            The tweet object to search for
     * @return A collection of screennames that retweeted the given tweet
     * @throws TwitterException
     *             If connection fails
     */
    public static Collection<String> getRetweeters(Tweet tweet)
            throws TwitterException {
        Collection<String> retweeters = new HashSet<String>();
        LockTwitter tw = twitters.get(current);
        for (Status s : tw.getTwitter().getRetweets(tweet.getId())) {
            retweeters.add(s.getUser().getScreenName());
        }
        tw.unlock();
        changeTwitter();
        return retweeters;
    }

    /**
     * Internal sleep for the current thread
     */
    private static void sleep() {
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
