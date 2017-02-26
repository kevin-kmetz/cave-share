// cave-share
// A very crude and primitive file transfer utility.
// Programmed by Kevin Kmetz

import java.net.*;
import java.util.Scanner;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.nio.ByteBuffer;
import java.util.Arrays;

import java.util.InputMismatchException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;


class CaveShare {

	static int waitTime = 750;

	class CaveServer {

		CaveServer() {
			System.out.println("\nInitializing server...\n");
			delay(waitTime);

			int serverPort = chooseServerPort();
			String token = chooseToken();
			File file = chooseFile();

			try {
				offerFile(file, serverPort, token);
			} catch (UnknownHostException e) {
				System.out.println("Error - the server has an invalid address.");
				System.out.println("Exiting the program early...\n");
			}
		}

		int chooseServerPort() {

			int port = 55555;
			Scanner input = new Scanner(System.in);
			boolean inputError;

			do {
				inputError = false;
				System.out.println("Which port should be used to send files?");
				delay(waitTime);
				System.out.print("Port number: ");
				try {
					port = input.nextInt();

					if (port <= 0) {
						inputError = true;
						System.out.println("Error - invalid port number. Please enter another port number.\n");
						delay(waitTime);
					}

				} catch (InputMismatchException e) {
					inputError = true;
					System.out.println("Error. Please enter another port number.\n");
					delay(waitTime);

					// The following line is needed to disregard a lingering '\n'.
					input.nextLine();
				}
			} while (inputError);

			return port;
		}

		String chooseToken () {

			Scanner input = new Scanner(System.in);

			System.out.println("\nPlease enter a token that others can use to initiate file transfers.");
			delay(waitTime);
			System.out.print("Enter a word or phrase: ");

			return input.nextLine();

		}

		File chooseFile() {

			Scanner input = new Scanner(System.in);
			String pathAndName = "";
			File file;

			do {
				System.out.println("\nPlease enter the path and filename of the file that will be offered.");
				delay(waitTime);
				System.out.print("Enter the path and filename: ");

				pathAndName = input.nextLine();

				file = new File(pathAndName);

			} while (!isValidFile(file));

			System.out.println("SHA-256 hash: " + getHash(file));

			return file;

		}

		boolean isValidFile (File file) {

			if (file.exists() && !file.isDirectory()) {
				return true;
			} else if (!file.exists()) {
				System.out.println("Error - file does not exist. Please enter another file.");
				return false;
			} else if (file.isDirectory()) {
				System.out.println("Error - this is a directory. Please enter a file instead.");
				return false;
			}

			// It shouldn't be possible to get to this return statement.
			return false;

		}

		void offerFile(File file, int serverPort, String token) throws UnknownHostException {

			System.out.println("\nPreparing to offer file...");

			InetAddress serverAddress = InetAddress.getLocalHost();

			System.out.println("\nServer address: " + serverAddress);
			System.out.println("Listening on port: " + serverPort);
			System.out.println("File: " + file.getName());
			System.out.println("Server token: " + token);

			try {
				DatagramSocket serverSocket = new DatagramSocket(serverPort);

				DatagramPacket clientPacket = listenForClient(serverSocket, token);
				sendFileInfo(file, clientPacket, serverSocket);
				boolean clientReady = waitForClientReadiness(clientPacket, serverSocket);

				if (clientReady) {
					sendFile(file, clientPacket, serverSocket);
				}

			} catch (SocketException e) {
				System.out.println("Error occurred while trying to open a port.");
			} catch (IOException e) {
				System.out.println("Error occurred while receiving data.");
			}

		}

