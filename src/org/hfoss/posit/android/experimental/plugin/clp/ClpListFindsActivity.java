package org.hfoss.posit.android.experimental.plugin.clp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.hfoss.posit.android.experimental.Constants;
import org.hfoss.posit.android.experimental.R;
import org.hfoss.posit.android.experimental.api.Camera;
import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.activity.ListFindsActivity;
import org.hfoss.posit.android.experimental.plugin.FindPluginManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class ClpListFindsActivity extends ListFindsActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	/**
	 * Sets up a custom list adapter specific to Clp finds.
	 */
	@Override
	protected ListAdapter setUpAdapter() {

		List<? extends Find> list = this.getHelper().getAllFinds();

		int resId = getResources().getIdentifier(FindPluginManager.mFindPlugin.mListFindLayout,
			    "layout", getPackageName());
		
		ClpFindsListAdapter adapter = new ClpFindsListAdapter(this,
				resId, list);

		return adapter;
	}

	/**
	 * Adapter for displaying finds, extends FindsListAdapter to 
	 * take care of displaying the extra fields in OutsideInFind.
	 * 
	 */
	private class ClpFindsListAdapter extends FindsListAdapter{

		public ClpFindsListAdapter(Context context, int textViewResourceId,
				List list) {
			super(context, textViewResourceId, list);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.list_row, null);
			}
			ClpFind find = (ClpFind)items.get(position);
			if (find != null) {
				TextView tv = null;

				tv = (TextView) v.findViewById(R.id.name);
				tv.setText(String.valueOf(find.getName()));

				tv = (TextView) v.findViewById(R.id.description_id);
				tv.setText(String.valueOf(find.getDescription()));
				
				tv = (TextView) v.findViewById(R.id.latitude);
				tv.setText(String.valueOf(find.getLatitude()));
				tv = (TextView) v.findViewById(R.id.longitude);
				tv.setText(String.valueOf(find.getLongitude()));
				tv = (TextView) v.findViewById(R.id.id);
				tv.setText(Integer.toString(find.getId()));
				tv = (TextView) v.findViewById(R.id.status);
				tv.setText(find.getStatusAsString());			
				tv = (TextView) v.findViewById(R.id.time);
				tv.setText(find.getTime().toLocaleString());
				
				//Display the thumbnail picture beside the find
				//or a default image if there isn't one
				ImageView iv = (ImageView) v.findViewById(R.id.find_image);
				Bitmap bmp = Camera.getPhotoAsBitmap(find.getGuid(), ClpListFindsActivity.this);
				if(bmp != null){
				    iv.setImageBitmap(bmp);
				}
				else{
				    iv.setImageResource(R.drawable.ic_menu_camera);
				}
			}
			return v;
		}
	}

}
