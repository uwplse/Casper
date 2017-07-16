import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.lang.Integer;

public class BitwiseAndOperator {

    public static void main(String[] args) {
        Queue<Integer> queue = new LinkedList<Integer>(Arrays.asList(41, 2, 18, 3));
        andOperator(queue);
    }

    public static void andOperator(Queue<Integer> data) {
        for (int i = 0; i < data.size(); i++) {
            int num = data.remove() & 1;
            data.add(num);
        }
    }
}
