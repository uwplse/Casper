import java.lang.Integer;
import java.util.Map;
import java.util.TreeMap;

public class WhileCount {

    public static void main(String[] args) {
        Map<Integer, Integer> map = new TreeMap<Integer, Integer>();
        map.put(0, 5);
        map.put(1, 3);
        map.put(2, 8);
        count(map);
    }

    public static int count(Map<Integer, Integer> data) {
        int count = 0;
        while (lessThanSize(count, data.size())) {
            count++;
        }
        return count;
    }

    public static boolean lessThanSize(int count, int size) {
        return count < size;
    }
}
