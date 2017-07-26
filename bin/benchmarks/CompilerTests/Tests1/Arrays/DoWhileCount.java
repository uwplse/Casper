public class DoWhileCount {

    public static void main(String[] args) {
        int[] list = {2, 6, 7, 3, 9};
        count(list);
    }

    public static int count(int[] data) {
        int count = 0;
        do {
            count++;
        } while (count < data.length);
        return count;
    }
}
