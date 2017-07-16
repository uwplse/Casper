import java.util.HashMap;
import java.lang.Integer;
import java.util.Map;

public class ContinueSum {

    public static void main(String[] args) {
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        map.put(0, -3);
        map.put(1, 5);
        map.put(2, 1);
        sum(map);
    }

    public static int sum(Map<Integer, Integer> data) {
        int sum = 0;
        for (int key : data.keySet()) {
            int num = data.get(key);
            if (num < 0) {
                continue;
            }
            sum += num;
        }
        return sum;
    }
}
