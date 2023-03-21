package aula3.clients;

import java.net.URI;
import java.util.List;

import aula3.api.User;
import aula3.api.rest.UsersService;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class RestUsersClient extends RestClient implements UsersService {

	final WebTarget target;
	
	RestUsersClient( URI serverURI ) {
		super( serverURI );
		target = client.target( serverURI ).path( UsersService.PATH );
	}
	
	private String clt_createUser( User user) {
		
		Response r = target.request()
				.accept(MediaType.APPLICATION_JSON)
				.post(Entity.entity(user, MediaType.APPLICATION_JSON));

		if( r.getStatus() == Status.OK.getStatusCode() && r.hasEntity() )
			return r.readEntity(String.class);
		else 
			System.out.println("Error, HTTP error status: " + r.getStatus() );
		
		return null;
	}
	
	private User clt_getUser(String name, String pwd) {

		Response r = target.path( name )
				.queryParam(UsersService.PWD, pwd).request()
				.accept(MediaType.APPLICATION_JSON)
				.get();

		if( r.getStatus() == Status.OK.getStatusCode() && r.hasEntity() )
			return r.readEntity(User.class);
		else
			System.out.println("Error, HTTP error status: " + r.getStatus() );
		
		return null;
	}
	
	@Override
	public String createUser(User user) {
		return super.reTry( () -> clt_createUser(user) );
	}

	@Override
	public User getUser(String name, String pwd) {
		return super.reTry( () -> clt_getUser(name, pwd) );
	}

	@Override
	public User updateUser(String name, String pwd, User user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public User deleteUser(String name, String pwd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<User> searchUsers(String pattern) {
		// TODO Auto-generated method stub
		return null;
	}
}
