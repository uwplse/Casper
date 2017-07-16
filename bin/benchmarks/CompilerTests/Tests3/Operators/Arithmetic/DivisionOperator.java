import java.util.Arrays;
import java.util.List;
import java.lang.Double;

public class DivisionOperator {

    public static void main(String[] args) {
        List<Double> list = Arrays.asList(5.5, 3.6, 1.3);
        System.out.println(divide(list));
    }

    public static List<Double> divide(List<Double> data) {
        for (Integer i = 0; i < data.size(); i = i + 1) {
            double num = data.get(i) / 2;
            data.set(i, num);
        }
        return data;
    }
}
