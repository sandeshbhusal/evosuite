public class BEV2 {
    public static void test(int cp1_off, int n1, int n2, int MAXDATA) {
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
        Vtrace.capture("startingconds", cp1_off, n1, n2, MAXDATA);

        int mc_i;
        for (mc_i = 0; mc_i < n2; mc_i++) {
            Vtrace.start();
            Vtrace.capture("loopinvariant", mc_i, cp1_off, MAXDATA);
        }
    }
}
