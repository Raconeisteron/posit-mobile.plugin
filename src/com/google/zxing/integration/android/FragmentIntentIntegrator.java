package com.google.zxing.integration.android;

import android.content.Intent;
import android.support.v4.app.Fragment;

public class FragmentIntentIntegrator extends IntentIntegrator {
    private final Fragment fragment;

    public FragmentIntentIntegrator(Fragment fragment) {
        super(fragment.getActivity());
        this.fragment = fragment;
    }

    @Override
    protected void startActivityForResult(Intent intent, int code) {
        fragment.startActivityForResult(intent, code);
    }
}
