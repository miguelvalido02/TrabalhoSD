package sd2223.trab1.api.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.User;
import sd2223.trab1.api.rest.FeedsService;

// Implementa FeedsService

public class FeedsResource implements FeedsService {

    // Message(long id, String user, String domain, String text)

    private Map<String, Map<Long, Message>> feeds;

    public FeedsResource() {
        this.feeds = new HashMap<String, Map<Long, Message>>();
    }

    /**
     * Posts a new message in the feed, associating it to the feed of the specific
     * user.
     * A message should be identified before publish it, by assigning an ID.
     * A user must contact the server of her domain directly (i.e., this operation
     * should not be
     * propagated to other domain)
     **/

    @Override
    public long postMessage(String user, String pwd, Message msg) {
        throw new UnsupportedOperationException("Unimplemented method 'postMessage'");
    }

    @Override
    public void removeFromPersonalFeed(String user, long mid, String pwd) {
        throw new UnsupportedOperationException("Unimplemented method 'removeFromPersonalFeed'");
    }

    @Override
    public Message getMessage(String user, long mid) {
        throw new UnsupportedOperationException("Unimplemented method 'getMessage'");
    }

    @Override
    public List<Message> getMessages(String user, long time) {
        throw new UnsupportedOperationException("Unimplemented method 'getMessages'");
    }

    @Override
    public void subUser(String user, String userSub, String pwd) {
        throw new UnsupportedOperationException("Unimplemented method 'subUser'");
    }

    @Override
    public void unsubscribeUser(String user, String userSub, String pwd) {
        throw new UnsupportedOperationException("Unimplemented method 'unsubscribeUser'");
    }

    @Override
    public List<User> listSubs(String user) {
        throw new UnsupportedOperationException("Unimplemented method 'listSubs'");
    }

}