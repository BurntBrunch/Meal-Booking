package name.kratunov.mealbooking;

import name.kratunov.mealbooking.MealService.ServiceBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

public class BaseActivity extends Activity {

	ServiceConnection mServiceConn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			mService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			mService = ((ServiceBinder) service).getService();
		}
	};

	protected MealService mService = null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Intent intent = new Intent(this, MealService.class);
		startService(intent);
		bindService(intent, mServiceConn, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		Intent intent = new Intent(this, MealService.class);
		stopService(intent);
	}
}
