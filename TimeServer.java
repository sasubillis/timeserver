package assignment;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;
import java.util.Set;

public class TimeServer {
	public static void main(String args[]) {
		try {
			CharsetEncoder encoder = Charset.forName("US-ASCII").newEncoder();

			// Read the port number from command line argument
			int port = 37; // default port.
			if (args.length > 0) {
				try {
					port = Integer.parseInt(args[0]);
				} catch (NumberFormatException e) {
					System.err.println("Argument" + " must be an integer");
					System.exit(1);
				}
			}

			SocketAddress localport = new InetSocketAddress(port);
			// TCP Channel
			ServerSocketChannel tcpserver = ServerSocketChannel.open();
			tcpserver.socket().bind(localport);
			tcpserver.configureBlocking(false);

			// UDP Channel
			DatagramChannel udpserver = DatagramChannel.open();
			udpserver.socket().bind(localport);
			udpserver.configureBlocking(false);

			// Block the channel while waiting actively for data
			Selector selector = Selector.open();
			tcpserver.register(selector, SelectionKey.OP_ACCEPT);
			udpserver.register(selector, SelectionKey.OP_READ);

			// Create buffer to receive udp inet adderss
			ByteBuffer receiveBuffer = ByteBuffer.allocate(0);

			// infinite loop waiting for connections
			for (;;) {
				try {
					selector.select(); // blocking call
					
					// Prepare the Time server response to connected client
					// date time in seconds since Jan 1900(RFC 868)
			        String date = ""+ new java.util.Date().getTime() + "\r\n";
					ByteBuffer response = encoder.encode(CharBuffer.wrap(date));

					// get the selectionkey objects from channels
					Set keys = selector.selectedKeys();
					for (Iterator i = keys.iterator(); i.hasNext();) {
						// Get a key from the set, and remove it from the set
						SelectionKey key = (SelectionKey) i.next();
						i.remove();
						// Get the channel associated with the key
						Channel c = (Channel) key.channel();

						// Check tcp or udp channel has got event?
						if (key.isAcceptable() && c == tcpserver) {
							// Handling TCP Channel
							SocketChannel client = tcpserver.accept();
							if (client != null) {
								// Send daytime message to TCP client
								client.write(response);
								client.close();
							}
						} else if (key.isReadable() && c == udpserver) {
							// Handling UDP Channel
							SocketAddress clientAddress = udpserver
									.receive(receiveBuffer);
							if (clientAddress != null) {
								udpserver.send(response, clientAddress);
							}
						}
					}
				} catch (java.io.IOException e) {
					System.err.println(e);
				}
			}

		} catch (Exception e) {
			System.err.println(e);
			System.exit(1);
		}
	}
}
