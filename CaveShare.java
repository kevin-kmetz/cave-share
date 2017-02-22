// cave-share
// A very crude and primitive file transfer utility.
// Programmed by Kevin Kmetz

import java.net.*;
import java.util.Scanner;
import java.util.InputMismatchException;

class CaveShare {

	static int waitTime = 750;

	class CaveServer {

		CaveServer() {
			System.out.println("\nInitializing server...\n");
			CaveShare.wait(waitTime);
			int port = getPort();
		}

		int getPort() {

			int port = 55555;
			Scanner input = new Scanner(System.in);
			boolean inputError;

			do {
				inputError = false;
				System.out.print("Which port should be to send files?\nPort number: ");
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

}
