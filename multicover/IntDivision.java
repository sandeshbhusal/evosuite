public class IntDivision {
    public void dividie(int a, int b) {
        if (a >= b) {
            int q = 0;
            int r = a;

            Vtrace.start();
            Vtrace.capture("entermethod", a, b);

            while (r >= b) {
                Vtrace.start();
                Vtrace.capture("loopcondition", a, b, q, r);

                r -= b;
                q += 1;
            }

            Vtrace.start();
            Vtrace.capture("div_end", a, b, q, r);
        }
    }
}
