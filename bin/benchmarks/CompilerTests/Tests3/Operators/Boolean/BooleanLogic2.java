import java.lang.Boolean;

public class BooleanLogic2 {

    public static void main(String[] args) {
        Boolean[] list = {true, false, true, false, true};
        testBoolean(list);
    }

    public static boolean testBoolean(Boolean[] data) {
        Boolean bool = false;
        for (boolean b : data) {
            bool = (bool || b) && b != bool;
        }
        return bool;
    }
}