		DatagramPacket listenForClient(DatagramSocket serverSocket, String token) throws SocketException, IOException {

			System.out.println("\nNow listening for a client...\n");

			int bufferSize = 256;
			byte dataBuffer[] = new byte[bufferSize];
			boolean keepGoing;

			DatagramPacket packet = new DatagramPacket(dataBuffer, dataBuffer.length);

			do {

				serverSocket.receive(packet);

				String receivedToken = new String(packet.getData(), 0, packet.getLength());

				if (!receivedToken.equals(token)) {
					keepGoing = true;
					System.out.print("Received an unauthorized connection attempt from ");
					System.out.print(packet.getAddress() + ":" + packet.getPort());
					System.out.println(" using token '" + receivedToken + "'.");
				} else {
					keepGoing = false;
					System.out.print("Connection initiated from ");
					System.out.print(packet.getAddress() + ":" + packet.getPort());
					System.out.println(", token '" + receivedToken + "' successfully received!");
				}

			} while (keepGoing);

			System.out.println("\nMoving on to the next step...\n");
			// This is a delay for manual testing purposes.
			//delay(15000);
			return packet;
		}

		void sendFileInfo(File file, DatagramPacket clientPacket, DatagramSocket serverSocket) throws SocketException, IOException {

			String fileName = file.getName();
			long fileSize = file.length();
			String hash = getHash(file);

			System.out.println("Sending file information to the client...");

			byte[] packetBody = formInfoByteArray(fileSize, hash, fileName);
			System.out.println("Packet size: " + packetBody.length + " bytes");

			DatagramPacket infoPacket = new DatagramPacket(packetBody, packetBody.length, clientPacket.getAddress(), clientPacket.getPort());

			serverSocket.send(infoPacket);

			System.out.println("File information sent!");

		}

		byte[] formInfoByteArray(long fileSize, String hash, String fileName) {

			byte[] arrayOne = concatenateByteArrays(longToBytes(fileSize), stringToBytes(hash));
			byte[] arrayTwo = concatenateByteArrays(arrayOne, stringToBytes(fileName));

			return arrayTwo;

		}

		byte[] longToBytes(long number) {

			ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
			buffer.putLong(number);
			return buffer.array();

		}

		byte[] stringToBytes(String str) {

			return str.getBytes();

		}

		byte[] concatenateByteArrays(byte[] arrayOne, byte[] arrayTwo) {

			byte[] newArray = new byte[arrayOne.length + arrayTwo.length];

			System.arraycopy(arrayOne, 0, newArray, 0, arrayOne.length);
			System.arraycopy(arrayTwo, 0, newArray, arrayOne.length, arrayTwo.length);

			return newArray;

		}

		boolean waitForClientReadiness(DatagramPacket clientPacket, DatagramSocket serverSocket) throws IOException {

			System.out.println("\nWaiting for the client to initiate the file transfer...");
			boolean clientReady = false;

			// Wait to receive the text "READY" from the client.
			// The body of the received packet should therefore be 5 bytes.
			int bufferSize = 5;
			byte[] buffer = new byte[bufferSize];
			DatagramPacket readyPacket = new DatagramPacket(buffer, buffer.length);

			serverSocket.receive(readyPacket);

			String packetText = new String(readyPacket.getData(), 0, readyPacket.getLength());

			if (packetText.equals("READY")) {
				System.out.println("Client has confirmed it is ready for the download with message " + packetText + ".");
				clientReady = true;
			} else {
				System.out.println("Error - malformed packet received. Packet body: " + packetText);
				clientReady = false;
			}

			return clientReady;

		}

		void sendFile(File file, DatagramPacket clientPacket, DatagramSocket serverSocket) throws FileNotFoundException, IOException {

			System.out.println("\nPreparing to send file...\n");

			FileInputStream fileStream = new FileInputStream(file);
			int packetSize = 64;
			int fileSize = (int) file.length();
			int packetsNeeded = 0;
			int packetRemainder = packetSize;

			if (fileSize % packetSize == 0) {
				packetsNeeded = fileSize / packetSize;
			} else {
				packetsNeeded = (fileSize / packetSize) + 1;
				packetRemainder = fileSize % packetSize;
			}

			byte[] fileBytes = new byte[fileSize];
			fileStream.read(fileBytes);

			// This is a temporary delay for manual testing purposes.
			// delay(15000);
			System.out.println("Now sending file...");

			for (int i = 0, currentPacketSize = packetSize; i < packetsNeeded; i++) {
				if (i == packetsNeeded-1){
					currentPacketSize = packetRemainder;
				}

				serverSocket.send(new DatagramPacket(fileBytes, i*packetSize, currentPacketSize, clientPacket.getAddress(), clientPacket.getPort()));
			}

			System.out.println("\nFile sent!");

		}

	}

