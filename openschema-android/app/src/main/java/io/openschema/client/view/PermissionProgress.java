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
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import io.openschema.client.R;
import io.openschema.client.databinding.ViewPermissionsProgressBinding;
import io.openschema.client.util.PermissionManager;


/**
 * Custom View used to filter displayed metrics by time windows.
 */
public class PermissionProgress extends ConstraintLayout {

    private static final String TAG = "PermissionProgress";

    private ViewPermissionsProgressBinding mBinding;

    public PermissionProgress(@NonNull Context context) { this(context, null, 0); }
    public PermissionProgress(@NonNull Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }
    public PermissionProgress(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (!isInEditMode()) {
            init(context);
        } else {
            inflate(context, R.layout.view_permissions_progress, this);
        }
    }

    private void init(Context context) {
        mBinding = ViewPermissionsProgressBinding.inflate(LayoutInflater.from(context), this, true);
        mBinding.setPermissionStatus(new PermissionStatus(context));
    }

    public static class PermissionStatus {
        public boolean mUsageGranted;
        public boolean mPhoneGranted;
        public boolean mLocationGranted;

        public PermissionStatus(Context context) {
            mUsageGranted = PermissionManager.isUsagePermissionGranted(context);
            mPhoneGranted = PermissionManager.isPhonePermissionGranted(context);
            mLocationGranted = PermissionManager.isLocationPermissionGranted(context) &&
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || PermissionManager.isBackgroundLocationPermissionGranted(context));
        }
    }
}
