package sd2223.trab1.clients;

import java.net.URI;
import java.util.List;

import sd2223.trab1.api.User;
import sd2223.trab1.api.rest.UsersService;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class RestUsersClient extends RestClient implements UsersService {

    final WebTarget target;

    RestUsersClient(URI serverURI) {
        super(serverURI);
        target = client.target(serverURI).path(UsersService.PATH);
    }

    private String clt_createUser(User user) { // Invocação jersey sem comportamento de repetição
        // teste de falhas -> metodo auxiliar, privado com outro nome
        // FAZ o PEDIDO do CREATE USER

        Response r = target.request()
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(user, MediaType.APPLICATION_JSON));

        if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity())
            return r.readEntity(String.class);
        else
            System.out.println("Error, HTTP error status: " + r.getStatus());

        return null;
    }

    private User clt_getUser(String name, String pwd) {
        User u = null;
        Response r = target.path(name)
                .queryParam(UsersService.PWD, pwd).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity()) {
            System.out.println("Success:");
            u = r.readEntity(User.class);
            System.out.println("User : " + u);
        } else
            System.out.println("Error, HTTP error status: " + r.getStatus());

        return u;
    }

    private User clt_updateUser(String name, String pwd, User user) {
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

        return u;

    }

    private User clt_deleteUser(String name, String pwd) {
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

        return u;

    }

    private List<User> clt_searchUsers(String pattern) {
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

        return users;
    }

    @Override
    public String createUser(User user) {
        return super.reTry(() -> clt_createUser(user));
    }

    @Override
    public User getUser(String name, String pwd) {
        return super.reTry(() -> clt_getUser(name, pwd));
    }

    @Override
    public User updateUser(String name, String pwd, User user) {
        return super.reTry(() -> clt_updateUser(name, pwd, user));
    }

    @Override
    public User deleteUser(String name, String pwd) {
        return super.reTry(() -> clt_deleteUser(name, pwd));
    }

    @Override
    public List<User> searchUsers(String pattern) {
        return super.reTry(() -> clt_searchUsers(pattern));
    }
}
