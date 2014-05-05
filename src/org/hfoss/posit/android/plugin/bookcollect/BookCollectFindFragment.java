package org.hfoss.posit.android.plugin.bookcollect;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.fragment.FindFragment;
import org.hfoss.posit.android.api.plugin.AddFindPluginCallback;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.api.plugin.FunctionPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.FragmentIntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class BookCollectFindFragment extends FindFragment {

	private static final String TAG = "BookCollectFindFragment";
	private static final String KEY_INDEX = "BookCollectFindFragment";

	private ArrayList<FunctionPlugin> mAddFindMenuPlugins = null;
	private EditText mTitleText, mAuthorText, mIsbnText;
	private Button mScanBtn;
	private TextView mGuidText;
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState)
	{
		Log.i(TAG, "onActivityCreated()");
		mAddFindMenuPlugins = FindPluginManager
				.getFunctionPlugins(FindPluginManager.ADD_FIND_MENU_EXTENSION);

        super.onActivityCreated(savedInstanceState);
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        View rootView
            = super.onCreateView(inflater, container, savedInstanceState);
        
        mTitleText = (EditText)rootView.findViewById(R.id.titleEditText);
        mAuthorText = (EditText)rootView.findViewById(R.id.authorEditText);
        mIsbnText = (EditText)rootView.findViewById(R.id.isbnEditText);
        
        mScanBtn = (Button)rootView.findViewById(R.id.scanButton);
        mScanBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v)
            {
                FragmentIntentIntegrator scanIntegrator
                = new FragmentIntentIntegrator(BookCollectFindFragment.this);
                scanIntegrator.initiateScan();
            }
        });
        
        mGuidText = (TextView)rootView.findViewById(R.id.guidRealValueTextView);
        
        return rootView;
    }

    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);
        
        IntentResult scanningResult 
        = FragmentIntentIntegrator
            .parseActivityResult(requestCode, resultCode, intent);
        
        if (scanningResult != null) { // we have a result
            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();
            
            if (scanContent != null && scanFormat != null
                    && scanFormat.equalsIgnoreCase("EAN_13")) { // book search
                mScanBtn.setTag(scanContent);
                String bookSearchString =
                        "https://www.googleapis.com/books/v1/volumes?" +
                        "q=isbn:" + scanContent + "&key="
                        + getResources().getString(R.string.apikey);
                
                new GetBookInfo().execute(bookSearchString);
                
            } else {
                Toast toast
                    = Toast.makeText(getActivity().getApplicationContext(),
                        "Not a book", Toast.LENGTH_SHORT);
                toast.show();
            }
            
            Log.v(TAG, "content: " + scanContent + " - format: " + scanFormat);
        }
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

        value = mTitleText.getText().toString();
        bcfind.setTitle(value);

        value = mAuthorText.getText().toString();
        bcfind.setAuthor(value);
        
		if(mIsbnText.getText().toString().equals(""))
			value = "0";
		else
			value = mIsbnText.getText().toString();
		bcfind.setIsbn(Long.parseLong(value));

		return bcfind;
	}

	@Override
	protected void displayContentInView(Find find)
	{
	    Log.i(TAG, "displayContentInView()");
		BookCollectFind oiFind = (BookCollectFind)find;
		
		mTitleText.setText(oiFind.getTitle());
		mAuthorText.setText(oiFind.getAuthor());
		mIsbnText.setText(Long.toString(oiFind.getIsbn()));
		mGuidText.setText(oiFind.getGuid());

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
	
	private class GetBookInfo extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... bookURLs)
        {
            StringBuilder bookBuilder = new StringBuilder();
            for (String bookSearchURL : bookURLs) { // search URLs
                Log.i(TAG, "URL: " + bookSearchURL);
                HttpClient bookClient = new DefaultHttpClient();
                try {
                    HttpGet bookGet = new HttpGet(bookSearchURL);
                    HttpResponse bookResponse = bookClient.execute(bookGet);
                    StatusLine bookSearchStatus = bookResponse.getStatusLine();
                    if (bookSearchStatus.getStatusCode() == 200) {
                        // we have a result
                        HttpEntity bookEntity = bookResponse.getEntity();
                        InputStream bookContent = bookEntity.getContent();
                        InputStreamReader bookInput
                            = new InputStreamReader(bookContent);
                        BufferedReader bookReader
                            = new BufferedReader(bookInput);
                        
                        String lineIn;
                        while ((lineIn = bookReader.readLine()) != null)
                            bookBuilder.append(lineIn);
                    } else {
                        Log.e(TAG, "Response code = "
                                + bookSearchStatus.getStatusCode());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            return bookBuilder.toString();
        }
	    
        @Override
        protected void onPostExecute(String result)
        {
            try {
                JSONObject resultObject = new JSONObject(result);
                JSONArray bookArray = resultObject.getJSONArray("items");
                JSONObject bookObject = bookArray.getJSONObject(0);
                JSONObject volumeObject = bookObject.getJSONObject("volumeInfo");
                
                String title;
                try {
                    title = volumeObject.getString("title");
                } catch (JSONException jse) {
                    title = "";
                    jse.printStackTrace();
                }
                
                try {
                    mTitleText.setText(title + ": "
                            + volumeObject.getString("subtitle"));
                } catch (JSONException jse) {
                    if (!title.equals(""))
                        mTitleText.setText(title);
                    jse.printStackTrace();
                }
                
                StringBuilder authorBuild = new StringBuilder("");
                try {
                    JSONArray authorArray = volumeObject.getJSONArray("authors");

                    Log.i(TAG, "authorArray = " + authorArray.toString());
                    for (int i = 0; i < authorArray.length(); i++) {
                        if (i > 0)
                            authorBuild.append(", ");
                        authorBuild.append(authorArray.getString(i));
                    }
                    mAuthorText.setText(authorBuild.toString());
                } catch (JSONException jse) {
                    jse.printStackTrace();
                }
                
                if (mScanBtn.getTag() != null)
                    mIsbnText.setText(mScanBtn.getTag().toString());
            } catch (Exception e) { // invalid search result
                Log.e(TAG, "", e);
                e.printStackTrace();
                Toast toast
                    = Toast.makeText(getActivity().getApplicationContext(),
                        "Book not found", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
	}
}
