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

package io.openschema.client.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import io.openschema.client.R;
import io.openschema.client.activity.OnboardingActivity;
import io.openschema.client.databinding.FragmentTosBinding;
import io.openschema.client.util.SharedPreferencesHelper;

public class TosFragment extends Fragment {

    private static final String TAG = "TosFragment";

    private FragmentTosBinding mBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinding = FragmentTosBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        SpannedString tosText = (SpannedString) getText(R.string.terms_of_service_txt);
        Annotation[] annotations = tosText.getSpans(0, tosText.length(), Annotation.class);
        SpannableString spannableString = new SpannableString(tosText);
        for (Annotation annotation : annotations) {
            if (annotation.getKey().equals("link")) {
                String tosLink = annotation.getValue();
                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(tosLink));
                        Log.d(TAG, "UI: Opening ToS in browser");
                        startActivity(browserIntent);
                    }
                };

                spannableString.setSpan(clickableSpan, tosText.getSpanStart(annotation), tosText.getSpanEnd(annotation), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        mBinding.tosDescription.setMovementMethod(LinkMovementMethod.getInstance());
        mBinding.tosDescription.setText(spannableString, TextView.BufferType.SPANNABLE);

        mBinding.tosContinueBtn.setOnClickListener(v -> {
            acceptTos();
            continueToNextPage();
        });
    }

    private void continueToNextPage() {
        ((OnboardingActivity) requireActivity()).loadNextPage();
    }

    private void acceptTos() {
        SharedPreferences sharedPreferences = SharedPreferencesHelper.getInstance(requireContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(SharedPreferencesHelper.KEY_TOS_ACCEPTED, true);
        editor.apply();
    }
}