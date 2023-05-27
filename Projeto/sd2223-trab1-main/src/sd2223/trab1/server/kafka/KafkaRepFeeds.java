package sd2223.trab1.server.kafka;

import java.net.URI;
import java.util.List;

import sd2223.trab1.api.User;
import sd2223.trab1.api.Message;
import sd2223.trab1.server.Domain;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.server.util.JSON;
import sd2223.trab1.server.kafka.sync.SyncPoint;
import sd2223.trab1.server.rest.RestResource;
import sd2223.trab1.server.Discovery;
import sd2223.trab1.api.java.Result.ErrorCode;
import sd2223.trab1.clients.UsersClientFactory;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/*This class receives requests from clients, publishes on Kafka and executes the operation
 * via RepFeeds class
*/
@Singleton
public class KafkaRepFeeds extends RestResource implements RepFeedsService, RecordProcessor {

    private static final String TOPIC = "topic";
    private static final String USERS_SERVICE = "users";
    private static final String FEEDS_SERVICE = "feeds";
    private static final String FROM_BEGINNING = "earliest";
    private static final String KAFKA_BROKERS = "kafka:9092";

    private static final String SUB = "s";
    private static final String UNSUB = "u";
    private static final String REMOVE = "r";
    private static final String DELETE_FEED = "d";
    private static final String POST_MESSAGE = "p";
    private static final String POST_OUTSIDE = "o";

    private RepFeeds impl;
    private ClientConfig config;
    private SyncPoint<Object> sync;
    private KafkaPublisher publisher;
    private KafkaSubscriber subscriber;

    public KafkaRepFeeds() {
        this.sync = new SyncPoint<Object>();
        impl = new RepFeeds();
        config = new ClientConfig();
        config.property(ClientProperties.READ_TIMEOUT, 5000);
        config.property(ClientProperties.CONNECT_TIMEOUT, 5000);
        publisher = KafkaPublisher.createPublisher(KAFKA_BROKERS);
        subscriber = KafkaSubscriber.createSubscriber(KAFKA_BROKERS, List.of(TOPIC), FROM_BEGINNING);
        subscriber.start(false, this);
    }

    @Override
    public void onReceive(ConsumerRecord<String, String> r) {
        System.out.println("pedro");
        String object = r.value();
        switch (r.key()) {
            case POST_MESSAGE:
                Message msg = JSON.decode(object, Message.class);
                System.out.println("offset " + r.offset());
                System.out.println(msg);
                Result<Long> a = impl.postMessage(msg.getUser() + "@" + msg.getDomain(), msg);
                System.out.println(a);
                sync.setResult(r.offset(), a);
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
                String[] userAndPwd = this.getUserAndPwd(object);
                sync.setResult(r.offset(), impl.deleteFeed(userAndPwd[0], userAndPwd[1], userAndPwd[2]));
                break;
            case SUB:
                String[] usersAndPwd = this.getUsersAndPwd(object);
                sync.setResult(r.offset(), impl.subUser(usersAndPwd[0], usersAndPwd[1], usersAndPwd[2]));
                break;
            case UNSUB:
                String[] usersAndpwd = this.getUsersAndPwd(object);
                sync.setResult(r.offset(), impl.unsubscribeUser(usersAndpwd[0], usersAndpwd[1], usersAndpwd[2]));
                break;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Response postMessage(String user, String pwd, Message msg, Long version) {
        if (version != null)
            sync.waitForVersion(version, Integer.MAX_VALUE);
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];
        if (!domain.equals(Domain.getDomain()))
            return Response.status(statusCodeFrom(Result.error(ErrorCode.BAD_REQUEST))).build();
        Result<User> u = verifyUser(name, domain, pwd);
        if (u.isOK()) {
            msg.setCreationTime(System.currentTimeMillis());
            String msgEncoded = JSON.encode(msg);
            long offset = publisher.publish(TOPIC, POST_MESSAGE, msgEncoded);
            System.out.println("offset dentro do postMessage: " + offset);
            System.out.println("msg: " + msgEncoded);
            // return (Result<Long>) sync.waitForResult(offset);
            Result<Long> res = (Result<Long>) sync.waitForResult(offset);
            System.out.println("pedro2");
            return Response.status(200).encoding(MediaType.APPLICATION_JSON)
                    .entity(res.value())
                    .header(RepFeedsService.HEADER_VERSION, version).build();

        }
        System.out.println(u);
        return Response.status(statusCodeFrom(u)).build();
    }

    @Override
    public void postOutside(String user, Message msg) {
        String msgEncoded = JSON.encode(msg);
        long offset = publisher.publish(TOPIC, POST_OUTSIDE, msgEncoded);
        sync.waitForResult(offset);
    }

    @Override
    public void removeFromPersonalFeed(String user, long mid, String pwd, Long version) {
        if (version != null)
            sync.waitForVersion(version, Integer.MAX_VALUE);
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];
        Result<User> u = verifyUser(name, domain, pwd);
        if (u.isOK()) {
            String nameAndMidEncoded = JSON.encode(name + "@" + String.valueOf(mid));
            publisher.publish(TOPIC, REMOVE, nameAndMidEncoded);
            // return (Result<Void>) sync.waitForResult(offset);
        }
        // return Result.error(u.error());
    }

