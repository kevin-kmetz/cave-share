// cave-share
// A very crude and primitive file transfer utility.
// Programmed by Kevin Kmetz

import java.net.*;
import java.util.Scanner;

class CaveShare {

	static int waitTime = 750;

	static void CaveServer(){

		// Add code for the server here.

	}

	static void CaveClient(){

		// Add code for the client here.

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
						System.out.println("Server!");
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
			// Nothing is done here.
			// If an InterruptedException is caught, just stop sleeping.
		}

	}

}
