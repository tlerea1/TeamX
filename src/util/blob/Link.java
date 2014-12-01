package util.blob;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import facebook4j.Post;

public class Link {

    private String link;
    private long time;

    public Link(String link, long dateCreated) {
        this.link = link;
        this.time = dateCreated;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Link) {
            Link l = ((Link) o);
            return getLink().equals(l.getLink());
        }
        return false;
    }

    public static Collection<Link> extractLink(Tweet s) {
        return getLinks(s.getTweet().split(" "), s.getDate());
    }
    
    public static Collection<Link> extractLink(Post p) {
        return getLinks(p.getMessage().split(" "), p.getCreatedTime().getTime());
    }
    
    private static Collection<Link> getLinks(String[] words, long date) {
        Collection<Link> toReturn = new ArrayList<Link>();
        String reg1 = "(linkd\\.in|t\\.co|bddy\\.me|tcrn\\.ch|\\w+\\.\\w{2})\\/(\\w+|$)";
        String reg2 = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    	for (int i = 0; i < words.length; i++) {
            if (words[i].matches(reg1) || words[i].matches(reg2)) {
            	toReturn.add(new Link (words[i], date));
            }
        }
        return toReturn;
    }

}
