import java.io.*;

public class ProjectTester {

    public static final String PATH = "./bin/benchmarks/CompilerTests/";

    public static final File TESTS1 = new File(PATH + "Tests1");
    public static final File TESTS2 = new File(PATH + "Tests2");
    public static final File TESTS3 = new File(PATH + "Tests3");

    public static final String SUCCESS_MESSAGE = "Summary successfully verified";
    public static final String SCRIPT = "./bin/run.sh";

    public static int testNumber;
    public static int errorCount;

    public static void main(String[] args) {
        testNumber = 0;
        errorCount = 0;

        if (args[0].equals("1")) {
            runTests(TESTS1);
        } else if (args[0].equals("2")){
            runTests(TESTS2);
        } else {
            runTests(TESTS3);
        }

        System.out.println(errorCount + " out of " + testNumber + " tests failed.");
        System.exit(errorCount);
    }

    public static void runTests(File file) {
         if (file.isDirectory()) {
            File[] subFiles = file.listFiles();

            if (subFiles != null) {
                System.out.println(file.getName() + "\n");
                for (File f : subFiles) {
                    runTests(f);
                }
            }
        } else if (file.getName().contains(".java")) {
             testNumber++;
             System.out.println("Test " + testNumber + ": " + file.getName());

             String[] script = {SCRIPT, file.getPath(), "Test" + testNumber + ".java"};
             try {
                 runScript(script);
             } catch (IOException e){
                 e.printStackTrace();
                 errorCount++;
             }
             System.out.println();
         }

    }

    public static void runScript(String[] script) throws IOException {
        boolean successful = false;
        boolean outputProduced = false;

        ProcessBuilder pb = new ProcessBuilder(script);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        InputStream output = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(output));

        String line;
        while ((line = reader.readLine()) != null) {
            outputProduced = true;
            System.out.println(line);

            if (line.equals(SUCCESS_MESSAGE)) {
                successful = true;
            }
        }

        if (!successful && outputProduced) {
            errorCount++;
        }
    }

}
