package org.hfoss.posit.android.plugin.bookcollect;

import java.sql.SQLException;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbManager;

import android.util.Log;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.TableUtils;

@DatabaseTable(tableName = DbManager.FIND_TABLE_NAME)
public class BookCollectFind extends Find {
	public static final String TAG = "BookCollectFind";
	public static final String ISBN = "isbn";
	
	@DatabaseField(columnName = ISBN)
	protected long isbn;
	
	public BookCollectFind()
	{
		// Necessary by ormlite
	}

	/**
	 * Creates the table for this class.
	 * 
	 * @param connectionSource
	 */
	public static void createTable(ConnectionSource connectionSource) {
		Log.i(TAG, "Creating BookCollectFind table");
		try {
			TableUtils.createTable(connectionSource, BookCollectFind.class);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

    public String getTitle()
    {
        return name;
    }

    public void setTitle(String title)
    {
        name = title;
    }

    public String getAuthor()
    {
        return description;
    }

    public void setAuthor(String author)
    {
        description = author;
    }
    
	public long getIsbn()
    {
        return isbn;
    }

    public void setIsbn(long isbn)
    {
        this.isbn = isbn;
    }

    @Override
	public String toString()
    {
		StringBuilder sb = new StringBuilder();
		sb.append(ORM_ID).append("=").append(id).append(",");
		sb.append(GUID).append("=").append(guid).append(",");
		sb.append(NAME).append("=").append(name).append(",");
		sb.append(DESCRIPTION).append("=").append(description).append(",");
		sb.append(PROJECT_ID).append("=").append(project_id).append(",");
		sb.append(LATITUDE).append("=").append(latitude).append(",");
		sb.append(LONGITUDE).append("=").append(longitude).append(",");
		if (time != null)
			sb.append(TIME).append("=").append(time.toString()).append(",");
		else
			sb.append(TIME).append("=").append("").append(",");
		if (modify_time != null)
			sb.append(MODIFY_TIME).append("=").append(modify_time.toString())
					.append(",");
		else
			sb.append(MODIFY_TIME).append("=").append("").append(",");
		//sb.append(REVISION).append("=").append(revision).append(",");
		sb.append(IS_ADHOC).append("=").append(is_adhoc).append(",");
		//sb.append(ACTION).append("=").append(action).append(",");
		sb.append(DELETED).append("=").append(deleted).append(",");
		sb.append(ISBN).append("=").append(isbn).append(",");
		return sb.toString();
	}
}
