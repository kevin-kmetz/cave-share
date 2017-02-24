// cave-share
// A very crude and primitive file transfer utility.
// Programmed by Kevin Kmetz

import java.net.*;
import java.util.Scanner;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

import java.util.InputMismatchException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;


class CaveShare {

	static int waitTime = 750;

	class CaveServer {

		CaveServer() {
			System.out.println("\nInitializing server...\n");
			CaveShare.wait(waitTime);

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
				CaveShare.wait(waitTime);
				System.out.print("Port number: ");
				try {
					port = input.nextInt();

					if (port <= 0) {
						inputError = true;
						System.out.println("Error - invalid port number. Please enter another port number.\n");
						CaveShare.wait(waitTime);
					}

				} catch (InputMismatchException e) {
					inputError = true;
					System.out.println("Error. Please enter another port number.\n");
					CaveShare.wait(waitTime);

					// The following line is needed to disregard a lingering '\n'.
					input.nextLine();
				}
			} while (inputError);

			return port;
		}

		String chooseToken () {

			Scanner input = new Scanner(System.in);

			System.out.println("\nPlease enter a token that others can use to initiate file transfers.");
			CaveShare.wait(waitTime);
			System.out.print("Enter a word or phrase: ");

			return input.nextLine();

		}

		File chooseFile() {

			Scanner input = new Scanner(System.in);
			String pathAndName = "";
			File file;

			do {
				System.out.println("\nPlease enter the path and filename of the file that will be offered.");
				CaveShare.wait(waitTime);
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
				DatagramPacket clientPacket = listenForClient(serverPort, token);
				sendFileInfo(file, clientPacket);
			} catch (SocketException e) {
				System.out.println("Error occurred while trying to open a port.");
			} catch (IOException e) {
				System.out.println("Error occurred while receiving data.");
			}

		}

		DatagramPacket listenForClient(int serverPort, String token) throws SocketException, IOException {

			System.out.println("\nNow listening for a client...\n");

			DatagramSocket serverSocket = new DatagramSocket(serverPort);

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
			return packet;
		}

		void sendFileInfo(File file, DatagramPacket packet) {

			String fileName = file.getName();
			long fileSize = file.length();
			//String hash = "";

		}

	}

	static void CaveClient(){

		// Add client code here later.

	}

	public static void main(String args[]) {

		if (args.length > 0) {

			// Add command-line interface here later.

		} else {

			System.out.println("\nCave Share\nBy Kevin Kmetz");
			wait(waitTime);

			String userInput;
			Scanner scanner = new Scanner(System.in);
			boolean inputError = false;

			do {
				System.out.println("\nWould you like to offer a file or obtain a file?");
				wait(waitTime);
				System.out.print("(Type 'obtain' or 'offer'): ");

				userInput = scanner.nextLine();

				switch (userInput) {
					case("obtain"):
						inputError = false;
						System.out.println("Client!");
						break;
					case("offer"):
						inputError = false;
						CaveServer server = new CaveShare().new CaveServer();
						//System.out.println("Server!");
						break;
					default:
						inputError = true;
						System.out.println("Error. Please try again.");
						wait(waitTime);
				}
			} while(inputError);

		}

	}

	static void wait(int time) {

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
