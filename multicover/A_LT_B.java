public class A_LT_B {
    public static boolean check_lt(int a, int b) {
        boolean rval = false;


        if (a < b) {
            Vtrace.start();
            Vtrace.capture("a_lt_b_truebranch", a, b);

            rval = true;
        } else {
            Vtrace.start();
            Vtrace.capture("a_lt_b_falsebranch", a, b);

            rval = false;
        }

        Vtrace.start();
        Vtrace.capture("exitmethod_a_lt_b", a, b);

        return rval;
    }
}
