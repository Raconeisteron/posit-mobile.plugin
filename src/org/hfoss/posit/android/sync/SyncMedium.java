package org.hfoss.posit.android.sync;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbHelper;
import org.hfoss.posit.android.api.plugin.FindPlugin;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.functionplugin.sms.ObjectCoder;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

public abstract class SyncMedium {
	private static final String TAG = "SyncMedium";
	
	protected Context m_context;
	protected int m_projectId;
	protected String m_authKey;
	
	public void sync( String authToken ) {
		m_authKey = authToken;
		getFinds();
		sendFinds();
	}
	
	public void getFinds(){
		List<String> findGuids = getFindsNeedingSync();
		
		for( String guid : findGuids ){
			String rawFind = retrieveRawFind(guid );
			Find newFind = convertRawToFind( rawFind );
			addFind( newFind );
		}
	}
	
	public void sendFinds(){
		List<Find> changedFinds = getChangedFinds();

		for( Find find : changedFinds ){
			sendFind( find );
		}
		
		postSendTasks();
	}
	
	protected abstract List<String> getFindsNeedingSync();
	protected abstract String retrieveRawFind( String guid );
	protected abstract boolean sendFind( Find find );
	protected abstract boolean postSendTasks();
	
	public Find convertRawToFind( String rawFind ){
		Find newFind 		= createTypedFind();
		Bundle bundle 		= newFind.getDbEntries();
		List<String> keys 	= getBundleKeys( bundle );
		List<String> values = parseValuesFromRaw( rawFind );
		boolean success		= false;
		
		if( validateNewDataSize( keys, values ) )
			success = fillBundleValues( bundle, keys, values, newFind );
		
		if( success )
			newFind.updateObject(bundle);
		else
			newFind = null;
		
		return newFind;
	}

	public String convertFindToRaw( Find find ){
		return convertBundleToRaw( find.getDbEntries() );
	}

	/**
	 * Adds the contents of an ENTIRE Find to be transmitted later. Also returns
	 * the raw message. The format of the message is as follows: <br>
	 * <br>
	 * 
	 * (prefix)value1,value2,value3,... <br>
	 * <br>
	 * 
	 * The values are, in lexicographical order of the attribute names, strings
	 * encoding the attributes' values. These encodings are handled by
	 * ObjectCoder.
	 * 
	 * @param dbEntries
	 *            A Bundle object containing all of a Find's database fields.
	 * @param phoneNumber
	 *            The phone number that the Find should be transmitted to
	 * @return A String containing the text message.
	 * @throws IllegalArgumentException
	 *             if one of the values could not be encoded.
	 * 
	 */
	public String convertBundleToRaw( Bundle dbEntries )
			throws IllegalArgumentException {
		List<String> keys = new ArrayList<String>(dbEntries.keySet());
		StringBuilder builder = new StringBuilder();
		
		Collections.sort(keys);
		
		for (String key : keys) {
			if (builder.length() > 0)
				builder.append(",");
			String code = ObjectCoder.encode(dbEntries.get(key));
			if (code != null) {
				builder.append(code);
			} else {
				Log.e(TAG, "Tried to encode object of unsupported type.");
				throw new IllegalArgumentException();
			}
		}
		String text = builder.toString();
		return text;
	}
	
	private List<String> parseValuesFromRaw( String rawFind ){
		List<String> values = new ArrayList<String>();
		StringBuilder current = new StringBuilder();
		
		for (int i = 0; i < rawFind.length(); i++) {
			char c = rawFind.charAt(i);
			if (c == ObjectCoder.ESCAPE_CHAR) {
				current.append(c);
				if (i + 1 < rawFind.length())
					c = rawFind.charAt(++i);
				current.append(c);
			} else if (c == ',') {
				values.add(current.toString());
				current = new StringBuilder();
			} else {
				current.append(c);
			}
		}
		values.add(current.toString());
		
		return values;
	}
	
	private Find createTypedFind(){
		Find find;
		
		try {
			FindPlugin plugin = FindPluginManager.mFindPlugin;
			if (plugin == null) {
				Log.e(TAG, "Could not retrieve Find Plugin.");
				return null;
			}
			find = plugin.getmFindClass().newInstance();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		} catch (InstantiationException e) {
			e.printStackTrace();
			return null;
		}
		
		return find;
	}
	
	private List<String> getBundleKeys( Bundle bundle ){
		List<String> keys = new ArrayList<String>(bundle.keySet());
		Collections.sort(keys);
		return keys;
	}
	
	private boolean validateNewDataSize( List<String> keys, List<String> values ){
		boolean valid = true;
		if (values.size() != keys.size()) {
			Log.e(TAG,
					"Received value set does not have expected size. values = "
							+ values.size() + ", keys = " + keys.size());
			valid = false;
		}
		
		return valid;
	}
	
	private boolean fillBundleValues( Bundle bundle, List<String> keys, List<String> values, Find newFind ){
		boolean success = true;
		
		for (int i = 0; i < values.size(); i++) {
			String key = keys.get(i);
			String value = values.get(i);
			Class<Object> type = getEntryType( newFind, key );
			
			if( type != null ){
				Serializable obj = null;
				
				try {
					obj = (Serializable) ObjectCoder.decode( value, type );
				} catch (IllegalArgumentException e) {
					Log.e(TAG, "Failed to decode value for attribute \"" + key
							+ "\", string was \"" + value + "\"");
					success = false;
				}
				
				if( success )
					bundle.putSerializable(key, obj);
			}
			else{
				success = false;
			}
		}
		
		return success;
	}

	private Class<Object> getEntryType( Find newFind, String key ){
		Class<Object> type = null;
		try {
			type = newFind.getType(key);
		} catch (NoSuchFieldException e) {
			Log.e(TAG, "Encountered no such field exception on field: "
					+ key);
			e.printStackTrace();
		}
		
		return type;
	}

	public void addFind( Find newFind ){
		Find find = DbHelper.getDbManager(m_context).getFindByGuid(newFind.getGuid());
		if (find != null) {
			Log.i("SyncMedium", "Updating existing find: " + find.getId());
			//TODO:Update find with newFind Do you even need to change this???
			DbHelper.getDbManager(m_context).updateWithoutHistory(newFind);				
		} else {
			Log.i("SyncMedium", "Inserting new find: " + newFind.getId());
			Log.i("SyncMedium", "Adding a new find " + newFind);
			
			DbHelper.getDbManager(m_context).insertWithoutHistory(newFind);
		}
	}
	
	public List<Find> getChangedFinds(){
		return DbHelper.getDbManager(m_context).getChangedFinds(m_projectId);
	}
}
