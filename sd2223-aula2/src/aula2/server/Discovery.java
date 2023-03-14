package aula2.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.logging.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

/**
 * <p>
 * A class interface to perform service discovery based on periodic
 * announcements over multicast communication.
 * </p>
 * 
 */

public interface Discovery {

	/**
	 * Used to announce the URI of the given service name.
	 * 
	 * @param serviceName - the name of the service
	 * @param serviceURI  - the uri of the service
	 */
	public void announce(String serviceName, String serviceURI);

	/**
	 * Get discovered URIs for a given service name
	 * 
	 * @param serviceName - name of the service
	 * @param minReplies  - minimum number of requested URIs. Blocks until the
	 *                    number is satisfied.
	 * @return array with the discovered URIs for the given service name.
	 * @throws InterruptedException
	 */
	public URI[] knownUrisOf(String serviceName, int minReplies) throws InterruptedException;

	/**
	 * Get the instance of the Discovery service
	 * 
	 * @return the singleton instance of the Discovery service
	 */
	public static Discovery getInstance() {
		return DiscoveryImpl.getInstance();
	}
}

/**
 * Implementation of the multicast discovery service
 */
class DiscoveryImpl implements Discovery {

	private static Logger Log = Logger.getLogger(Discovery.class.getName());
	private Map<String, Cache<String, URI>> urisMap;

	// The pre-aggreed multicast endpoint assigned to perform discovery.

	static final int DISCOVERY_RETRY_TIMEOUT = 5000;
	static final int DISCOVERY_ANNOUNCE_PERIOD = 1000;

	// Replace with appropriate values...
	static final InetSocketAddress DISCOVERY_ADDR = new InetSocketAddress("230.120.130.145", 1234);

	// Used separate the two fields that make up a service announcement.
	private static final String DELIMITER = "\t";

	private static final int MAX_DATAGRAM_SIZE = 65536;

	private static Discovery singleton;

	synchronized static Discovery getInstance() {
		if (singleton == null) {
			singleton = new DiscoveryImpl();
		}
		return singleton;
	}

	private DiscoveryImpl() {
		urisMap = new HashMap<String, Cache<String, URI>>();
		this.startListener();

	}

	@Override
	public void announce(String serviceName, String serviceURI) {
		Log.info(String.format("Starting Discovery announcements on: %s for: %s -> %s\n", DISCOVERY_ADDR, serviceName,
				serviceURI));

		var pktBytes = String.format("%s%s%s", serviceName, DELIMITER, serviceURI).getBytes();
		var pkt = new DatagramPacket(pktBytes, pktBytes.length, DISCOVERY_ADDR);

		// start thread to send periodic announcements
		new Thread(() -> {
			try (var ds = new DatagramSocket()) {
				while (true) {
					try {
						ds.send(pkt);
						Thread.sleep(DISCOVERY_ANNOUNCE_PERIOD);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	@Override
	public URI[] knownUrisOf(String serviceName, int minEntries) throws InterruptedException {
		Cache<String, URI> cache = urisMap.get(serviceName);
		while (cache == null || cache.size() < minEntries) {
			Thread.sleep(DISCOVERY_ANNOUNCE_PERIOD);
			cache = urisMap.get(serviceName);
		}
		Collection<URI> colUris = cache.asMap().values();
		return Arrays.copyOf(colUris.toArray(), colUris.size(), URI[].class);

	}

	private void startListener() {
		Log.info(String.format("Starting discovery on multicast group: %s, port: %d\n", DISCOVERY_ADDR.getAddress(),
				DISCOVERY_ADDR.getPort()));

		new Thread(() -> {
			try (var ms = new MulticastSocket(DISCOVERY_ADDR.getPort())) {
				ms.joinGroup(DISCOVERY_ADDR, NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
				for (;;) {
					try {
						var pkt = new DatagramPacket(new byte[MAX_DATAGRAM_SIZE], MAX_DATAGRAM_SIZE);
						ms.receive(pkt);
						var msg = new String(pkt.getData(), 0, pkt.getLength());
						Log.info(String.format("Received: %s", msg));

						var parts = msg.split(DELIMITER);
						if (parts.length == 2) {
							var serviceName = parts[0];
							var uri = URI.create(parts[1]);
							Cache<String, URI> cache = urisMap.get(serviceName);
							if (cache == null) {
								cache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();
								urisMap.put(serviceName, cache);
							}
							cache.put(parts[1], uri);

						}

					} catch (Exception x) {
						x.printStackTrace();
					}
				}
			} catch (Exception x) {
				x.printStackTrace();
			}
		}).start();
	}
}