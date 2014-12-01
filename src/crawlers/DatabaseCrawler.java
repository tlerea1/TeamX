package crawlers;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.Instance;
import util.api.TwitterApiClient;
import util.blob.TwitterAccount;
import util.db.TwitterDB;

public class DatabaseCrawler extends Thread {
    private Map<String, Instance> instances_;
    private Dataset dataset_;

    /**
     * Constructor. Updates accounts on creation
     */
    public DatabaseCrawler() {
        dataset_ = new DefaultDataset();
        updateAccounts();
    }

    public void run() {
        while (true) {
            crawlDatabase();
            sleepWithCatch(30 * 1000 * 60); // 30 min sleep
        }
    }

    /**
     * Initializes a crawl of the database that will initiate new means to be
     * written to the database
     */
    private void crawlDatabase() {
        updateAccounts();
        writeMeans(determineMeans());
    }

    /**
     * Updates the accounts being stored by the crawler. Checks for any new
     * Twitter accounts that have been added to the database. If there are new
     * accounts, it will generate dense instances for them
     */
    private void updateAccounts() {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("data.csv", "UTF-8");
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
        } catch (UnsupportedEncodingException e2) {
            e2.printStackTrace();
        }
        if (instances_ == null) {
            instances_ = new HashMap<String, Instance>();
        }
        Collection<String> usernames;
        try {
            usernames = TwitterDB.getUsers();
            for (String name : usernames) {
                if (!instances_.containsKey(name)) {
                    TwitterAccount account = new TwitterAccount(name);
                    try {
                        account.attemptToFillFromDatabase();
                    } catch (SQLException e) {
                        // account.attemptToFillFromAPI();
                    }
                    instances_.put(name, account.generateMLInstance());
                    dataset_.add(account.generateMLInstance());
                    double[] metrics = account.getMetircs();
                    writer.write(name + "," + metrics[0] + "," + metrics[1]);
                }
            }
        } catch (SQLException e1) {
            sleepWithCatch(1000);
            e1.printStackTrace();
        }

        writer.close();
    }

    /**
     * Uses Kmean with 2 means and 100 itterations to find the centroids of the
     * instances in the database
     * 
     * @return Map of Bot/Human to their means as Maps
     */
    private Map<String, Map<String, Double>> determineMeans() {
        /*
         * for (SocialMediaAccount a : accounts_) { double[] metrics =
         * a.getMetircs(); Instance i = new DenseInstance(metrics);
         * dataset_.add(i); }
         */
        Clusterer kmeans = new KMeans(2);
        Dataset[] clusters = kmeans.cluster(dataset_);

        double[] mean1 = meanOfDataset(clusters[0]);
        double[] mean2 = meanOfDataset(clusters[1]);
        Map<String, Map<String, Double>> toReturn = new TreeMap<String, Map<String, Double>>();
        Map<String, Double> botMap = new TreeMap<String, Double>();
        Map<String, Double> humanMap = new TreeMap<String, Double>();

        // Determine which on is the Bot and which one is the nonBot
        if (mean1[0] > mean2[0]) { // Hack. Tests to see which has more internal
                                   // similarity of tweets.
            botMap.put("internalSimilarity", mean1[0]);
            botMap.put("linkAverage", mean1[1]);

            humanMap.put("internalSimilarity", mean2[0]);
            humanMap.put("linkAverage", mean2[1]);
        } else {
            botMap.put("internalSimilarity", mean2[0]);
            botMap.put("linkAverage", mean2[1]);

            humanMap.put("internalSimilarity", mean1[0]);
            humanMap.put("linkAverage", mean1[1]);
        }
        toReturn.put("Bot", botMap);
        toReturn.put("Human", humanMap);
        return toReturn;
    }

    /**
     * Writes the determined means to the database so the filter can access them
     * 
     * @param means
     */
    private void writeMeans(Map<String, Map<String, Double>> means) {

        try {
            TwitterDB.updateBotMean(means.get("Bot"));
            TwitterDB.updateBotMean(means.get("Human"));
        } catch (SQLException e) {
            // we attempt to crawl again.
            // this should give the database plenty of time to figure out its
            // problem
            // And this way we get new datapoints if they appeared
            e.printStackTrace();
            crawlDatabase();
        }
    }

    /**
     * Takes a cluster and returns its mean
     * 
     * @param d
     *            cluster
     * @return n dimensional vector representing the mean of the cluster in R^n
     */
    private double[] meanOfDataset(Dataset d) {
        Iterator<Instance> ittr = d.iterator();
        Instance firstInstance = ittr.next();
        double[] mean = new double[firstInstance.entrySet().size()];
        while (ittr.hasNext()) {
            for (Entry<Integer, Double> E : ittr.next().entrySet()) {
                mean[E.getKey()] += E.getValue();
            }
        }
        for (int i = 0; i < mean.length; i++) {
            mean[i] /= d.size();
        }
        return mean;
    }

    /**
     * Convinence method. Wraps the try-catch of sleep so code is cleaner
     * 
     * @param milis
     *            milliseconds to sleep
     */
    private void sleepWithCatch(long milis) {
        try {
            Thread.sleep(milis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws SQLException {
        TwitterDB.init();
        TwitterApiClient.init();
        DatabaseCrawler dbc = new DatabaseCrawler();

    }

}