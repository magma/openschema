package io.openschema.mma.utils;

import android.os.SystemClock;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.WorkerThread;

//TODO: change from static class to instantiated?
public class DnsTester {
    private static final String TAG = "DnsTester";
    private static final int TIMEOUT = 5000;
    private static final int DNS_PORT = 53;
    private static final Short QUERY_TYPE = 0x0001; //Type A

    private static final String[] TEST_DNS_SERVERS = {"8.8.8.8", "9.9.9.9", "1.1.1.1", "185.228.168.9", "76.76.19.19"};
    private static final byte[][] TEST_DOMAIN_REQUESTS;
    private static final int TEST_DOMAIN_COUNT = 10;
    private static String[] TEST_DOMAINS;

    static {
        TEST_DOMAINS = randomizeTestDomains();
        TEST_DOMAIN_REQUESTS = new byte[TEST_DOMAINS.length][0];
        for (int i = 0; i < TEST_DOMAINS.length; i++) {
            try {
                TEST_DOMAIN_REQUESTS[i] = buildQuestion(TEST_DOMAINS[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Generate a new set of random domains to be used in DNS tests. Used to prevent DNS servers from returning a cached response.
    public static void randomizeDomains() {
        TEST_DOMAINS = randomizeTestDomains();
    }

    //Test a list of specified DNS servers.
    @WorkerThread
    public static List<QosInfo> testServers(String[] dnsServers) throws InterruptedException {
        Log.d(TAG, "MMA: Starting DNS test on specified list of servers.");

        //Split DNS server tests into different threads
        final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        final CountDownLatch latch = new CountDownLatch(dnsServers.length);
        final List<QosInfo> testResults = Collections.synchronizedList(new ArrayList<>());

        for (final String dnsServer : dnsServers) {
            threadPoolExecutor.execute(() -> {
                try {
                    QosInfo testResult = requestAllDomains(dnsServer, threadPoolExecutor);
                    testResults.add(testResult);
                } catch (InterruptedException e) {
                    Log.d(TAG, "MMA: This DNS task was interrupted");
                } finally {
                    latch.countDown();
                }
            });
        }

        //Wait until all servers have completed their tests
        Log.d(TAG, "MMA: Waiting for DNS tests to complete...");
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.d(TAG, "MMA: Main DNS test latch was interrupted");

            //Make sure child threads are interrupted correctly before finishing interruption.
            threadPoolExecutor.shutdownNow();
            Log.d(TAG, "MMA: Awaiting threadpool shutdown...");
            threadPoolExecutor.awaitTermination(10, TimeUnit.SECONDS);

            //Continue interruption sequence.
            throw new InterruptedException();
        }

        return testResults;
    }

    //Test our default list DNS servers.
    @WorkerThread
    public static List<QosInfo> testDefaultServers() throws InterruptedException {
        return testServers(TEST_DNS_SERVERS);
    }

    //Run a test with every randomized test domains on the specified DNS server.
    private static QosInfo requestAllDomains(String dnsServer, ThreadPoolExecutor threadPoolExecutor) throws InterruptedException {
        //TODO: Implement Retries

        //Split DNS requests into separate threads
        final List<Long> individualValues = Collections.synchronizedList(new ArrayList<>());
        final AtomicInteger failures = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(TEST_DOMAINS.length);

        for (final byte[] requestQuestion : TEST_DOMAIN_REQUESTS) {
            threadPoolExecutor.execute(() -> {
                try {
                    //TODO: Is this working? Latch seems to be handling all cancelations automatically
                    if (Thread.currentThread().isInterrupted()) {
                        Log.d(TAG, "MMA: This DNS test was interrupted");
                        return;
                    }

                    individualValues.add(requestDomain(dnsServer, requestQuestion));
//                Log.d(TAG, "MMA: DNS RTT Result " + dnsServer + " on " + TEST_DOMAINS[i] + ": " + individualValues.get(individualValues.size() - 1));
                } catch (IOException e) {
                    failures.incrementAndGet();
                    Log.e(TAG, "MMA: DNS RTT Error " + dnsServer + ": " + e);
                } finally {
                    latch.countDown();
                }
            });
        }

        //Wait until all requests have returned
        latch.await();

        //TODO: cleanup test logs
//        Log.d(TAG, "MMA: DNS RTT values size " + dnsServer + ": " + individualValues.size());
//        QosInfo qosInfo = new QosInfo(dnsServer, individualValues, failures.get());
//        Log.d(TAG, "MMA: DNS RTT Min Result " + qosInfo.getDnsServer() + ": " + qosInfo.getMinRTTValue());
//        Log.d(TAG, "MMA: DNS RTT Average Result " + qosInfo.getDnsServer() + ": " + qosInfo.getRttMean());
//        Log.d(TAG, "MMA: DNS RTT variance " + qosInfo.getDnsServer() + ": " + qosInfo.getRttVariance());
//        Log.d(TAG, "MMA: DNS RTT failures " + qosInfo.getDnsServer() + ": " + qosInfo.getTotalFailedRequests());
//        Log.d(TAG, "MMA: DNS RTT SuccessRate " + qosInfo.getDnsServer() + ": " + qosInfo.getSuccessRate());
//        Log.d(TAG, "MMA: DNS RTT StdDev " + qosInfo.getDnsServer() + ": " + qosInfo.getRttStdDev());
        return new QosInfo(dnsServer, individualValues, failures.get());
    }

    //Make the DNS request to the specified DNS server using a specified domain.
    private static long requestDomain(String dnsServer, byte[] requestQuestion) throws IOException {
        //Request
        DatagramPacket requestPacket;
        if (dnsServer.contains(":")) {
            InetAddress ipv6Dns = InetAddress.getByName(dnsServer);
            requestPacket = new DatagramPacket(requestQuestion, requestQuestion.length, InetAddress.getByAddress(ipv6Dns.getAddress()), DNS_PORT);
        } else {
            requestPacket = new DatagramPacket(requestQuestion, requestQuestion.length, InetAddress.getByAddress(getServer(dnsServer)), DNS_PORT);
        }

        //Response
        DatagramPacket responsePacket;
        byte[] byteArray = new byte[1024];
        responsePacket = new DatagramPacket(byteArray, byteArray.length);

        //Operation
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(TIMEOUT);

        long startTime = SystemClock.elapsedRealtime();
        socket.send(requestPacket);
        socket.receive(responsePacket);
        long endTime = SystemClock.elapsedRealtime();
        socket.close();

        return (endTime - startTime);
    }

    private static int parseNumericAddress(String ipaddr) {
        //  Check if the string is valid
        if (ipaddr == null || ipaddr.length() < 7 || ipaddr.length() > 15)
            return 0;

        //  Check the address string, should be n.n.n.n format
        StringTokenizer token = new StringTokenizer(ipaddr, ".");
        if (token.countTokens() != 4)
            return 0;

        int ipInt = 0;

        while (token.hasMoreTokens()) {
            //  Get the current token and convert to an integer value
            String ipNum = token.nextToken();

            try {

                //  Validate the current address part
                int ipVal = Integer.valueOf(ipNum).intValue();
                if (ipVal < 0 || ipVal > 255)
                    return 0;

                //  Add to the integer address
                ipInt = (ipInt << 8) + ipVal;
            } catch (NumberFormatException ex) {
                return 0;
            }
        }

        //  Return the integer address
        return ipInt;
    }

    private static byte[] getServer(String address) {

        int ipInt = parseNumericAddress(address);
        if (ipInt == 0)
            return null;

        byte[] server = new byte[4];

        server[3] = (byte) (ipInt & 0xFF);
        server[2] = (byte) ((ipInt >> 8) & 0xFF);
        server[1] = (byte) ((ipInt >> 16) & 0xFF);
        server[0] = (byte) ((ipInt >> 24) & 0xFF);

        return server;
    }

    //Convert a domain from a String representation into a format ready to be sent as a request.
    private static byte[] buildQuestion(String domain) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        // *** Build a DNS Request Frame ****

        // Identifier: A 16-bit identification field generated by the device that creates the DNS query.
        // It is copied by the server into the response, so it can be used by that device to match that
        // query to the corresponding reply received from a DNS server. This is used in a manner similar
        // to how the Identifier field is used in many of the ICMP message types.
        dos.writeShort(0x0000);

        // Write Query Flags
        dos.writeShort(0x0100);

        // Question Count: Specifies the number of questions in the Question section of the message.
        dos.writeShort(0x0001);

        // Answer Record Count: Specifies the number of resource records in the Answer section of the message.
        dos.writeShort(0x0000);

        // Authority Record Count: Specifies the number of resource records in the Authority section of
        // the message. (“NS” stands for “name server”)
        dos.writeShort(0x0000);

        // Additional Record Count: Specifies the number of resource records in the Additional section of the message.
        dos.writeShort(0x0000);

        String[] domainParts = domain.split("\\.");

        for (String domainPart : domainParts) {
            byte[] domainBytes = domainPart.getBytes(StandardCharsets.UTF_8);
            dos.writeByte(domainBytes.length);
            dos.write(domainBytes);
        }

        // No more parts
        dos.writeByte(0x00);

        // Type 0x01 = A (Host Request)
        dos.writeShort(QUERY_TYPE);

        // Class 0x01 = IN
        dos.writeShort(0x0001);

        return baos.toByteArray();
    }

    //Generate a new set of test domains.
    private static String[] randomizeTestDomains() {
        String[] testDomains = new String[DnsTester.TEST_DOMAIN_COUNT];

        for (int i = 0; i < testDomains.length; i++) {
            testDomains[i] = generateRandomDomain();
        }

        return testDomains;
    }

    //Generate a random string to be used as a test domain.
    private static String generateRandomDomain() {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();
        int targetStringLength = random.nextInt(12 - 5 + 1) + 5;

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return generatedString + ".com";
    }
}
