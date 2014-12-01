package util.api;

import java.util.ArrayList;
import java.util.Collection;

import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.Friend;
import facebook4j.FriendRequest;
import facebook4j.Link;
import facebook4j.Post;
import facebook4j.User;

/**
 * API wrapper for the Facebook API
 * 
 * @author chandlernicole
 * 
 */
public class FacebookApiClient {

    // authenticated Facebook account to make the calls
    private static Facebook fb_;

    public FacebookApiClient(Facebook fb) {
        this.fb_ = fb;
    }

    /**
     * Get the id's of the user's friends
     * 
     * @param userID
     * @return collection of friends' ids
     * @throws FacebookException
     */
    public static Collection<String> getFriends(String userID)
            throws FacebookException {
        Collection<Friend> friends = (Collection<Friend>) fb_
                .getFriends(userID);
        ArrayList<String> toReturn = new ArrayList<String>();
        for (Friend f : friends) {
            toReturn.add(f.getId());
        }
        return toReturn;
    }

    /**
     * get the pending friend requests of the user
     * 
     * @param userID
     * @return collection of the Ids of the sending user
     * @throws FacebookException
     */
    public static Collection<String> getFriendRequests(String userID)
            throws FacebookException {
        Collection<FriendRequest> friends = (Collection<FriendRequest>) fb_
                .getFriendRequests(userID);
        ArrayList<String> toReturn = new ArrayList<String>();
        for (FriendRequest f : friends) {
            toReturn.add(f.getFrom().getId());
        }
        return toReturn;
    }

    /**
     * Get the posts that are on the user's feed
     * 
     * @param userId
     * @return collection of posts
     * @throws FacebookException
     */
    public static Collection<Post> getPosts(String userId)
            throws FacebookException {
        return (Collection<Post>) fb_.getFeed(userId);
    }

    /**
     * Get the statuses of the user
     * 
     * @return the collection of posts
     * @throws FacebookException
     */
    public static Collection<Post> getStatuses() throws FacebookException {
        return (Collection<Post>) fb_.getStatuses();
    }

    /**
     * Get the text of a status
     * 
     * @param post
     * @return the text in the status
     */
    public static String getText(Post post) {
        return post.getMessage();
    }

    public static String getUsername() throws IllegalStateException,
            FacebookException {
        return fb_.getName();
    }

    public static String getId() throws IllegalStateException,
            FacebookException {
        return fb_.getId();
    }

    public static Link getLink() throws IllegalStateException,
            FacebookException {
        return fb_.getLink(FacebookApiClient.getId());
    }

    public static User geUser() throws IllegalStateException, FacebookException {
        return fb_.getUser(FacebookApiClient.getId());
    }

}
