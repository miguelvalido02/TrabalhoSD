package sd2223.trab1.server.rest;

import java.util.List;
import sd2223.trab1.api.User;
import jakarta.inject.Singleton;
import sd2223.trab1.api.java.Users;
import sd2223.trab1.server.java.JavaUsers;
import sd2223.trab1.api.rest.UsersService;

@Singleton
public class RestUsersResource extends RestResource implements UsersService {

	final Users impl;

	public RestUsersResource() {
		this.impl = new JavaUsers();
	}

	@Override
	public String createUser(User user) {
		return super.fromJavaResult(impl.createUser(user));
	}

	@Override
	public User getUser(String name, String pwd) {
		return super.fromJavaResult(impl.getUser(name, pwd));
	}

	@Override
	public User userExists(String name) {
		return super.fromJavaResult(impl.userExists(name));
	}

	@Override
	public User updateUser(String name, String oldPwd, User user) {
		return super.fromJavaResult(impl.updateUser(name, oldPwd, user));
	}

	@Override
	public User deleteUser(String name, String pwd) {
		return super.fromJavaResult(impl.deleteUser(name, pwd));
	}

	private void deleteFeed(String name, String pwd) {
		// manhoso
	}

	@Override
	public List<User> searchUsers(String pattern) {
		return super.fromJavaResult(impl.searchUsers(pattern));
	}

	private User checkUser(String name, String pwd) {
		return null;
	}
}
