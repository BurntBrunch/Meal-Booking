package name.kratunov.mealbooking;

import name.kratunov.mealbooking.MealService.ServiceBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class BaseActivity extends Activity {
	protected String logtag = "MealBooking";
	ServiceConnection mServiceConn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			mService = null;
			Log.d(logtag,
					"Service disconnected in " + BaseActivity.this.getClass());
			BaseActivity.this.onServiceDisconnected();
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			mService = ((ServiceBinder) service).getService();
			Log.d(logtag,
					"Service connected in " + BaseActivity.this.getClass());
			BaseActivity.this.onServiceConnected();
		}
	};

	protected MealService mService = null;

	protected void onServiceConnected()
	{
	}

	protected void onServiceDisconnected()
	{
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Intent intent = new Intent("name.kratunov.mealbooking.MEAL_SERVICE");
		startService(intent);
		bindService(intent, mServiceConn, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		unbindService(mServiceConn);
	}
}
