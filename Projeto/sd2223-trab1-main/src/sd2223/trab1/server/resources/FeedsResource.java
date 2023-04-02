package sd2223.trab1.server.resources;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response.Status;
import java.util.concurrent.ConcurrentHashMap;

import sd2223.trab1.api.User;
import sd2223.trab1.api.Message;
import sd2223.trab1.server.Domain;
import sd2223.trab1.server.Discovery;
import sd2223.trab1.api.rest.UsersService;
import sd2223.trab1.api.rest.FeedsService;

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

    private static final int MAX_RETRIES = 10;
    private static final int RETRY_SLEEP = 3000;
    private static final String USERS_SERVICE = "users";

    private static final String FEEDS_SERVICE = "feeds";

    private Client client;
    private ClientConfig config;
    private Map<String, Set<String>> following; // <username,<user@domain>>
    private Map<String, Map<Long, Message>> feeds;// <username,<mid,message>>
    private Map<String, Map<String, List<String>>> followers; // <username,<domain,<user@domain>>>

    public FeedsResource() {
        config = new ClientConfig();
        config.property(ClientProperties.READ_TIMEOUT, 5000);
        config.property(ClientProperties.CONNECT_TIMEOUT, 5000);
        client = ClientBuilder.newClient(config);
        this.following = new ConcurrentHashMap<String, Set<String>>();
        this.feeds = new ConcurrentHashMap<String, Map<Long, Message>>();
        this.followers = new ConcurrentHashMap<String, Map<String, List<String>>>();
    }

    @Override
    public long postMessage(String user, String pwd, Message msg) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];

        User u = verifyUser(name, domain, pwd);
        UUID id = UUID.randomUUID();
        long mid = id.getMostSignificantBits();
        msg.setId(mid);
        msg.setCreationTime(System.currentTimeMillis());

        // post no proprio feed
        Map<Long, Message> userFeed = feeds.get(name);
        if (userFeed == null)
            feeds.put(name, userFeed = new ConcurrentHashMap<Long, Message>());
        userFeed.put(mid, msg);

        Map<String, List<String>> userFollowers = followers.get(u.getName()); // domain <nomeUser, User>
        if (userFollowers != null) {
            // colocar mensagens nos feeds dos users do mesmo dominio
            postInDomain(msg, userFollowers);
            // correr todos os seus seguidores e dar post no feed deles
            sendOutsideDomain(u, msg, userFollowers);
        }
        return mid;
    }

    private void postInDomain(Message msg, Map<String, List<String>> userFollowers) {
        List<String> followersInDomain = userFollowers.get(Domain.getDomain());
        if (followersInDomain != null) {
            for (String follower : followersInDomain) {
                String name = follower.split("@")[0];
                Map<Long, Message> followerFeed = feeds.get(name);
                if (followerFeed == null)
                    feeds.put(name, followerFeed = new ConcurrentHashMap<Long, Message>());
                followerFeed.put(msg.getId(), msg);
            }
        }
    }

    private void sendOutsideDomain(User u, Message msg, Map<String, List<String>> userFollowers) {
        // Para cada dominio dos followers enviar um pedido de postOutside
        Discovery d = Discovery.getInstance();
        for (String domain : userFollowers.keySet()) {
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

    @Override
    public void postOutside(Object[] data) {
        User user = (User) data[0];
        Message msg = (Message) data[1];
        String nameDomain = user.getName() + "@" + user.getDomain();

        for (Map.Entry<String, Set<String>> entry : following.entrySet()) {
            String userName = entry.getKey();
            Set<String> followingList = entry.getValue();
            if (followingList.contains(nameDomain)) {
                Map<Long, Message> feed = feeds.get(userName);
                if (feed == null)
                    feeds.put(userName, feed = new ConcurrentHashMap<Long, Message>());
                feed.put(msg.getId(), msg);
            }
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
                Response r = reTry(() -> getMessage(target, user, mid));
                if (r.getStatus() == Status.NOT_FOUND.getStatusCode())
                    throw new WebApplicationException(Status.NOT_FOUND);// 404 user or message does not exist
                return r.readEntity(Message.class);// 200 OK
            }
        } catch (InterruptedException e) {
        }
        return null;
    }

    private Response getMessage(WebTarget target, String user, Long mid) {
        return target.path(user).path(Long.toString(mid)).request().accept(MediaType.APPLICATION_JSON)
                .get();
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
                Response r = reTry(() -> getMessages(target, user, time));
                if (r.getStatus() == Status.NOT_FOUND.getStatusCode())
                    throw new WebApplicationException(Status.NOT_FOUND);// 404 user or message does not exist
                return r.readEntity(new GenericType<List<Message>>() {
                });// 200 OK
            }
        } catch (InterruptedException e) {
        }
        return null;
    }

    private Response getMessages(WebTarget target, String user, long time) {
        return target.path(user).queryParam(FeedsService.TIME, time).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
    }

    @Override
    public void subUser(String user, String userSub, String pwd) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];

        String[] nameDomainSub = userSub.split("@");
        String nameSub = nameDomainSub[0];
        String domainSub = nameDomainSub[1];
        // Garantir que o userSub existe (pedido ao servico users)
        findUser(domainSub, nameSub);
        // Garantir que o user existe
        verifyUser(name, domain, pwd);

        // User subscreve userSub
        if (domain.equals(Domain.getDomain())) {
            Set<String> userFollowing = following.get(name);
            if (userFollowing == null)
                following.put(name, userFollowing = new HashSet<String>());
            userFollowing.add(userSub);
        }
        if (domainSub.equals(Domain.getDomain())) {
            Map<String, List<String>> subFollowers = followers.get(nameSub);
            if (subFollowers == null)
                followers.put(nameSub, subFollowers = new ConcurrentHashMap<String, List<String>>());
            List<String> followersInDomain = subFollowers.get(domain);
            if (followersInDomain == null)
                subFollowers.put(domain, followersInDomain = new ArrayList<String>());
            followersInDomain.add(user);
        } else {
            reTry(() -> {
                subOutside(domainSub, user, userSub, pwd);
                return null;
            });
        }
    }

    private void subOutside(String subDomain, String user, String userSub, String pwd) {
        try {
            Discovery d = Discovery.getInstance();
            URI userURI = d.knownUrisOf(subDomain, FEEDS_SERVICE);
            WebTarget target = client.target(userURI).path(FeedsService.PATH);
            target.path("sub").path(user).path(userSub)
                    .queryParam(FeedsService.PWD, pwd).request()
                    .accept(MediaType.APPLICATION_JSON).post(Entity.json(null));
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

        // Garantir que o user existe
        verifyUser(name, domain, pwd);

        // Garantir que o userSub existe (pedido ao servico users)
        findUser(domainSub, nameSub);

        // Garantir que o user subscreve o sub
        if (Domain.getDomain().equals(domain)) {
            Set<String> userFollowing = following.get(name);
            if (userFollowing == null || !userFollowing.contains(userSub))
                throw new WebApplicationException(Status.NOT_FOUND);// 404 if the userSub is not subscribed
            userFollowing.remove(userSub);
        }
        // User deixa de subscrever userSub
        if (Domain.getDomain().equals(domainSub)) {
            Map<String, List<String>> subFollowers = followers.get(nameSub);
            List<String> subFollowersInDomain = null;
            if (subFollowers == null || (subFollowersInDomain = subFollowers.get(domain)) == null
                    || !subFollowersInDomain.contains(user))
                throw new WebApplicationException(Status.NOT_FOUND);// 404 if the userSub is not subscribed
            subFollowersInDomain.remove(user);
        } else {
            reTry(() -> {
                unsubOutside(domainSub, user, userSub, pwd);
                return null;
            });
        }
    }

    private void unsubOutside(String subDomain, String user, String userSub, String pwd) {
        try {
            Discovery d = Discovery.getInstance();
            URI userURI = d.knownUrisOf(subDomain, FEEDS_SERVICE);
            WebTarget target = client.target(userURI).path(FeedsService.PATH);
            target.path("sub").path(user).path(userSub)
                    .queryParam(FeedsService.PWD, pwd).request()
                    .accept(MediaType.APPLICATION_JSON)
                    .delete();
        } catch (InterruptedException e) {
        }
    }

    @Override
    public List<String> listSubs(String user) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];

        // Garantir que o user existe (pedido ao servico users)
        findUser(domain, name);
        if (following.get(name) == null)
            return new ArrayList<String>();
        return new ArrayList<String>(following.get(name));
    }

    @Override
    public void deleteFeed(String user, String domain, String pwd) {
        String nameDomain = user + "@" + domain;
        if (domain.equals(Domain.getDomain())) {
            feeds.remove(user);

            Set<String> followings = following.get(user);
            if (followings != null) {
                for (String sub : followings)
                    unsubscribeUser(nameDomain, sub, pwd);
                following.remove(user);
            }

            Map<String, List<String>> userFollowers = followers.get(user);
            if (userFollowers != null) {
                // tirar os followings do proprio dominio
                List<String> followersInDomain = userFollowers.get(Domain.getDomain());
                if (followersInDomain != null)
                    for (String follower : followersInDomain) {
                        String followerName = follower.split("@")[0];
                        Set<String> followingUserToDelete = following.get(followerName);
                        if (followingUserToDelete != null)
                            followingUserToDelete.remove(nameDomain);
                    }

                // tirar o user que vai ser apagado da lista following dos users doutros
                // dominios
                Discovery d = Discovery.getInstance();
                for (String followerDomain : userFollowers.keySet()) {
                    if (domain.equals(followerDomain))
                        continue;
                    Thread thread = new Thread(() -> {
                        reTry(() -> {
                            requestDeleteOutsideDomain(user, domain, pwd, followerDomain, d);
                            return null;
                        });
                    });
                    thread.start();
                }
                followers.remove(user);
            }
        } else {
            for (Set<String> followingList : following.values())
                followingList.remove(nameDomain);
        }
    }

    private void requestDeleteOutsideDomain(String user, String domain, String pwd, String followerDomain,
            Discovery d) {
        try {
            URI userURI = d.knownUrisOf(followerDomain, FEEDS_SERVICE);
            WebTarget target = client.target(userURI).path(FeedsService.PATH);
            target.path("delete").path(user).path(domain).queryParam(FeedsService.PWD, pwd)
                    .request().accept(MediaType.APPLICATION_JSON).delete();
        } catch (InterruptedException e) {
        }
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

    private User findUser(String domain, String name) {
        Discovery d = Discovery.getInstance();
        User u = reTry(() -> findUser(name, domain, d));

        return u;
    }

    private User findUser(String name, String domain, Discovery d) {
        try {
            URI userURI = d.knownUrisOf(domain, USERS_SERVICE);
            WebTarget target = client.target(userURI).path(UsersService.PATH);
            Response r = target.path("find").path(name).request().accept(MediaType.APPLICATION_JSON).get();
            if (r.getStatus() == Status.NOT_FOUND.getStatusCode())
                throw new WebApplicationException(Status.NOT_FOUND);// 404 user does not exist
            if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity())
                return r.readEntity(User.class);
        } catch (InterruptedException e) {
        }
        return null;
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