package name.kratunov.mealbooking;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class ViewMeals extends Activity {
    /** Called when the activity is first created. */
	final private String logtag = "ViewMeals";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HttpScraper scraper = new HttpScraper();
        if(!scraper.login("2777707", "s3r3n1ty"))
        	Log.w(logtag, "The login credentials are invalid");
        
        List<HttpScraper.Meal> meals = scraper.getMeals(true);
        if(meals != null)
        {
			for (HttpScraper.Meal meal : meals) {
				Log.i(logtag, "Found meal " + meal.id + " on " + meal.date + " at " + meal.time
						+ ": " + meal.sitting);
				if(meal.menu != null)
					Log.i(logtag, "Menu: " + meal.getMenuString());
				if(meal.info != null)
					Log.i(logtag, "Info: " + meal.info);
			}
        }
        
        Log.d(logtag, "Setting view");
        setContentView(R.layout.main);
    }
}