package com.example.edgesecai.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class IsolationForestModel {
    private static final int NUM_TREES = 15;
    private static final int SUBSAMPLE_SIZE = 64;
    private static final int MAX_TREE_DEPTH = 8;
    private static final double EULER_CONSTANT = 0.5772156649;

    private final List<ITreeNode> trees = new ArrayList<>();
    private final Random random = new Random(42);
    private boolean isTrained = false;

    public IsolationForestModel() {
        trainWithBaselineData();
    }

    public synchronized void trainWithBaselineData() {
        trees.clear();
        List<double[]> baselineSamples = generateNominalBaselineDataset();

        for (int i = 0; i < NUM_TREES; i++) {
            List<double[]> subsample = drawSubsample(baselineSamples, SUBSAMPLE_SIZE);
            ITreeNode root = buildTree(subsample, 0, MAX_TREE_DEPTH);
            trees.add(root);
        }
        isTrained = true;
    }

    public double calculateAnomalyScore(double[] features) {
        if (!isTrained || trees.isEmpty()) return 0.5;

        double totalPathLength = 0;
        for (ITreeNode tree : trees) {
            totalPathLength += computePathLength(features, tree, 0);
        }
        double avgPathLength = totalPathLength / trees.size();
        double cFactor = computeAverageUnsuccessfulPath(SUBSAMPLE_SIZE);

        if (cFactor <= 0) return 0.0;

        // Anomaly score: s(x, n) = 2^(- E(h(x)) / c(n))
        double exponent = -(avgPathLength / cFactor);
        double score = Math.pow(2.0, exponent);

        // Bound to [0.0, 1.0]
        return Math.max(0.0, Math.min(1.0, score));
    }

    private double computePathLength(double[] point, ITreeNode node, int currentDepth) {
        if (node == null || node.isLeaf) {
            int size = node != null ? node.size : 1;
            return currentDepth + computeAverageUnsuccessfulPath(size);
        }

        int featureIdx = node.splitFeature;
        if (featureIdx < 0 || featureIdx >= point.length) {
            return currentDepth;
        }

        if (point[featureIdx] < node.splitValue) {
            return computePathLength(point, node.left, currentDepth + 1);
        } else {
            return computePathLength(point, node.right, currentDepth + 1);
        }
    }

    private ITreeNode buildTree(List<double[]> data, int currentDepth, int maxDepth) {
        if (currentDepth >= maxDepth || data.size() <= 1) {
            return new ITreeNode(data.size());
        }

        int numFeatures = data.get(0).length;
        int featureIdx = random.nextInt(numFeatures);

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double[] pt : data) {
            double v = pt[featureIdx];
            if (v < min) min = v;
            if (v > max) max = v;
        }

        if (Math.abs(max - min) < 1e-6) {
            return new ITreeNode(data.size());
        }

        double splitValue = min + random.nextDouble() * (max - min);

        List<double[]> leftData = new ArrayList<>();
        List<double[]> rightData = new ArrayList<>();

        for (double[] pt : data) {
            if (pt[featureIdx] < splitValue) {
                leftData.add(pt);
            } else {
                rightData.add(pt);
            }
        }

        if (leftData.isEmpty() || rightData.isEmpty()) {
            return new ITreeNode(data.size());
        }

        ITreeNode node = new ITreeNode(featureIdx, splitValue);
        node.left = buildTree(leftData, currentDepth + 1, maxDepth);
        node.right = buildTree(rightData, currentDepth + 1, maxDepth);
        return node;
    }

    private double computeAverageUnsuccessfulPath(int n) {
        if (n <= 1) return 0.0;
        if (n == 2) return 1.0;
        return 2.0 * (Math.log(n - 1) + EULER_CONSTANT) - (2.0 * (n - 1) / n);
    }

    private List<double[]> drawSubsample(List<double[]> source, int count) {
        List<double[]> sample = new ArrayList<>(count);
        int n = source.size();
        for (int i = 0; i < count; i++) {
            sample.add(source.get(random.nextInt(n)));
        }
        return sample;
    }

    private List<double[]> generateNominalBaselineDataset() {
        List<double[]> nominalData = new ArrayList<>();
        // Nominal mobile traffic profiles:
        // features: [normDuration, normPacketRate, normByteRate, normBytesPerPacket, normOutInRatio,
        //            normIatMean, normIatCV, normPayloadEntropy, normDnsEntropy, normSynAckRatio, normPortEntropy]
        for (int i = 0; i < 200; i++) {
            // Normal browsing / HTTPS session:
            nominalData.add(new double[]{
                    0.15 + (random.nextDouble() * 0.2), // Duration
                    0.05 + (random.nextDouble() * 0.1), // Packet rate (modest)
                    0.08 + (random.nextDouble() * 0.15),// Byte rate
                    0.40 + (random.nextDouble() * 0.3), // Bytes per packet (TLS payload)
                    0.10 + (random.nextDouble() * 0.1), // Out/In ratio (~1:3 to 1:5)
                    0.10 + (random.nextDouble() * 0.2), // IAT mean (~100-300ms)
                    0.60 + (random.nextDouble() * 0.3), // IAT CV (sporadic human activity: CV > 0.6)
                    0.45 + (random.nextDouble() * 0.2), // Payload entropy
                    0.35 + (random.nextDouble() * 0.1), // DNS entropy (~2.5-3.2 / 8)
                    0.05 + (random.nextDouble() * 0.05),// SYN/ACK ratio (1-to-1)
                    0.02 + (random.nextDouble() * 0.02) // Port entropy (mostly single target port 443/80)
            });

            // Normal idle background ping / push notification socket:
            nominalData.add(new double[]{
                    0.80 + (random.nextDouble() * 0.15),// Long persistent connection
                    0.01 + (random.nextDouble() * 0.02),// Very low packet rate
                    0.01 + (random.nextDouble() * 0.01),// Low bytes
                    0.10 + (random.nextDouble() * 0.1), // Small keep-alive packets
                    0.15 + (random.nextDouble() * 0.1),
                    0.70 + (random.nextDouble() * 0.2), // High IAT (10-30s keep-alives)
                    0.75 + (random.nextDouble() * 0.2),
                    0.30 + (random.nextDouble() * 0.15),
                    0.20 + (random.nextDouble() * 0.1),
                    0.05 + (random.nextDouble() * 0.05),
                    0.01 + (random.nextDouble() * 0.01)
            });
        }
        return nominalData;
    }

    private static class ITreeNode {
        int splitFeature = -1;
        double splitValue = 0.0;
        int size = 0;
        boolean isLeaf = false;
        ITreeNode left = null;
        ITreeNode right = null;

        ITreeNode(int size) {
            this.size = size;
            this.isLeaf = true;
        }

        ITreeNode(int splitFeature, double splitValue) {
            this.splitFeature = splitFeature;
            this.splitValue = splitValue;
            this.isLeaf = false;
        }
    }
}
