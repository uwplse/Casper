import java.util.Arrays;
import java.util.List;
import java.lang.Integer;
import java.lang.Boolean;

public class BooleanLogic1 {

    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(4, 1, 2);
        testBoolean(list);
    }

    public static Boolean testBoolean(List<Integer> data) {
        boolean bool = true;
        for (int i = 0; i < data.size(); i++) {
            bool = !(!bool || !(data.get(i) >= 0));
        }
        return bool; // All numbers non-negative
    }

}
