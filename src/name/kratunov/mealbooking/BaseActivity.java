package name.kratunov.mealbooking;

import name.kratunov.mealbooking.MealService.ServiceBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

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

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		menu.setGroupVisible(R.id.MainMenuGroup, true);
		return true;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
			case R.id.PreferencesMenuItem:
				Intent intent = new Intent(this, PreferencesActivity.class);
				startActivity(intent);
				return true;
			case R.id.LogoutMenuItem:
				// Remove all the settings
				Editor editor = getSharedPreferences(PreferencesActivity.PREFERENCES_KEY,
						MODE_PRIVATE).edit();
				editor.remove(PreferencesActivity.AUTOMATIC_LOGIN_KEY);
				editor.remove(PreferencesActivity.CARD_NUMBER_KEY);
				editor.remove(PreferencesActivity.PASSWORD_KEY);
				editor.commit();
				
				// Clear the database
				if(mService != null)
					mService.ClearMeals();
				
				// Redirect to the login screen
				Intent loginIntent = new Intent(this, Login.class);
				loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
						Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startActivity(loginIntent);
		}
		return false;
	}
}