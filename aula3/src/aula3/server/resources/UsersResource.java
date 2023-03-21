package aula3.server.resources;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import aula3.api.User;
import aula3.api.rest.UsersService;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

@Singleton
public class UsersResource implements UsersService {

	private final Map<String,User> users = new HashMap<>();

	private static Logger Log = Logger.getLogger(UsersResource.class.getName());
	
	public UsersResource() {
	}

	@Override
	public String createUser(User user) {
		Log.info("createUser : " + user);
		
		// Check if user data is valid
		if(user.getName() == null || user.getPwd() == null || user.getDisplayName() == null || user.getDomain() == null) {
			Log.info("User object invalid.");
			throw new WebApplicationException( Status.BAD_REQUEST );
		}
		
		// Insert user, checking if name already exists
		if( users.putIfAbsent(user.getName(), user) != null ) {
			Log.info("User already exists.");
			throw new WebApplicationException( Status.CONFLICT );
		}

		return user.getName();
	}
	
	@Override
	public User getUser(String name, String pwd) {
			Log.info("getUser : user = " + name + "; pwd = " + pwd);
			
			// Check if user is valid
			if(name == null || pwd == null) {
				Log.info("Name or Password null.");
				throw new WebApplicationException( Status.BAD_REQUEST );
			}
			
			User user = users.get(name);			
			// Check if user exists 
			if( user == null ) {
				Log.info("User does not exist.");
				throw new WebApplicationException( Status.NOT_FOUND );
			}
			
			//Check if the password is correct
			if( !user.getPwd().equals( pwd)) {
				Log.info("Password is incorrect.");
				throw new WebApplicationException( Status.FORBIDDEN );
			}
			
			return user;
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
