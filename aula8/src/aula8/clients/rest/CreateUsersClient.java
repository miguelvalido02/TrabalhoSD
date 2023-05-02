package aula8.clients.rest;

import java.net.URI;

import javax.net.ssl.HttpsURLConnection;

import aula8.api.User;
import aula8.tls.InsecureHostnameVerifier;

public class CreateUsersClient {

	public static void main(String[] args) {
		if( args.length != 5) {
			System.err.println("usage: serverUri name pwd domain displayName");
			System.exit(0);
		}
		
		var serverURI = args[0];
		var name = args[1];
		var pwd = args[2];
		var domain = args[3];
		var displayName = args[4];
		
		HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());
		
		var users = new RestUsersClient( URI.create( serverURI ));
		
		var res = users.createUser( new User( name, pwd, domain, displayName) );
		System.out.println( res );
	}

}
