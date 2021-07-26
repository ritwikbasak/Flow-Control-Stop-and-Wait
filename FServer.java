
//@author Ritwik Basak
import java.net.*;
import java.io.*;
import java.util.*;

public class FServer {

	public static void main(String[] args) {

		try {
			DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt(args[1]), InetAddress.getByName(args[0]));

			System.out.println();
			System.out.println("Server is up....");

			while (true) {
				byte[] readBuffer = new byte[1000];
				DatagramPacket receivedPacket = new DatagramPacket(readBuffer, readBuffer.length);
				serverSocket.receive(receivedPacket);// wait for client REQUEST

				new MServer(new DatagramSocket(), receivedPacket).start();
			}

		} catch (Exception e) {
			System.out.println(e);
		}

	}
}
