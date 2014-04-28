package org.hfoss.posit.android.plugin.bookcollect;

import java.util.ArrayList;
import java.util.List;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.fragment.FindFragment;
import org.hfoss.posit.android.api.fragment.ListFindsFragment;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.api.plugin.FunctionPlugin;
import org.hfoss.posit.android.api.plugin.ListFindPluginCallback;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

public class BookCollectListFindsFragment extends ListFindsFragment {
    
	@Override
    protected void displayFind(int index, String action, Bundle extras,
            FindFragment findFragment)
    {
        super.displayFind(index, action, extras, new BookCollectFindFragment());
    }

    /**
	 * Sets up a custom list adapter specific to BookCollect finds.
	 */
	@Override
	protected ListAdapter setUpAdapter()
	{

		List<? extends Find> list = this.getHelper().getAllFinds();

		int resId = getResources().getIdentifier(
		        FindPluginManager.mFindPlugin.mListFindLayout,
			    "layout", getActivity().getPackageName());
		
		BookCollectFindsListAdapter adapter =
		        new BookCollectFindsListAdapter(getActivity(), resId, list);

		return adapter;
	}

	/**
	 * Adapter for displaying finds, extends FindsListAdapter to 
	 * take care of displaying the extra fields in BookCollectFind.
	 * 
	 */
	private class BookCollectFindsListAdapter extends FindsListAdapter {
        private Context context;

		public BookCollectFindsListAdapter(Context context,
		        int textViewResourceId, List list)
		{
			super(context, textViewResourceId, list);
		    this.context = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater)getActivity()
				    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.bookcollect_list_row, null);
			}
			BookCollectFind find = (BookCollectFind)items.get(position);
			if (find != null) {
				TextView tv = (TextView) v.findViewById(R.id.latitude);
				tv.setText(String.valueOf(find.getLatitude()));
				tv = (TextView) v.findViewById(R.id.longitude);
				tv.setText(String.valueOf(find.getLongitude()));
				tv = (TextView) v.findViewById(R.id.title);
				tv.setText(String.valueOf(find.getTitle()));
				tv = (TextView) v.findViewById(R.id.author);
				tv.setText(String.valueOf(find.getAuthor()));
				tv = (TextView) v.findViewById(R.id.id);
				tv.setText(Integer.toString(find.getId()));
				tv = (TextView) v.findViewById(R.id.synced);
				tv.setText(find.getStatusAsString());
				tv = (TextView) v.findViewById(R.id.time);
				tv.setText(find.getTime().toLocaleString());
			}

            ArrayList<FunctionPlugin> plugins = FindPluginManager.getFunctionPlugins();
            
            // Call each plugin's callback method to update view
            for (FunctionPlugin plugin: plugins) {
//					Log.i(TAG, "Call back for plugin=" + plugin);
                Class<ListFindPluginCallback> callbackClass = null;
                Object o;
                try {
                    String className = plugin.getListFindCallbackClass();
                    if (className != null) {
                        callbackClass = (Class<ListFindPluginCallback>) Class.forName(className);
                        o = (ListFindPluginCallback) callbackClass.newInstance();
                        ((ListFindPluginCallback) o).listFindCallback(context,find,v);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (java.lang.InstantiationException e) {
                    e.printStackTrace();
                }
            }
			
			return v;
		}
	}
}
