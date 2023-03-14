package aula1;

import java.net.*;
import java.util.*;

/**
 * Basic TCP client...
 *
 */
public class TcpClient {

	private static final String QUIT = "!quit";

	public static void main(String[] args) throws Exception {

		// Use Discovery to obtain the hostname and port of the server;
		var port = -1;
		var hostname = "";
		Discovery d = Discovery.getInstance();
		URI[] uris = d.knownUrisOf("serviceName", 1);
		// serviceName /t tcp://localhost:9000
		String uriString = uris[0].toString().split("//")[1]; // localhost:9000
		port = Integer.parseInt(uriString.split(":")[1]); // 9000
		hostname = uriString.split(":")[0]; // localhost

		try (var cs = new Socket(hostname, port); var sc = new Scanner(System.in)) {
			String input;
			do {
				input = sc.nextLine();
				cs.getOutputStream().write((input + System.lineSeparator()).getBytes());
			} while (!input.equals(QUIT));

		}
	}
}
