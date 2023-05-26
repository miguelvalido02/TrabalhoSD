package sd2223.trab1.server.kafka;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.List;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.User;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.server.Domain;
import sd2223.trab1.server.kafka.sync.SyncPoint;
import sd2223.trab1.server.util.JSON;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.glassfish.hk2.api.ErrorType;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

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

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

public class KafkaRepFeeds implements KafkaRepFeedsInterface, RecordProcessor {

    private static final String FROM_BEGINNING = "earliest";
    private static final String KAFKA_BROKERS = "kafka:9092";
    private static final String TOPIC = "topic";
    private static final String USERS_SERVICE = "users";
    private static final String FEEDS_SERVICE = "feeds";

    private static final String POST_MESSAGE = "p";
    private static final String POST_OUTSIDE = "o";
    private static final String DELETE_FEED = "d";
    private static final String SUB = "s";
    private static final String UNSUB = "u";
    private static final String REMOVE = "r";

    private ClientConfig config;
    private KafkaSubscriber subscriber;
    private KafkaPublisher publisher;
    private SyncPoint<Object> sync;
    private RepFeeds impl;

    // Faz publish no kafka e fala com os clientes

    public KafkaRepFeeds(SyncPoint<Object> sync) {
        this.sync = sync;
        impl = new RepFeeds();
        sync = new SyncPoint<>();
        config = new ClientConfig();
        config.property(ClientProperties.READ_TIMEOUT, 5000);
        config.property(ClientProperties.CONNECT_TIMEOUT, 5000);
        publisher = KafkaPublisher.createPublisher(KAFKA_BROKERS);
        subscriber = KafkaSubscriber.createSubscriber(KAFKA_BROKERS, List.of(TOPIC), FROM_BEGINNING);
        subscriber.start(false, this);

    }

    @Override
    public void onReceive(ConsumerRecord<String, String> r) {
        String object = r.value();
        switch (r.key()) {
            case POST_MESSAGE:
                Message msg = JSON.decode(object, Message.class);
                sync.setResult(r.offset(), impl.postMessage(msg.getUser() + "@" + msg.getDomain(), msg));
                break;
            case POST_OUTSIDE:
                Message m = JSON.decode(object, Message.class);
                sync.setResult(r.offset(), impl.postOutside(m.getUser() + "@" + m.getDomain(), m));
                break;
            case REMOVE:
                String nameAndMid = JSON.decode(object, String.class);
                String[] res = nameAndMid.split("@");
                sync.setResult(r.offset(), impl.removeFromPersonalFeed(res[0], Long.valueOf(res[1])));
                break;
            case DELETE_FEED:
                break;
            case SUB:
                break;
            case UNSUB:
                break;
        }

    }

    @Override
    public Result<Long> postMessage(String user, String pwd, Message msg, Long version) {
        if (version != null)
            sync.waitForVersion(version, Integer.MAX_VALUE);
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];
        if (!domain.equals(Domain.getDomain()))
            return Result.error(ErrorCode.BAD_REQUEST);
        Result<User> u = verifyUser(name, domain, pwd);
        if (u.isOK()) {
            msg.setCreationTime(System.currentTimeMillis());
            String msgEncoded = JSON.encode(msg);
            long offset = publisher.publish(TOPIC, POST_MESSAGE, msgEncoded);
            return Result.ok((Long) sync.waitForResult(offset));
        }
        return Result.error(u.error());
    }

    @Override
    public Result<Void> postOutside(String user, Message msg) {
        String msgEncoded = JSON.encode(msg);
        long offset = publisher.publish(TOPIC, POST_OUTSIDE, msgEncoded);
        sync.waitForResult(offset);
        return Result.ok();
    }

    @Override
    public Result<Void> removeFromPersonalFeed(String user, long mid, String pwd, Long version) {
        if (version != null)
            sync.waitForVersion(version, Integer.MAX_VALUE);
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];
        Result<User> u = verifyUser(name, domain, pwd);
        if (u.isOK()) {
            String msgEncoded = JSON.encode(name + "@" + String.valueOf(mid));
            long offset = publisher.publish(TOPIC, REMOVE, msgEncoded);
            sync.waitForResult(offset);
            return Result.ok();
        }
        return Result.error(u.error());

    }

    @Override
    public Result<Message> getMessage(String user, long mid, Long version) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];
        try {
            if (Domain.getDomain().equals(domain)) {
                if (version != null)
                    sync.waitForVersion(version, Integer.MAX_VALUE);
                // Verificar que o user existe
                Result<User> u = findUser(domain, name);
                if (u.isOK())
                    return impl.getMessage(name, mid);
                return Result.error(u.error());
            } else {
                Discovery d = Discovery.getInstance();
                URI userURI = d.knownUrisOf(domain, FEEDS_SERVICE);
                return RepFeedsClientFactory.get(userURI).getMessage(user, mid, version);
            }
        } catch (InterruptedException e) {
        }
        return null;
    }

    @Override
    public Result<List<Message>> getMessages(String user, long time, Long version) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];
        try {
            if (Domain.getDomain().equals(domain)) {
                if (version != null)
                    sync.waitForVersion(version, Integer.MAX_VALUE);
                // Verificar que o user existe
                Result<User> u = findUser(domain, name);
                if (u.isOK())
                    return impl.getMessages(name, time);
                return Result.error(u.error());
            } else {
                Discovery d = Discovery.getInstance();
                URI userURI = d.knownUrisOf(domain, FEEDS_SERVICE);
                return RepFeedsClientFactory.get(userURI).getMessages(user, time, version);
            }
        } catch (InterruptedException e) {
        }
        return null;

    }

    @Override
    public Result<Void> subUser(String user, String userSub, String pwd, Long version) {
        if (version != null)
            sync.waitForVersion(version, Integer.MAX_VALUE);
    }

    @Override
    public Result<Void> unsubscribeUser(String user, String userSub, String pwd, Long version) {
        if (version != null)
            sync.waitForVersion(version, Integer.MAX_VALUE);
    }

    @Override
    public Result<List<String>> listSubs(String user, Long version) {
        if (version != null)
            sync.waitForVersion(version, Integer.MAX_VALUE);
    }

    @Override
    public Result<Void> deleteFeed(String user, String domain, String pwd) {
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

    private Result<User> findUser(String domain, String name) {
        Discovery d = Discovery.getInstance();
        try {
            URI userURI = d.knownUrisOf(domain, USERS_SERVICE);
            return UsersClientFactory.get(userURI).userExists(name);
        } catch (InterruptedException e) {
        }
        return null;
    }
}
