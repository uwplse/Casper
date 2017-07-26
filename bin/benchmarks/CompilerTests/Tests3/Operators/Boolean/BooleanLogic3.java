import java.util.Arrays;
import java.util.List;
import java.lang.Boolean;

public class BooleanLogic3 {

    public static void main(String[] args) {
        List<Boolean> list = Arrays.asList(true, false, false, true, true);
        testBoolean(list);
    }

    public static boolean testBoolean(List<Boolean> data) {
        boolean bool = false;
        for (int i = 0; i < data.size(); i++) {
            boolean b = i % 2 == 0 && !data.get(i);
            bool = !(b == bool) || (bool || data.get(i)) && b;
        }
        return bool;
    }
}
