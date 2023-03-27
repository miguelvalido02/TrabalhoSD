package sd2223.trab1.server.resources;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Singleton;
import sd2223.trab1.api.Message;
import sd2223.trab1.api.User;
import sd2223.trab1.api.rest.FeedsService;

// Implementa FeedsService
@Singleton
public class FeedsResource implements FeedsService {
    private static final String USERS_SERVICE = "users";
    // Message(long id, String user, String domain, String text)

    private Map<String, Map<Long, Message>> feeds;// <name,<mid,feed>>

    public FeedsResource() {
        this.feeds = new ConcurrentHashMap<String, Map<Long, Message>>();
    }

    /**
     * Posts a new message in the feed, associating it to the feed of the specific
     * user.
     * A message should be identified before publish it, by assigning an ID.
     * A user must contact the server of her domain directly (i.e., this operation
     * should not be
     * propagated to other domain)
     * * @return 200 the unique numerical identifier for the posted message;
     * 403 if the publisher does not exist in the current domain or if the
     * pwd is not correct
     * 400 otherwise
     **/

    @Override
    public long postMessage(String user, String pwd, Message msg) {
        // getUser->verificar se deu erro
        // se houver erro,trata los da maneira certa, ou seja, tranformar erros do user
        // em erros do feed
        // se der td bem, adicionar a mensagem ao proprio feed e aos seguidores

        return mid;
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