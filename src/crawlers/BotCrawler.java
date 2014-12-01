package crawlers;

import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;

import util.Filter;
import util.blob.SocialMediaAccount;

/**
 * Crawler to itterate over the twitterverse We conceptualize twitter as a graph
 * and this BOT will execute a BFS over the nodes that announce themselves as
 * bots
 * 
 * @author Gabe
 * 
 */
public class BotCrawler extends Thread {

    // Start point for the current crawl.
    private SocialMediaAccount startPoint_;
    // universal self updating filter
    private Filter filter_ = Filter.FILTER_SINGLETON;
    // Hashtable holding all the nodes we have already visted so we dont loop
    private Hashtable<String, Integer> visitedNodes_ = new Hashtable<String, Integer>();
    // Queue of SocialMediaAccounts to visit. We are doing BFS, so this is a
    // FIFO structure
    private Queue<SocialMediaAccount> queue_ = new LinkedList<SocialMediaAccount>();

    public BotCrawler(SocialMediaAccount startPoint) {
        startPoint_ = startPoint;
    }

    /**
     * this class executes on a separate thread
     */
    public void run() {
        crawl();
    }

    private void crawl() {

        // We execute a BFS of the graph starting at the start point
        // Initialize the queue to include all the connnextions of the starting
        // node
        enqueueNodes(startPoint_.getConnectedNodes());

        SocialMediaAccount currentNode = null;

        while (queue_.peek() != null) {
            // Visit the next node on the list
            currentNode = queue_.poll();
            // Make sure we dont visit the same node twice
            if (visitedNodes_.containsKey(currentNode.getId())) {
                // Jump to the next node if we have already visited this one
                continue;
            } else {
                // Add this one to the visited list
                visitedNodes_.put(currentNode.getId(), 1);
            }

            // We allow the filter to make the decision if the current node is
            // actually a bot. All DB writing is handled by the filter.
            if (filter_.isBot(currentNode)) {
                // We think this node is a bot. We add all of its nodes to the
                // queue
                enqueueNodes(currentNode.getConnectedNodes());
            }
        }
    }

    /**
     * Adds all the SocialMediaAccounts in the collection to the current queue.
     * 
     * @param accounts
     */
    private void enqueueNodes(Collection<SocialMediaAccount> accounts) {
        for (SocialMediaAccount a : accounts) {
            queue_.add(a);
        }
    }

}
