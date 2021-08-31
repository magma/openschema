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

package io.openschema.client.view;

import android.content.Context;
import android.net.NetworkCapabilities;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.BindingAdapter;
import androidx.databinding.BindingMethod;
import androidx.databinding.BindingMethods;
import io.openschema.client.R;
import io.openschema.client.databinding.ViewNetworkQualityBinding;


/**
 * Custom View used to display the primary network's status and quality.
 */
@BindingMethods({
                        @BindingMethod(type = android.widget.ImageView.class,
                                       attribute = "android:tint",
                                       method = "setImageTint"),
                })
public class NetworkQualityView extends ConstraintLayout {

    private static final String TAG = "NetworkQualityView";

    private ViewNetworkQualityBinding mBinding;

    public NetworkQualityView(@NonNull Context context) { this(context, null, 0); }
    public NetworkQualityView(@NonNull Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }
    public NetworkQualityView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (!isInEditMode()) {
            init(context);
        } else {
            inflate(context, R.layout.view_network_quality, this);
        }
    }

    private void init(Context context) {
        mBinding = ViewNetworkQualityBinding.inflate(LayoutInflater.from(context), this, true);
        mBinding.setNetworkData(new NetworkStatus(NetworkCapabilities.TRANSPORT_WIFI, 3));
    }

    public void setNetworkData(NetworkStatus networkStatus) {
        mBinding.setNetworkData(networkStatus);
    }

    @BindingAdapter("app:tint")
    public static void setImageTint(ImageView view, @ColorInt int color) {
        view.setColorFilter(color);
    }

    public static class NetworkStatus {
        public enum NetworkQuality {
            WEAK,
            GOOD,
            VERY_GOOD,
            EXCELLENT;

            @NonNull
            @Override
            public String toString() {
                return this.name().substring(0, 1) + this.name().substring(1).toLowerCase().replace("_", " ");
            }
        }

        public int mTransportType;
        public NetworkQuality mNetworkQuality;

        public NetworkStatus(int transportType, double qualityScore) {
            mTransportType = transportType;
            mNetworkQuality = convertQualityScore(qualityScore);
        }

        private NetworkQuality convertQualityScore(double qualityScore) {
            if (qualityScore > 4) {
                return NetworkQuality.EXCELLENT;
            } else if (qualityScore > 3) {
                return NetworkQuality.VERY_GOOD;
            } else if (qualityScore > 2) {
                return NetworkQuality.GOOD;
            } else {
                return NetworkQuality.WEAK;
            }
        }

        public String getNetworkQuality() {
            return mNetworkQuality.toString();
        }
    }
}
