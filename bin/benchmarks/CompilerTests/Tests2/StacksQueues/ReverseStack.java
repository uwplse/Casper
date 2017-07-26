import java.util.Stack;
import java.lang.Integer;

public class ReverseStack {

    public static void main(String[] args) {
        Stack<Integer> stack = new Stack<Integer>();
        stack.push(3);
        stack.push(5);
        stack.push(1);
        reverse(stack);
    }

    private static Stack<Integer> reverse(Stack<Integer> data) {
        Stack<Integer> stack = new Stack<Integer>();
        for (int i = data.size(); i > 0; i--) {
            stack.push(data.pop());
        }
        return stack;
    }
}
