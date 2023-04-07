package sd2223.trab1.clients.rest;

import java.net.URI;
import java.util.List;

import sd2223.trab1.api.User;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.java.Users;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import sd2223.trab1.api.rest.UsersService;
import jakarta.ws.rs.core.Response.Status;

public class RestUsersClient extends RestClient implements Users {

    final WebTarget target;

    public RestUsersClient(URI serverURI) {
        super(serverURI);
        target = client.target(serverURI).path(UsersService.PATH);
    }

    private Result<String> clt_createUser(User user) {

        Response r = target.request()
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.json(user));

        if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity()) {
            System.out.println("User criado " + r.getStatus());
            return super.toJavaResult(r, String.class);
        } else
            System.out.println("Error, HTTP error status: " + r.getStatus());

        return null;
    }

    private Result<User> clt_getUser(String name, String pwd) {
        User u = null;
        Response r = target.path(name)
                .queryParam(UsersService.PWD, pwd).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity()) {
            System.out.println("Success:");
            u = r.readEntity(User.class); // return aqui?
            System.out.println("User : " + u);
        } else
            System.out.println("Error, HTTP error status: " + r.getStatus());
        return super.toJavaResult(r, User.class);
    }

    private Result<User> clt_updateUser(String name, String pwd, User user) {
        User u = null;
        Response r = target.path(name)
                .queryParam(UsersService.PWD, pwd).request()
                .accept(MediaType.APPLICATION_JSON)
                .put(Entity.entity(user, MediaType.APPLICATION_JSON));

        if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity()) {
            System.out.println("Success:");
            u = r.readEntity(User.class);
            System.out.println("User updated: " + u);
        } else
            System.out.println("Error, HTTP error status: " + r.getStatus());

        return super.toJavaResult(r, User.class);

    }

    private Result<User> clt_deleteUser(String name, String pwd) {
        User u = null;
        Response r = target.path(name)
                .queryParam(UsersService.PWD, pwd).request()
                .accept(MediaType.APPLICATION_JSON).delete();

        if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity()) {
            System.out.println("Success:");
            u = r.readEntity(User.class);
            System.out.println("User Deleted: " + u);
        } else
            System.out.println("Error, HTTP error status: " + r.getStatus());

        return super.toJavaResult(r, User.class);

    }

    private Result<List<User>> clt_searchUsers(String pattern) {
        List<User> users = null;
        Response r = target.path("/").queryParam(UsersService.QUERY, pattern).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity()) {
            users = r.readEntity(new GenericType<List<User>>() {
            });
            System.out.println("Success: (" + users.size() + " users)");
            users.stream().forEach(u -> System.out.println(u));
        } else
            System.out.println("Error, HTTP error status: " + r.getStatus());

        return Result.ok(users); // como fazer com List<User>?
    }

    @Override
    public Result<String> createUser(User user) {
        return super.reTry(() -> clt_createUser(user));
    }

    @Override
    public Result<User> getUser(String name, String pwd) {
        return super.reTry(() -> clt_getUser(name, pwd));
    }

    @Override
    public Result<User> updateUser(String name, String pwd, User user) {
        return super.reTry(() -> clt_updateUser(name, pwd, user));
    }

    @Override
    public Result<User> deleteUser(String name, String pwd) {
        return super.reTry(() -> clt_deleteUser(name, pwd));
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        return super.reTry(() -> clt_searchUsers(pattern));
    }

    @Override
    public Result<User> userExists(String name) {
        return Result.ok();
    }
}
