package sd2223.trab1.server.kafka;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CopyOnWriteArrayList;

import sd2223.trab1.api.Message;
import sd2223.trab1.server.Domain;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.server.Discovery;
import sd2223.trab1.api.java.Result.ErrorCode;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

public class RepFeeds implements RepFeedsInterface {
    private static final String FEEDS_SERVICE = "feeds";

    private ClientConfig config;
    private ExecutorService executor;
    private Map<String, Set<String>> following; // <username,<user@domain>>
    private Map<String, Map<Long, Message>> feeds;// <username,<mid,message>>
    private Map<String, Map<String, List<String>>> followers; // <username,<domain,<user@domain>>>

    public RepFeeds() {
        config = new ClientConfig();
        executor = Executors.newFixedThreadPool(50);
        config.property(ClientProperties.READ_TIMEOUT, 5000);
        config.property(ClientProperties.CONNECT_TIMEOUT, 5000);
        this.following = new ConcurrentHashMap<String, Set<String>>();
        this.feeds = new ConcurrentHashMap<String, Map<Long, Message>>();
        this.followers = new ConcurrentHashMap<String, Map<String, List<String>>>();
    }

    @Override
    public Result<Long> postMessage(String user, Message msg) {
        String name = user.split("@")[0];
        Map<Long, Message> userFeed = feeds.get(name);
        if (userFeed == null)
            feeds.put(name, userFeed = new ConcurrentHashMap<Long, Message>());
        userFeed.put(msg.getId(), msg);

        Map<String, List<String>> userFollowers = followers.get(name); // domain <nomeUser, User>
        if (userFollowers != null) {
            // colocar mensagens nos feeds dos users do mesmo dominio
            postInDomain(user, msg, userFollowers);
            // correr todos os seus seguidores e dar post no feed deles
            sendOutsideDomain(name, msg, userFollowers);
        }
        return Result.ok((Long) msg.getId());
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

    private void sendOutsideDomain(String name, Message msg, Map<String, List<String>> userFollowers) {
        // Para cada dominio dos followers enviar um pedido de postOutside
        Discovery d = Discovery.getInstance();
        for (String domain : userFollowers.keySet()) {
            if (domain.equals(Domain.getDomain()))
                continue;
            executor.submit(() -> {
                requestPostOutsideDomain(domain, d, name + "@" + Domain.getDomain(), msg);
            });
        }
    }

    private void requestPostOutsideDomain(String domain, Discovery d, String user, Message msg) {
        try {
            URI userURI = d.knownUrisOf(domain, FEEDS_SERVICE);
            RepFeedsClientFactory.get(userURI).postOutside(user, msg);
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
    public Result<Void> removeFromPersonalFeed(String name, long mid) {
        Map<Long, Message> feed = feeds.get(name);
        if (feed == null || feed.get(mid) == null)
            return Result.error(ErrorCode.NOT_FOUND);
        feed.remove(mid);
        return Result.ok();
    }

    @Override
    public Result<Message> getMessage(String name, long mid) {
        Map<Long, Message> userFeed = feeds.get(name);
        if (userFeed == null || userFeed.get(mid) == null)
            return Result.error(ErrorCode.NOT_FOUND);// 404 message does not exist
        return Result.ok(userFeed.get(mid));
    }

    @Override
    public Result<List<Message>> getMessages(String name, long time) {
        Map<Long, Message> userFeed = feeds.get(name);
        if (userFeed == null) {
            return Result.ok(new CopyOnWriteArrayList<Message>());
        }
        return Result.ok(userFeed.values().stream().filter(m -> m.getCreationTime() > time)
                .collect(Collectors.toList()));
    }

    @Override
    public Result<Void> subUser(String user, String userSub, String pwd) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];

        String[] nameDomainSub = userSub.split("@");
        String nameSub = nameDomainSub[0];
        String domainSub = nameDomainSub[1];
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

    private void subOutside(String subDomain, String user, String userSub, String pwd) {
        try {
            Discovery d = Discovery.getInstance();
            URI userURI = d.knownUrisOf(subDomain, FEEDS_SERVICE);
            RepFeedsClientFactory.get(userURI).subUser(user, userSub, pwd, Long.MIN_VALUE);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public Result<Void> unsubscribeUser(String user, String userSub, String pwd) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];

        String[] nameDomainSub = userSub.split("@");
        String nameSub = nameDomainSub[0];
        String domainSub = nameDomainSub[1];
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

    private void unsubOutside(String subDomain, String user, String userSub, String pwd) {
        try {
            Discovery d = Discovery.getInstance();
            URI userURI = d.knownUrisOf(subDomain, FEEDS_SERVICE);
            RepFeedsClientFactory.get(userURI).unsubscribeUser(user, userSub, pwd, Long.MIN_VALUE);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public Result<List<String>> listSubs(String name) {
        if (following.get(name) == null)
            return Result.ok(new CopyOnWriteArrayList<String>());
        return Result.ok(new CopyOnWriteArrayList<String>(following.get(name)));
    }

    @Override
    public Result<Void> deleteFeed(String name, String domain, String pwd) {
        String nameDomain = name + "@" + domain;
        if (domain.equals(Domain.getDomain())) {
            feeds.remove(name);

            Set<String> followings = following.get(name);
            if (followings != null) {
                for (String sub : followings)
                    executor.submit(() -> unsubscribeUser(nameDomain, sub, pwd));
                following.remove(name);
            }

            Map<String, List<String>> userFollowers = followers.get(name);
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
                    executor.submit(() -> requestDeleteOutsideDomain(name, domain, pwd, followerDomain, d));
                }
                followers.remove(name);
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
            RepFeedsClientFactory.get(userURI).deleteFeed(user, followerDomain, pwd);
        } catch (InterruptedException e) {
        }
    }
}
