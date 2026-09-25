package com.packsmart.service.engine;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * TOPSIS (Technique for Order of Preference by Similarity to Ideal Solution). Pure.
 *
 * <pre>
 * r_ij = x_ij / sqrt(Σ_i x_ij²)                      (vector normalisation per criterion j)
 * v_ij = w_j × r_ij                                   (weighted)
 * A⁺_j = max_i v_ij (benefit) | min_i v_ij (cost)     (ideal best)
 * A⁻_j = min_i v_ij (benefit) | max_i v_ij (cost)     (ideal worst)
 * D⁺_i = sqrt(Σ_j (v_ij − A⁺_j)²),  D⁻_i = sqrt(Σ_j (v_ij − A⁻_j)²)
 * C_i  = D⁻_i / (D⁺_i + D⁻_i)                         (closeness, 0..1, higher = better)
 * </pre>
 */
@Service
public class TopsisRanker {

    /** One decision criterion: its weight and whether larger values are better. */
    public record Criterion(String name, double weight, boolean benefit) {
    }

    /**
     * Closeness coefficient C_i for every alternative (rows) over the criteria (columns).
     * A single alternative, or alternatives that are identical on every criterion, get 1.0.
     */
    public List<Double> closeness(List<double[]> matrix, List<Criterion> criteria) {
        int n = matrix.size();
        int m = criteria.size();
        List<Double> out = new ArrayList<>(n);
        if (n == 0) {
            return out;
        }
        double[][] v = new double[n][m];
        for (int j = 0; j < m; j++) {
            double norm = 0;
            for (double[] row : matrix) {
                norm += row[j] * row[j];
            }
            norm = Math.sqrt(norm);
            for (int i = 0; i < n; i++) {
                v[i][j] = norm == 0 ? 0 : criteria.get(j).weight() * matrix.get(i)[j] / norm;
            }
        }
        double[] best = new double[m];
        double[] worst = new double[m];
        for (int j = 0; j < m; j++) {
            double max = Double.NEGATIVE_INFINITY;
            double min = Double.POSITIVE_INFINITY;
            for (int i = 0; i < n; i++) {
                max = Math.max(max, v[i][j]);
                min = Math.min(min, v[i][j]);
            }
            boolean benefit = criteria.get(j).benefit();
            best[j] = benefit ? max : min;
            worst[j] = benefit ? min : max;
        }
        for (int i = 0; i < n; i++) {
            double dPlus = 0;
            double dMinus = 0;
            for (int j = 0; j < m; j++) {
                dPlus += Math.pow(v[i][j] - best[j], 2);
                dMinus += Math.pow(v[i][j] - worst[j], 2);
            }
            dPlus = Math.sqrt(dPlus);
            dMinus = Math.sqrt(dMinus);
            out.add(dPlus + dMinus == 0 ? 1.0 : dMinus / (dPlus + dMinus));
        }
        return out;
    }
}
