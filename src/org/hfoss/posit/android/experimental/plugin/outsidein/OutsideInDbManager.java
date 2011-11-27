package org.hfoss.posit.android.experimental.plugin.outsidein;
/**
 * 
 */

import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.database.DbManager;

import android.content.Context;
import java.sql.SQLException;
import java.util.List;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

/**
 * 
 *
 */
public class OutsideInDbManager extends DbManager {


		private static final String TAG = "OutsideInDbManager";

		// DAO objects used to access the Db tables
		private Dao<OutsideInFind, Integer> outsideInFindDao = null;

		
		/**
		 * Constructor just saves and opens the Db.
		 * @param context
		 */
		public OutsideInDbManager(Context context) {
			super(context);
		}
		
		/**
		 * Invoked automatically if the Database does not exist.
		 */
		@Override
		public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
				Log.i(TAG, "onCreate");
				super.onCreate(db, connectionSource);
				//OutsideInFind.createTable(connectionSource);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
			try {
				Log.i(TAG, "onUpgrade");
				TableUtils.dropTable(connectionSource, OutsideInFind.class, true);
				// after we drop the old databases, we create the new ones
				onCreate(db, connectionSource);
			} catch (SQLException e) {
				Log.e(TAG, "Can't drop databases", e);
				throw new RuntimeException(e);
			}
		}
//			

//		
//		/**
//		 * Returns the Database Access Object (DAO) for the OutsideInFind class. 
//		 * It will create it or just give the cached value.
//		 */
//		public Dao<OutsideInFind, Integer> getOutsideInFindDao(){
//			if (outsideInFindDao == null) {
//				try {
//					outsideInFindDao = getDao(OutsideInFind.class);
//				} catch (SQLException e) {
//					e.printStackTrace();
//				}
//			}
//			return outsideInFindDao;
//		}
//		
//		
//		/**
//		 * Looks up a find by its ID.
//		 * @param id the id of the find to look up
//		 * @return the find
//		 */
//		public Find getFindById(int id) {
//			OutsideInFind find = null;
//			try {
//				find = getOutsideInFindDao().queryForId(id);
//			} catch (SQLException e) {
//				e.printStackTrace();
//			}
//			return find;
//		}
//		
//		/**
//		 * Fetches all finds currently in the database.
//		 * @return A list of all the finds.
//		 */
//		public List<? extends Find> getAllFinds() {
//			List<OutsideInFind> list = null;
//			try {
//				list = getOutsideInFindDao().queryForAll();
//			} catch (SQLException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			return list;
//
//		}
		
		/**
		 * Close the database connections and clear any cached DAOs.
		 */
		@Override
		public void close() {
			super.close();
			outsideInFindDao = null;

		}

	}

