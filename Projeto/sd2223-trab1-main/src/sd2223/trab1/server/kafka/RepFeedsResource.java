package sd2223.trab1.server.kafka;

import java.util.List;
import sd2223.trab1.api.Message;
import sd2223.trab1.api.rest.FeedsService;
import sd2223.trab1.server.rest.RestResource;

public class RepFeedsResource extends RestResource implements FeedsService {
    final RepFeedsInterface impl;

    public RepFeedsResource(RepFeedsInterface impl) {
        this.impl = impl;
    }

    @Override
    public long postMessage(String user, String pwd, Message msg) {
        return super.fromJavaResult(impl.postMessage(user, pwd, msg));
    }

    @Override
    public void postOutside(String user, Message msg) {
        super.fromJavaResult(impl.postOutside(user, msg));
    }

    @Override
    public void removeFromPersonalFeed(String user, long mid, String pwd) {
        super.fromJavaResult(impl.removeFromPersonalFeed(user, mid, pwd));
    }

    @Override
    public Message getMessage(String user, long mid) {
        return super.fromJavaResult(impl.getMessage(user, mid));
    }

    @Override
    public List<Message> getMessages(String user, long time) {
        return super.fromJavaResult(impl.getMessages(user, time));
    }

    @Override
    public void subUser(String user, String userSub, String pwd) {
        super.fromJavaResult(impl.subUser(user, userSub, pwd));
    }

    @Override
    public void unsubscribeUser(String user, String userSub, String pwd) {
        super.fromJavaResult(impl.unsubscribeUser(user, userSub, pwd));
    }

    @Override
    public List<String> listSubs(String user) {
        return super.fromJavaResult(impl.listSubs(user));
    }

    @Override
    public void deleteFeed(String user, String domain, String pwd) {
        super.fromJavaResult(impl.deleteFeed(user, domain, pwd));
    }
}
