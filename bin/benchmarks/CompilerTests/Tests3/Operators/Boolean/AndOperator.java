import java.util.HashMap;
import java.util.Map;
import java.lang.Integer;

public class AndOperator {

    public static void main(String[] args) {
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        map.put(0, 0);
        map.put(4, -1);
        map.put(3, 12);
        map.put(-7, 5);
        andOperator(map);
    }

    public static Map<Integer, Integer> andOperator(Map<Integer, Integer> data) {
        Map<Integer, Integer> temp = new HashMap<Integer, Integer>();

        for (int key : data.keySet()) {
            if (!(key < 0) && data.get(key) >= 0) {
                temp.put(key, data.get(key));
            }
        }
        return temp;
    }
}
