package sd2223.trab1.server.rest;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.List;

import java.util.logging.Logger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response.Status;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sd2223.trab1.api.User;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.Message;
import sd2223.trab1.server.Domain;
import sd2223.trab1.server.java.JavaFeeds;
import sd2223.trab1.server.Discovery;
import sd2223.trab1.api.rest.UsersService;
import sd2223.trab1.api.rest.FeedsService;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.WebApplicationException;

@Singleton
public class RestFeedsResource extends RestResource implements FeedsService {
    final Feeds impl;

    public RestFeedsResource() {
        this.impl = new JavaFeeds();
    }

    // APAGAR PRIVADOS

    @Override
    public long postMessage(String user, String pwd, Message msg) {
        return super.fromJavaResult(impl.postMessage(user, pwd, msg));
    }

    private void postInDomain(String user, Message msg, Map<String, List<String>> userFollowers) {
    }

    private void sendOutsideDomain(User u, Message msg, Map<String, List<String>> userFollowers) {
    }

    private void requestPostOutsideDomain(String domain, Discovery d, String user, Message msg) {
    }

    @Override
    public Void postOutside(String user, Message msg) {
        return super.fromJavaResult(impl.postOutside(user, msg)); // MERDA DO VOID EM UPPER CASE
    }

    @Override
    public Void removeFromPersonalFeed(String user, long mid, String pwd) { // SO PODEM ESTAR A GOZAR...
        return super.fromJavaResult(impl.removeFromPersonalFeed(user, mid, pwd));

    }

    @Override
    public Message getMessage(String user, long mid) {
        return super.fromJavaResult(impl.getMessage(user, mid));
    }

    private Message getMessage(WebTarget target, String user, Long mid) {
    }

    @Override
    public List<Message> getMessages(String user, long time) {
        return super.fromJavaResult(impl.getMessages(user, time));
    }

    private List<Message> getMessages(WebTarget target, String user, long time) {
    }

    @Override
    public Void subUser(String user, String userSub, String pwd) { // SD É MERDA
        return super.fromJavaResult(impl.subUser(user, userSub, pwd));
    }

    private void subOutside(String subDomain, String user, String userSub, String pwd) {
    }

    @Override
    public Void unsubscribeUser(String user, String userSub, String pwd) { // SEM COMENTARIOS
        return super.fromJavaResult(impl.unsubscribeUser(user, userSub, pwd));
    }

    private void unsubOutside(String subDomain, String user, String userSub, String pwd) {
    }

    @Override
    public List<String> listSubs(String user) {
        return super.fromJavaResult(impl.listSubs(user));
    }

    @Override
    public Void deleteFeed(String user, String domain, String pwd) { // O PROF ESTA MM A GOZAR
        return super.fromJavaResult(impl.deleteFeed(user, domain, pwd));
    }

    private void requestDeleteOutsideDomain(String user, String domain, String pwd, String followerDomain,
            Discovery d) {
    }

    private User findUser(String domain, String name) {
        return null;
    }

    private User findUser(String name, String domain, WebTarget target, Discovery d) {
        return null;
    }

    private User verifyUser(String name, String domain, String pwd) {
        return null;
    }

    private User getUser(String name, String pwd, WebTarget target) {
        return null;
    }
}