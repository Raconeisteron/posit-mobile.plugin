package org.hfoss.posit.android.plugin.outsidein;

import org.hfoss.posit.android.api.Find;

import com.j256.ormlite.field.DatabaseField;

public class OutsideInFind extends Find {

	public static final String SYRINGES_IN = "syringes_in";
	public static final String SYRINGES_OUT = "syringes_out";
	public static final String IS_NEW = "is_new";

	@DatabaseField(columnName = SYRINGES_IN)
	protected int syringesIn;
	@DatabaseField(columnName = SYRINGES_OUT)
	protected int syringesOut;
	@DatabaseField(columnName = IS_NEW)
	protected boolean isNew;

	public int getSyringesIn() {
		return syringesIn;
	}

	public void setSyringesIn(int syringesIn) {
		this.syringesIn = syringesIn;
	}

	public int getSyringesOut() {
		return syringesOut;
	}

	public void setSyringesOut(int syringesOut) {
		this.syringesOut = syringesOut;
	}

	public boolean isNew() {
		return isNew;
	}

	public void setNew(boolean isNew) {
		this.isNew = isNew;
	}

}
