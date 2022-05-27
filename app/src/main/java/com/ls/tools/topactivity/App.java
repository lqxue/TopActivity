package com.ls.tools.topactivity;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import java.io.File;

public class App extends Application {

	private static App sApp;

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		sApp = this;
//		CrashHandler.getInstance(getApp()).init();
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	public static String getCrashLogDir() {
		return getCrashLogFolder().getAbsolutePath();
	}

	public static File getCrashLogFolder() {
		return sApp.getExternalFilesDir(null);
	}

	public static App getApp() {
		return sApp;
	}

	public static void showToast(String str, int length) {
		Toast.makeText(getApp(), str, length).show();
	}

}
