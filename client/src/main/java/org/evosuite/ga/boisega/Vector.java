package org.evosuite.ga.boisega;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Vector {
    int[] data;

    public Vector(int[] data) {
        if (data == null)
            throw new IllegalArgumentException("Data cannot be null");
        this.data = data.clone(); // Prevent external modification
    }

    public Vector(List<Integer> data) {
        if (data == null)
            throw new IllegalArgumentException("Data cannot be null");
        this.data = data.stream().mapToInt(i -> i).toArray();
    }

    public int[] getData() {
        return data.clone();
    }
    
    // Count the number of unique items
    public double internalDiversity() {
        HashSet<Integer> uniqueItems = new HashSet<>();
        for (int i = 0; i < data.length; i ++) {
            uniqueItems.add(data[i]);
        }
        return (double) uniqueItems.size() / data.length;
    }

    public Vector add(Vector other) {
        checkLengths(other);
        int[] result = new int[data.length];
        for (int i = 0; i < data.length; i++)
            result[i] = data[i] + other.data[i];
        return new Vector(result);
    }

    public Vector subtract(Vector other) {
        checkLengths(other);
        int[] result = new int[data.length];
        for (int i = 0; i < data.length; i++)
            result[i] = data[i] - other.data[i];
        return new Vector(result);
    }

    public int dotProduct(Vector other) {
        checkLengths(other);
        int sum = 0;
        for (int i = 0; i < data.length; i++)
            sum += data[i] * other.data[i];
        return sum;
    }

    public double magnitude() {
        double sum = 0;
        for (int value : data)
            sum += value * value;
        return Math.sqrt(sum);
    }

    private void checkLengths(Vector other) {
        if (data.length != other.data.length)
            throw new IllegalArgumentException("Vectors must be of the same length");
    }

    @Override
    public String toString() {
        return Arrays.toString(data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Vector vector = (Vector) o;
        return Arrays.equals(data, vector.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
