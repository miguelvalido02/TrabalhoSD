package sd2223.trab1.server.java;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.List;

import java.util.logging.Logger;
//import java.util.function.Supplier;
import java.util.stream.Collectors;

/*import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response.Status;*/
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sd2223.trab1.api.User;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.java.Result.ErrorCode;
import sd2223.trab1.api.Message;
import sd2223.trab1.server.Domain;
import sd2223.trab1.server.Discovery;
//import sd2223.trab1.api.rest.UsersService;
import sd2223.trab1.clients.FeedsClientFactory;
import sd2223.trab1.clients.UsersClientFactory;
//import sd2223.trab1.api.rest.FeedsService;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

//import jakarta.ws.rs.client.Client;
//import jakarta.ws.rs.ProcessingException;
//import jakarta.ws.rs.client.ClientBuilder;
//import jakarta.ws.rs.WebApplicationException;

public class JavaFeeds implements Feeds {

    private static Logger Log = Logger.getLogger(JavaFeeds.class.getName());

    // private static final int MAX_RETRIES = 10;
    // private static final int RETRY_SLEEP = 500;
    private static final String USERS_SERVICE = "users";

    private static final String FEEDS_SERVICE = "feeds";

    // private Client client;
    private ClientConfig config;
    private Map<String, Set<String>> following; // <username,<user@domain>>
    private Map<String, Map<Long, Message>> feeds;// <username,<mid,message>>
    private Map<String, Map<String, List<String>>> followers; // <username,<domain,<user@domain>>>
    private ExecutorService executor;
    private int counter;

    public JavaFeeds() {
        config = new ClientConfig();
        config.property(ClientProperties.READ_TIMEOUT, 5000);
        config.property(ClientProperties.CONNECT_TIMEOUT, 5000);
        // client = ClientBuilder.newClient(config);
        this.following = new ConcurrentHashMap<String, Set<String>>();
        this.feeds = new ConcurrentHashMap<String, Map<Long, Message>>();
        this.followers = new ConcurrentHashMap<String, Map<String, List<String>>>();
        executor = Executors.newFixedThreadPool(50);
        this.counter = 0;
    }

