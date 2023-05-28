package sd2223.trab1.server.kafka;

import java.util.List;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Result;

public interface KafkaRepFeedsInterface {

    Result<Void> postOutside(String user, Message msg);

    Result<Message> getMessage(String user, long mid, Long version);

    Result<List<Message>> getMessages(String user, long time, Long version);

    Result<Void> subUser(String user, String userSub, String pwd, Long version);

    Result<Void> unsubscribeUser(String user, String userSub, String pwd, Long version);

    Result<Void> deleteFeed(String user, String domain, String pwd);
}