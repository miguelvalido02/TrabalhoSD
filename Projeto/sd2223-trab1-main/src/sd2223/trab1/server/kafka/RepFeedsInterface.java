package sd2223.trab1.server.kafka;

import java.util.List;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Result;

public interface RepFeedsInterface {

    Result<Long> postMessage(String user, String pwd, Message msg);

    Result<Void> postOutside(String user, Message msg);

    Result<Void> removeFromPersonalFeed(String user, long mid, String pwd);

    Result<Message> getMessage(String user, long mid);

    Result<List<Message>> getMessages(String user, long time);

    Result<Void> subUser(String user, String userSub, String pwd);

    Result<Void> unsubscribeUser(String user, String userSub, String pwd);

    Result<List<String>> listSubs(String user);

    Result<Void> deleteFeed(String user, String domain, String pwd);
}