    @Override
    public Result<Long> postMessage(String user, String pwd, Message msg) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];
        if (!domain.equals(Domain.getDomain()))
            return Result.error(ErrorCode.BAD_REQUEST);
        Log.info("Antes Do verify");
        Result<User> u = verifyUser(name, domain, pwd);
        Log.info("Depois Do verify");
        Log.info("Is ok?: " + u.isOK());
        if (u.isOK()) {
            User us = u.value();
            long mid = 256 * counter + Domain.getSeq();
            counter++;
            msg.setId(mid);
            msg.setCreationTime(System.currentTimeMillis());
            Log.info("post:" + user + msg);
            // post no proprio feed

            Map<Long, Message> userFeed = feeds.get(name);
            if (userFeed == null)
                feeds.put(name, userFeed = new ConcurrentHashMap<Long, Message>());
            userFeed.put(mid, msg);

            Map<String, List<String>> userFollowers = followers.get(us.getName()); // domain <nomeUser, User>

            if (userFollowers != null) {
                // colocar mensagens nos feeds dos users do mesmo dominio
                postInDomain(user, msg, userFollowers);
                // correr todos os seus seguidores e dar post no feed deles
                sendOutsideDomain(us, msg, userFollowers);
            }
            return Result.ok(mid);
        }
        return Result.error(u.error());
    }

    private void postInDomain(String user, Message msg, Map<String, List<String>> userFollowers) {
        List<String> followersInDomain = userFollowers.get(Domain.getDomain());
        if (followersInDomain != null) {
            for (String follower : followersInDomain) {
                String name = follower.split("@")[0];
                Set<String> followingListOfFollower = following.get(name);
                if (followingListOfFollower != null && followingListOfFollower.contains(user)) {
                    Map<Long, Message> followerFeed = feeds.get(name);
                    if (followerFeed == null)
                        feeds.put(name, followerFeed = new ConcurrentHashMap<Long, Message>());
                    followerFeed.put(msg.getId(), msg);
                }
            }
        }
    }

    private void sendOutsideDomain(User us, Message msg, Map<String, List<String>> userFollowers) {
        // Para cada dominio dos followers enviar um pedido de postOutside
        Discovery d = Discovery.getInstance();
        for (String domain : userFollowers.keySet()) {
            if (domain.equals(us.getDomain()))
                continue;
            executor.submit(() -> {
                requestPostOutsideDomain(domain, d, us.getName() + "@" + us.getDomain(), msg);
            });
        }
    }

    private void requestPostOutsideDomain(String domain, Discovery d, String user, Message msg) {
        try {
            URI userURI = d.knownUrisOf(domain, FEEDS_SERVICE);
            // WebTarget target =
            // client.target(userURI).path(FeedsService.PATH).path("post").path(user);
            // target.request().post(Entity.json(msg));
            FeedsClientFactory.get(userURI).postOutside(user, msg);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public Result<Void> postOutside(String user, Message msg) {
        for (Map.Entry<String, Set<String>> entry : following.entrySet()) {
            String userName = entry.getKey();
            Set<String> followingList = entry.getValue();
            if (followingList.contains(user)) {
                Map<Long, Message> feed = feeds.get(userName);
                if (feed == null)
                    feeds.put(userName, feed = new ConcurrentHashMap<Long, Message>());
                feed.put(msg.getId(), msg);
            }
        }
        return Result.ok();
    }

    @Override
    public Result<Void> removeFromPersonalFeed(String user, long mid, String pwd) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];
        Result<User> u = verifyUser(name, domain, pwd);
        if (u.isOK()) {
            Map<Long, Message> feed = feeds.get(name);
            if (feed == null || feed.get(mid) == null)
                return Result.error(ErrorCode.NOT_FOUND);
            feed.remove(mid);
            return Result.ok();
        }
        return Result.error(u.error());
    }

    @Override
    public Result<Message> getMessage(String user, long mid) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];
        try {
            if (Domain.getDomain().equals(domain)) {
                // Verificar que o user existe
                Result<User> u = findUser(domain, name);
                if (u.isOK()) {
                    Map<Long, Message> userFeed = feeds.get(name);
                    if (userFeed == null || userFeed.get(mid) == null)
                        return Result.error(ErrorCode.NOT_FOUND);// 404 message does not exist
                    return Result.ok(userFeed.get(mid));
                }
                return Result.error(u.error());

            } else {
                Discovery d = Discovery.getInstance();
                URI userURI = d.knownUrisOf(domain, FEEDS_SERVICE);
                // WebTarget target = client.target(userURI).path(FeedsService.PATH);
                return FeedsClientFactory.get(userURI).getMessage(user, mid);
                // Message m = reTry(() -> getMessage(target, user, mid));
                // return Result.ok(m);
            }
        } catch (InterruptedException e) {
        }
        return null;// manhoso
    }

    /*
     * private Message getMessage(WebTarget target, String user, Long mid) {
     * Response r =
     * target.path(user).path(Long.toString(mid)).request().accept(MediaType.
     * APPLICATION_JSON)
     * .get();
     * if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity())
     * return r.readEntity(Message.class);// 200 OK
     * if (r.getStatus() == Status.NOT_FOUND.getStatusCode())
     * throw new WebApplicationException(Status.NOT_FOUND);// 404 user or message
     * does not exist
     * return null;
     * }
     */

    @Override
    public Result<List<Message>> getMessages(String user, long time) {
        Log.info("getMessages:" + user + " time: " + time + " currentTime: " + System.currentTimeMillis());
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];
        try {
            if (Domain.getDomain().equals(domain)) {
                // Verificar que o user existe
                Result<User> u = findUser(domain, name);
                if (u.isOK()) {
                    Map<Long, Message> userFeed = feeds.get(name);
                    if (userFeed == null) {
                        Log.severe("Entrei no userFeed==null");
                        return Result.ok(new CopyOnWriteArrayList<Message>());
                    }
                    return Result.ok(userFeed.values().stream().filter(m -> m.getCreationTime() > time)
                            .collect(Collectors.toList()));
                } else
                    return Result.error(u.error());
            } else {
                Discovery d = Discovery.getInstance();
                URI userURI = d.knownUrisOf(domain, FEEDS_SERVICE);
                // WebTarget target = client.target(userURI).path(FeedsService.PATH);
                // List<Message> l = reTry(() -> getMessages(target, user, time));
                // return Result.ok(l);
                return FeedsClientFactory.get(userURI).getMessages(user, time);
            }
        } catch (InterruptedException e) {
        }
        return null;// manhoso
    }

    /*
     * private List<Message> getMessages(WebTarget target, String user, long time) {
     * Response r = target.path(user).queryParam(FeedsService.TIME, time).request()
     * .accept(MediaType.APPLICATION_JSON)
     * .get();
     * if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity())
     * return r.readEntity(new GenericType<List<Message>>() {
     * });// 200 OK
     * if (r.getStatus() == Status.NOT_FOUND.getStatusCode())
     * throw new WebApplicationException(Status.NOT_FOUND);// 404 user or message
     * does not exist
     * return null;
     * }
     */

    @Override
    public Result<Void> subUser(String user, String userSub, String pwd) {
        Log.info("subUser:" + user + " sub:" + userSub);
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];

        String[] nameDomainSub = userSub.split("@");
        String nameSub = nameDomainSub[0];
        String domainSub = nameDomainSub[1];
        // Garantir que o userSub existe (pedido ao servico users)
        Result<User> uf = findUser(domainSub, nameSub);
        if (uf.isOK()) {
            // Garantir que o user existe
            Result<User> uv = verifyUser(name, domain, pwd);
            if (uv.isOK()) {
                // User subscreve userSub
                if (domain.equals(Domain.getDomain())) {
                    Set<String> userFollowing = following.get(name);
                    if (userFollowing == null)
                        following.put(name, userFollowing = new CopyOnWriteArraySet<String>());
                    userFollowing.add(userSub);

                }
                if (domainSub.equals(Domain.getDomain())) {
                    Map<String, List<String>> subFollowers = followers.get(nameSub);
                    if (subFollowers == null)
                        followers.put(nameSub, subFollowers = new ConcurrentHashMap<String, List<String>>());
                    List<String> followersInDomain = subFollowers.get(domain);
                    if (followersInDomain == null)
                        subFollowers.put(domain, followersInDomain = new CopyOnWriteArrayList<String>());
                    followersInDomain.add(user);

                } else
                    executor.submit(() -> subOutside(domainSub, user, userSub, pwd));
                return Result.ok();
            }
            return Result.error(uv.error());
        }
        return Result.error(uf.error());
    }

    private void subOutside(String subDomain, String user, String userSub, String pwd) {
        try {
            Discovery d = Discovery.getInstance();
            URI userURI = d.knownUrisOf(subDomain, FEEDS_SERVICE);
            // WebTarget target = client.target(userURI).path(FeedsService.PATH);
            // target.path("sub").path(user).path(userSub)
            // .queryParam(FeedsService.PWD, pwd).request()
            // .post(Entity.json(null));
            FeedsClientFactory.get(userURI).subUser(user, userSub, pwd);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public Result<Void> unsubscribeUser(String user, String userSub, String pwd) {
        Log.info("unsub: " + user + " sub: " + userSub);
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];

        String[] nameDomainSub = userSub.split("@");
        String nameSub = nameDomainSub[0];
        String domainSub = nameDomainSub[1];

        // Garantir que o user existe
        Result<User> uv = verifyUser(name, domain, pwd);
        if (uv.isOK()) {
            // Garantir que o userSub existe (pedido ao servico users)
            Result<User> uf = findUser(domainSub, nameSub);
            if (uf.isOK()) {
                // Garantir que o user subscreve o sub
                if (Domain.getDomain().equals(domain)) {
                    Set<String> userFollowing = following.get(name);
                    if (userFollowing == null || !userFollowing.contains(userSub))
                        return Result.error(ErrorCode.NOT_FOUND);// 404 if the userSub is not subscribed
                    userFollowing.remove(userSub);
                }
                // User deixa de subscrever userSub
                if (Domain.getDomain().equals(domainSub)) {
                    Map<String, List<String>> subFollowers = followers.get(nameSub);
                    List<String> subFollowersInDomain = null;
                    if (subFollowers == null || (subFollowersInDomain = subFollowers.get(domain)) == null
                            || !subFollowersInDomain.contains(user))
                        return Result.error(ErrorCode.NOT_FOUND);// 404 if the userSub is not subscribed
                    subFollowersInDomain.remove(user);
                } else
                    executor.submit(() -> unsubOutside(domainSub, user, userSub, pwd));
                return Result.ok();
            }
            return Result.error(uf.error());
        }
        return Result.error(uv.error());
    }

    private void unsubOutside(String subDomain, String user, String userSub, String pwd) {
        try {
            Discovery d = Discovery.getInstance();
            URI userURI = d.knownUrisOf(subDomain, FEEDS_SERVICE);
            // WebTarget target = client.target(userURI).path(FeedsService.PATH);
            // target.path("sub").path(user).path(userSub)
            // .queryParam(FeedsService.PWD, pwd).request().delete();
            FeedsClientFactory.get(userURI).unsubscribeUser(user, userSub, pwd);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public Result<List<String>> listSubs(String user) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];

        // Garantir que o user existe (pedido ao servico users)
        Result<User> u = findUser(domain, name);
        if (u.isOK()) {
            if (following.get(name) == null)
                return Result.ok(new CopyOnWriteArrayList<String>());
            return Result.ok(new CopyOnWriteArrayList<String>(following.get(name)));
        }
        return Result.error(u.error());
    }

    @Override
    public Result<Void> deleteFeed(String user, String domain, String pwd) {
        String nameDomain = user + "@" + domain;
        if (domain.equals(Domain.getDomain())) {
            feeds.remove(user);

            Set<String> followings = following.get(user);
            if (followings != null) {
                for (String sub : followings)
                    executor.submit(() -> unsubscribeUser(nameDomain, sub, pwd));
                following.remove(user);
            }

            Map<String, List<String>> userFollowers = null;

            userFollowers = followers.get(user);

            if (userFollowers != null) {
                // tirar os followings do proprio dominio
                List<String> followersInDomain = null;
                followersInDomain = userFollowers.get(Domain.getDomain());

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
                    executor.submit(() -> requestDeleteOutsideDomain(user, domain, pwd, followerDomain, d));
                }
                followers.remove(user);
            }
        } else {
            for (Set<String> followingList : following.values())
                followingList.remove(nameDomain);

        }
        return Result.ok();
    }

    private void requestDeleteOutsideDomain(String user, String domain, String pwd, String followerDomain,
            Discovery d) {
        try {
            URI userURI = d.knownUrisOf(followerDomain, FEEDS_SERVICE);
            // WebTarget target = client.target(userURI).path(FeedsService.PATH);
            // target.path("delete").path(user).path(domain).queryParam(FeedsService.PWD,
            // pwd)
            // .request().accept(MediaType.APPLICATION_JSON).delete();
            FeedsClientFactory.get(userURI).deleteFeed(user, followerDomain, pwd);
        } catch (InterruptedException e) {
        }
    }

    /*
     * protected <T> T reTry(Supplier<T> func) {
     * // método generico recebe uma funçao que quando invocada devolve T.
     * // createUser, etc...
     * for (int i = 0; i < MAX_RETRIES; i++)
     * try {
     * return func.get();
     * } catch (ProcessingException x) {
     * System.err.println(x.getMessage());
     * Log.fine("ProcessingException: " + x.getMessage());
     * sleep(RETRY_SLEEP);
     * }
     * return null;
     * }
     * 
     * private void sleep(int ms) {
     * try {
     * Thread.sleep(ms);
     * } catch (InterruptedException x) { // nothing to do...
     * }
     * }
     */

    private Result<User> findUser(String domain, String name) {
        Discovery d = Discovery.getInstance();
        try {
            URI userURI = d.knownUrisOf(domain, USERS_SERVICE);
            // WebTarget target = client.target(userURI).path(UsersService.PATH);
            // return reTry(() -> findUser(name, domain, target, d));
            return UsersClientFactory.get(userURI).userExists(name);
        } catch (InterruptedException e) {

        }
        return null;
    }

    /*
     * private User findUser(String name, String domain, WebTarget target, Discovery
     * d) {
     * Response r =
     * target.path("find").path(name).request().accept(MediaType.APPLICATION_JSON).
     * get();
     * if (r.getStatus() == Status.NOT_FOUND.getStatusCode())
     * throw new WebApplicationException(Status.NOT_FOUND);// 404 user does not
     * exist
     * if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity())
     * return r.readEntity(User.class);
     * return null;
     * }
     */

    private Result<User> verifyUser(String name, String domain, String pwd) {
        try {
            Discovery d = Discovery.getInstance();
            URI userURI = d.knownUrisOf(domain, USERS_SERVICE);
            Log.info("userURI: " + userURI);
            // WebTarget target = client.target(userURI).path(UsersService.PATH);
            // return reTry(() -> getUser(name, pwd, target));
            return UsersClientFactory.get(userURI).getUser(name, pwd);
        } catch (InterruptedException e) {
        }
        return null;
    }

    /*
     * private User getUser(String name, String pwd, WebTarget target) {
     * Response r = target.path(name)
     * .queryParam(UsersService.PWD, pwd).request()
     * .accept(MediaType.APPLICATION_JSON)
     * .get();
     * 
     * if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity())
     * return r.readEntity(User.class);// 200 OK
     * else if (r.getStatus() == Status.NOT_FOUND.getStatusCode())
     * throw new WebApplicationException(Status.NOT_FOUND);
     * else if (r.getStatus() == Status.FORBIDDEN.getStatusCode())
     * throw new WebApplicationException(Status.FORBIDDEN);// 403 pwd is wrong or
     * user does not exist
     * else
     * throw new WebApplicationException(Status.BAD_REQUEST);// 400 otherwise
     * }
     */
}
