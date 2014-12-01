package util.blob;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import twitter4j.TwitterException;
import util.api.TwitterApiClient;
import util.db.TwitterDB;

public class TwitterAccount implements SocialMediaAccount {

    private long SLEEP_TIME = 1000;

    // private TwitterApiClient client_;
    private final String username_;

    private int numFollowers_ = -1;
    private int numFollowing_ = -1;
    private long twitterID_ = -1;
    private Collection<Tweet> tweets_ = null;
    private Collection<Link> links_ = null;
    private boolean isBot_ = false;

    /**
     * Constructor. Username is final
     * 
     * @param username
     */
    public TwitterAccount(String username) {
        username_ = username;
        // client_ = client;
    }

    /**
     * Attempts to look into the database for information about the account.
     * 
     * @return False if the database doesn't have information about this user.
     *         Returns true if it successfully fills the instance
     * @throws SQLException
     */
    public boolean attemptToFillFromDatabase() throws SQLException {
        if (!TwitterDB.hasUser(username_)) {
            return false;
        }

        numFollowers_ = TwitterDB.getFollowers(username_).size();
        numFollowing_ = TwitterDB.getFollowing(username_).size();
        // getTwitterId();
        tweets_ = TwitterDB.getTweets(username_);
        links_ = TwitterDB.getLinks(username_);
        isBot_ = TwitterDB.isBot(username_);
        return true;

    }

    public void attemptToFillFromAPI() {
        getNumFollowers();
        getNumFollowing();
        getTweets();
        extractLinks();
        getTwitterId();
    }

    public Instance generateMLInstance() {
        double[] toReturn = new double[2];
        toReturn[0] = getSim(this);
        toReturn[1] = getLinksPerTweet(this);
        return new DenseInstance(toReturn);
    }

    public boolean getIsBot() {
        return isBot_;
    }

    public void setIsBot(boolean isBot) {
        isBot_ = isBot;
    }

    public int getNumFollowers() {
        if (numFollowers_ < 0) {
            int numFollowers = 0;
            try {
                numFollowers = TwitterApiClient.getNumFollowers(username_);
            } catch (TwitterException e) {
                e.printStackTrace();
                sleep();
            }
            return numFollowers;
        }
        return numFollowers_;
    }

    public int getNumFollowing() {
        if (numFollowing_ < 0) {
            try {
                numFollowing_ = TwitterApiClient.getNumFollowing(username_);
            } catch (TwitterException e) {
                e.printStackTrace();
                sleep();
            }
        }
        return numFollowing_;
    }

    public long getTwitterId() {
        if (twitterID_ < 0) {
            try {
                twitterID_ = TwitterApiClient.getId(username_);
            } catch (TwitterException e) {
                sleep();
                e.printStackTrace();
            }
        }
        return twitterID_;
    }

    public Collection<Tweet> getTweets() {
        if (tweets_ == null) {
            try {
                tweets_ = TwitterApiClient.getTweets(username_);
            } catch (TwitterException e) {
                sleep();
                e.printStackTrace();
                return new ArrayList<Tweet>();
            }
        }
        return tweets_;
    }

    public Collection<TwitterAccount> getFollowers() {
        Collection<TwitterAccount> toReturn = new ArrayList<TwitterAccount>();
        try {
            Collection<String> followers = TwitterApiClient
                    .getFollowers(username_);
            for (String s : followers) {
                toReturn.add(new TwitterAccount(s));
            }
        } catch (TwitterException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    public Collection<TwitterAccount> getFollowing() {
        Collection<TwitterAccount> toReturn = new ArrayList<TwitterAccount>();
        try {
            Collection<String> following = TwitterApiClient
                    .getFollowing(username_);
            for (String s : following) {
                toReturn.add(new TwitterAccount(s));
            }
        } catch (TwitterException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    public Collection<Link> extractLinks() {
        if (links_ == null) {
            Collection<Tweet> tweets = getTweets();
            links_ = new ArrayList<Link>();
            for (Tweet t : tweets) {
                links_.addAll(Link.extractLink(t));
            }
        }
        return links_;
    }

    public String getName() {
        return username_;
    }

    // ------------------------------------------------------------------------------------------
    // Calculations for Filtering of Twitter
    // ------------------------------------------------------------------------------------------

    /**
     * Computes the jaccard similarity between each of the user's own tweets
     * 
     * @param account
     *            : the user's Twitter account
     * @return the median of all of the jaccard similarities
     */
    public static double getSim(TwitterAccount account) {
        ArrayList<Tweet> tweets = new ArrayList<Tweet>(account.getTweets());
        ArrayList<Double> jaccards = new ArrayList<Double>();

        for (int i = 0; i < tweets.size(); i++) {
            List<String> currentTweet = Arrays.asList(tweets.get(i).getTweet()
                    .split(" "));

            for (int j = i + 1; j < tweets.size(); j++) {
                int sim = 0;
                int total = 0;
                List<String> cmpTweet = Arrays.asList(tweets.get(j).getTweet()
                        .split(" "));
                for (int h = 0; h < currentTweet.size(); h++) {
                    if (cmpTweet.contains(currentTweet.get(h))) {
                        sim++;
                    }
                    total++;
                }
                double jaccard = sim / total;
                jaccards.add(jaccard);

            }
        }
        Collections.sort(jaccards);
        int i = jaccards.size() / 2;

        if (jaccards.size() == 0) {
            return 0;
        }

        return jaccards.get(i);
    }

    /**
     * Computes percent of tweets containing links
     * 
     * @param account
     *            : the user's Twitter account
     * @return the percent of tweets containing links
     */
    public static double getLinksPerTweet(TwitterAccount account) {
        ArrayList<Tweet> tweets = new ArrayList<Tweet>(account.getTweets());
        if (tweets.size() == 0) {
            return 0;
        }
        int counter = 0;
        Pattern pat1 = Pattern
                .compile("(linkd\\.in|t\\.co|bddy\\.me|tcrn\\.ch|\\w+\\.\\w{2})\\/(\\w+|$)");
        Pattern pat2 = Pattern
                .compile("\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

        for (int i = 0; i < tweets.size(); i++) {
            Matcher m1 = pat1.matcher(tweets.get(i).getTweet());
            Matcher m2 = pat2.matcher(tweets.get(i).getTweet());
            if (m1.find() || m2.find()) {
                counter++;
            }
        }

        return counter / tweets.size();
    }

    @Override
    public Collection<SocialMediaAccount> getConnectedNodes() {
        Collection<String> col;
        try {
            col = TwitterApiClient.getFollowers(getId());
            for (Tweet t : getTweets()) {
                col.addAll(TwitterApiClient.getRetweeters(t));
            }
        } catch (TwitterException e) {
            e.printStackTrace();
            sleep();
            return new ArrayList<SocialMediaAccount>();
        }
        Collection<SocialMediaAccount> toReturn = new ArrayList<SocialMediaAccount>();
        for (String s : col) {
            toReturn.add(new TwitterAccount(s));
        }
        return toReturn;
    }

    @Override
    public String getId() {
        return username_;
    }

    private void sleep() {
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public double[] getMetircs() {
        double[] toReturn = new double[2];
        toReturn[0] = getSim(this);
        toReturn[1] = getLinksPerTweet(this);
        return toReturn;
    }

    @Override
    public String getLabel() {
        // TODO Auto-generated method stub
        return null;
    }
}
