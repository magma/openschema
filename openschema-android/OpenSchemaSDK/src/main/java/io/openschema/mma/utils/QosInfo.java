package io.openschema.mma.utils;

import java.util.Arrays;
import java.util.ArrayList;

public class QosInfo {

    private final String mDnsServer;
    private long[] mRttValues;
    private double mRttMean;
    private float mRttVariance;
    private final int mTotalFailedRequests;
    private long mMinRTTValue;
    private double mRttStdDev;
    private final double mSuccessRate;

    public QosInfo(String dnsServer, long[] rttValues, int totalFailedRequests) {
        mDnsServer = dnsServer;
        mRttValues = rttValues;
        mRttMean = calculateMean();
        mTotalFailedRequests = totalFailedRequests;
        mRttVariance = calculateVariance();
        mMinRTTValue = getMinRTT();
        mRttStdDev = calculateStandardDeviation();
        mSuccessRate = calculateSuccessRate();
    }

    private long getMinRTT() {
        return Arrays.stream(mRttValues)
                .filter(value -> value != 0)
                .min()
                .orElse(mRttValues[0]);
    }

    private double calculateMean() {
        return Arrays.stream(mRttValues)
                .average()
                .orElse(Double.NaN);
    }

    private float calculateVariance() {
        float temp = 0;
        for (float a : mRttValues) {
            temp += (a - mRttMean) * (a - mRttMean);
        }
        return temp / (mRttValues.length - 1);
    }

    private double calculateStandardDeviation() {
        return Math.sqrt(mRttVariance);
    }

    private double calculateSuccessRate() {
        return (mRttValues.length - mTotalFailedRequests)/mRttValues.length;
    }

    private void deleteArrayItem(int elementPosition) {

        long[] tempRttValues = new long[mRttValues.length - 1];

        for(int i = 0, j = 0; i < mRttValues.length; i++) {
            if(i == elementPosition) continue;
            tempRttValues[j++] = mRttValues[i];
        }

        mRttValues = tempRttValues;
    }

    public void cleanData() {

        double plusStdDev = mRttMean + mRttStdDev;
        double minusStdDev = mRttMean - mRttStdDev;

        long[] tempRttValues = mRttValues;

        for (int i = 0; i < tempRttValues.length; i++) {
            if(tempRttValues[i] > plusStdDev || tempRttValues[i] < minusStdDev) deleteArrayItem(i);
        }

        mRttMean = calculateMean();
        mMinRTTValue = getMinRTT();
        mRttVariance = getRttVariance();

    }

    public String getDnsServer() { return mDnsServer; }

    public double getRttMean() { return mRttMean; }

    public float getRttVariance() { return mRttVariance; }

    public double getRttStdDev() { return mRttStdDev; }

    public int getTotalFailedRequests() { return mTotalFailedRequests; }

    public long getMinRTTValue() { return mMinRTTValue; }

    public double getSuccessRate() { return mSuccessRate; }

    public long[] getRttValues() { return mRttValues;}
}
