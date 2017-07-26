import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.Integer;
import java.util.PriorityQueue;

public class PriorityQueueSort {

    public static void main(String[] args) {
        List<Integer> list = new ArrayList<Integer>(Arrays.asList(5, -2, 7, 11, 3));
        sort(list);
    }

    public static void sort(List<Integer> data) {
        PriorityQueue<Integer> pq = new PriorityQueue<Integer>();
        for (int i = data.size() - 1; i >= 0; i--) {
            pq.add(data.remove(i));
        }

        for (int i = pq.size(); i > 0; i--) {
            data.add(pq.poll());
        }
    }
}
