package sd2223.trab1.server.resources;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import sd2223.trab1.api.User;
import jakarta.inject.Singleton;
import sd2223.trab1.api.Message;
import sd2223.trab1.server.Domain;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import sd2223.trab1.server.Discovery;
import jakarta.ws.rs.client.WebTarget;
import sd2223.trab1.api.rest.UsersService;
import sd2223.trab1.api.rest.FeedsService;
import jakarta.ws.rs.core.Response.Status;
import java.util.concurrent.ConcurrentHashMap;

import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.function.Supplier;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.WebApplicationException;

// Implementa FeedsService
@Singleton
public class FeedsResource implements FeedsService {
    private static Logger Log = Logger.getLogger(FeedsResource.class.getName());

    private static final String USERS_SERVICE = "users";
    private static final int RETRY_SLEEP = 3000;
    private static final int MAX_RETRIES = 10;

    private static final String FEEDS_SERVICE = "feeds";

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
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];
        User u = null;

        u = verifyUser(name, domain, pwd);
        UUID id = UUID.randomUUID();
        long mid = id.getMostSignificantBits();
        msg.setId(mid);
        msg.setCreationTime(System.currentTimeMillis());

        Map<Long, Message> userFeed = feeds.get(name);
        if (userFeed == null)
            feeds.put(name, userFeed = new ConcurrentHashMap<Long, Message>());
        userFeed.put(mid, msg);

        // colocar mensagens nos feeds dos users do mesmo dominio
        postInDomain(u, msg);
        // correr todos os seus seguidores e dar post no feed deles
        sendOutsideDomain(u, msg);
        return mid;

    }

    private void sendOutsideDomain(User u, Message msg) {
        Discovery d = Discovery.getInstance();
        // Para cada dominio dos followers enviar um pedido de postOutside
        for (String domain : u.obtainFollowers().keySet()) {
            if (domain.equals(u.getDomain()))
                continue;
            Object[] data = new Object[] { u, msg };
            Thread thread = new Thread(() -> {
                reTry(() -> {
                    requestPostOutsideDomain(domain, d, data);
                    return null;
                });
            });

            thread.start();
        }
    }

    private void requestPostOutsideDomain(String domain, Discovery d, Object[] data) {
        try {
            URI userURI = d.knownUrisOf(domain, FEEDS_SERVICE);
            WebTarget target = client.target(userURI).path(FeedsService.PATH).path("post");
            target.request().post(Entity.json(data));

        } catch (InterruptedException e) {
        }
    }

    private void postInDomain(User u, Message msg) {
        Map<String, List<String>> followers = u.obtainFollowers(); // domain <nomeUser, User>
        List<String> followersInDomain = followers.get(Domain.getDomain());
        if (followersInDomain != null) {
            for (String follower : followersInDomain) {
                String name = follower.split("@")[0];
                Map<Long, Message> followerFeed = feeds.get(name);
                if (followerFeed == null) {
                    followerFeed = new ConcurrentHashMap<Long, Message>();
                    feeds.put(name, followerFeed);
                }
                followerFeed.put(msg.getId(), msg);
            }
        }

    }

    @Override
    public void postOutside(Object[] data) {
        User user = (User) data[0];
        Message msg = (Message) data[1];
        List<String> followersInDomain = user.obtainFollowers().get(Domain.getDomain());
        if (followersInDomain != null)
            for (String follower : followersInDomain) {
                String name = follower.split("@")[0];
                Map<Long, Message> feed = feeds.get(name);
                if (feed == null)
                    feeds.put(name, feed = new ConcurrentHashMap<Long, Message>());
                feed.put(msg.getId(), msg);
            }
    }

    @Override
    public void removeFromPersonalFeed(String user, long mid, String pwd) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];
        verifyUser(name, domain, pwd);
        Map<Long, Message> feed = feeds.get(name);
        if (feed == null || feed.get(mid) == null)
            throw new WebApplicationException(Status.NOT_FOUND);
        feed.remove(mid);
    }

    @Override
    public Message getMessage(String user, long mid) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];
        try {
            if (Domain.getDomain().equals(domain)) {
                // Verificar que o user existe
                findUser(domain, name);
                Map<Long, Message> userFeed = feeds.get(name);
                if (userFeed == null || userFeed.get(mid) == null)
                    throw new WebApplicationException(Status.NOT_FOUND);// 404 message does not exist
                return userFeed.get(mid);
            } else {
                Discovery d = Discovery.getInstance();
                URI userURI = d.knownUrisOf(domain, FEEDS_SERVICE);
                WebTarget target = client.target(userURI).path(FeedsService.PATH);
                Response r = target.path(user).path(Long.toString(mid)).request().accept(MediaType.APPLICATION_JSON)
                        .get();
                if (r.getStatus() == Status.NOT_FOUND.getStatusCode())
                    throw new WebApplicationException(Status.NOT_FOUND);// 404 user or message does not exist
                return r.readEntity(Message.class);// 200 OK
            }
        } catch (InterruptedException e) {
        }
        return null;
    }

    @Override
    public List<Message> getMessages(String user, long time) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];
        try {
            if (Domain.getDomain().equals(domain)) {
                // Verificar que o user existe
                findUser(domain, name);

                Map<Long, Message> userFeed = feeds.get(name);
                if (userFeed == null)
                    return new ArrayList<Message>();
                return userFeed.values().stream().filter(m -> m.getCreationTime() > time).collect(Collectors.toList());
            } else {
                Discovery d = Discovery.getInstance();
                URI userURI = d.knownUrisOf(domain, FEEDS_SERVICE);
                WebTarget target = client.target(userURI).path(FeedsService.PATH);
                Response r = target.path(user).queryParam(FeedsService.TIME, time).request()
                        .accept(MediaType.APPLICATION_JSON)
                        .get();
                if (r.getStatus() == Status.NOT_FOUND.getStatusCode())
                    throw new WebApplicationException(Status.NOT_FOUND);// 404 user or message does not exist
                return r.readEntity(new GenericType<List<Message>>() {
                });// 200 OK
            }
        } catch (InterruptedException e) {
        }
        return null;
    }

    @Override
    public void subUser(String user, String userSub, String pwd) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];

        String[] nameDomainSub = userSub.split("@");
        String nameSub = nameDomainSub[0];
        String domainSub = nameDomainSub[1];

        try {
            // Garantir que o userSub existe (pedido ao servico users)
            User sub = findUser(domainSub, nameSub);
            // Garantir que o user existe
            User u = verifyUser(name, domain, pwd);

            // User subscreve userSub
            u.addFollowing(userSub);
            updateUser(domain, name, u);

            sub.addFollower(user);
            updateUser(domainSub, nameSub, sub);

        } catch (InterruptedException e) {
        }

    }

    @Override
    public void unsubscribeUser(String user, String userSub, String pwd) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];

        String[] nameDomainSub = userSub.split("@");
        String nameSub = nameDomainSub[0];
        String domainSub = nameDomainSub[1];

        try {
            // Garantir que o user existe
            User u = verifyUser(name, domain, pwd);

            // Garantir que o userSub existe (pedido ao servico users)
            User sub = findUser(domainSub, nameSub);

            // Garantir que o user subscreve o sub
            if (!u.obtainFollowing().contains(userSub))
                throw new WebApplicationException(Status.NOT_FOUND);// 404 if the userSub is not subscribed

            // User deixa de subscrever userSub
            sub.removeFollower(user);
            updateUser(domainSub, nameSub, sub);

            u.removeFollowing(userSub);
            updateUser(domain, name, u);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public List<String> listSubs(String user) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];

        try {
            // Garantir que o user existe (pedido ao servico users)
            User u = findUser(domain, name);
            return u.obtainFollowing();

        } catch (InterruptedException e) {
        }
        return new ArrayList<String>();
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
            }
        return null;
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException x) { // nothing to do...
        }
    }

    private void updateUser(String domain, String name, User u) throws InterruptedException {
        Discovery d = Discovery.getInstance();
        URI userURI = d.knownUrisOf(domain, USERS_SERVICE);
        WebTarget target = client.target(userURI).path(UsersService.PATH);
        target.path(name)
                .queryParam(UsersService.PWD, u.getPwd()).request()
                .accept(MediaType.APPLICATION_JSON)
                .put(Entity.entity(u, MediaType.APPLICATION_JSON));
    }

    private User findUser(String domain, String name) throws InterruptedException {
        Discovery d = Discovery.getInstance();
        URI userURI = d.knownUrisOf(domain, USERS_SERVICE);
        WebTarget target = client.target(userURI).path(UsersService.PATH);
        Response r = target.path("find").path(name).request().accept(MediaType.APPLICATION_JSON).get();

        if (r.getStatus() == Status.NOT_FOUND.getStatusCode())
            throw new WebApplicationException(Status.NOT_FOUND);// 404 user does not exist
        return r.readEntity(User.class);
    }

    private User verifyUser(String name, String domain, String pwd) {
        try {
            Discovery d = Discovery.getInstance();
            URI userURI = d.knownUrisOf(domain, USERS_SERVICE);
            WebTarget target = client.target(userURI).path(UsersService.PATH);
            return reTry(() -> getUser(name, pwd, target));
        } catch (InterruptedException e) {
        }
        return null;
    }

    private User getUser(String name, String pwd, WebTarget target) {
        Response r = target.path(name)
                .queryParam(UsersService.PWD, pwd).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity())
            return r.readEntity(User.class);// 200 OK
        else if (r.getStatus() == Status.NOT_FOUND.getStatusCode())
            throw new WebApplicationException(Status.NOT_FOUND);
        else if (r.getStatus() == Status.FORBIDDEN.getStatusCode())
            throw new WebApplicationException(Status.FORBIDDEN);// 403 pwd is wrong or user does not exist
        else
            throw new WebApplicationException(Status.BAD_REQUEST);// 400 otherwise
    }

}