	class CaveClient {

		static final int packetSize = 64;

		CaveClient() {
			System.out.println("\nInitializing client...\n");
			delay(waitTime);

			try {
				InetSocketAddress serverSocketAddr = inputServerDetails();
				String token = inputServerToken();
				int clientPort = chooseClientPort();

				try {
					obtainFile(serverSocketAddr, token, clientPort);
				} catch (Exception e) {
					System.out.println("Error - something has gone wrong.");
				}

			} catch (UnknownHostException e) {
				System.out.println("Error establishing a valid server hostname/IP address. Terminating...");
			}
		}

		InetSocketAddress inputServerDetails() throws UnknownHostException{
			
			return new InetSocketAddress(inputServerAddress(), inputServerPort());

		}

		InetAddress inputServerAddress() throws UnknownHostException {

			Scanner input = new Scanner(System.in);
			boolean inputError;
			String hostOrIP;

			// This next statement is problematic.
			// Because an empty InetAddress cannot be declared to prepare for the do...while loop,
			// and because the return statement doesn't trust that the variable will have a value,
			// this must done and the function must mention 'throws'.
			InetAddress address = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});

			do {
				inputError = false;
				System.out.println("What is the hostname or IP address of the server?");
				System.out.print("Server hostname/IP address: ");
				hostOrIP = new String(input.nextLine());

				try {
					address = InetAddress.getByName(hostOrIP);
				} catch (UnknownHostException e) {
					inputError = true;
					System.out.println("Error - invalid hostname or IP address.\n");
				}

			} while (inputError);

