
//@author Ritwik Basak
import java.net.*;
import java.io.*;
import java.util.*;

public class MServer extends Thread {

	private static final int TIMEOUT = 30;
	private static final double LOSS_RATE = 0.3;

	private final DatagramSocket serverSocket;
	private final InetAddress clientIp;
	private final int clientPort;

	private final String request;

	public MServer(DatagramSocket serverSocket, DatagramPacket requestPacket) throws IOException {
		this.serverSocket = serverSocket;
		this.clientIp = requestPacket.getAddress();
		this.clientPort = requestPacket.getPort();

		request = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(requestPacket.getData())))
				.readLine();

	}

	private static int increment(int num) {
		return (num) % 127 + 1;
	}

	private static byte[] getNextConsignment(FileInputStream fileIn, int currentPacket) throws IOException {

		byte[] begin = new byte[6];
		byte[] begin0 = "RDT ".getBytes();
		byte[] begin1 = " ".getBytes();
		System.arraycopy(begin0, 0, begin, 0, 4);
		begin[4] = (byte) currentPacket;
		System.arraycopy(begin1, 0, begin, 5, 1);
		// ("RDT " + byte(currentPacket) + " ").getBytes();

		byte[] payload = new byte[512];

		int result = fileIn.read(payload);
		result = (result == -1) ? 0 : result;

		String lastString = " \r\n";

		if (result < 512)

			lastString = " END" + lastString;

		byte[] lastBytes = lastString.getBytes();

		byte[] sentData = new byte[begin.length + result + lastBytes.length];
		System.arraycopy(begin, 0, sentData, 0, begin.length);

		if (result != 0)

			System.arraycopy(payload, 0, sentData, begin.length, result);

		System.arraycopy(lastBytes, 0, sentData, begin.length + result, lastBytes.length);

		return sentData;

	}

	public void run() {

		FileInputStream fileIn = null;

		try {

			if (!request.startsWith("REQUEST")) {

				System.out.println();
				System.out.println("CLIENT PORT = " + clientPort);

				System.out.println();
				System.out.println("Invalid Client Request");
				return;

			}

			String fileName = request.substring(request.indexOf("REQUEST") + "REQUEST".length(), request.length());

			System.out.println();
			System.out.println("CLIENT PORT = " + clientPort + "\nFile = " + fileName);

			System.out.println();
			System.out.println("Received request for " + fileName + " from " + clientIp + " port " + clientPort);

			// read file into buffer
			fileIn = new FileInputStream(fileName);

			int currentPacket = 1;
			Random random = new Random();

			serverSocket.setSoTimeout(TIMEOUT);

			byte[] lastPacket = null;
			boolean resend = false;
			boolean end = false;

			while (!end) {

				byte[] readBuffer = new byte[1000];

				DatagramPacket receivedPacket = new DatagramPacket(readBuffer, readBuffer.length);

				if (resend != true) {

					lastPacket = getNextConsignment(fileIn, currentPacket);

				}

				DatagramPacket sentPacket = new DatagramPacket(lastPacket, lastPacket.length, clientIp, clientPort);

				// drop packet probabilistically
				if (random.nextDouble() >= LOSS_RATE) {

					System.out.println();
					System.out.println("CLIENT PORT = " + clientPort + "\nFile = " + fileName);

					System.out.println();
					System.out.println("Sent CONSIGNMENT " + currentPacket);

					serverSocket.send(sentPacket);

					// System.out.println();
					// System.out.println("CLIENT PORT = " + clientPort + "\nFile = " + fileName);

					// System.out.println();
					// System.out.println(new String(lastPacket));

				} else {

					System.out.println();
					System.out.println("CLIENT PORT = " + clientPort + "\nFile = " + fileName);

					System.out.println();
					System.out.println("Forgot CONSIGNMENT " + currentPacket);

				}

				try {

					serverSocket.receive(receivedPacket);
					resend = false;

					currentPacket = increment(currentPacket);

					int ack = readBuffer[4];

					// The consignment numbers and acknowledgements are in the range 1 .. 127
					// The acknowledgement 0 means END OF TRANSMISSION
					if (ack == 0)

						end = true;

					else {

						System.out.println();
						System.out.println("CLIENT PORT = " + clientPort + "\nFile = " + fileName);

						System.out.println();
						System.out.println("Received ACK " + ack);

					}
				} catch (SocketTimeoutException e) {

					System.out.println();
					System.out.println("CLIENT PORT = " + clientPort + "\nFile = " + fileName);

					System.out.println();
					System.out.println("Timeout");
					resend = true;

				}

				receivedPacket = null;
				sentPacket = null;

			}

			System.out.println();
			System.out.println("CLIENT PORT = " + clientPort + "\nFile = " + fileName);

			System.out.println();
			System.out.println("END");

		} catch (IOException e) {
			System.out.println(e);
			System.out.println(e.getStackTrace()[0].getLineNumber());

		} finally {
			try {
				if (fileIn != null)
					fileIn.close();
			} catch (IOException e) {
				System.out.println(e);
				System.out.println(e.getStackTrace()[0].getLineNumber());
			}
		}

	}

}
