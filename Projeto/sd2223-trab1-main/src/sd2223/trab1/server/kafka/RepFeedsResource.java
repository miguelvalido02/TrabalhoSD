package sd2223.trab1.server.kafka;

import jakarta.ws.rs.core.Response;
import sd2223.trab1.api.Message;
import sd2223.trab1.server.rest.RestResource;

public class RepFeedsResource extends RestResource implements RepFeedsService {
    final KafkaRepFeedsInterface impl;

    public RepFeedsResource(KafkaRepFeedsInterface impl) {
        this.impl = impl;
    }

    @Override
    public Response postMessage(String user, String pwd, Message msg, Long version) {
        return null;
        // super.fromJavaResult(impl.postMessage(user, pwd, msg, version));
    }

    @Override
    public void postOutside(String user, Message msg) {
        super.fromJavaResult(impl.postOutside(user, msg));
    }

    @Override
    public Response removeFromPersonalFeed(String user, long mid, String pwd, Long version) {
        return null;
        // super.fromJavaResult(impl.removeFromPersonalFeed(user, mid, pwd, version));
    }

    @Override
    public Response getMessage(String user, long mid, Long version) {
        return null;
        // return super.fromJavaResult(impl.getMessage(user, mid, version));
    }

    @Override
    public Response getMessages(String user, long time, Long version) {
        return null;
        // super.fromJavaResult(impl.getMessages(user, time, version));
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
    public Response listSubs(String user, Long version) {
        return null;
        // super.fromJavaResult(impl.listSubs(user, version));
    }

    @Override
    public void deleteFeed(String user, String domain, String pwd) {
        super.fromJavaResult(impl.deleteFeed(user, domain, pwd));
    }
}
