import java.lang.Number;
import java.lang.Double;
import java.lang.Integer;

public class InstanceOfCount {

    public static void main(String[] args) {

        Number[] list = {11.5, 2, 7.6};
        count(list);
    }

    public static int count(Number[] data) {
        int count = 0;
        for(Integer i = 0; i < data.length; i++) {
            if (data[i] instanceof Double) {
                count++;
            }
        }
        return count;
    }
}
