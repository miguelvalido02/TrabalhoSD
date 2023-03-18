package aula1;

import java.net.*;
import java.util.*;

/**
 * Basic TCP client...
 *
 */
public class TcpClient {

	private static final String QUIT = "!quit";
	private static final String DELIMETER = "\t";
	private static final String DIV = ":";

	public static void main(String[] args) throws Exception {

		// Use Discovery to obtain the hostname and port of the server;
		var port = -1;
		var hostname = "";
		Discovery d = Discovery.getInstance();
		URI[] uris = d.knownUrisOf("serviceName", 1);
		// <nome-do-domínio>:<serviço><tab><uri-do-servidor>
		String[] decodedUri = decodeUri(uris[0].toString());
		String domain = decodedUri[0];
		String service = decodedUri[1];
		// port = Integer.parseInt(uriString.split(":")[1]); // 9000
		// hostname = uriString.split(":")[0]; // localhost

		try (var cs = new Socket(hostname, port); var sc = new Scanner(System.in)) {
			String input;
			do {
				input = sc.nextLine();
				cs.getOutputStream().write((input + System.lineSeparator()).getBytes());
			} while (!input.equals(QUIT));

		}
	}

	private static String[] decodeUri(String uriString) {
		String[] messageInfo = uriString.split(DIV);
		String domain = messageInfo[0];
		String[] messageInfo2 = messageInfo[1].split(DELIMETER);
		String service = messageInfo2[0];
		String uri = messageInfo2[1];
		return new String[] { domain, service, uri };
	}
}
