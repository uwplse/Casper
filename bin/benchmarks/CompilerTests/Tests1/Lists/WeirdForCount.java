import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.Integer;

public class WeirdForCount {

    public static void main(String[] args) {
        List<Integer> list = new ArrayList<Integer>(Arrays.asList(2, -10, 1, 3, 9));
        count(list);
    }

    public static int count(List<Integer> data) {
        int count = 0;
        boolean done = false;
        for (int i = 0; !done; i += 2 - 1) {
            count = i;
            if (count == data.size()) {
                done = true;
            }
        }
        return count;
    }
}
