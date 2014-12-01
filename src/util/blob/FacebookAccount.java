package util.blob;

import java.util.ArrayList;
import java.util.Collection;

import util.api.FacebookApiClient;
import facebook4j.FacebookException;
import facebook4j.Post;

public class FacebookAccount implements SocialMediaAccount {

    private FacebookApiClient client_;
    private String username_;
    private String id_;

    private int numFriends_ = -1;
    private Collection<Post> statuses_ = null;
    private Collection<Post> feed_ = null;
    private Collection<Link> links_ = null;
    private Collection<FacebookAccount> friends_ = null;

    public FacebookAccount(String id, String username) {
        id_ = id;
        username_ = username;
        // client_ = client;
    }

    public Collection<FacebookAccount> getFriends() {
        if (friends_ != null) {
            return friends_;
        }
        ArrayList<FacebookAccount> friends = new ArrayList<FacebookAccount>();
        try {
            ArrayList<String> friend_ids = (ArrayList<String>) FacebookApiClient
                    .getFriends(id_);
            for (String id : friend_ids) {
                friends.add(new FacebookAccount(id, FacebookApiClient
                        .getUsername()));
            }
        } catch (FacebookException e) {
            e.printStackTrace();
        }
        friends_ = friends;
        return friends;
    }

    public int getNumFriends() {
        if (numFriends_ < 0) {
            numFriends_ = this.getFriends().size();
        }
        return numFriends_;
    }

    public Collection<Post> getStatuses() {
        if (statuses_ != null) {
            return statuses_;
        }
        ArrayList<Post> statuses = new ArrayList<Post>();
        try {
            statuses = (ArrayList<Post>) FacebookApiClient.getPosts(id_);
        } catch (FacebookException e) {
            e.printStackTrace();
        }
        statuses_ = statuses;
        return statuses;
    }

    public Collection<Post> getFeed() {
        if (feed_ != null) {
            return feed_;
        }
        ArrayList<Post> feed = new ArrayList<Post>();
        try {
            feed = (ArrayList<Post>) FacebookApiClient.getPosts(id_);
        } catch (FacebookException e) {
            e.printStackTrace();
        }
        feed_ = feed;
        return feed;
    }

    public Collection<Link> extractLinks() {
        if (links_ == null) {
            if (statuses_ == null) {
                statuses_ = getStatuses();
            }
            links_ = new ArrayList<Link>();
            for (Post p : statuses_) {
                links_.addAll(Link.extractLink(p));
            }
        }
        return links_;
    }

    @Override
    public String getId() {
        return username_;
    }

    @Override
    public double[] getMetircs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getLabel() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<SocialMediaAccount> getConnectedNodes() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

}
