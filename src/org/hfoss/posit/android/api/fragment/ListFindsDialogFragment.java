package org.hfoss.posit.android.api.fragment;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.database.DbManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;

public class ListFindsDialogFragment extends OrmLiteDialogFragment<DbManager> {
	protected int mNum;

	private static final int CONFIRM_DELETE_DIALOG = 0;
	
	public static ListFindsDialogFragment newInstance(int num) {
		ListFindsDialogFragment lfdf = new ListFindsDialogFragment();
		
		//Supply num input as argument
		Bundle args = new Bundle();
		args.putInt("num", num);
		lfdf.setArguments(args);
		
		return lfdf;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mNum = getArguments().getInt("num");
		
		switch (mNum) {
		case CONFIRM_DELETE_DIALOG:
			return new AlertDialog.Builder(getActivity()).setIcon(R.drawable.alert_dialog_icon)
			.setTitle(R.string.confirm_delete)
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// User clicked OK so do some stuff
					if (deleteAllFind()) {
						getActivity().finish();
					}
				}
			}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// User clicked cancel so do nothing
				}
			}).create();
		default:
			return null;
		}
	}
	
	protected boolean deleteAllFind() {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		int projectId = prefs.getInt(getString(R.string.projectPref), 0);
		boolean success = getHelper().deleteAll(projectId);
		if (success) {
			Toast.makeText(getActivity(), R.string.deleted_from_database, Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(getActivity(), R.string.delete_failed, Toast.LENGTH_SHORT).show();
		}
		return success;
	}
}
