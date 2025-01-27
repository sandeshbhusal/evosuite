public class TriangleCheck {
    public boolean check_triangle(int a, int b, int c) {
        boolean rval = false;
        int ab = a + b;
        int ac = a + c;
        int bc = b + c;

        if (ab > c) {
            if (ac > b) {
                if (bc > a) {
                    Vtrace.start();
                    Vtrace.capture("triangle_ok", a, b, c, ab, ac, bc);
                    rval = true;
                } else {
                    Vtrace.start();
                    Vtrace.capture("triangle_not_ok_ab_ac_ok", a, b, c, ab, ac, bc);

                    rval = false;
                }
            } else {
                Vtrace.start();
                Vtrace.capture("triangle_not_ok_ab_ok", a, b, c, ab, ac, bc);
                rval = false;
            }
        } else {
            Vtrace.start();
            Vtrace.capture("triangle_not_ok", a, b, c, ab, ac, bc);
            rval = false;
        }
        return rval;
    }
}
