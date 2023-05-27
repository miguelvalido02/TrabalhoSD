package sd2223.trab1.server.kafka;

import jakarta.ws.rs.core.Response;
import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Result;

public interface KafkaRepFeedsInterface {

    Response postMessage(String user, String pwd, Message msg, Long version);

    Result<Void> postOutside(String user, Message msg);

    Result<Void> removeFromPersonalFeed(String user, long mid, String pwd, Long version);

    Response getMessage(String user, long mid, Long version);

    Response getMessages(String user, long time, Long version);

    Result<Void> subUser(String user, String userSub, String pwd, Long version);

    Result<Void> unsubscribeUser(String user, String userSub, String pwd, Long version);

    Response listSubs(String user, Long version);

    Result<Void> deleteFeed(String user, String domain, String pwd);
}