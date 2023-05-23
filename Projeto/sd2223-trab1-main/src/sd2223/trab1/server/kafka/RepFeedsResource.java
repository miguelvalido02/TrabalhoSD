package sd2223.trab1.server.kafka;

import java.util.List;
import sd2223.trab1.api.Message;
import sd2223.trab1.server.rest.RestResource;

public class RepFeedsResource extends RestResource implements RepFeedsService {
    final RepFeedsInterface impl;

    public RepFeedsResource(RepFeedsInterface impl) {
        this.impl = impl;
    }

    @Override
    public long postMessage(String user, String pwd, Message msg, Long version) {
        return super.fromJavaResult(impl.postMessage(user, pwd, msg, version));
    }

    @Override
    public void postOutside(String user, Message msg) {
        super.fromJavaResult(impl.postOutside(user, msg));
    }

    @Override
    public void removeFromPersonalFeed(String user, long mid, String pwd, Long version) {
        super.fromJavaResult(impl.removeFromPersonalFeed(user, mid, pwd, version));
    }

    @Override
    public Message getMessage(String user, long mid, Long version) {
        return super.fromJavaResult(impl.getMessage(user, mid, version));
    }

    @Override
    public List<Message> getMessages(String user, long time, Long version) {
        return super.fromJavaResult(impl.getMessages(user, time, version));
    }

    @Override
    public void subUser(String user, String userSub, String pwd, Long version) {
        super.fromJavaResult(impl.subUser(user, userSub, pwd, version));
    }

    @Override
    public void unsubscribeUser(String user, String userSub, String pwd, Long version) {
        super.fromJavaResult(impl.unsubscribeUser(user, userSub, pwd, version));
    }

    @Override
    public List<String> listSubs(String user, Long version) {
        return super.fromJavaResult(impl.listSubs(user, version));
    }

    @Override
    public void deleteFeed(String user, String domain, String pwd) {
        super.fromJavaResult(impl.deleteFeed(user, domain, pwd));
    }
}
