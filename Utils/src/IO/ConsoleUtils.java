package IO;

import java.util.Scanner;

public class ConsoleUtils {
    public static int GetUserChoice() throws NumberFormatException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Please choose one of the above:");
        System.out.print(">> ");

        String userInput = scanner.nextLine();
        int userChoice = Integer.parseInt(userInput);

        return userChoice;
    }
}