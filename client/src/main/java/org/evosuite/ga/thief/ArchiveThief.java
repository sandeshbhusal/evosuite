package org.evosuite.ga.thief;

import org.evosuite.Properties;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.utils.LoggingUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import java.lang.String;

/*
 * This is not a part of my final thesis, but a PoC that shows that
 * you can keep stealing chromosomes from the original testsuite
 * and make a new testsuite out of the stolen chromosomes that can
 * satisfy our multicover criteria.
 * */
public class ArchiveThief {
    private static final int requiredCoverage = Properties.MULTICOVER_TARGET;
    private static int foundMinCoverage = 0;

    private static final HashMap<TestFitnessFunction, HashSet<TestChromosome>> coverageTracker = new HashMap<>();

    public static boolean isSat() {
        // This gets printed.
//        LoggingUtils.getEvoLogger().error("What the what");
        boolean rval = foundMinCoverage >= requiredCoverage;
        if (rval) {
            dumpTests("/tmp/check");
        }
        return rval;
    }

    public static void steal(TestFitnessFunction ff, TestChromosome tc) {
        HashSet<TestChromosome> chromosomes = coverageTracker.getOrDefault(ff, new HashSet<>());

        if (chromosomes.contains(tc)) {
            // Removed the logging for now.
//            LoggingUtils.getEvoLogger().warn("Adding a duplicate chromosome is not allowed");
//            LoggingUtils.getEvoLogger().warn("Trying to add a duplicate testcase: " + tc);
            return;
        }

        chromosomes.add(tc);
        coverageTracker.put(ff, chromosomes);

        foundMinCoverage = Math.min(foundMinCoverage, chromosomes.size());
    }

    public static void dumpTests(String path) {
        File outfile = new File(path);
        if (outfile.exists()) {
            // Try to delete this.
            if (!outfile.delete()) {
                throw new IllegalArgumentException("Cannot delete existing multicover tests");
            }
        }

        try {
            if (!outfile.createNewFile()) {
                throw new IllegalArgumentException("Cannot create a dump file to dump all tests");
            }

            FileWriter writer = new FileWriter(outfile);
            writer.write(ArchiveThief.getString());
            writer.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getString() {
        StringBuilder sb = new StringBuilder();
        for (TestFitnessFunction target: coverageTracker.keySet()) {
            sb.append("\n Target is: \n").append(target.toString()).append("\n");
            sb.append("-----------------\n");

            for (TestChromosome generatedCase: coverageTracker.get(target)) {
                sb.append(generatedCase.getTestCase().toString());
                sb.append("\n-----------------\n");
            }

            sb.append("\n Covered ").append(coverageTracker.get(target).size()).append(" times");
        }

        return sb.toString();
    }
}
