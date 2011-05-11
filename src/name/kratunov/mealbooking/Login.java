package name.kratunov.mealbooking;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

public class Login extends Activity {

	private EditText cardnumber, password;
	private CheckBox automaticLogin ;
    
	final class LoginTask extends AsyncTask<String, Void, Boolean>
	{
		private ProgressDialog dlg;
		@Override
		public void onPreExecute()
		{
			dlg = ProgressDialog.show(Login.this, "", "Logging in", true);
			dlg.show();
		}
		@Override
		public Boolean doInBackground(String... data)
		{
			HttpScraper scraper = HttpScraper.getInstance();
			return scraper.login(data[0], data[1]);
		}
		@Override
		public void onPostExecute(Boolean data)
		{
			dlg.dismiss();
			Login.this.onTaskEnd(data);
		}
	}

	final private String logtag = "MealBooking";
	private class LoginButtonListener implements OnClickListener{
		@Override
		public void onClick(View v) {
	        LoginTask task = new LoginTask();
	        task.execute(cardnumber.getText().toString(), 
					     password.getText().toString());    			
		}
	}
	
	private class CheckboxChangeListener implements CompoundButton.OnCheckedChangeListener {
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if (buttonView == automaticLogin && !isChecked)
				Login.this.removeSettings();
		}
	}
	
	private void onTaskEnd(boolean res)
	{
		TextView message = (TextView) findViewById(R.id.MessageTextView);
		if(!res)
		{
			message.setText(R.string.loginError);
			removeSettings();
			automaticLogin.setChecked(false);
		}
		else
		{
			if(automaticLogin.isChecked())
				saveSettings();
			else
				removeSettings();
			
			Intent intent = new Intent(Login.this, ViewMeals.class);
			startActivity(intent);
		}
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.login);
        
        cardnumber = (EditText) findViewById(R.id.CardNumberEditText);
        password = (EditText) findViewById(R.id.PasswordEditText);
        automaticLogin = (CheckBox) findViewById(R.id.AutomaticLoginCheckboxButton);
        
        restoreSettings();
        
        Button loginButton = (Button) findViewById(R.id.LoginButton);
        loginButton.setOnClickListener(new LoginButtonListener());
        
        automaticLogin.setOnCheckedChangeListener(new CheckboxChangeListener());
        
        if(automaticLogin.isChecked())
        	new LoginButtonListener().onClick(loginButton);
	} 
	
	private void restoreSettings()
    {
		Log.d(logtag, "Restoring settings");
		
    	SharedPreferences settings = getSharedPreferences("login", 0);    		
    	cardnumber.setText(settings.getString("card_number", ""));
    	password.setText(settings.getString("password", ""));
    	automaticLogin.setChecked(settings.getBoolean("automatic", false));
    }
    
    private void saveSettings()
    {
    	Log.d(logtag, "Saving settings");
    	SharedPreferences settings = getSharedPreferences("login", 0);
    	SharedPreferences.Editor editor = settings.edit();
    	
		editor.putString("card_number", cardnumber.getText().toString());
		editor.putString("password", password.getText().toString());
		editor.putBoolean("automatic", automaticLogin.isChecked());
		editor.commit();

    }
    
    private void removeSettings()
    {
    	Log.d(logtag, "Removing settings");
    	
    	SharedPreferences settings = getSharedPreferences("login", 0);
    	SharedPreferences.Editor editor = settings.edit();
	
    	editor.remove("card_number");
		editor.remove("password");
		editor.remove("automatic");
		
		editor.commit();
    }
}
