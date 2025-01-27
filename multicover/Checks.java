public class Checks {
    /* Benchmark 1: a_lt_b */
    public static boolean check_lt(int a, int b) {
        boolean rval = false;

        if (a < b) {
            Vtrace.start();
            Vtrace.capture("A_LT_B$a_lt_b_truebranch", a, b);
            rval = true;
        } else {
            Vtrace.start();
            Vtrace.capture("A_LT_B$a_lt_b_falsebranch", a, b);
            rval = false;
        }

        Vtrace.start();
        Vtrace.capture("A_LT_B$a_lt_b_end", a, b);
        return rval;
    }

    /* Benchmark 2: BEV2 */
    public static void BindExpandsVars2(int cp1_off, int n1, int n2, int MAXDATA) {
        if (MAXDATA < 0)
            return;
        if (n1 < 0)
            return;
        if (n2 < 0)
            return;
        if (cp1_off < 0)
            return;
        if (n1 > MAXDATA * 2)
            return;
        if (cp1_off > n1)
            return;
        if (n2 > MAXDATA * 2 - n1)
            return;

        Vtrace.start();
        Vtrace.capture("BindExpandsVars2$funcstart", cp1_off, n1, n2, MAXDATA);

        int mc_i;
        for (mc_i = 0; mc_i < n2; mc_i++) {
            Vtrace.start();
            Vtrace.capture("BindExpandsVars2$loopinvariant", mc_i, cp1_off, MAXDATA);
        }
    }

    /* Benchmark 3: Cars */
//    public static void cars(int unknown_int, boolean innerif, int v1, int v2, int v3) {
//        int x1 = 100;
//        int x2 = 75;
//        int x3 = -50;
//        int t  = 0;
//
//        boolean cond1 = (v3 >= 0);
//        boolean cond2 = (v1 <= 5);
//        boolean cond3 = (v1 - v3 >= 0);
//        boolean cond4 = (2 * v2 - v1 - v3 == 0);
//        boolean cond5 = (v2 + 5 >= 0);
//        boolean cond6 = (v2 <= 5);
//
//        if (cond1 && cond2 && cond3 && cond4 && cond5 && cond6) {
//            while (unknown_int > 0) {
//                boolean c1 = v2 + 5 >= 0;
//                boolean c2 = v2 <= 5;
//                if (!(c1 && c2)) break;
//
//                Vtrace.start();
//                Vtrace.capture("Cars$loopinv", v2);
//
//                if (innerif) {
//                    Vtrace.start();
//                    Vtrace.capture("Cars$loopinvtrue", x1, x2, x3);
//
//                    x1 = x1 + v1;
//                    x3 = x3 + v3;
//                    x2 = x2 + v2;
//                    v2 = v2 - 1;
//                    t = t + 1;
//                } else {
//                    Vtrace.start();
//                    Vtrace.capture("Cars$loopinvfalse", x1, x2, x3);
//
//                    x1 = x1 + v1;
//                    x3 = x3 + v3;
//                    x2 = x2 + v2;
//                    v2 = v2 + 1;
//                    t = t + 1;
//                }
//
//                unknown_int -= 1;
//            }
//        }
//
//        Vtrace.start();
//        Vtrace.capture("Cars$exitloop", v1, v2, v3, t);
//
//        // Original Assertions
//        // assert v1 <= 5 : "Assertion failed: v1 <= 5";
//        // assert 2 * v2 + 2 * t >= v1 + v3 : "Assertion failed: 2*v2 + 2*t >= v1 + v3";
//        // assert 5 * t + 75 >= x2 : "Assertion failed: 5*t + 75 >= x2";
//        // assert v2 <= 6 : "Assertion failed: v2 <= 6";
//        // assert v3 >= 0 : "Assertion failed: v3 >= 0";
//        // assert v2 + 6 >= 0 : "Assertion failed: v2 + 6 >= 0";
//        // assert x2 + 5 * t >= 75 : "Assertion failed: x2 + 5*t >= 75";
//        // assert v1 - 2 * v2 + v3 + 2 * t >= 0 : "Assertion failed: v1 - 2*v2 + v3 + 2*t >= 0";
//        // assert v1 - v3 >= 0 : "Assertion failed: v1 - v3 >= 0";
//    }
}
