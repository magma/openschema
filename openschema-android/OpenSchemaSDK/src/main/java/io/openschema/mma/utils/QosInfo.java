package io.openschema.mma.utils;

import java.util.Arrays;

public class QosInfo {

    private final String mDnsServer;
    private final long[] mRttValues;
    private final double mRttMean;
    private final float mRttVariance;
    private final int mTotalFailedRequests;
    private final long mMinRTTValue;

    public QosInfo(String dnsServer, long[] rttValues, int totalFailedRequests) {
        mDnsServer = dnsServer;
        mRttValues = rttValues;
        mRttMean = calculateMean();
        mTotalFailedRequests = totalFailedRequests;
        mRttVariance = calculateVariance();
        mMinRTTValue = getMinRTT();
    }

    private long getMinRTT() {
        return Arrays.stream(mRttValues)
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

    public String getDnsServer() {
        return mDnsServer;
    }

    public double getRttMean() {
        return mRttMean;
    }

    public float getRttVariance() {
        return mRttVariance;
    }

    public int getTotalFailedRequests() {
        return mTotalFailedRequests;
    }

    public long getMinRTTValue() { return mMinRTTValue; }
}
