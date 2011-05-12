package name.kratunov.mealbooking;

import name.kratunov.mealbooking.HttpScraper.ProgressReporter;
import name.kratunov.mealbooking.MealsContentProviderHelpers.MealsMetadata;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

public class MealService extends Service {
	public class ServiceBinder extends Binder {
		public MealService getService()
		{
			return MealService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return new ServiceBinder();
	}

	@Override
	public boolean onUnbind(Intent intent)
	{
		stopSelf();
		return false;
	}

	private Cursor mMealsCursor = null;
	private Handler mHandler;
	@Override
	public void onCreate()
	{
		super.onCreate();
		mMealsCursor = getContentResolver().query(MealsMetadata.CONTENT_URI,
				null, null, null, null);
		mHandler = new Handler();
	}

	public void RefreshMeals(boolean blocking)
	{
		final ProgressReporter reporter = new ProgressReporter() {
			@Override
			public void onProgressStart()
			{
				requery();
			}
			@Override
			public void onProgressEnd()
			{
				requery();
			}
			@Override
			public void onProgress(int n, int cnt)
			{
				requery();
			}
		};
		if (blocking)
			HttpScraper.getInstance().getMeals(reporter);
		else
		{
			AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
				@Override
				public Void doInBackground(Void... data)
				{
					HttpScraper.getInstance().getMeals(reporter);

					return null;
				}
			};
			task.execute();
		}

		requery();
	}

	public Cursor GetMealsCursor()
	{
		return mMealsCursor;
	}

	public boolean CancelMeal(Uri mealUri)
	{
		boolean res = HttpScraper.getInstance().cancelMeal(mealUri);
		requery();

		return res;
	}

	public BookingInfo GetBookingInfo(Uri mealUri, boolean change)
	{
		return HttpScraper.getInstance().getBookingInfo(mealUri, change);
	}

	public boolean BookMeal(BookingInfo info)
	{
		boolean res = HttpScraper.getInstance().bookMeal(info);
		requery();

		return res;
	}

	public boolean IsLoggedIn()
	{
		return HttpScraper.getInstance().isLoggedIn();
	}

	public boolean Login(String username, String password)
	{
		return HttpScraper.getInstance().login(username, password);
	}

	private void requery()
	{
		mHandler.post(new Runnable() {
			@Override
			public void run()
			{
				mMealsCursor.requery();
			}
		});
	}
}
