public class Ex1 {
    public static void ex1(int outerloop, int condition, int xa, int ya) {
        int x = 0;
        int y = 0;

        while (outerloop > 0) {
            x = xa + 2 * ya;
            y = -2 * xa + ya;

            x++;
            if (condition > 0) {
                y = y + x;
            } else {
                y = y - x;
            }

            xa = x - 2 * y;
            ya = 2 * x + y;

            outerloop -= 1;
        }

        Vtrace.start();
        Vtrace.capture("exitmethod", xa, ya);
    }
}

