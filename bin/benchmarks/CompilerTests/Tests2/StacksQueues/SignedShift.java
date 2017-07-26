import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.lang.Integer;

public class SignedShift {

    public static void main(String[] args) {
        Queue<Integer> queue = new ArrayDeque<Integer>(Arrays.asList(7, 3, -9, -2));
        shift(queue);
    }

    public static Queue<Integer> shift(Queue<Integer> data) {
        for (int i = 0; i < data.size(); i++) {
            int num = data.remove() << 1;
            data.add(num);
        }
        return data;
    }
}
