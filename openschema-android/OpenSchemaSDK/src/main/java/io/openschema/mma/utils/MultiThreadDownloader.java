/*
 * Copyright (c) 2020, The Magma Authors
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openschema.mma.utils;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;

public class MultiThreadDownloader {

    private static final String TAG = "MultiThreadDownloader";

    private static int flag = 0;
    public static String filePath = ""; //Downloads Android folder
    public static String fileUrl = "https://file-examples-com.github.io/uploads/2017/02/zip_10MB.zip"; //File address -> https://file-examples.com/
    public static int threadCount = 4; //Number of threads

    public MultiThreadDownloader(String filePath) {
        this.filePath = filePath + "/TestFile";
        Log.d(TAG,"Download folder Path: " + filePath);
    }
    
    public boolean executeDownLoad() throws Exception {
        try {
            URL url = new URL(fileUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(); //Create a connection object
            conn.setConnectTimeout(5000);//Set timeout
            conn.setRequestMethod("GET");//Set request method
            conn.setRequestProperty("Connection", "Keep-Alive");

            int code = conn.getResponseCode();
            Log.d(TAG,"Server Response Code: " + code);

            if(code != 200) {
                Log.d(TAG,"Invalid network address: " + fileUrl);
            }

            long fileLength = conn.getContentLength(); //Get file size
            getRemoteFileSize(fileUrl);
            Log.d(TAG,"Total file length:" + fileLength + " bytes");

            RandomAccessFile raf = new RandomAccessFile(filePath, "rwd");
            //Specifies the length of the file created
            raf.setLength(fileLength);
            raf.close();

            //Split file
            int blockSize = (int)(fileLength/threadCount); //Calculate the length of each thread
            final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount);
            final CountDownLatch latch = new CountDownLatch(threadCount);

            for (int threadId = 1; threadId <= threadCount; threadId++) {
                // Start location of each thread Download
                long startIndex = (threadId - 1) * blockSize;
                // End location of each thread Download
                long endIndex = startIndex + blockSize - 1;
                if (threadId == threadCount) {
                    //The length of the last thread download is a little longer
                    endIndex = fileLength;
                }

                Log.d(TAG,"thread " + threadId + "download: " + startIndex + "byte~" + endIndex + "byte");
                threadPoolExecutor.execute(new DownloadThread (filePath, fileUrl, "Thread " + threadId, startIndex, endIndex, latch));

            }

            latch.await();
            if(flag == 0){
                return true;
            }

        } catch(Exception e) {
            Log.d(TAG,"File download failed, Failure reason: " + e);
        }

        return false;
    }

    private long getRemoteFileSize(String remoteFileUrl) throws IOException {
        long fileSize = 0;
        HttpURLConnection httpConnection = (HttpURLConnection) new URL(remoteFileUrl).openConnection();
        httpConnection.setRequestMethod("HEAD");
        int responseCode = 0;
        try {
            responseCode = httpConnection.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (responseCode >= 400) {
            Log.d(TAG,"Web Server response error!");
            return 0;
        }
        String sHeader;
        for (int i = 1;; i++) {
            sHeader = httpConnection.getHeaderFieldKey(i);
            if (sHeader != null && sHeader.equals("Content-Length")) {
                fileSize = Long.parseLong(httpConnection.getHeaderField(sHeader));
                break;
            }
        }
        return fileSize;
    }

    public synchronized String downloadFile() {
        ReentrantLock lock = new ReentrantLock();
        lock.lock();

        String[] names = fileUrl.split("\\.");
        if (names == null || names.length <= 0) {
            return null;
        }

        String fileTypeName = names[names.length - 1];
        Log.d(TAG,"File type: " + fileTypeName);

        long startTime = System.currentTimeMillis();
        boolean isDownloadFinished = false;
        try{
            isDownloadFinished = executeDownLoad();
            long endTime = System.currentTimeMillis();
            if(isDownloadFinished){
                Log.d(TAG,"End of file download,Total time consuming " + (endTime - startTime)+ " ms");
                return filePath;
            }
            Log.d(TAG,"File download failed");
            return null;
        }catch (Exception e){
            Log.d(TAG, e.getMessage());
            return null;
        }finally {
            flag = 0; // Reset download status
            if(!isDownloadFinished){
                File file = new File(filePath);
                file.delete();
            }
            lock.unlock();
        }
    }
}
