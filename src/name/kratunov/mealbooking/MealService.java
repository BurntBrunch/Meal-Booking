package name.kratunov.mealbooking;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
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

}
