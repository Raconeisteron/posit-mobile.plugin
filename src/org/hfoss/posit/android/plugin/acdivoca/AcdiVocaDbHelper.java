/*
 * File: AcdiVocaDbHelper.java
 * 
 * Copyright (C) 2011 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of the ACDI/VOCA plugin for POSIT, Portable Open Search 
 * and Identification Tool.
 *
 * This plugin is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License (LGPL) as published 
 * by the Free Software Foundation; either version 3.0 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU LGPL along with this program; 
 * if not visit http://www.gnu.org/licenses/lgpl.html.
 * 
 */

package org.hfoss.posit.android.plugin.acdivoca;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.sql.Date;
import java.sql.SQLException;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * The class is the interface with the Database. 
 *  It controls all Db access 
 *  and directly handles all Db queries.
 */
public class AcdiVocaDbHelper extends OrmLiteSqliteOpenHelper  {

	private static final String TAG = "DbHelper";

	private static final String DATABASE_NAME ="posit";
	public static final int DATABASE_VERSION = 2;

	// the DAO objects we use to access the Db tables
	private Dao<AcdiVocaUser, Integer> avUser = null;
	private static Dao<AcdiVocaFind, Integer> acdiVocaFind = null;
	private Dao<AcdiVocaMessage, Integer> acdiVocaMessage = null;
	
