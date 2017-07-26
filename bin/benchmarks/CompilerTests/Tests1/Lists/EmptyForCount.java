import java.util.LinkedList;
import java.util.Arrays;
import java.util.List;
import java.lang.Integer;

public class EmptyForCount {

    public static void main(String[] args) {
        List<Integer> list = new LinkedList<Integer>(Arrays.asList(2, 6, 7, 3, 9));
        count(list);
    }

    public static int count(List<Integer> data) {
        Integer count = 0;
        for ( ; ; ) {
            if (count >= data.size()) {
                break;
            }
            count++;
        }
        return count;
    }
}
