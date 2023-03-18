package sd2223.trab1.api.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import sd2223.trab1.api.User;
import sd2223.trab1.api.rest.UsersService;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

@Singleton
public class UsersResource implements UsersService {

	private final Map<String, User> users = new HashMap<>();

	private static Logger Log = Logger.getLogger(UsersResource.class.getName());

	public UsersResource() {
	}

	@Override
	public String createUser(User user) {
		Log.info("createUser : " + user);

		// Check if user data is valid
		if (user.getDisplayName() == null || user.getPwd() == null || user.getName() == null ||
				user.getDomain() == null) {
			Log.info("User object invalid.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		// Insert new user, checking if name already exists
		if (users.putIfAbsent(user.getName(), user) != null) {
			Log.info("User already exists.");
			throw new WebApplicationException(Status.CONFLICT);
		}
		return user.getName() + "@" + user.getDomain();
	}

	@Override
	public User getUser(String name, String pwd) {
		Log.info("getUser : user = " + name + "; pwd = " + pwd);

		// Check if user is valid
		if (name == null || pwd == null) {
			Log.info("name or pwd null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		var user = users.get(name);

		// Check if user exists
		if (user == null) {
			Log.info("User does not exist.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		// Check if the pwd is correct
		if (!user.getPwd().equals(pwd)) {
			Log.info("pwd is incorrect.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}

		return user;
	}

	@Override
	public User updateUser(String name, String oldPwd, User user) {
		Log.info("updateUser : name = " + name + "; pwd = " + oldPwd + " ; user = " + user);
		var u = getUser(name, oldPwd);
		User updatedUser = null;
		if (u != null) {
			String pwd = user.getPwd() == null ? oldPwd : user.getPwd();
			String domain = user.getDomain() == null ? u.getDomain() : user.getDomain();
			String displayName = user.getDisplayName() == null ? u.getDisplayName() : user.getDisplayName();
			updatedUser = new User(name, pwd, domain, displayName);
			users.put(name, updatedUser);
		}
		return updatedUser;
	}

	@Override
	public User deleteUser(String name, String pwd) {
		Log.info("deleteUser : user = " + name + "; pwd = " + pwd);
		getUser(name, pwd);
		return users.remove(name);
	}

	@Override
	public List<User> searchUsers(String pattern) {
		Log.info("searchUsers : pattern = " + pattern);
		List<User> l = new ArrayList<User>();
		for (User user : users.values()) {
			if (user.getName().contains(pattern))
				l.add(new User(user.getName(), "", user.getDomain(), user.getDisplayName()));
		}
		return l;
	}

}
