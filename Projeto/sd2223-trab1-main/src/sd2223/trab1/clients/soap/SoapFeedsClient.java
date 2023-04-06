package sd2223.trab1.clients.soap;

import java.net.URI;
import java.util.List;

import javax.xml.namespace.QName;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.soap.FeedsService;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;

public class SoapFeedsClient extends SoapClient implements Feeds {

    public SoapFeedsClient(URI serverURI) {
        super(serverURI);
    }

    private FeedsService stub;

    synchronized private FeedsService stub() {
        if (stub == null) {
            QName QNAME = new QName(FeedsService.NAMESPACE, FeedsService.NAME);
            Service service = Service.create(super.toURL(super.uri + WSDL), QNAME);
            this.stub = service.getPort(sd2223.trab1.api.soap.FeedsService.class);
            super.setTimeouts((BindingProvider) stub);
        }
        return stub;
    }
    /*
     * @Override
     * public Result<String> createUser(User user) {
     * return super.reTry(() -> super.toJavaResult(() -> stub().createUser(user)));
     * }
     * 
     * @Override
     * public Result<User> getUser(String name, String pwd) {
     * return super.reTry(() -> super.toJavaResult(() -> stub().getUser(name,
     * pwd)));
     * }
     */

    @Override
    public Result<Long> postMessage(String user, String pwd, Message msg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'postMessage'");
    }

    @Override
    public Result<Void> postOutside(String user, Message msg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'postOutside'");
    }

    @Override
    public Result<Void> removeFromPersonalFeed(String user, long mid, String pwd) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeFromPersonalFeed'");
    }

    @Override
    public Result<Message> getMessage(String user, long mid) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getMessage'");
    }

    @Override
    public Result<List<Message>> getMessages(String user, long time) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getMessages'");
    }

    @Override
    public Result<Void> subUser(String user, String userSub, String pwd) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'subUser'");
    }

    @Override
    public Result<Void> unsubscribeUser(String user, String userSub, String pwd) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'unsubscribeUser'");
    }

    @Override
    public Result<List<String>> listSubs(String user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listSubs'");
    }

    @Override
    public Result<Void> deleteFeed(String user, String domain, String pwd) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteFeed'");
    }

}
