import java.lang.Integer;

public class NestedIfAbsMax {

    public static void main(String[] args) {
        Integer[] list = {2, -10, 1, 3, 9};
        farthestFromZero(list);
    }

    public static int farthestFromZero(Integer[] data) {
        int farthest = 0;
        int k = 0;
        while (k < data.length) {
            int num = data[k];
            if (num > 0) {
                if (num > farthest) {
                    farthest = num;
                }
            } else if (num < 0) {
                if (-num > farthest) {
                    farthest = -num;
                }
            }
            k++;
        }
        return farthest;
    }
}
