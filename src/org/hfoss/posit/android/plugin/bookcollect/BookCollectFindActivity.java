package org.hfoss.posit.android.plugin.bookcollect;

import org.hfoss.posit.android.api.activity.FindActivity;

import android.os.Bundle;

/**
 * FindActivity subclass for BookCollect plugin.
 *
 */
public class BookCollectFindActivity extends FindActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        find = new BookCollectFindFragment();
        super.onCreate(savedInstanceState);
    }
}
