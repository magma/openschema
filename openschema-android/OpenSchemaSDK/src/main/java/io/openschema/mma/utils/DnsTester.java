package io.openschema.mma.utils;

import android.util.Log;

import com.google.common.primitives.Bytes;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.lang.NumberFormatException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.StringTokenizer;

public class DnsPing {

    private static final String TAG = "DNS Test";
    private static final int timeout = 5000;
    private static final int dnsPort = 53;
    private static final Short queryType = 0x0001; //Type A

    private static String[] testDomains = {"google.com", "youtube.com", "facebook.com", "youku.com", "adobe.com", "news.yandex.ru"};
    private static byte[][] testDomainsQuestions;
    private static byte[] requestHeader;

    private final Executor executor;

    public DnsPing(Executor executor){

        this.executor = executor;
        requestHeader = getRequestHeader();
        testDomainsQuestions = new byte[testDomains.length][0];
        for(int i = 0; i < testDomains.length; i++) {
            try {
                testDomainsQuestions[i] = getRequestQuestion(testDomains[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void dnsTest(String dnsServer) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    requestAllDomains(dnsServer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    private static float requestAllDomains(String dnsServer) throws IOException {
        //TODO: Implement Retries
        try {
            float result = 0f;
            for (int i = 0; i < testDomains.length; i++) {
                result += requestDomain(dnsServer, dnsFrame(testDomains[i]));
            }
            Log.d(TAG, "DNS Ping Result: " + Float.toString(result/testDomains.length));
            return result/testDomains.length;
        } catch (Exception e) {
            Log.d(TAG, "DNS Ping Error: " + e);
            return -1;
        }
    }

    private static float requestDomain(String dnsServer, byte[] requestQuestion) throws IOException {
        //Request
        DatagramPacket requestPacket;
        /*ByteBuffer byteBuffer = ByteBuffer.allocate(requestHeader.length + requestQuestion.length);
        byteBuffer.put(requestHeader);
        byteBuffer.put(requestQuestion);*/

        //requestPacket = new DatagramPacket(byteBuffer.array(), byteBuffer.array().length, InetAddress.getByAddress(getServer(dnsServer)), dnsPort);
        requestPacket = new DatagramPacket(requestQuestion, requestQuestion.length, InetAddress.getByAddress(getServer(dnsServer)), dnsPort);

        //Response
        DatagramPacket responsePacket;
        byte [] byteArray = new byte[1024];
        responsePacket = new DatagramPacket(byteArray, byteArray.length);

        //Operation
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(timeout);

        long startTime = System.nanoTime();
        String packetAsString = new String(requestPacket.getData(), 0, requestPacket.getLength());
        Log.d(TAG, "DNS Request Packet: " + packetAsString);
        socket.send(requestPacket);
        socket.receive(responsePacket);
        packetAsString = new String(responsePacket.getData(), 0, responsePacket.getLength());
        Log.d(TAG, "DNS Response Packet: " + packetAsString);
        long endTime = System.nanoTime();
        socket.close();

        long queryDuration = (endTime - startTime);  //divide by 1000000 to get milliseconds.

        return queryDuration/1000000;
    }

    private final static int parseNumericAddress(String ipaddr) {

        //  Check if the string is valid

        if ( ipaddr == null || ipaddr.length() < 7 || ipaddr.length() > 15)
            return 0;

        //  Check the address string, should be n.n.n.n format

        StringTokenizer token = new StringTokenizer(ipaddr,".");
        if ( token.countTokens() != 4)
            return 0;

        int ipInt = 0;

        while ( token.hasMoreTokens()) {

            //  Get the current token and convert to an integer value

            String ipNum = token.nextToken();

            try {

                //  Validate the current address part

                int ipVal = Integer.valueOf(ipNum).intValue();
                if ( ipVal < 0 || ipVal > 255)
                    return 0;

                //  Add to the integer address

                ipInt = (ipInt << 8) + ipVal;
            }
            catch (NumberFormatException ex) {
                return 0;
            }
        }

        //  Return the integer address

        return ipInt;
    }

    private static byte[] getServer(String address) {

        int ipInt = parseNumericAddress(address);
        if ( ipInt == 0)
            return null;

        byte [] server = new byte[4];

        server[3] = (byte) (ipInt & 0xFF);
        server[2] = (byte) ((ipInt >> 8) & 0xFF);
        server[1] = (byte) ((ipInt >> 16) & 0xFF);
        server[0] = (byte) ((ipInt >> 24) & 0xFF);

        return server;
    }

    private static byte[] dnsFrame(String domain) throws IOException {

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
        System.out.println(domain + " has " + domainParts.length + " parts");

        for (int i = 0; i<domainParts.length; i++) {
            System.out.println("Writing: " + domainParts[i]);
            byte[] domainBytes = domainParts[i].getBytes("UTF-8");
            dos.writeByte(domainBytes.length);
            dos.write(domainBytes);
        }

        // No more parts
        dos.writeByte(0x00);

        // Type 0x01 = A (Host Request)
        dos.writeShort(0x0001);

        // Class 0x01 = IN
        dos.writeShort(0x0001);

        byte[] dnsFrame = baos.toByteArray();

        return dnsFrame;
    }

    private static byte[] getRequestHeader() {

        ByteBuffer header = ByteBuffer.allocate(12);

        header.putShort((short)0x0000); //Transaction ID
        header.putShort((short)0x0100); //Flags
        header.putShort((short)0x0001); //Question RR
        header.putShort((short)0x0000); //Answer RR
        header.putShort((short)0x0000); //Authority RR
        header.putShort((short)0x0000); //Additional RR

        return header.array();
    }

    private static byte[] getRequestQuestion(String domain) throws IOException {
        //TODO: Should we iterate then domain twice instead of using a List? Is it too expensive?
        List<Byte> questionName = new ArrayList<>();

        for (String word : domain.split("\\.")) {
            questionName.add(((Integer)(word.length())).byteValue()); //length of value

            for(char letter : word.toCharArray()) {
                questionName.add((byte)letter); //value
            }

        }
        questionName.add((byte)0x00); //Question name end

        ByteBuffer question = ByteBuffer.allocate(questionName.size() + 4);
        byte[] bytes = Bytes.toArray(questionName);
        Log.d(TAG,"Given Array List: " + questionName.size());
        question.put(bytes); //Question name
        question.putShort(queryType); //Question Type
        question.putShort((short)0x0001); //Question class

        return question.array();
    }
}
