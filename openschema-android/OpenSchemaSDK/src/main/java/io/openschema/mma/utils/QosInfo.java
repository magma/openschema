package io.openschema.mma.utils;

import android.util.Log;
import java.util.ArrayList;

public class QosInfo {

    private static final String TAG = "QosInfo";

    private final String mDnsServer;
    private final ArrayList<Long> mRttValues;
    private double mRttMean;
    private float mRttVariance;
    private final int mTotalFailedRequests;
    private long mMinRTTValue;
    private double mRttStdDev;
    private final float mSuccessRate;

    public QosInfo(String dnsServer, ArrayList<Long> rttValues, int totalFailedRequests) {
        mDnsServer = dnsServer;
        mRttValues = rttValues;
        mTotalFailedRequests = totalFailedRequests;
        mRttMean = calculateMean();
        mMinRTTValue = getMinRTT();
        mRttVariance = calculateVariance();
        mRttStdDev = calculateStandardDeviation();
        mSuccessRate = calculateSuccessRate();
        Log.d(TAG, "QoSInfo object created successfully for DNS: " + mDnsServer);
    }

    private long getMinRTT() {
        return  mRttValues.stream()
                .mapToLong(d -> d)
                .filter(d -> d != 0)
                .min()
                .orElse(0);
    }

    private double calculateMean() {
        return mRttValues.stream()
                .mapToDouble(d -> d)
                .average()
                .orElse(0.0);
    }

    private float calculateVariance() {
        float temp = 0;
        for (Long a : mRttValues) {
            temp += (a - mRttMean) * (a - mRttMean);
        }

        if(temp == 0) return 0;
        return temp / (mRttValues.size() - 1);
    }

    private double calculateStandardDeviation() {
        if(mRttVariance == 0.0) return 0.0;
        return Math.sqrt(mRttVariance);
    }

    private float calculateSuccessRate() {
        if(mRttValues.isEmpty()) return 0;
        return (mRttValues.size() - mTotalFailedRequests)/mRttValues.size();
    }

    public void cleanData() {

        double plusStdDev = mRttMean + mRttStdDev;
        double minusStdDev = mRttMean - mRttStdDev;

        Log.d(TAG, "Cleaning DNS: " + mDnsServer);
        Log.d(TAG, "Mean + StdDev: " + plusStdDev);
        Log.d(TAG, "Mean - StdDev: " + minusStdDev);

        for (int i = 0; i < mRttValues.size(); i++) {
            if(mRttValues.get(i) > plusStdDev || mRttValues.get(i) < minusStdDev) {
                Log.d(TAG, "Value to be deleted:" + mRttValues.get(i));
                mRttValues.remove(i);
            }
        }

        Log.d(TAG, "Finished Cleaning DNS: " + mDnsServer);
        mRttMean = calculateMean();
        mMinRTTValue = getMinRTT();
        mRttVariance = getRttVariance();
        mRttStdDev = getRttStdDev();

    }

    public String getDnsServer() { return mDnsServer; }

    public double getRttMean() { return mRttMean; }

    public float getRttVariance() { return mRttVariance; }

    public double getRttStdDev() { return mRttStdDev; }

    public int getTotalFailedRequests() { return mTotalFailedRequests; }

    public long getMinRTTValue() { return mMinRTTValue; }

    public double getSuccessRate() { return mSuccessRate; }

    public ArrayList<Long> getRttValues() { return mRttValues;}
}
