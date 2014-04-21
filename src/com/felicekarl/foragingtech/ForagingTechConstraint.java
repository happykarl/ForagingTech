package com.felicekarl.foragingtech;

import android.os.Environment;

public class ForagingTechConstraint {
	public static final String PT = "PT";
	public static final String IG = "IG";
	public static final String defaultPhotoPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ForagingTech/";
	public static final String defaultMapPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/maps/";
}
