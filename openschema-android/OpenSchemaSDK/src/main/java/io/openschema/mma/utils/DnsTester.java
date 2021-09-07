package io.openschema.mma.utils;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import androidx.annotation.WorkerThread;

public class DnsTester {
    private static final String TAG = "DnsTester";
    private static final int TIMEOUT = 200;
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

    //Test a single specified DNS server.
    @WorkerThread
    public static QosInfo testServer(String dnsServer) {
        Log.d(TAG, "MMA: Starting DNS test specified server.");
        return requestAllDomains(dnsServer);
    }

    //Test a list of specified DNS servers.
    @WorkerThread
    public static List<QosInfo> testServers(String[] dnsServers) {
        Log.d(TAG, "MMA: Starting DNS test on specified list of servers.");
        List<QosInfo> testResults = new ArrayList<>();
        for (int i = 0; i < dnsServers.length; i++) {
            testResults.add(requestAllDomains(dnsServers[i]));
        }
        return testResults;
    }

    //Test our default list DNS servers.
    @WorkerThread
    public static List<QosInfo> testDefaultServers() {
        Log.d(TAG, "MMA: Starting DNS test on our default list of servers.");
        List<QosInfo> testResults = new ArrayList<>();
        for (int i = 0; i < TEST_DNS_SERVERS.length; i++) {
            testResults.add(requestAllDomains(TEST_DNS_SERVERS[i]));
        }
        return testResults;
    }

    //Run a test with every randomized test domains on the specified DNS server.
    private static QosInfo requestAllDomains(String dnsServer) {
        //TODO: Implement Retries

        ArrayList<Long> individualValues = new ArrayList<Long>();
        int failures = 0;

        for (int i = 0; i < TEST_DOMAINS.length; i++) {
            try {
                individualValues.add(requestDomain(dnsServer, TEST_DOMAIN_REQUESTS[i]));
                Log.d(TAG, "MMA: DNS RTT Result " + dnsServer + " on " + TEST_DOMAINS[i] + ": " + individualValues.get(individualValues.size()-1));
            } catch (IOException e) {
                failures++;
                Log.e(TAG, "MMA: DNS RTT Error " + dnsServer + ": " + e);
            }
        }

        Log.d(TAG, "MMA: DNS RTT values size " + dnsServer + ": " + individualValues.size());
        QosInfo qosInfo = new QosInfo(dnsServer, individualValues, failures);
        Log.d(TAG, "MMA: DNS RTT Min Result " + qosInfo.getDnsServer() + ": " + qosInfo.getMinRTTValue());
        Log.d(TAG, "MMA: DNS RTT Average Result " + qosInfo.getDnsServer() + ": " + qosInfo.getRttMean());
        Log.d(TAG, "MMA: DNS RTT variance " + qosInfo.getDnsServer() + ": " + qosInfo.getRttVariance());
        Log.d(TAG, "MMA: DNS RTT failures " + qosInfo.getDnsServer() + ": " + qosInfo.getTotalFailedRequests());
        Log.d(TAG, "MMA: DNS RTT SuccessRate " + qosInfo.getDnsServer() + ": " + qosInfo.getSuccessRate());
        Log.d(TAG, "MMA: DNS RTT StdDev " + qosInfo.getDnsServer() + ": " + qosInfo.getRttStdDev());
        return qosInfo;
    }

    //Make the DNS request to the specified DNS server using a specified domain.
    private static long requestDomain(String dnsServer, byte[] requestQuestion) throws IOException {
        //Request
        DatagramPacket requestPacket;
        if(dnsServer.contains(":")){
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

        long startTime = System.currentTimeMillis();
        socket.send(requestPacket);
        socket.receive(responsePacket);
        long endTime = System.currentTimeMillis();
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

        for (int i = 0; i < domainParts.length; i++) {
            byte[] domainBytes = domainParts[i].getBytes(StandardCharsets.UTF_8);
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
