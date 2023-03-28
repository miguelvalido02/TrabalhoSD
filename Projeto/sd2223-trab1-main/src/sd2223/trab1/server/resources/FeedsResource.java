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
import sd2223.trab1.server.Domain;
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
    private static Logger Log = Logger.getLogger(RestClient.class.getName());

    private static final String USERS_SERVICE = "users";
    private static final int RETRY_SLEEP = 3000;
    private static final int MAX_RETRIES = 10;

    private Map<String, Map<Long, Message>> feeds;// <username,<mid,message>>
    private ClientConfig config;
    private Client client;

    public FeedsResource() {
        config = new ClientConfig();

        config.property(ClientProperties.READ_TIMEOUT, 5000);
        config.property(ClientProperties.CONNECT_TIMEOUT, 5000);

        client = ClientBuilder.newClient(config);

        this.feeds = new ConcurrentHashMap<String, Map<Long, Message>>();
    }

    @Override
    public long postMessage(String user, String pwd, Message msg) {
        Discovery d = Discovery.getInstance();
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];
        User u = null;
        URI userURI = null;
        try {
            userURI = d.knownUrisOf(domain, USERS_SERVICE);
        } catch (InterruptedException e) {
        }
        WebTarget target = client.target(userURI).path(UsersService.PATH);

        // reTry()
        u = reTry(() -> getUser(name, pwd, target, Status.FORBIDDEN));

        UUID id = UUID.randomUUID();
        long mid = id.getMostSignificantBits();
        msg.setId(mid);
        Map<Long, Message> userFeed = feeds.get(name);
        if (userFeed == null)
            userFeed = new ConcurrentHashMap<Long, Message>();
        userFeed.put(mid, msg);
        // colocar mensagens nos feeds dos users do mesmo dominio
        postInDomain(u, msg);
        // correr todos os seus seguidores e dar post no feed deles
        sendOutsideDomain(u, msg);

        return mid;
    }

    private void sendOutsideDomain(User u, Message msg) {

    }

    private void postInDomain(User u, Message msg) {
        Map<String, Map<String, User>> followers = u.getFollowers();
        Map<String, User> followersInDomain = followers.get(u.getDomain());
        Iterator<User> it = followersInDomain.values().iterator();
        while (it.hasNext()) {
            User follower = it.next();
            Map<Long, Message> followerFeed = feeds.get(follower.getName());
            if (followerFeed == null)
                followerFeed = new ConcurrentHashMap<Long, Message>();
            followerFeed.put(msg.getId(), msg);
        }
    }

    @Override
    public void postOutside(User user, String pwd, Message msg) {
        for (User follower : user.getFollowers().get(Domain.getDomain()).values())
            feeds.get(follower.getName()).put(msg);
    }

    private User getUser(String name, String pwd, WebTarget target, Status userPwdError) {
        Response r = target.path(name)
                .queryParam(UsersService.PWD, pwd).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity()) {
            return r.readEntity(User.class);// 200 OK
        } else if (r.getStatus() == Status.NOT_FOUND.getStatusCode()
                || r.getStatus() == Status.FORBIDDEN.getStatusCode()) {
            throw new WebApplicationException(userPwdError);// 403 pwd is wrong or user does not exist
        } else
            throw new WebApplicationException(Status.BAD_REQUEST);// 400 otherwise
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

    protected <T> T reTry(Supplier<T> func) {
        // método generico recebe uma funçao que quando invocada devolve T.
        // createUser, etc...
        for (int i = 0; i < MAX_RETRIES; i++)
            try {
                return func.get();
            } catch (ProcessingException x) {
                System.err.println(x.getMessage());
                Log.fine("ProcessingException: " + x.getMessage());
                sleep(RETRY_SLEEP);
            } catch (Exception x) {
                Log.fine("Exception: " + x.getMessage());
                x.printStackTrace();
                break;
            }
        return null;
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException x) { // nothing to do...
        }
    }

}