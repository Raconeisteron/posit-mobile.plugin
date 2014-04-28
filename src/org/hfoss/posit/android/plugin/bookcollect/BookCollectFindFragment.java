package org.hfoss.posit.android.plugin.bookcollect;

import java.util.ArrayList;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.fragment.FindFragment;
import org.hfoss.posit.android.api.plugin.AddFindPluginCallback;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.api.plugin.FunctionPlugin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

public class BookCollectFindFragment extends FindFragment {

	private static final String TAG = "BookCollectFindFragment";
	private static final String KEY_INDEX = "BookCollectFindFragment";

	private ArrayList<FunctionPlugin> mAddFindMenuPlugins = null;
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState)
	{
		Log.i(TAG, "onActivityCreated()");
		mAddFindMenuPlugins = FindPluginManager
				.getFunctionPlugins(FindPluginManager.ADD_FIND_MENU_EXTENSION);

        super.onActivityCreated(savedInstanceState);
	}

    /**
	 * Retrieves values from the BookCollectFind fields and stores them in a
	 * Find instance. 
	 * This method is invoked from the Save menu item. It also marks the find
	 * 'unsynced' so it will be updated to the server.
	 * 
	 * @return a new Find object with data from the view.
	 */
	@Override
	protected Find retrieveContentFromView()
	{
	    BookCollectFind bcfind =
	            (BookCollectFind)super.retrieveContentFromView();

		String value; //used to get the string from the textbox

		EditText eText = (EditText)getView().findViewById(R.id.titleEditText);
        value = eText.getText().toString();
        bcfind.setTitle(value);

		eText = (EditText)getView().findViewById(R.id.authorEditText);
        value = eText.getText().toString();
        bcfind.setAuthor(value);
        
		eText = (EditText)getView().findViewById(R.id.isbnEditText);
		if(eText.getText().toString().equals(""))
			value = "0";
		else
			value = eText.getText().toString();
		bcfind.setIsbn(Long.parseLong(value));

		return bcfind;
	}

	@Override
	protected void displayContentInView(Find find)
	{
	    Log.i(TAG, "displayContentInView()");
		BookCollectFind oiFind = (BookCollectFind)find;
		
		EditText et = (EditText)getView().findViewById(R.id.titleEditText);
		et.setText(oiFind.getTitle());
		
		et = (EditText)getView().findViewById(R.id.authorEditText);
		et.setText(oiFind.getAuthor());
		
		et = (EditText)getView().findViewById(R.id.isbnEditText);
		et.setText(Long.toString(oiFind.getIsbn()));
		
		TextView tv = (TextView)getView().findViewById(R.id.guidRealValueTextView);
		tv.setText(oiFind.getGuid());

		/**
		 * For each plugin, call its displayFindInViewCallback.
		 */
		for (FunctionPlugin plugin : mAddFindMenuPlugins) {
			Log.i(TAG, "plugin=" + plugin);
			Class<AddFindPluginCallback> callbackClass = null;
			Object o;
			try {
				View view = getView();
				String className = plugin.getAddFindCallbackClass();
				if (className != null) {
					callbackClass =
					    (Class<AddFindPluginCallback>) Class.forName(className);
					o = (AddFindPluginCallback) callbackClass.newInstance();
					((AddFindPluginCallback) o).displayFindInViewCallback(
							getActivity().getApplication(),
							find,
							view);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (java.lang.InstantiationException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void prepareForSave(Find find)
	{
		if (this.mCurrentLocation != null) {
			find.setLatitude(mCurrentLocation.getLatitude());
			find.setLongitude(mCurrentLocation.getLongitude());
		}
		super.prepareForSave(find);
	}
}
