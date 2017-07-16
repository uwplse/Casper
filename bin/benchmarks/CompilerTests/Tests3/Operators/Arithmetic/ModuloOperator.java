public class ModuloOperator {

    public static void main(String[] args) {
        int[] list = {12, 5, 88, 54};
        modulo(list);
    }

    public static int[] modulo(int[] data) {
        int k = 0;
        while (k < data.length) {
            data[k] = data[k] % 10;
            k++;
        }
        return data;
    }
}
