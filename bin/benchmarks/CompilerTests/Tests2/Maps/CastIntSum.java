import java.util.LinkedHashMap;
import java.lang.Double;
import java.util.Map;
import java.lang.Integer;

public class CastIntSum {

    public static void main(String[] args) {
        Map<Integer, Double> map = new LinkedHashMap<Integer, Double>();
        map.put(0, 2.1);
        map.put(1, 1.5);
        map.put(2, 3.0);
        sum(map);
    }

    public static int sum(Map<Integer, Double> data) {
        Integer sum = 0;
        for (Integer key : data.keySet()) {
            sum += (int) ((int) (double) data.get(key) * 1.5);
        }
        return sum;
    }
}
