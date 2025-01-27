public class Cars {
    public static void cars(int unknown_int, boolean innerif, int v1, int v2, int v3) {
        int x1 = 100;
        int x2 = 75;
        int x3 = -50;
        int t  = 0;

        boolean cond1 = (v3 >= 0);
        boolean cond2 = (v1 <= 5);
        boolean cond3 = (v1 - v3 >= 0);
        boolean cond4 = (2 * v2 - v1 - v3 == 0);
        boolean cond5 = (v2 + 5 >= 0);
        boolean cond6 = (v2 <= 5);

        // Beginning assertions.
        // if these are false, we don't even enter the loop.

        if (cond1 && cond2 && cond3 && cond4 && cond5 && cond6) {
            while (unknown_int > 0) {
                boolean c1 = v2 + 5 >= 0;
                boolean c2 = v2 <= 5;
                if (!(c1 && c2)) break;

                // assume(v2 + 5 >= 0);
                // assume(v2 <= 5);

                Vtrace.start();
                Vtrace.capture("loopinv", v2);

                if (innerif) {
                    //assume(2 * x2 - x1 - x3 >= 0);

                    Vtrace.start();
                    Vtrace.capture("loopinvtrue", x1, x2, x3);

                    x1 = x1 + v1;
                    x3 = x3 + v3;
                    x2 = x2 + v2;
                    v2 = v2 - 1;
                    t = t + 1;
                } else {
                    //assume(2 * x2 - x1 - x3 <= 0);

                    Vtrace.start();
                    Vtrace.capture("loopinvfalse", x1, x2, x3);

                    x1 = x1 + v1;
                    x3 = x3 + v3;
                    x2 = x2 + v2;
                    v2 = v2 + 1;
                    t = t + 1;
                }

                unknown_int -= 1;
            }
        }

        // datagen_guard_end
        ;

        // Run instrumentation at the end, and in the loop for v2.


        Vtrace.start();
        Vtrace.capture("exitloop", v1, v2, v3, t);

        // Original Assertions
        // assert v1 <= 5 : "Assertion failed: v1 <= 5";
        // assert 2 * v2 + 2 * t >= v1 + v3 : "Assertion failed: 2*v2 + 2*t >= v1 + v3";
        // assert 5 * t + 75 >= x2 : "Assertion failed: 5*t + 75 >= x2";
        // assert v2 <= 6 : "Assertion failed: v2 <= 6";
        // assert v3 >= 0 : "Assertion failed: v3 >= 0";
        // assert v2 + 6 >= 0 : "Assertion failed: v2 + 6 >= 0";
        // assert x2 + 5 * t >= 75 : "Assertion failed: x2 + 5*t >= 75";
        // assert v1 - 2 * v2 + v3 + 2 * t >= 0 : "Assertion failed: v1 - 2*v2 + v3 + 2*t >= 0";
        // assert v1 - v3 >= 0 : "Assertion failed: v1 - v3 >= 0";
    }
}

