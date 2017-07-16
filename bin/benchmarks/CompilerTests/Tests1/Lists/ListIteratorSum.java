import java.util.Arrays;
import java.util.List;
import java.lang.Long;
import java.util.ListIterator;

public class ListIteratorSum {

    public static void main(String[] args) {
        List<Long> list = Arrays.asList(2l, -10l, 1l, 3l, 9l);
        sum(list);
    }

    public static long sum(List<Long> data) {
        ListIterator<Long> iterator = data.listIterator();
        Long sum = 0l;

        while (iterator.hasNext()) {
            sum += iterator.next();
        }
        return sum;
    }
}
