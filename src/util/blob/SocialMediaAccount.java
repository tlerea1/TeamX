package util.blob;

import java.util.Collection;

/**
 * Top level interface representing an account on social media
 * 
 * @author Gabe
 * 
 */
public interface SocialMediaAccount {

    /**
     * Returns all the nodes connected to the current account. The relevant
     * connection itself is implemented below this level.
     * 
     * @return Collection of SocialMediaAccounts
     */
    public Collection<SocialMediaAccount> getConnectedNodes();

    /**
     * Unique ID of the object
     * 
     * @return ID
     */
    public String getId();

    /**
     * Retreives the relevant metrics Map represents the n dimentional vector
     * and the labels of the metric
     * 
     * @return map of Metric names to metric values
     */
    public double[] getMetircs();

    /**
     * 
     * @return
     */
    public String getLabel();

}
