package name.kratunov.mealbooking;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class PreferencesActivity extends PreferenceActivity {
	public static final String PREFERENCES_KEY = "MealBooking",
			SHOW_LEGEND_KEY = "guiShowLegend",
			UPDATE_STARTUP_KEY = "updateOnStartup",
			CARD_NUMBER_KEY = "card_number",
			PASSWORD_KEY = "password",
			AUTOMATIC_LOGIN_KEY = "automatic";
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		PreferenceManager manager = getPreferenceManager();

		manager.setSharedPreferencesMode(MODE_PRIVATE);
		manager.setSharedPreferencesName(PREFERENCES_KEY);

		addPreferencesFromResource(R.xml.prefs);
	}
}