			return address;

		}

		int inputServerPort() {

			int port = 55555;
			Scanner input = new Scanner(System.in);
			boolean inputError;

			do {
				inputError = false;
				System.out.println("\nWhich port does the server use to send and receive data?");
				delay(waitTime);
				System.out.print("Server port number: ");
				try {
					port = input.nextInt();

					if (port <= 0) {
						inputError = true;
						System.out.println("Error - invalid port number. Please enter another port number.");
						delay(waitTime);
					}

				} catch (InputMismatchException e) {
					inputError = true;
					System.out.println("Error. Please enter another port number.\n");
					delay(waitTime);

					// The following line is needed to disregard a lingering '\n'.
					input.nextLine();
				}
			} while (inputError);

			return port;
		}

		String inputServerToken() {

			Scanner input = new Scanner(System.in);

			System.out.println("\nEnter the token that the server will require to initiate the file transfer.");
			delay(waitTime);
			System.out.print("Enter a word or phrase: ");

			return input.nextLine();

		}

		int chooseClientPort() {

			int port = 55555;
			Scanner input = new Scanner(System.in);
			boolean inputError;

			do {
				inputError = false;
				System.out.println("\nUse which port to communicate with the server?");
				delay(waitTime);
				System.out.print("Client port number: ");
				try {
					port = input.nextInt();

					if (port <= 0) {
						inputError = true;
						System.out.println("Error - invalid port number. Please enter another port number.");
						delay(waitTime);
					}

				} catch (InputMismatchException e) {
					inputError = true;
					System.out.println("Error. Please enter another port number.\n");
					delay(waitTime);

					// The following line is needed to disregard a lingering '\n'.
					input.nextLine();
				}
			} while (inputError);

			return port;
		}

		void obtainFile(InetSocketAddress serverSocketAddr, String token, int clientPort) throws UnknownHostException {

			System.out.println("\nPreparing to establish connection with server...");

			System.out.println("\nServer address: " + serverSocketAddr.getHostString());
			System.out.println("Server port: " + serverSocketAddr.getPort());
			System.out.println("Server token: " + token);
			System.out.println("Client port: " + clientPort);

			try {
				DatagramSocket clientSocket = new DatagramSocket(clientPort);

				establishConnection(serverSocketAddr, token, clientSocket);
			} catch (SocketException e) {
				System.out.println("Error occurred while trying to open client port.");
			} catch (IOException e) {
				System.out.println("Error occurred while trying to communicate with the server.");
			}

		}

		void establishConnection(InetSocketAddress serverSocketAddr, String token, DatagramSocket clientSocket) throws SocketException, IOException {

			sendToken(serverSocketAddr, token, clientSocket);

			byte[] fileInfoArray = receiveFileInfoPacket(clientSocket);

			int fileSize = extractFileSize(fileInfoArray);
			String serversHash = extractServerHash(fileInfoArray);
			String fileName = extractFileName(fileInfoArray);

			displayServerFileInfo(fileName, fileSize, serversHash);

			File file = createFile(fileName);

			receiveFile(file, fileSize, serverSocketAddr, clientSocket);

			checkFileIntegrity(file, serversHash);

			System.out.println("Now closing the client...\nThanks for using CaveShare!");

		}

		void sendToken(InetSocketAddress serverSocketAddr, String token, DatagramSocket clientSocket) throws IOException {

			DatagramPacket tokenPacket = new DatagramPacket(token.getBytes(), token.length(), serverSocketAddr.getAddress(), serverSocketAddr.getPort());

			System.out.println("\nSending token '" + token + "' to server...");
			clientSocket.send(tokenPacket);
			System.out.println("Token sent.");

		}

		byte[] receiveFileInfoPacket(DatagramSocket clientSocket) throws IOException {

			byte[] buffer = new byte[128];
			DatagramPacket infoPacket = new DatagramPacket(buffer, buffer.length);

			System.out.println("Waiting for reply from server...");
			clientSocket.receive(infoPacket);

			System.out.println("\nFile info received!");

			return buffer;

		}

		int extractFileSize(byte[] fileInfo) {

			ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[] {fileInfo[0], fileInfo[1], fileInfo[2], fileInfo[3], fileInfo[4], fileInfo[5], fileInfo[6], fileInfo[7]});

			// Eventually, a maximum file size of 2 GB should be checked for and
			// error handling should be implemented here.
			long fileSize = byteBuffer.getLong();
			int fileSizeInt = (int) fileSize;

			return fileSizeInt;

		}

		String extractServerHash(byte[] fileInfo) {

			String serversFileHash = new String(Arrays.copyOfRange(fileInfo, 8, 72));

			return serversFileHash;

		}

		String extractFileName(byte[] fileInfo) {

			int endOfFileNameIndex = locateFileNameEnd(fileInfo);

			String fileName = new String(Arrays.copyOfRange(fileInfo, 72, endOfFileNameIndex+1));

			return fileName;

		}

		int locateFileNameEnd(byte[] fileInfo) {

			int lastCharIndex = fileInfo.length - 1;

			for (int i = lastCharIndex; i >= 72; i--) {
				if ((int) fileInfo[i] != 0) {
					lastCharIndex = i;
					break;
				}
			}

			return lastCharIndex;

		}

		void displayServerFileInfo(String fileName, int fileSize, String serversHash) {

			System.out.println("\nFile name: " + fileName);
			System.out.println("File size: " + fileSize + " bytes");
			System.out.println("Server's file hash: " + serversHash);

		}

		File createFile(String fileName) {

			System.out.println("Instantiating file...");
			File file = new File(fileName);
			System.out.println("Creating file...");

			try {
				file.createNewFile();
			} catch(IOException e) {
				System.out.println("IOEXCEPTION");
				e.printStackTrace();
			} catch (SecurityException e) {
				System.out.println("SECURITY EXCEPTION");
			}

			return file;

		}

		void receiveFile(File file, int fileSize, InetSocketAddress serverSocketAddr, DatagramSocket clientSocket) throws FileNotFoundException, IOException {

			FileOutputStream fileOutputStream = new FileOutputStream(file);
			byte[] receiveBuffer = new byte[fileSize];
			int packetsNeeded = calculatePacketsNeeded(fileSize);

			// This following line is needed because the final packet sent by the server
			// isn't padded with empty data to match the size of the other packets.
			int finalPacketSize = calculateFinalPacketSize(fileSize);

			System.out.println("\nBeginning the file transfer...");

			clientSocket.send(new DatagramPacket("READY".getBytes(), 5, serverSocketAddr.getAddress(), serverSocketAddr.getPort()));

			for (int i = 0, currentPacketSize = packetSize; i < packetsNeeded; i++) {
				if (i == packetsNeeded-1){
					currentPacketSize = finalPacketSize;
				}
				clientSocket.receive(new DatagramPacket(receiveBuffer, packetSize*i, currentPacketSize));
			}

			System.out.println("File transfer complete!");

			fileOutputStream.write(receiveBuffer);
			fileOutputStream.close();
			clientSocket.close();

		}

		int calculatePacketsNeeded(int fileSize) {

			int packetsNeeded;
			int packetRemainder = packetSize;

			if (fileSize % packetSize == 0) {
				packetsNeeded = fileSize / packetSize;
			} else {
				packetsNeeded = (fileSize / packetSize) + 1;
				packetRemainder = fileSize % packetSize;
			}

			return packetsNeeded;

		}

		int calculateFinalPacketSize(int fileSize) {

			int packetRemainder;

			if (fileSize % packetSize == 0) {
				packetRemainder = packetSize;
			} else {
				packetRemainder = fileSize % packetSize;
			}

			return packetRemainder;

		}

		void checkFileIntegrity(File file, String serversHash) {

			String clientsHash = getHash(file);
			System.out.println("\nReceived file's hash: " + clientsHash);
			System.out.println("Server's hash: " + serversHash);

			if (!clientsHash.equals(serversHash)) {
				System.out.println("File integrity corrupted!");
			} else if (clientsHash.equals(serversHash)) {
				System.out.println("File integrity verified!");
			}

		}

	}

	public static void main(String args[]) {

		if (args.length > 0) {

			// Add command-line interface here later.

		} else {

			System.out.println("\nCave Share\nBy Kevin Kmetz");
			delay(waitTime);

			String userInput;
			Scanner scanner = new Scanner(System.in);
			boolean inputError = false;

			do {
				System.out.println("\nWould you like to offer a file or obtain a file?");
				delay(waitTime);
				System.out.print("(Type 'obtain' or 'offer'): ");

				userInput = scanner.nextLine();

				switch (userInput) {
					case("obtain"):
						inputError = false;
						//System.out.println("Client!");
						CaveClient client = new CaveShare().new CaveClient();
						break;
					case("offer"):
						inputError = false;
						CaveServer server = new CaveShare().new CaveServer();
						//System.out.println("Server!");
						break;
					default:
						inputError = true;
						System.out.println("Error. Please try again.");
						delay(waitTime);
				}
			} while(inputError);

		}

	}

	static void delay(int time) {
	// 'delay' was chosen as the method name to avoid confusion and conflicts
	// with other common methods like 'sleep()' and 'wait()'.

		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			// Do nothing if an InterruptedException occurs,
			// as the program can just continue on.
			// System.out.println("An error!");
		}

	}

	static String getHash(File file) {

		String hash;

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");

			FileInputStream fileInputStream = new FileInputStream(file);
			byte[] fileBytes = new byte[fileInputStream.available()];

			fileInputStream.read(fileBytes);

			digest.update(fileBytes);
			byte[] hashedBytes = digest.digest();

			StringBuilder builder = new StringBuilder();

			for(byte b: hashedBytes) {
				builder.append(String.format("%02x", b));
			}

			hash = builder.toString();
		} catch (Exception e) {
			hash =  "ERROR - NO HASH";
		}

		return hash;

	}

}
