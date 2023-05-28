package sd2223.trab1.server.kafka;

import java.net.URI;
import java.util.List;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.rest.FeedsService;
import sd2223.trab1.clients.rest.RestClient;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.client.WebTarget;

public class RepFeedsClient extends RestClient implements KafkaRepFeedsInterface {

    final WebTarget target;

    public RepFeedsClient(URI serverURI) {
        super(serverURI);
        target = client.target(serverURI).path(FeedsService.PATH);
    }

    private Result<Message> clt_getMessage(String user, long mid, Long version) {
        Response r = target.path(user).path(Long.toString(mid)).request()
                .header(RepFeedsService.HEADER_VERSION, version)
                .accept(MediaType.APPLICATION_JSON)
                .get();
        return super.toJavaResult(r, Message.class);
    }

    private Result<List<Message>> clt_getMessages(String user, long time, Long version) {
        Response r = target.path(user)
                .queryParam(FeedsService.TIME, time).request().header(RepFeedsService.HEADER_VERSION, version)
                .accept(MediaType.APPLICATION_JSON).get();
        return super.toJavaResult(r, new GenericType<List<Message>>() {
        });

    }

    private Result<Void> clt_subUser(String user, String userSub, String pwd, Long version) {
        Response r = target.path("sub").path(user).path(userSub)
                .queryParam(FeedsService.PWD, pwd).request().header(RepFeedsService.HEADER_VERSION, version)
                .post(Entity.json(null));

        return super.toJavaResult(r, Void.class);
    }

    private Result<Void> clt_unsubscribeUser(String user, String userSub, String pwd, Long version) {
        Response r = target.path("sub").path(user).path(userSub)
                .queryParam(FeedsService.PWD, pwd).request().header(RepFeedsService.HEADER_VERSION, version).delete();

        return super.toJavaResult(r, Void.class);
    }

    private Result<Void> clt_postOutside(String user, Message msg) {
        Response r = target.path("post")
                .path(user).request().post(Entity.json(msg));

        return super.toJavaResult(r, Void.class);
    }

    private Result<Void> clt_deleteFeed(String user, String domain, String pwd) {
        Response r = target.path("delete")
                .path(user).path(domain).queryParam(FeedsService.PWD, pwd)
                .request().delete();

        return super.toJavaResult(r, Void.class);
    }

    @Override
    public Result<Message> getMessage(String user, long mid, Long version) {
        return super.reTry(() -> clt_getMessage(user, mid, version));
    }

    @Override
    public Result<List<Message>> getMessages(String user, long time, Long version) {
        return super.reTry(() -> clt_getMessages(user, time, version));
    }

    @Override
    public Result<Void> subUser(String user, String userSub, String pwd, Long version) {
        return super.reTry(() -> clt_subUser(user, userSub, pwd, version));
    }

    @Override
    public Result<Void> unsubscribeUser(String user, String userSub, String pwd, Long version) {
        return super.reTry(() -> clt_unsubscribeUser(user, userSub, pwd, version));
    }

    @Override
    public Result<Void> postOutside(String user, Message msg) {
        return super.reTry(() -> clt_postOutside(user, msg));
    }

    @Override
    public Result<Void> deleteFeed(String user, String domain, String pwd) {
        return super.reTry(() -> clt_deleteFeed(user, domain, pwd));
    }
}
