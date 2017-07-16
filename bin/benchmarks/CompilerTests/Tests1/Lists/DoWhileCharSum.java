import java.util.Arrays;
import java.util.List;
import java.lang.Character;

public class DoWhileCharSum {

    public static void main(String[] args) {
        List<Character> list = Arrays.asList('a', 'U', 'f', 'R', 's');
        sumChars(list);
    }

    public static int sumChars(List<Character> data) {
        int sum = 0;
        int k = 0;
        do {
            sum += Character.getNumericValue(Character.toLowerCase(data.get(k++)));
        } while (k < data.size());
        return sum;
    }
}
