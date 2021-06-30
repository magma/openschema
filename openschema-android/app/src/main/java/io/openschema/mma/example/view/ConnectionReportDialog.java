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

package io.openschema.mma.example.view;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.fragment.NavHostFragment;
import io.openschema.mma.example.databinding.ViewConnectionReportDialogBinding;

public class ConnectionReportDialog extends DialogFragment {

    public static String KEY_REPORT_DESCRIPTION;

    private ViewConnectionReportDialogBinding mBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = ViewConnectionReportDialogBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        mBinding.reportDialogCancelBtn.setOnClickListener(v -> dismiss());

        mBinding.reportDialogSubmitBtn.setOnClickListener(v -> {
            String reportDescription = mBinding.reportDialogFeedback.getText().toString();
            if (!reportDescription.equals("")) {
                NavHostFragment.findNavController(this).getPreviousBackStackEntry().getSavedStateHandle().set(KEY_REPORT_DESCRIPTION, reportDescription);
                dismiss();
            } else {
                //TODO: Listen to the user typing something to remove the error.
                mBinding.reportDialogFeedback.setError("Please describe your issue.");
            }
        });
    }
}
