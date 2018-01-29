package generated.stats;

import java.util.Scanner;
import java.io.*;

public class IOUtil {
    public static double skipToDouble(Scanner scanner) {
        while (((Scanner) scanner).hasNext() &&
                !((Scanner) scanner).hasNextDouble()) {
            ((Scanner) scanner).next();
        }
        return ((Scanner) scanner).hasNextDouble()
                ? ((Scanner) scanner).nextDouble()
                : Double.NaN;
    }

    public static FileReader file(String fileName)
            throws FileNotFoundException {
        return new FileReader((String) fileName);
    }

    public static String typedInput() throws IOException {
        BufferedReader keyIn =
                new BufferedReader(new InputStreamReader(System.in));
        return ((BufferedReader) keyIn).readLine();
    }

    /**

     * does not accept anything but integers

     * @ return the first integer value

     */
    public static int skipToInt(Scanner scanner) {
        while (((Scanner) scanner).hasNext() &&
                !((Scanner) scanner).hasNextInt()) {
            ((Scanner) scanner).next();
        }
        return ((Scanner) scanner).hasNextInt()
                ? ((Scanner) scanner).nextInt()
                : (int) Double.NaN;
    }

    /**

     * Call this every time wrong thing is typed.

     */
    public static void abuse() {
        System.out.println("Invalid entry.");
        System.out.println("Abusive statement.");
        System.exit(0);
    }

    public static String fileName() throws IOException {
        System.out.printf("Type file name or hit \'!\' \n",
                (Object[]) (new Object[] {  }));
        String typed = typedInput();
        System.out.printf("Found %s \n", (Object[]) (new Object[] { typed }));
        return (String) typed;
    }

    public static String getFileName() throws IOException {
        System.out.printf("Type file name or hit \'!\' \n",
                (Object[]) (new Object[] {  }));
        String typed = typedInput();
        System.out.printf("Found %s \n", (Object[]) (new Object[] { typed }));
        return (String) typed;
    }

    public static String enterValue(String kindOf) {
        return (String) ("Enter " + kindOf + "value:");
    }

    public IOUtil() { super(); }
}