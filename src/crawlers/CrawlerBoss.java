package crawlers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import util.api.TwitterApiClient;
import util.blob.FacebookAccount;
import util.blob.InstagramAccount;
import util.blob.SocialMediaAccount;
import util.blob.TwitterAccount;
import util.db.FacebookDB;
import util.db.InstagramDB;
import util.db.TwitterDB;
import util.writer.TwitterDatabaseWriter;

/**
 * Main thread that sits on our twitter accounts and checks for new events. This
 * drives all the BotCrawlers
 * 
 * @author Gabe
 * 
 */
public class CrawlerBoss {

    // 2hr wait between crawling all of our nodes. We check for updates more
    // often than this, but we do a complete recrawl every 2hr
    private long TIME_BETWEEN_CRAWLS = 7200000;
    // The list of our known accounts which are being watched for activity
    private ArrayList<SocialMediaAccount> ourAccounts_;
    // Known human accounts for the database
    private ArrayList<SocialMediaAccount> realAccounts_;
    // Time at which we last initialized a complete crawl
    private long lastCrawlTime_;

    /**
     * Sets up the known accounts Real Accounts are to provide a second cluster
     * for the Kmeans algorithm They are a selection of the most followed
     * twitter personalities
     */
    public CrawlerBoss() {
        ourAccounts_ = new ArrayList<SocialMediaAccount>();
        ourAccounts_.add(new TwitterAccount("cjshores99"));
        ourAccounts_.add(new TwitterAccount("pjholloway5"));
        ourAccounts_.add(new TwitterAccount("hvrich2"));
        ourAccounts_.add(new TwitterAccount("beverly_oleary"));
        ourAccounts_.add(new TwitterAccount("mgarcia575"));

        realAccounts_ = new ArrayList<SocialMediaAccount>();
        realAccounts_.add(new TwitterAccount("katyperry")); // Katy Perry
        realAccounts_.add(new TwitterAccount("justinbieber")); // Justin Beiber
        realAccounts_.add(new TwitterAccount("BarackObama")); // Barak Obama
        realAccounts_.add(new TwitterAccount("taylorswift13")); // Taylor Swift
        realAccounts_.add(new TwitterAccount("ladygaga")); // Lady Gaga
        realAccounts_.add(new TwitterAccount("britneyspears")); // Britney
                                                                // Spears
        realAccounts_.add(new TwitterAccount("rihanna")); // Rihanna
        realAccounts_.add(new TwitterAccount("jtimberlake")); // Justin
                                                              // Timberlake
        realAccounts_.add(new TwitterAccount("joshto")); // Josh To
        realAccounts_.add(new TwitterAccount("TheEllenShow")); // Ellen Degenes
        realAccounts_.add(new TwitterAccount("Cristiano")); // Ronaldo
        realAccounts_.add(new TwitterAccount("JLo")); // Jenifer Lopez
        realAccounts_.add(new TwitterAccount("shakira")); // Shakira
        realAccounts_.add(new TwitterAccount("KimKardashian")); // Kim
                                                                // Kardashian
        realAccounts_.add(new TwitterAccount("Harry_Styles")); // Harry Styles
        realAccounts_.add(new TwitterAccount("BrunoMars")); // Bruno Mars
        realAccounts_.add(new TwitterAccount("BillGates")); // Bill Gates
        realAccounts_.add(new TwitterAccount("EmWatson")); // Emma Watson
        realAccounts_.add(new TwitterAccount("kelly_clarkson")); // Kelly
                                                                 // Clarkson
        realAccounts_.add(new TwitterAccount("ZacEfron")); // Zac Efron
    }

    /**
     * Writes the known entities to the database. This allows for a first pass
     * of the DB crawler
     */
    public void initializeDataset() {
        // Writing the known bots to the DB to update the means
        for (SocialMediaAccount acc : ourAccounts_) {
            Collection<SocialMediaAccount> connectedAccounts = acc
                    .getConnectedNodes();
            for (SocialMediaAccount conAcc : connectedAccounts) {
                if (conAcc instanceof TwitterAccount) {
                    ((TwitterAccount) conAcc).setIsBot(true);
                    TwitterDatabaseWriter writer = new TwitterDatabaseWriter(
                            (TwitterAccount) conAcc);
                    writer.start();
                }
            }
        }
        // Go and get some known real accounts to stick in the database
        for (SocialMediaAccount acc : realAccounts_) {
            if (acc instanceof TwitterAccount) {
                TwitterDatabaseWriter writer = new TwitterDatabaseWriter(
                        (TwitterAccount) acc);
                writer.start();
            }
        }
    }

    /**
     * starts up the Boss. Watches the known accounts for any activity and will
     * init a full crawl as needed
     */
    public void watchAccounts() {
        while (true) {
            if (System.currentTimeMillis() - lastCrawlTime_ > TIME_BETWEEN_CRAWLS) {
                // reset crawl timer
                lastCrawlTime_ = System.currentTimeMillis();
                // initialize a crawl of all of our bots
                initializeCrawl();
            } else {
                for (SocialMediaAccount account : ourAccounts_) {
                    checkForNewEvents(account);
                }
            }

            try {
                Thread.sleep(600000); // 10 min sleep
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks to see if a new edge has been added to the current node
     * 
     * @param account
     *            Current Node
     */
    private void checkForNewEvents(SocialMediaAccount account) {
        for (SocialMediaAccount conAcc : account.getConnectedNodes()) {
            if (!isInDatabase(conAcc)) {
                BotCrawler crawler = new BotCrawler(account);
                crawler.start();
                // We can initialize at most a single crawl per twitter account
                return;
            }
        }
    }

    /**
     * Checks to see if the appropriate database is holding an entry for the
     * account in question
     * 
     * @param account
     *            Account in question
     * @return true iff the correct database has an account with the ID
     */
    private boolean isInDatabase(SocialMediaAccount account) {
        try {
            if (account instanceof TwitterAccount) {
                return TwitterDB.hasUser(account.getId());
            } else if (account instanceof FacebookAccount) {
                return FacebookDB.hasUser(account.getId());
            } else if (account instanceof InstagramAccount) {
                return InstagramDB.hasUser(account.getId());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    /**
     * Starts a new crawl. Creates a BotCrawler to do the actual crawling
     */
    private void initializeCrawl() {
        for (SocialMediaAccount account : ourAccounts_) {
            BotCrawler crawler = new BotCrawler(account);
            crawler.start();
        }
    }

    /**
     * Driving main for the whole deployment
     * 
     * @param args
     * @throws SQLException
     */
    public static void main(String args[]) throws SQLException {
        TwitterDB.init();
        TwitterApiClient.init();
        CrawlerBoss boss = new CrawlerBoss();
        // boss.initializeDataset();
        boss.watchAccounts();
        Thread dBCrawThread = new DatabaseCrawler();
        dBCrawThread.start();

    }

}
