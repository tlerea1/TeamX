package util.api;

import util.blob.SocialMediaAccount;

/**
 * Interface to represent and API client for all social networks we use
 * 
 * Makes the references to the API's transparent
 * 
 * @author Gabe
 * 
 */
public interface ApiClient {

    /**
     * Get the social media account in question
     * 
     * @return SocialMediaAccount object retreived through the API call
     */
    public SocialMediaAccount getAccount();

    /**
     * 
     */

}
