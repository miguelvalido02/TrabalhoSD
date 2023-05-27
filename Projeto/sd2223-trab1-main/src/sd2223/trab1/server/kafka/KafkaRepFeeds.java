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
    private int counter;

    public KafkaRepFeeds() {
        this.counter = 0;
        this.sync = new SyncPoint<Object>();
        impl = new RepFeeds();
        config = new ClientConfig();
        config.property(ClientProperties.READ_TIMEOUT, 5000);
        config.property(ClientProperties.CONNECT_TIMEOUT, 5000);
        publisher = KafkaPublisher.createPublisher(KAFKA_BROKERS);
        subscriber = KafkaSubscriber.createSubscriber(KAFKA_BROKERS, List.of(TOPIC), FROM_BEGINNING); // ?
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
            long mid = 256 * counter + Domain.getSeq();
            counter++;
            msg.setId(mid);
            msg.setCreationTime(System.currentTimeMillis());
            String msgEncoded = JSON.encode(msg);
            long offset = publisher.publish(TOPIC, POST_MESSAGE, msgEncoded);
            Result<Long> res = (Result<Long>) sync.waitForResult(offset);
            return Response.status(200).encoding(MediaType.APPLICATION_JSON)
                    .entity(res.value())
                    .header(RepFeedsService.HEADER_VERSION, version).build();
        }
        return Response.status(statusCodeFrom(u)).build();
    }

    @Override
    public void postOutside(String user, Message msg) {
        String msgEncoded = JSON.encode(msg);
        long offset = publisher.publish(TOPIC, POST_OUTSIDE, msgEncoded);
        sync.waitForResult(offset);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Response removeFromPersonalFeed(String user, long mid, String pwd, Long version) {
        if (version != null)
            sync.waitForVersion(version, Integer.MAX_VALUE);
        String[] nameDomain = user.split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];
        Result<User> u = verifyUser(name, domain, pwd);
        if (u.isOK()) {
            String nameAndMidEncoded = JSON.encode(name + "@" + String.valueOf(mid));
            Result<Void> res = (Result<Void>) sync.waitForResult(publisher.publish(TOPIC, REMOVE, nameAndMidEncoded));
            return Response.status(statusCodeFrom(res)).header(RepFeedsService.HEADER_VERSION, version).build();
        }
        return Response.status(statusCodeFrom(u)).build();
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
                if (u.isOK()) {
                    Result<Message> res = impl.getMessage(name, mid);
                    return Response.status(200).encoding(MediaType.APPLICATION_JSON)
                            .entity(res.value())
                            .header(RepFeedsService.HEADER_VERSION, version).build();
                } else
                    return Response.status(statusCodeFrom(u)).header(RepFeedsService.HEADER_VERSION, version).build(); // corrigir
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
                if (u.isOK()) {
                    Result<List<Message>> res = impl.getMessages(name, time);
                    return Response.status(200).encoding(MediaType.APPLICATION_JSON)
                            .entity(res.value())
                            .header(RepFeedsService.HEADER_VERSION, version).build();
                } else
                    return Response.status(statusCodeFrom(u)).header(RepFeedsService.HEADER_VERSION, version).build(); // corrigir
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
    @SuppressWarnings("unchecked")
    public Response subUser(String user, String userSub, String pwd, Long version) {
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
                Result<Void> res = (Result<Void>) sync.waitForResult(publisher.publish(TOPIC, SUB, usersAndPwdEncoded));
                return Response.status(statusCodeFrom(res)).header(RepFeedsService.HEADER_VERSION, version).build();
            }
            return Response.status(statusCodeFrom(uv)).header(RepFeedsService.HEADER_VERSION, version).build(); // corrigir
        }
        return Response.status(statusCodeFrom(uf)).header(RepFeedsService.HEADER_VERSION, version).build(); // corrigir
    }

    @Override
    @SuppressWarnings("unchecked")
    public Response unsubscribeUser(String user, String userSub, String pwd, Long version) {
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
                Result<Void> res = (Result<Void>) sync
                        .waitForResult(publisher.publish(TOPIC, UNSUB, usersAndPwdEncoded));
                return Response.status(statusCodeFrom(res)).header(RepFeedsService.HEADER_VERSION, version).build();
            }
            return Response.status(statusCodeFrom(uf)).header(RepFeedsService.HEADER_VERSION, version).build(); // corrigir
        }
        return Response.status(statusCodeFrom(uv)).header(RepFeedsService.HEADER_VERSION, version).build(); // corrigir
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
        if (u.isOK()) {
            Result<List<String>> res = impl.listSubs(name);
            return Response.status(200).encoding(MediaType.APPLICATION_JSON)
                    .entity(res.value())
                    .header(RepFeedsService.HEADER_VERSION, version).build();
        }
        return Response.status(statusCodeFrom(u)).header(RepFeedsService.HEADER_VERSION, version).build(); // corrigir
    }

    @Override
    public void deleteFeed(String name, String domain, String pwd) {
        String nameDomainPwdEncoded = JSON.encode(name + "@" + domain + "@" + pwd);
        long offset = publisher.publish(TOPIC, DELETE_FEED, nameDomainPwdEncoded);
        sync.waitForResult(offset);
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
