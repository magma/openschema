package io.openschema.client.fragment;

import android.content.Intent;
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
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import io.openschema.client.R;
import io.openschema.client.databinding.FragmentAboutBinding;

public class AboutFragment extends Fragment {

    private static final String TAG = "AboutFragment";

    private FragmentAboutBinding mBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinding = FragmentAboutBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mBinding.aboutTos.setMovementMethod(LinkMovementMethod.getInstance());
        mBinding.aboutTos.setText(getSpannableString(R.string.about_terms_link), TextView.BufferType.SPANNABLE);

        mBinding.aboutPrivacyPolicy.setMovementMethod(LinkMovementMethod.getInstance());
        mBinding.aboutPrivacyPolicy.setText(getSpannableString(R.string.about_privacy_policy_link), TextView.BufferType.SPANNABLE);
    }

    private SpannableString getSpannableString(@StringRes int stringId) {
        SpannedString tosText = (SpannedString) getText(stringId);
        Annotation[] annotations = tosText.getSpans(0, tosText.length(), Annotation.class);
        SpannableString spannableString = new SpannableString(tosText);
        for (Annotation annotation : annotations) {
            if (annotation.getKey().equals("link")) {
                String tosLink = annotation.getValue();
                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(tosLink));
                        Log.d(TAG, "UI: Opening link in browser: " + tosLink);
                        startActivity(browserIntent);
                    }
                };

                spannableString.setSpan(clickableSpan, tosText.getSpanStart(annotation), tosText.getSpanEnd(annotation), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return spannableString;
    }
}
