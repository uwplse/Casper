import java.lang.String;

public class StringLengthCount {

    public static void main(String[] args) {
        String[] list = {"A", "BC", "D", "EFG"};
        count(list);
    }

    public static int count(String[] data) {
        String count = "";
        for (String str : data) {
            count = count + str;
        }
        return count.length();
    }
}
