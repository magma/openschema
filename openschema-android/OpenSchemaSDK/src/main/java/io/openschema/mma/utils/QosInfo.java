package io.openschema.mma.utils;

public class QosInfo {

    private final String mDnsServer;
    private final long[] mRttValues;
    private final double mRttMean;
    private final float mRttVariance;
    private final int mTotalFailedRequests;

    public QosInfo(String dnsServer, long[] rttValues, int totalFailedRequests) {
        mDnsServer = dnsServer;
        mRttValues = rttValues;
        mRttMean = calculateMean();
        mTotalFailedRequests = totalFailedRequests;
        mRttVariance = calculateVariance();
    }

    private double calculateMean() {
        long sum = 0;
        for (long value : mRttValues) {
            sum += value;
        }
        return (double) sum / mRttValues.length;
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
}
