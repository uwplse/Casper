import java.util.Arrays;
import java.util.List;
import java.lang.Integer;

public class BitwiseComplement {

    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(17, 6, -10, 9);
        complement(list);
    }

    public static void complement(List<Integer> data) {
        for (int i = 0; i < data.size(); i++) {
            int num = ~data.get(i);
            data.set(i, num);
        }
    }
}
