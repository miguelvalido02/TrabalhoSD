package sd2223.trab1.server.resources;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import sd2223.trab1.api.User;
import sd2223.trab1.api.rest.UsersService;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.inject.Singleton;
import sd2223.trab1.api.Message;
import sd2223.trab1.api.rest.FeedsService;
import sd2223.trab1.clients.RestClient;
import sd2223.trab1.clients.RestUsersClient;
import sd2223.trab1.server.Discovery;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.util.function.Supplier;
import java.util.logging.Logger;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

// Implementa FeedsService
@Singleton
public class FeedsResource implements FeedsService {
    private static final String USERS_SERVICE = "users";
    // Message(long id, String user, String domain, String text)

    private Map<String, Map<Long, Message>> feeds;// <username,<mid,message>>

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
        Discovery d = Discovery.getInstance();
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];
        URI userURI = null;
        try {
            userURI = d.knownUrisOf(domain, USERS_SERVICE);
        } catch (InterruptedException e) {
        }
        ClientConfig config = new ClientConfig();

        config.property(ClientProperties.READ_TIMEOUT, 5000);
        config.property(ClientProperties.CONNECT_TIMEOUT, 5000);

        Client client = ClientBuilder.newClient(config);
        WebTarget target = client.target(userURI).path(UsersService.PATH);
        Response r = target.path(name)
                .queryParam(UsersService.PWD, pwd).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        User u = null;

        if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity()) {
            u = r.readEntity(User.class);// 200 OK
        } else if (r.getStatus() == Status.NOT_FOUND.getStatusCode()
                || r.getStatus() == Status.FORBIDDEN.getStatusCode()) {
            throw new WebApplicationException(Status.FORBIDDEN);// 403 pwd is wrong or user does not exist
        } else
            throw new WebApplicationException(Status.BAD_REQUEST);// 400 otherwise
        UUID id = UUID.randomUUID();
        long mid = id.getMostSignificantBits();
        msg.setId(mid);
        Map<Long, Message> userFeed = feeds.get(name);
        if (userFeed == null)
            userFeed = new ConcurrentHashMap<Long, Message>();
        userFeed.put(mid, msg);
        Map<String, Map<String, User>> followers = u.getFollowers();
        // colocar mensagens nos feeds dos users do mesmo dominio
        Map<String, User> followersInDomain = followers.get(u.getDomain());
        Iterator<User> it = followersInDomain.values().iterator();
        while (it.hasNext()) {
            User follower = it.next();
            Map<Long, Message> followerFeed = feeds.get(follower.getName());
            if (followerFeed == null)
                followerFeed = new ConcurrentHashMap<Long, Message>();
            followerFeed.put(mid, msg);
        }
        // correr todos os seus seguidores e dar post no feed deles

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
    public List<String> listSubs(String user) {
        throw new UnsupportedOperationException("Unimplemented method 'listSubs'");
    }

}