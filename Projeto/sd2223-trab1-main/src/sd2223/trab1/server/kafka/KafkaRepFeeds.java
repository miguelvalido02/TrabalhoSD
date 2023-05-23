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

import sd2223.trab1.api.User;
import sd2223.trab1.api.Message;
import sd2223.trab1.server.Domain;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.server.Discovery;
import sd2223.trab1.api.java.Result.ErrorCode;
import sd2223.trab1.clients.UsersClientFactory;
import sd2223.trab1.server.kafka.sync.SyncPoint;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

public class KafkaRepFeeds implements RepFeedsInterface, RecordProcessor {

    private static final String USERS_SERVICE = "users";
    private static final String FEEDS_SERVICE = "feeds";

    private int counter;
    private ClientConfig config;
    private SyncPoint<Object> sync;
    private ExecutorService executor;
    private Map<String, Set<String>> following; // <username,<user@domain>>
    private Map<String, Map<Long, Message>> feeds;// <username,<mid,message>>
    private Map<String, Map<String, List<String>>> followers; // <username,<domain,<user@domain>>>

    public KafkaRepFeeds(SyncPoint<Object> sync) {
        this.sync = sync;
        config = new ClientConfig();
        config.property(ClientProperties.READ_TIMEOUT, 5000);
        config.property(ClientProperties.CONNECT_TIMEOUT, 5000);
        this.following = new ConcurrentHashMap<String, Set<String>>();
        this.feeds = new ConcurrentHashMap<String, Map<Long, Message>>();
        this.followers = new ConcurrentHashMap<String, Map<String, List<String>>>();
        executor = Executors.newFixedThreadPool(50);
        this.counter = 0;

    }

    @Override
    public Result<Long> postMessage(String user, String pwd, Message msg, Long version) {
        return null;
    }

    @Override
    public Result<Void> postOutside(String user, Message msg) {
        return null;
    }

    @Override
    public Result<Void> removeFromPersonalFeed(String user, long mid, String pwd, Long version) {
        return null;
    }

    @Override
    public Result<Message> getMessage(String user, long mid, Long version) {
        return null;
    }

    @Override
    public Result<List<Message>> getMessages(String user, long time, Long version) {
        return null;
    }

    @Override
    public Result<Void> subUser(String user, String userSub, String pwd, Long version) {
        return null;
    }

    @Override
    public Result<Void> unsubscribeUser(String user, String userSub, String pwd, Long version) {
        return null;
    }

    @Override
    public Result<List<String>> listSubs(String user, Long version) {
        return null;
    }

    @Override
    public Result<Void> deleteFeed(String user, String domain, String pwd) {
        return null;
    }

    private void requestDeleteOutsideDomain(String user, String domain, String pwd, String followerDomain,
            Discovery d) {
        try {
            URI userURI = d.knownUrisOf(followerDomain, FEEDS_SERVICE);
            RepFeedsClientFactory.get(userURI).deleteFeed(user, followerDomain, pwd);
        } catch (InterruptedException e) {
        }
    }

    private Result<User> findUser(String domain, String name) {
        Discovery d = Discovery.getInstance();
        try {
            URI userURI = d.knownUrisOf(domain, USERS_SERVICE);
            return UsersClientFactory.get(userURI).userExists(name);
        } catch (InterruptedException e) {
        }
        return null;
    }

    private Result<User> verifyUser(String name, String domain, String pwd) {
        try {
            Discovery d = Discovery.getInstance();
            URI userURI = d.knownUrisOf(domain, USERS_SERVICE);
            return UsersClientFactory.get(userURI).getUser(name, pwd);
        } catch (InterruptedException e) {
        }
        return null;
    }

    @Override
    public void onReceive(ConsumerRecord<String, String> r) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onReceive'");
    }
}
