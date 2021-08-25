package io.openschema.mma.utils;

public class QosInfo {

    private final String dnsServer;
    private final float[] individualDomainResult;
    private final float mean;
    private final float variance;
    private final int totalFailure;

    public QosInfo(String dnsServer, float[] individualDomainResult, float mean, int totalFailure){
        this.dnsServer = dnsServer;
        this.individualDomainResult = individualDomainResult;
        this.mean = mean;
        this.totalFailure = totalFailure;
        variance = calculateVariance();
    }

    private float calculateVariance(){
        float temp = 0;
        for(float a :individualDomainResult){
            temp += (a-mean)*(a-mean);
        }
        return temp/(individualDomainResult.length-1);
    }

    public String getDnsServer() {
        return dnsServer;
    }

    public float getMean() {
        return mean;
    }

    public float getVariance() {
        return variance;
    }

    public int getTotalFailure() {
        return totalFailure;
    }
}
