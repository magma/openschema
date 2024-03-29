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

package io.openschema.client.util;

import java.sql.Timestamp;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.concurrent.TimeUnit;

public class FormattingUtils {

    //Converts bytes to a SI formatted String
    public static String humanReadableByteCountSI(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }

    //Converts time duration to a HH:MM:SS formatted String
    public static String humanReadableTime(long durationMillis) {
        if (durationMillis < 1000) {
            return String.format("%02dms", durationMillis);
        } else if (durationMillis < 1000 * 10) { //Less than 10 seconds
            return String.format("%2ds", TimeUnit.MILLISECONDS.toSeconds(durationMillis));
        } else if (durationMillis < 1000 * 60) {
            return String.format("%02ds", TimeUnit.MILLISECONDS.toSeconds(durationMillis));
        } else if (durationMillis < 1000 * 60 * 60) {
            return String.format("%02dm:%02ds",
                    TimeUnit.MILLISECONDS.toMinutes(durationMillis),
                    TimeUnit.MILLISECONDS.toSeconds(durationMillis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationMillis)));
        } else {
            return String.format("%02dh:%02dm:%02ds",
                    TimeUnit.MILLISECONDS.toHours(durationMillis),
                    TimeUnit.MILLISECONDS.toMinutes(durationMillis) -
                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(durationMillis)),
                    TimeUnit.MILLISECONDS.toSeconds(durationMillis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationMillis)));
        }
    }

    //Converts a timestamp to a date formatted String
    public static String humanReadableDate(long timestamp) {
        return new Timestamp(timestamp).toString();
    }
}
