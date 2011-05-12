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

	public void RefreshMeals(boolean blocking, final ProgressReporter reporter)
	{
		final ProgressReporter inlineReporter = new ProgressReporter() {
			@Override
			public void onProgressStart()
			{
				requery();
				if (reporter != null)
					mHandler.post(new Runnable() {
						@Override
						public void run()
						{
							reporter.onProgressStart();
						}
					});
			}
			@Override
			public void onProgressEnd()
			{
				requery();
				if (reporter != null)
					mHandler.post(new Runnable() {
						@Override
						public void run()
						{
							reporter.onProgressEnd();
						}
					});
			}
			@Override
			public void onProgress(final int n, final int cnt)
			{
				requery();
				if (reporter != null)
					mHandler.post(new Runnable() {
						@Override
						public void run()
						{
							reporter.onProgress(n, cnt);
						}
					});
			}
		};
		if (blocking)
			HttpScraper.getInstance().getMeals(inlineReporter);
		else
		{
			AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
				@Override
				public Void doInBackground(Void... data)
				{
					HttpScraper.getInstance().getMeals(inlineReporter);

					return null;
				}
			};
			task.execute();
		}

		requery();
	}

	public Cursor GetMeal(Uri mealUri)
	{
		Cursor cursor = getContentResolver().query(mealUri, null, null, null,
				null);
		cursor.moveToFirst();

		return cursor;
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