    @Override
    public Response getMessage(String user, long mid, Long version) {
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
                    return null;
                // return impl.getMessage(name, mid);
                // return Result.error(u.error());
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
    public Response getMessages(String user, long time, Long version) {
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
                    return null;
                // return impl.getMessages(name, time);
                // return Result.error(u.error());
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
    public void subUser(String user, String userSub, String pwd, Long version) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];

        String[] nameDomainSub = userSub.split("@");
        String nameSub = nameDomainSub[0];
        String domainSub = nameDomainSub[1];

        if (version != null && Domain.getDomain().equals(domain))
            sync.waitForVersion(version, Integer.MAX_VALUE);
        // Garantir que o userSub existe (pedido ao servico users)
        Result<User> uf = findUser(domainSub, nameSub);
        if (uf.isOK()) {
            // Garantir que o user existe
            Result<User> uv = verifyUser(name, domain, pwd);
            if (uv.isOK()) {
                String usersAndPwdEncoded = JSON.encode(user + "@" + userSub + "@" + pwd);
                long offset = publisher.publish(TOPIC, SUB, usersAndPwdEncoded);
                sync.waitForResult(offset);
                // return Result.ok();
            }
            // return Result.error(uv.error());
        }
        // return Result.error(uf.error());
    }

    @Override
    public void unsubscribeUser(String user, String userSub, String pwd, Long version) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];

        String[] nameDomainSub = userSub.split("@");
        String nameSub = nameDomainSub[0];
        String domainSub = nameDomainSub[1];

        if (version != null && Domain.getDomain().equals(domain))
            sync.waitForVersion(version, Integer.MAX_VALUE);
        // Garantir que o user existe
        Result<User> uv = verifyUser(name, domain, pwd);
        if (uv.isOK()) {
            // Garantir que o userSub existe (pedido ao servico users)
            Result<User> uf = findUser(domainSub, nameSub);
            if (uf.isOK()) {
                String usersAndPwdEncoded = JSON.encode(user + "@" + userSub + "@" + pwd);
                sync.waitForResult(publisher.publish(TOPIC, UNSUB, usersAndPwdEncoded));
                // return (Result<Void>) sync.waitForResult(offset);
            }
            // return Result.error(uf.error());
        }
        // return Result.error(uv.error());
    }

    @Override
    public Response listSubs(String user, Long version) {
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];
        if (version != null && Domain.getDomain().equals(domain))
            sync.waitForVersion(version, Integer.MAX_VALUE);
        // Garantir que o user existe (pedido ao servico users)
        Result<User> u = findUser(domain, name);
        return null;
        // if (u.isOK())
        // return impl.listSubs(name);
        // return Result.error(u.error());
    }

    @Override
    public void deleteFeed(String name, String domain, String pwd) {
        String nameDomainPwdEncoded = JSON.encode(name + "@" + domain + "@" + pwd);
        long offset = publisher.publish(TOPIC, DELETE_FEED, nameDomainPwdEncoded);
        sync.waitForResult(offset);
        // return Result.ok();
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

    private String[] getUsersAndPwd(String object) {
        String usersAndPwd = JSON.decode(object, String.class);
        String[] result = usersAndPwd.split("@", 5);
        String user = result[0] + "@" + result[1];
        String userSub = result[2] + "@" + result[3];
        String pwd = result[4];
        return new String[] { user, userSub, pwd };
    }

    private String[] getUserAndPwd(String object) {
        String usersAndPwd = JSON.decode(object, String.class);
        String[] result = usersAndPwd.split("@", 3);
        String user = result[0];
        String domain = result[1];
        String pwd = result[2];
        return new String[] { user, domain, pwd };
    }
}
