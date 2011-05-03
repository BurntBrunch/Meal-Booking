package name.kratunov.mealbooking;

import android.content.Context;

public class Application extends android.app.Application {
	private static Context mContext = null;
	public static Context getContext() {
		if (mContext == null)
			throw new IllegalStateException("We haven't set the application context yet!");
		
		return mContext;
	}
	@Override
	public void onCreate()
	{
		super.onCreate();
		mContext = getApplicationContext();
	}
}
