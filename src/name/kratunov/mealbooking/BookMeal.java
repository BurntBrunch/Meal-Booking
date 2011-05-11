package name.kratunov.mealbooking;

import name.kratunov.mealbooking.MealsContentProviderHelpers.MealsMetadata;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class BookMeal extends Activity {
	final private String logtag = "BookMeal";
	final public static int BOOK_SUCCESS = 5;
	final public static int BOOK_FAILURE = 6;
	final public static int BOOK_CANCEL = 7;
	
	private Uri mealUri;
	private BookingInfo info;
	
	private Spinner mealsSpinner, dietsSpinner;
	private EditText dietsEdit, infoEdit;
	private Button button;
	
	private boolean change = false;

	private class GetBookingInfoTask extends AsyncTask<Uri, Void, BookingInfo> {
		private ProgressDialog dlg;

		@Override
		public void onPreExecute() {
			Log.d(logtag, "Showing progress dialog");
			dlg = ProgressDialog.show(BookMeal.this, "",
					"Getting booking info", true);
			dlg.show();
		}

		@Override
		public BookingInfo doInBackground(Uri... params) {
			return HttpScraper.getInstance().getBookingInfo(params[0], change);
		}

		@Override
		public void onPostExecute(BookingInfo res) {
			Log.d(logtag, "Dismissing progress dialog");
			dlg.dismiss();
			if(res != null)
			{
				info = res;
				BookMeal.this.finalizeInit();
			}
			else
				Log.e(logtag, "Null booking info!");
		}
	}

	private class BookMealTask extends AsyncTask<Void, Void, Boolean>
	{
		private ProgressDialog dlg;
		public BookingInfo info;
		
		@Override
		public void onPreExecute() {
			dlg = ProgressDialog.show(BookMeal.this, "",
					BookMeal.this.getResources().getString(R.string.request), 
					true, true);
			dlg.setOnCancelListener(new OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					BookMealTask.this.cancel(true);
					BookMealTask.this.onPostExecute(false);
				}
			});
			
			dlg.show();
		}
		
		@Override
		protected Boolean doInBackground(Void... params) {
			HttpScraper scraper = HttpScraper.getInstance();
			if(info != null)
				return scraper.bookMeal(info);
			else
				return false;
		}
		
		@Override
		public void onPostExecute(Boolean res)
		{
			dlg.dismiss();
			
			Intent intent = new Intent(BookMeal.this.getIntent());
			if(res)
				BookMeal.this.setResult(BOOK_SUCCESS, intent);
			else
				BookMeal.this.setResult(BOOK_FAILURE, intent);
			
			BookMeal.this.finish();
		}
		
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		mealUri = Uri.parse(intent.toUri(0));
		if(intent.getAction().equalsIgnoreCase("name.kratunov.mealbooking.BOOK_MEAL"))
		{
			Log.d(logtag, "Booking meal " + mealUri.getLastPathSegment());
			change = false;
		} else if (intent.getAction().equalsIgnoreCase("name.kratunov.mealbooking.CHANGE_MEAL"))
		{
			Log.d(logtag, "Changing meal " + mealUri.getLastPathSegment());
			change = true;
		} else 
		{
			Log.e(logtag, "Unknown action, how did we get here?! Finishing..");
			finish();
		}
		
		GetBookingInfoTask task = new GetBookingInfoTask();
		task.execute(mealUri);
	}

	private void finalizeInit() {
		setContentView(R.layout.bookmeal);
		
		mealsSpinner = (Spinner) findViewById(R.id.MealSpinner);
		dietsSpinner = (Spinner) findViewById(R.id.DietarySpinner);
		dietsEdit = (EditText) findViewById(R.id.DietaryInfoEditText);
		infoEdit = (EditText) findViewById(R.id.DietaryInfoEditText);
		button = (Button) findViewById(R.id.BookButton);

		ArrayAdapter<String> meals_adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, info.meals.keySet()
						.toArray(new String[0]));
		ArrayAdapter<String> diets_adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, info.dietary_requirements
						.keySet().toArray(new String[0]));
		mealsSpinner.setAdapter(meals_adapter);
		dietsSpinner.setAdapter(diets_adapter);
		
		meals_adapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		diets_adapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		if(change)
		{
			Log.i(logtag, "Changing meal; choices are " + info.meal_choice + ", " + info.diet_choice);
			mealsSpinner.setSelection(meals_adapter.getPosition(info.meal_choice));
			dietsSpinner.setSelection(diets_adapter.getPosition(info.diet_choice));

			button.setText(this.getResources().getString(R.string.changeBooking));
		}
		
		dietsEdit.setText(info.additional_dietary);
		button.setOnClickListener(new ButtonOnClickListener());
	}

	private class ButtonOnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			info.meal_choice = (String) mealsSpinner.getSelectedItem();
			info.diet_choice = (String) dietsSpinner.getSelectedItem();
			info.additional_dietary = dietsEdit.getText().toString();
			info.additional_info = infoEdit.getText().toString();

			Log.d(logtag, "Selected meal: " + info.meal_choice + " value: "
					+ info.meals.get(info.meal_choice));
			Log.d(logtag, "Selected diet: " + info.diet_choice + " value: "
					+ info.dietary_requirements.get(info.diet_choice));

			BookMealTask task = new BookMealTask();
			task.info = info;
			task.execute();
		}
	}
	
	@Override 
	public void onPause()
	{
		super.onPause();
		
		this.setResult(BOOK_FAILURE);
		this.finish();
	}
}