	/**
	 * Constructor just saves and opens the Db. The Db
	 * is closed in the public methods.
	 * @param context
	 */
	public AcdiVocaDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	/**
	 * Invoked automatically if the Database does not exist.
	 */
	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
		try {
			Log.i(TAG, "onCreate");

			AcdiVocaUser.createTable(connectionSource, getAvUserDao());
			AcdiVocaFind.createTable(connectionSource, getAcdiVocaFindDao());

			
			// Message Table
			TableUtils.createTable(connectionSource, AcdiVocaMessage.class);
			
		} catch (SQLException e) {
			Log.e(TAG, "Can't create database", e);
			throw new RuntimeException(e);
		}
		
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		try {
			Log.i(TAG, "onUpgrade");
			TableUtils.dropTable(connectionSource, AcdiVocaUser.class, true);
			TableUtils.dropTable(connectionSource, AcdiVocaFind.class, true);
			TableUtils.dropTable(connectionSource, AcdiVocaMessage.class, true);
			// after we drop the old databases, we create the new ones
			onCreate(db, connectionSource);
		} catch (SQLException e) {
			Log.e(TAG, "Can't drop databases", e);
			throw new RuntimeException(e);
		}
	}
		
	/**
	 * Returns the Database Access Object (DAO) for the AcdiVocaUser class. 
	 * It will create it or just give the cached value.
	 */
	public Dao<AcdiVocaUser, Integer> getAvUserDao() throws SQLException {
		if (avUser == null) {
			avUser = getDao(AcdiVocaUser.class);
		}
		return avUser;
	}
	
	/**
	 * Returns the Database Access Object (DAO) for the AcdiVocaFind class. 
	 * It will create it or just give the cached value.
	 */
	public Dao<AcdiVocaFind, Integer> getAcdiVocaFindDao() throws SQLException {
		if (acdiVocaFind == null) {
			acdiVocaFind = getDao(AcdiVocaFind.class);
		}
		return acdiVocaFind;
	}
	
	/**
	 * Returns the Database Access Object (DAO) for the AcdiVocaFind class. 
	 * It will create it or just give the cached value.
	 */
	public Dao<AcdiVocaMessage, Integer> getAcdiVocaMessageDao() throws SQLException {
		if (acdiVocaMessage == null) {
			acdiVocaMessage = getDao(AcdiVocaMessage.class);
		}
		return acdiVocaMessage;
	}
	
	/**
	 * For the message table.
	 */
	public static final String MESSAGE_TABLE = "sms_message_log";
	public static final String MESSAGE_ID = "_id";
	public static final int UNKNOWN_ID = -9999;
	public static final String MESSAGE_BENEFICIARY_ID = AttributeManager.MESSAGE_BENEFICIARY_ID;  // Row Id in Beneficiary table
	public static final String MESSAGE_TEXT = AttributeManager.MESSAGE_TEXT;
	public static final String MESSAGE_STATUS = AcdiVocaFind.MESSAGE_STATUS; 
	public static final String[] MESSAGE_STATUS_STRINGS = {"Unsent", "Pending", "Sent", "Ack", "Deleted"};
	public static final int MESSAGE_STATUS_UNSENT = 0;
	public static final int MESSAGE_STATUS_PENDING = 1;
	public static final int MESSAGE_STATUS_SENT = 2;
	public static final int MESSAGE_STATUS_ACK = 3;
	public static final int MESSAGE_STATUS_DEL = 4;
	public static final String MESSAGE_CREATED_AT = AttributeManager.MESSAGE_CREATED_AT;
	public static final String MESSAGE_SENT_AT = AttributeManager.MESSAGE_SENT_AT;
	public static final String MESSAGE_ACK_AT = AttributeManager.MESSAGE_ACK_AT;
	

	public static final int DELETE_FIND = 1;
	public static final int UNDELETE_FIND = 0;
	public static final String WHERE_NOT_DELETED = " " + AcdiVocaFind.DELETED + " != " + DELETE_FIND + " ";
	public static final String DATETIME_NOW = "`datetime('now')`";

	public static final String FINDS_HISTORY_TABLE = "acdi_voca_finds_history";
	public static final String HISTORY_ID = "_id" ;	

	
	/**
	 * This function changes the date format from 0...11 to 1..12 format for the SMS Reader.
	 * @param date
	 * @return
	 */
	@SuppressWarnings("finally")
	public static String adjustDateForSmsReader(String date) {
		try { 
			String[] yrmonday = date.split("/");
			date =  yrmonday[0] + "/" + (Integer.parseInt(yrmonday[1]) + 1) + "/" + yrmonday[2];	
		} catch (Exception e) {
			Log.i(TAG, "Bad date = " + date + " " + e.getMessage());
			e.printStackTrace();
		} finally {
			return date;
		}
	}


	/**
	 * Helper method to process ACKs for bulk update messages. Bulk updates
	 * are messages of the form AV=msgid,d1&d2&...&dN,  where d's are dossier numbers.
	 * Bulk update messages are stored in the message table. 
	 * 
	 * Uses the the message id to look up the bulk update message in the message table
	 * and updates the FINDS table with STATUS = ACK for each dossier number in the 
	 * bulk message. Finally, updates the MESSAGE table with STATUS=ACK.
	 * 
	 * @param acdiVocaMsg the message constructed by SmsManager.
	 * @return
	 */
	private boolean recordAcknowledgedBulkMessage(AcdiVocaMessage acdiVocaMsg) {
		int msg_id = acdiVocaMsg.getMessageId();
		int beneId = acdiVocaMsg.getBeneficiaryId();
		int rows = 0;
		int count = 0;
		Log.i(TAG, "Recording Bulk ACK, msg_id = " + msg_id);
		
		// Update the message status
		boolean result = updateMessageStatus(beneId, msg_id,MESSAGE_STATUS_ACK);
		
		Dao<AcdiVocaMessage, Integer> avMsgDao = null;
		AcdiVocaMessage avMsg = null;
		try {
			avMsgDao = getAcdiVocaMessageDao();
			avMsg = avMsgDao.queryForId(msg_id);  // Retrieve the message
			if (avMsg != null) {

				// Update each beneficiary
				Dao<AcdiVocaFind, Integer> avFindDao = getAcdiVocaFindDao();
				AcdiVocaFind avFind = null;
				String msg = avMsg.smsMessage;
				String dossiers[] = msg.split(AttributeManager.LIST_SEPARATOR);
				for (int k = 0; k < dossiers.length; k++) {
					
					avFind = AcdiVocaFind.fetchByAttributeValue(avFindDao, AcdiVocaFind.DOSSIER,  dossiers[k]);
					if (avFind != null) {
						avFind.message_status = MESSAGE_STATUS_ACK;
						rows = avFindDao.update(avFind);
						if (rows == 1) 
							++count;
						}
					else {
						Log.e(TAG, "Db Error, unable to retrieve Find with dossier = " + dossiers[k]);
					}
				}
			} else {
				Log.e(TAG, "Error, unable to retrieve message id = " + msg_id);
			}
		} catch (SQLException e) {
			Log.e(TAG, "SQL Exception " + e.getMessage());
			e.printStackTrace();
		}
		return count > 0;
	}

	
	/**
	 * Record an incoming ACK from the modem. For normal messages, the modem 
	 * sends back the beneficiary id for the sent message.  This method looks 
	 * up the message id and updates the MESSAGE and FIND tables. 
	 * 
	 * NOTE: This method also receives ACKs for bulk messages, which it passes
	 * to a helper method. 
	 * 
	 * @param acdiVocaMsg the message constructed by SmsManager.
	 * @return
	 */
	public boolean recordAcknowledgedMessage(AcdiVocaMessage acdiVocaMsg) {
		int beneficiary_id = acdiVocaMsg.getBeneficiaryId();
		int msg_id = acdiVocaMsg.messageId;
		Log.i(TAG, "Recording ACK, bene_id = " + acdiVocaMsg.getBeneficiaryId() + " msg_id " + msg_id);

		AcdiVocaFind avFind = null; 
		boolean result = false;

		// Update the Beneficiary table for this message
		if (beneficiary_id < 0) {  // This is an ACK for a bulk message, msg_id is -beneficiary_id
			return recordAcknowledgedBulkMessage(acdiVocaMsg);
		} else {  // In this case, beneficiary id is known and message Id must be looked up
			try {
				Dao<AcdiVocaFind, Integer> dao = getAcdiVocaFindDao();
				avFind = dao.queryForId(beneficiary_id);
				if (avFind != null) {
					msg_id = avFind.message_id;
					avFind.message_status = MESSAGE_STATUS_ACK;
					int rows = dao.update(avFind);
					result = rows == 1;
					if (result) {
						Log.d(TAG, "Updated ACK, for beneficiary_id = " +  beneficiary_id);
						result = updateMessageStatus(beneficiary_id, msg_id, MESSAGE_STATUS_ACK);
						if (result) {
							Log.d(TAG, "Updated ACK, for msg_id = " +  msg_id);
						} else {
							Log.e(TAG, "Unable to process ACK, for msg_id = " +  msg_id);	
						}
					} else {
						Log.e(TAG, "Unable to process ACK, for beneficiary_id = " +  beneficiary_id);
					}
				} else {
					Log.e(TAG, "Error retrieving beneficiary_id = " +  beneficiary_id);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return result;
	}


	/**
	 * Handles updating the message table and beneficiary table for bulk messages. 
	 * In this case the msg_id is known. So we directly update the message table then
	 * update the beneficiary table for each of the DOSIER numbers. 
	 * @param acdiVocaMsg
	 * @param msgStatus
	 * @param huh
	 * @return
	 */
	public boolean updateMessageStatusForBulkMsg(AcdiVocaMessage acdiVocaMsg, int msgStatus) {
		Log.i(TAG, "Updating, msg_id " + acdiVocaMsg.getMessageId() + 
				" bene_id = " +  " to status = " + msgStatus);
		
		int beneficiary_id = acdiVocaMsg.getBeneficiaryId();
		int msg_id = acdiVocaMsg.getMessageId();
		int rows = 0;
		boolean result = updateMessageStatus(beneficiary_id, msg_id, msgStatus );
		if (result) {
//			rows = updateBeneficiaryTableForBulkIds(acdiVocaMsg, msg_id, status);
			try {
				rows = AcdiVocaFind.updateMessageStatusForBulkMsg(getAcdiVocaFindDao(), acdiVocaMsg, msg_id, msgStatus);
				Log.i(TAG, "Updated " + rows + " beneficiaries");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return result;
	}
	
	/**
	 * Updates the message status in the message table and Finds table for
	 * distribution update messages.  In this case the msg_id is UNKNOWN and
	 * the beneficiary is KNOWN.
	 * @param message the SMS message
	 * @param status the new status
	 * @return
	 */
	public boolean updateMessageStatusForNonBulkMessage(AcdiVocaMessage acdiVocaMsg, int status) {
		Log.i(TAG, "Updating, msg_id " + acdiVocaMsg.getMessageId() + 
				" bene_id = " + acdiVocaMsg.getBeneficiaryId() + " to status = " + status);
		
		int beneficiary_id = acdiVocaMsg.getBeneficiaryId();
		int msg_id = acdiVocaMsg.getMessageId();
		
		boolean result = false;
		
		try {
			Dao<AcdiVocaFind, Integer> dao = getAcdiVocaFindDao();
			AcdiVocaFind avFind = dao.queryForId(beneficiary_id);
			if (avFind != null) {
				avFind.message_status = status;
				avFind.message_id = msg_id;
				int rows = dao.update(avFind);
				result = rows == 1;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (result) {
			result = updateMessageStatus(beneficiary_id, msg_id, status);
		}
		return result;
	}
	
	
	/**
	 * Creates a new entry in the message table and update the FINDS
	 * table to point to the message.
	 * @return
	 */
	public long createNewMessageTableEntry(AcdiVocaMessage acdiVocaMsg, int beneficiary_id, int msgStatus) {
		Log.i(TAG, "createNewMessage for beneficiary = " + beneficiary_id + " status= " + msgStatus);

		Date now = new Date(System.currentTimeMillis());

		Dao<AcdiVocaMessage, Integer> avAvMsgDao = null;
		int result = 0;
		try {
			avAvMsgDao = getAcdiVocaMessageDao();
			acdiVocaMsg.message_created_at = now;
			result = avAvMsgDao.create(acdiVocaMsg);
		} catch (SQLException e) {
			Log.e(TAG, "SQL Exception " + e.getMessage());
			e.printStackTrace();
		}
		if (result != 1) {
			Log.i(TAG, "Unable to insert NEW message for beneficiary id= " + beneficiary_id);
		} else {
			Log.i(TAG, "Inserted NEW message, id= " + acdiVocaMsg.id + " bene_id=" + beneficiary_id); 
			
			boolean success = false;
			try {
				success = AcdiVocaFind.updateMessageStatus(getAcdiVocaFindDao(), beneficiary_id, acdiVocaMsg.id, msgStatus);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			if (!updateBeneficiaryMessageStatus(beneficiary_id, acdiVocaMsg.id, msgStatus))
			if (success)
				Log.i(TAG, "Updated beneficiary id = " + beneficiary_id + " for message " + acdiVocaMsg.id);
			else
				Log.e(TAG, "Unable to update beneficiary id = " + beneficiary_id + " for message " + acdiVocaMsg.id);
		} 
		return acdiVocaMsg.id;  // Return the message Id.
	}

	/**
	 * Updates the message table for an existing message.
	 * @return
	 */
	public boolean updateMessageStatus(int beneficiary_id, int msg_id, int status ) {
		Log.i(TAG, "Updating message, bene_id = " + beneficiary_id + " message = " + msg_id + " status=" + status);

		int result = 0;
		Date now = new Date(System.currentTimeMillis());
//		String now = new Date(System.currentTimeMillis()).toString()
//		+ " " + new Time(System.currentTimeMillis()).toString();
		Log.i(TAG, "Time now = " + now);
		
		Dao<AcdiVocaMessage, Integer> avMsgDao = null;
		AcdiVocaMessage avMsg = null;
		try {
			avMsgDao = getAcdiVocaMessageDao();
			avMsg = avMsgDao.queryForId(msg_id);  // Retrieve the message
			if (avMsg != null) {
				avMsg.setMsgStatus(status);
				avMsg.setBeneficiaryId(beneficiary_id);
				if (status == MESSAGE_STATUS_SENT) {
					avMsg.message_sent_at = now;
				} else if (status == MESSAGE_STATUS_ACK) 
					avMsg.message_ack_at = now;
				result = avMsgDao.update(avMsg);
			} else {
				Log.e(TAG, "Unable to retrieve message id = " + msg_id ); 	
			}
		} catch (SQLException e) {
			Log.e(TAG, "SQL Exception " + e.getMessage());
			e.printStackTrace();
		}
		if (result == 1) 
			Log.d(TAG, "Updated message id = " + msg_id + " for message " + msg_id + " status=" + status);
		return result == 1;
	}	
	
	
	/**
	 * Helper method to retrieve selected messages from message table. 
	 * @param filter
	 * @param order_by
	 * @return
	 */
	private List<AcdiVocaMessage> lookupMessages(int filter, int bene_status, String order_by) {
		int msg_status = 0;
		// Map the select result to the message status
		switch (filter) {
		case SearchFilterActivity.RESULT_SELECT_PENDING:
			msg_status = MESSAGE_STATUS_PENDING;
			break;
		case SearchFilterActivity.RESULT_SELECT_SENT:
			msg_status = MESSAGE_STATUS_SENT;
			break;
		case SearchFilterActivity.RESULT_SELECT_ACKNOWLEDGED:
			msg_status = MESSAGE_STATUS_ACK;
			break;
		default:
			break;
		}
		
		Dao<AcdiVocaMessage, Integer> avMessageDao = null;
		List<AcdiVocaMessage> list = null;
		AcdiVocaMessage avMsg = null;
		try {
			avMessageDao = getAcdiVocaMessageDao();
			QueryBuilder<AcdiVocaMessage, Integer> queryBuilder =
				avMessageDao.queryBuilder();
			if (msg_status != 0) {
				Where<AcdiVocaMessage, Integer> where = queryBuilder.where();
				where.eq("msgStatus", msg_status);
			}
			PreparedQuery<AcdiVocaMessage> preparedQuery = queryBuilder.prepare();
			list = avMessageDao.query(preparedQuery);
		} catch (SQLException e) {
			Log.e(TAG, "SQL Exception " + e.getMessage());
			e.printStackTrace();
		}
		return list;
	}


	
	/**
	 * Returns an array of AcdiVocaMessages for new or updated beneficiaries. 
	 * Fetches the beneficiary records from the Db and converts the column names
	 * and their respective values to abbreviated attribute-value pairs.
	 * @param filter
	 * @param order_by
	 * @return
	 */
	public ArrayList<AcdiVocaMessage> createMessagesForBeneficiaries(int filter, String order_by, String distrCtr) {
		Log.i(TAG, "Creating messages for beneficiaries");
//		List<AcdiVocaFind> list = lookupBeneficiaryRecords(filter, order_by, distrCtr);

		List<AcdiVocaFind> list = null;
		try {
			list = AcdiVocaFind.fetchAllByMessageStatus(getAcdiVocaFindDao(), filter, order_by, distrCtr);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ArrayList<AcdiVocaMessage> acdiVocaMsgs = new ArrayList<AcdiVocaMessage>();
		if (list != null) {
			Log.i(TAG,"createMessagesForBeneficiaries " +  " count=" + list.size() + " filter= " + filter);
		
		// Construct the messages and return as a String array
			Iterator<AcdiVocaFind> it = list.iterator();

			while (it.hasNext()) {
				AcdiVocaFind avFind = it.next();   // Process the next beneficiary
				AcdiVocaMessage avMessage = avFind.toSmsMessage();
				acdiVocaMsgs.add(avMessage);
			}
		}
		return acdiVocaMsgs;		
	}

	/**
	 * Deletes all rows from the Beneficiary Table.
	 * @return
	 */
	public int clearBeneficiaryTable() {
		Log.i(TAG, "Clearing Beneficiary Table");
		int count = 0;
		Dao<AcdiVocaFind, Integer> dao = null;
		List<AcdiVocaFind> list = null;
		try {
			dao = getAcdiVocaFindDao();
			DeleteBuilder<AcdiVocaFind, Integer> deleteBuilder =  dao.deleteBuilder();
			// Delete all rows -- no where clause
			count = dao.delete(deleteBuilder.prepare());
		} catch (SQLException e) {
			Log.e(TAG, "SQL Exception " + e.getMessage());
			e.printStackTrace();
		}
		return count;
	}
	
	/**
	 * Deletes all rows from the Beneficiary Table.
	 * @return
	 */
	public int clearMessageTable() {
		Log.i(TAG, "Clearing Message Table");
		int count = 0;
		Dao<AcdiVocaMessage, Integer> avMsg = null;
		List<AcdiVocaMessage> list = null;
		try {
			avMsg = getAcdiVocaMessageDao();
			DeleteBuilder<AcdiVocaMessage, Integer> deleteBuilder =
				avMsg.deleteBuilder();
			// Delete all rows -- no where clause
			count = avMsg.delete(deleteBuilder.prepare());
		} catch (SQLException e) {
			Log.e(TAG, "SQL Exception " + e.getMessage());
			e.printStackTrace();
		}
		return count;
	}
	
	/**
	 * Creates an array list of messages each of which consists of a list of the
	 * dossier numbers of beneficiaries who did not show up at the distribution event.
	 * Beneficiaries who did show up and who have changes are updated with individual
	 * messages.  Beneficiaries who showed up but there was no change are not processed.
	 * Their status is deduced on the server by process of elimination. 
	 * @param distrCtr
	 * @return
	 */
	public ArrayList<AcdiVocaMessage> createBulkUpdateMessages(String distrCtr) {
		Log.i(TAG, "Creating bulk update messages distribution center = " + distrCtr);
		
		ArrayList<AcdiVocaMessage> acdiVocaMsgs = new ArrayList<AcdiVocaMessage>();

		Dao<AcdiVocaFind, Integer> avFindDao = null;
		AcdiVocaFind avFind = null;
		List<AcdiVocaFind> list = null;
		try {
			avFindDao = getAcdiVocaFindDao();
			QueryBuilder<AcdiVocaFind, Integer> queryBuilder =
				avFindDao.queryBuilder();
			Where<AcdiVocaFind, Integer> where = queryBuilder.where();
			where.eq(AcdiVocaFind.STATUS,  AcdiVocaFind.STATUS_UPDATE);
			where.and();
			where.eq(AcdiVocaFind.MESSAGE_STATUS, MESSAGE_STATUS_UNSENT);
			where.and();
			where.eq(AcdiVocaFind.DISTRIBUTION_POST, distrCtr);
			where.and();
			where.eq(AcdiVocaFind.Q_PRESENT, false);
			PreparedQuery<AcdiVocaFind> preparedQuery = queryBuilder.prepare();
			list = avFindDao.query(preparedQuery);
		} catch (SQLException e) {
			Log.e(TAG, "SQL Exception " + e.getMessage());
			e.printStackTrace();
		}

		Log.i(TAG,"fetchBulkUpdateMessages " +  " count=" + list.size() + " distrPost " + distrCtr);
		
		if (list.size() != 0) {
			Iterator<AcdiVocaFind> it = list.iterator();
			String smsMessage = "";
			String msgHeader = "";
			while (it.hasNext()) {
				avFind = it.next();
				smsMessage += avFind.dossier + AttributeManager.LIST_SEPARATOR;
				
				if (smsMessage.length() > 120) {
					// Add a header (length and status) to message
					msgHeader = "MsgId: bulk, Len:" + smsMessage.length();

					acdiVocaMsgs.add(new AcdiVocaMessage(UNKNOWN_ID, 
							UNKNOWN_ID, 
							MESSAGE_STATUS_UNSENT,
							"", smsMessage, msgHeader, 
							!AcdiVocaMessage.EXISTING));
					smsMessage = "";
				}
			}
			if (!smsMessage.equals("")) {
				msgHeader = "MsgId: bulk, Len:" + smsMessage.length();
				acdiVocaMsgs.add(new AcdiVocaMessage(UNKNOWN_ID, 
							UNKNOWN_ID, 
							MESSAGE_STATUS_UNSENT,
							"", smsMessage, msgHeader, 
							!AcdiVocaMessage.EXISTING));
			}
		}
		return acdiVocaMsgs;
	}
	
	/** 
	 * Returns an array of Strings where each String represents an SMS
	 * message for a Beneficiary.
	 * @param filter a int that selects messages by status
	 * @return an array of SMS strings
	 */
	public ArrayList<AcdiVocaMessage> fetchSmsMessages(int filter, int bene_status, String order_by) {
		List<AcdiVocaMessage> list = lookupMessages(filter, bene_status, order_by);

		ArrayList<AcdiVocaMessage> acdiVocaMsgs = new ArrayList<AcdiVocaMessage>();

		Iterator<AcdiVocaMessage> it = list.iterator();
		while (it.hasNext()) {
			AcdiVocaMessage msg = it.next();
			String header = "MsgId:" + msg.id + " Stat:" + msg.msgStatus + " Len:" 
				+ msg.smsMessage.length() + " Bid = " + msg.beneficiaryId;
			msg.setMsgHeader(header);
			acdiVocaMsgs.add(msg);
		}
		
		return acdiVocaMsgs;
	}
}
