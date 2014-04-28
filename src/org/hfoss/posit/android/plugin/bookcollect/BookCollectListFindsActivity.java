package org.hfoss.posit.android.plugin.bookcollect;

import org.hfoss.posit.android.api.activity.ListFindsActivity;

import android.os.Bundle;

public class BookCollectListFindsActivity extends ListFindsActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        finds = new BookCollectListFindsFragment();
        super.onCreate(savedInstanceState);
    }
}
