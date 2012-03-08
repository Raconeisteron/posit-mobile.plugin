package org.hfoss.posit.android.api.fragment;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.activity.FindActivity;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.api.plugin.FindPluginManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.widget.Toast;

public class DeleteFindsDialogFragment extends OrmLiteDialogFragment<DbManager> {
	protected int mNum;

	public static final int CONFIRM_DELETE_ALL_FINDS_DIALOG = 0;
	public static final int CONFIRM_DELETE_FIND_DIALOG = 1;
	
	public static DeleteFindsDialogFragment newInstance(int num) {
		DeleteFindsDialogFragment f = new DeleteFindsDialogFragment();
		
		//Supply num input as argument
		Bundle args = new Bundle();
		args.putInt("num", num);
		f.setArguments(args);
		
		return f;
	}
	
	public static DeleteFindsDialogFragment newInstance(int num, int findID) {
		DeleteFindsDialogFragment f = new DeleteFindsDialogFragment();
		
		//Supply num input as argument
		Bundle args = new Bundle();
		args.putInt("num", num);
		args.putInt(Find.ORM_ID, findID);
		f.setArguments(args);
		
		return f;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mNum = getArguments().getInt("num");
		
		switch (mNum) {
		case CONFIRM_DELETE_ALL_FINDS_DIALOG:
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
		case CONFIRM_DELETE_FIND_DIALOG:
			return new AlertDialog.Builder(getActivity()).setIcon(
				R.drawable.alert_dialog_icon).setTitle(
				R.string.alert_dialog_2).setPositiveButton(
				R.string.alert_dialog_ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int whichButton) {
						// User clicked OK so do some stuff
						if (deleteFind()) {
							//TODO: Handle this better
							getActivity().finish();
						}
					}
				}).setNegativeButton(R.string.alert_dialog_cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int whichButton) {
						// User clicked cancel so do nothing
					}
				}).create();
		default:
			return null;
		}
	}

	protected boolean deleteFind() {
		int rows = 0;
		String guid = null;
		// Get the appropriate find class from the plugin manager and
		// make an instance of it.
		Class<Find> findClass = FindPluginManager.mFindPlugin.getmFindClass();
		Find find = null;

		try {
			find = findClass.newInstance();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (java.lang.InstantiationException e) {
			e.printStackTrace();
		}

		find.setId(getArguments().getInt(Find.ORM_ID));

		// store the guid of this find so that I can delete photos on phone
		find = getHelper().getFindById(find.getId());
		guid = find.getGuid();

		rows = getHelper().delete(find);

		if (rows > 0) {
			Toast.makeText(getActivity(), R.string.deleted_from_database,
					Toast.LENGTH_SHORT).show();

			// delete photo if it exists
			if (getActivity().deleteFile(guid)) {
				Log.i(TAG, "Image with guid: " + guid + " deleted.");
			}

//			this.startService(new Intent(this, ToDoReminderService.class));
		} else {
			Toast.makeText(getActivity(), R.string.delete_failed,
					Toast.LENGTH_SHORT).show();
		}

		return rows > 0;

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
