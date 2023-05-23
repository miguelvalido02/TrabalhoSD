package sd2223.trab1.server.soap;

import jakarta.jws.WebService;
import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.soap.FeedsException;

import java.util.List;
import java.util.logging.Logger;
import sd2223.trab1.api.soap.FeedsService;
import sd2223.trab1.server.java.JavaFeeds;

@WebService(serviceName = FeedsService.NAME, targetNamespace = FeedsService.NAMESPACE, endpointInterface = FeedsService.INTERFACE)
public class SoapFeedsWebService extends SoapWebService<FeedsException> implements FeedsService {

    static Logger Log = Logger.getLogger(SoapFeedsWebService.class.getName());

    final Feeds impl;

    public SoapFeedsWebService() {
        super((result) -> new FeedsException(result.error().toString()));
        this.impl = new JavaFeeds();
    }

    @Override
    public long postMessage(String user, String pwd, Message msg) throws FeedsException {
        return super.fromJavaResult(impl.postMessage(user, pwd, msg));
    }

    @Override
    public void removeFromPersonalFeed(String user, long mid, String pwd) throws FeedsException {
        super.fromJavaResult(impl.removeFromPersonalFeed(user, mid, pwd));
    }

    @Override
    public Message getMessage(String user, long mid) throws FeedsException {
        return super.fromJavaResult(impl.getMessage(user, mid));
    }

    @Override
    public List<Message> getMessages(String user, long time) throws FeedsException {
        return super.fromJavaResult(impl.getMessages(user, time));
    }

    @Override
    public void subUser(String user, String userSub, String pwd) throws FeedsException {
        super.fromJavaResult(impl.subUser(user, userSub, pwd));
    }

    @Override
    public void unsubscribeUser(String user, String userSub, String pwd) throws FeedsException {
        super.fromJavaResult(impl.unsubscribeUser(user, userSub, pwd));
    }

    @Override
    public List<String> listSubs(String user) throws FeedsException {
        return super.fromJavaResult(impl.listSubs(user));
    }

    @Override
    public void deleteFeed(String user, String domain, String pwd) throws FeedsException {
        super.fromJavaResult(impl.deleteFeed(user, domain, pwd));
    }

    @Override
    public void postOutside(String user, Message msg) throws FeedsException {
        super.fromJavaResult(impl.postOutside(user, msg));
    }

}
