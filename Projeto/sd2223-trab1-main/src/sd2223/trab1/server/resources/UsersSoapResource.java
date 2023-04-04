package sd2223.trab1.server.resources;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import jakarta.jws.WebService;
import sd2223.trab1.api.User;
import sd2223.trab1.api.soap.UsersException;
import sd2223.trab1.api.soap.UsersSoapService;

import sd2223.trab1.api.java.Users;

@WebService(serviceName = UsersSoapService.NAME, targetNamespace = UsersSoapService.NAMESPACE, endpointInterface = UsersSoapService.INTERFACE)
public class UsersSoapResource implements Users {
    private final Map<String, User> users = new ConcurrentHashMap<String, User>();

    private static Logger Log = Logger.getLogger(UsersSoapResource.class.getName());

    @Override
    public String createUser(User user) throws UsersException {
        Log.info("createUser : " + user);

        // Check if user data is valid
        if (user.getName() == null || user.getPwd() == null || user.getDisplayName() == null
                || user.getDomain() == null) {
            Log.info("User object invalid.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        // Insert user, checking if name already exists
        if (users.putIfAbsent(user.getName(), user) != null) {
            Log.info("User already exists.");
            return Result.error(ErrorCode.CONFLICT);
        }
        return Result.ok(user.getName());
    }

    @Override
    public User getUser(String name, String pwd) throws UsersException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUser'");
    }

    @Override
    public User updateUser(String name, String pwd, User user) throws UsersException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateUser'");
    }

    @Override
    public User deleteUser(String name, String pwd) throws UsersException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteUser'");
    }

    @Override
    public List<User> searchUsers(String pattern) throws UsersException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'searchUsers'");
    }
}
/*
 * package aula5.servers.java;
 * 
 * import java.util.HashMap;
 * import java.util.List;
 * import java.util.Map;
 * import java.util.logging.Logger;
 * 
 * import aula5.api.User;
 * import aula5.api.java.Result;
 * import aula5.api.java.Result.ErrorCode;
 * import aula5.api.java.Users;
 * 
 * public class JavaUsers implements Users {
 * private final Map<String,User> users = new HashMap<>();
 * 
 * private static Logger Log = Logger.getLogger(JavaUsers.class.getName());
 * 
 * @Override
 * public Result<String> createUser(User user) {
 * Log.info("createUser : " + user);
 * 
 * // Check if user data is valid
 * if(user.getName() == null || user.getPwd() == null || user.getDisplayName()
 * == null || user.getDomain() == null) {
 * Log.info("User object invalid.");
 * return Result.error( ErrorCode.BAD_REQUEST);
 * }
 * 
 * // Insert user, checking if name already exists
 * if( users.putIfAbsent(user.getName(), user) != null ) {
 * Log.info("User already exists.");
 * return Result.error( ErrorCode.CONFLICT);
 * }
 * return Result.ok( user.getName() );
 * }
 * 
 * @Override
 * public Result<User> getUser(String name, String pwd) {
 * Log.info("getUser : user = " + name + "; pwd = " + pwd);
 * 
 * // Check if user is valid
 * if(name == null || pwd == null) {
 * Log.info("Name or Password null.");
 * return Result.error( ErrorCode.BAD_REQUEST);
 * }
 * 
 * User user = users.get(name);
 * // Check if user exists
 * if( user == null ) {
 * Log.info("User does not exist.");
 * return Result.error( ErrorCode.NOT_FOUND);
 * }
 * 
 * //Check if the password is correct
 * if( !user.getPwd().equals( pwd)) {
 * Log.info("Password is incorrect.");
 * return Result.error( ErrorCode.FORBIDDEN);
 * }
 * 
 * return Result.ok(user);
 * }
 * 
 * @Override
 * public Result<User> updateUser(String name, String pwd, User user) {
 * return Result.error( ErrorCode.NOT_IMPLEMENTED);
 * }
 * 
 * @Override
 * public Result<User> deleteUser(String name, String pwd) {
 * return Result.error( ErrorCode.NOT_IMPLEMENTED);
 * }
 * 
 * @Override
 * public Result<List<User>> searchUsers(String pattern) {
 * return Result.error( ErrorCode.NOT_IMPLEMENTED);
 * }
 * 
 * @Override
 * public Result<Void> verifyPassword(String name, String pwd) {
 * var res = getUser(name, pwd);
 * if( res.isOK() )
 * return Result.ok();
 * else
 * return Result.error( res.error() );
 * }
 * }
 * 
 * 
 * }
 */
