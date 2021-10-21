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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

public class DownloadThread implements Runnable {

    private static final String TAG = "MultiThreadDownloader DownloadThread";

    private String filePath;
    private String urlPath;
    private String threadName;
    private long startIndex;
    private long endIndex;
    private CountDownLatch latch;

    public DownloadThread(String filePath,String urlPath,String threadName,long startIndex,long endIndex, CountDownLatch latch) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.filePath = filePath;
        this.urlPath = urlPath;
        this.threadName = threadName;
        this.latch = latch;
    }

    @Override
    public void run() {
          try {
              Log.d(TAG, threadName + " is downloading...");
              URL url = new URL(urlPath);
              HttpURLConnection conn = (HttpURLConnection) url.openConnection();
              conn.setRequestProperty("Connection", "Keep-Alive");
              conn.setRequestMethod("GET");
              conn.setRequestProperty("Range", "bytes=" + startIndex + "-" + endIndex);
              conn.setConnectTimeout(5000);

              int code = conn.getResponseCode();
              Log.d(TAG, threadName + "request return code = " + code);
              InputStream is = conn.getInputStream();//Return resources
              RandomAccessFile raf = new RandomAccessFile(filePath, "rwd");
              //Where to start when writing files at random
              raf.seek(startIndex);//Location file
              int len = 0;
              byte[] buffer = new byte[1024];
              while ((len = is.read(buffer)) != -1) {
                  raf.write(buffer, 0, len);
              }

              is.close();
              raf.close();
              Log.d(TAG, threadName + " Download completed");

          } catch (MalformedURLException e) {
              e.printStackTrace();
          } catch (IOException e) {
              e.printStackTrace();
          } finally {
              latch.countDown();
          }
    }
}

