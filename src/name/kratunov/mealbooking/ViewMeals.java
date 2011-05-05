package name.kratunov.mealbooking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.kratunov.mealbooking.MealsContentProviderHelpers.MealsMetadata;
import name.kratunov.mealbooking.R.drawable;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Spanned;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;
import android.widget.Toast;

public class ViewMeals extends Activity {
	final private String logtag = "ViewMeals";
	final public int MENU_MENU = 0, MENU_INFO = 1, MENU_BOOK = 2, MENU_CANCEL = 3,
		MENU_CHANGE = 4, MENU_ITEMS = 5;
	
	private enum ListItemId {ItemStatus, ItemText, ItemMenu, ItemGuest};
	
	private List<Meal> meals;
	private List<Map<String, Object>> meals_list = 
		new ArrayList<Map<String, Object>>();
	private SimpleCursorAdapter adapter;
	
	private ListView mealsListView;
	private Cursor cursor;
	
	private class LegendClickListener implements OnClickListener
	{	
		@Override
		public void onClick(View v) {
			AlertDialog.Builder builder = new AlertDialog.Builder(ViewMeals.this);
			Spanned msg = Html.fromHtml("<img src=\"menu\"> View menu <br>" +
					"<img src=\"book_allowed\"> Booking is allowed <br>" +
					"<img src=\"book_full\"> Sitting is fully booked <br>" +
					"<img src=\"book_forbidden\"> Booking is disabled <br>" +
					"<img src=\"book_cancel\"> Cancel booking <br>" +
					"<img src=\"guests_none\"> Guests are allowed (and not booked) <br>" +
					"<img src=\"guests_added\"> Guest booked <br>" +
					"<img src=\"guests_forbidden\"> Guests are not allowed <br>"+
					"<i> Long click an item for more options </i> ",
					new LegendImageFormatter(), null);
			
			
			builder.setMessage(msg).setOnCancelListener(new DialogInterface.OnCancelListener(){
				@Override
				public void onCancel(DialogInterface dialog) {
					dialog.dismiss();
				}
			}).setCancelable(true);
			AlertDialog dlg = builder.create();
			dlg.setCanceledOnTouchOutside(true);
			
			dlg.show();
		}
	}
	
	private class LegendImageFormatter implements ImageGetter{

		@Override
		public Drawable getDrawable(String source) {
			Drawable ret;
			Resources res = getResources(); 
			if (source.equals("book_allowed"))
				ret = res.getDrawable(R.drawable.book_allowed);
			else if(source.equals("book_full"))
				ret = res.getDrawable(R.drawable.book_full);
			else if(source.equals("book_forbidden"))
				ret = res.getDrawable(R.drawable.book_forbidden);
			else if(source.equals("book_cancel"))
				ret = res.getDrawable(R.drawable.book_cancel);
			else if(source.equals("menu"))
				ret = res.getDrawable(R.drawable.menu);
			else if(source.equals("guests_none"))
				ret = res.getDrawable(R.drawable.guests_none);
			else if(source.equals("guests_added"))
				ret = res.getDrawable(R.drawable.guests_added);
			else if(source.equals("guests_forbidden"))
				ret = res.getDrawable(R.drawable.guests_forbidden);
			else
				ret = null;
			
			if(ret!=null)
			{
				ret.setBounds(0, 0, 20, 20);
			}
			return ret;
		}
	}
	
	// Background task to acquire the list of meals
	private class GetMealsTask extends AsyncTask<Void,Void,Void>
	{
		private ProgressDialog dlg;
		@Override
		public void onPreExecute()
		{
			/*Log.d(logtag, "Showing progress dialog");
			dlg = ProgressDialog.show(ViewMeals.this, "", "Updating meals", true, true);
			dlg.setOnCancelListener(new OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					Log.d(logtag, "Cancelling dialog");
					GetMealsTask.this.cancel(true);
					ViewMeals.this.finish();
				}
			});*/
		}
		
		@Override
		public Void doInBackground(Void... data)
		{
	        HttpScraper scraper = HttpScraper.getInstance();
			meals = scraper.getMeals();
			
			return null;
		}
		
		@Override
		public void onPostExecute(Void res)
		{
			//Log.d(logtag, "Dismissing progress dialog");
			//dlg.dismiss();
		}
	}
	
	// Background task to cancel a meal
	private class CancelMealTask extends AsyncTask<Uri,Void,Boolean>
	{
		private ProgressDialog dlg;
		@Override
		public void onPreExecute()
		{
			dlg = ProgressDialog.show(ViewMeals.this, "", 
					ViewMeals.this.getResources().getString(R.string.bookCancel), 
					true, true);
			dlg.setOnCancelListener(new OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					CancelMealTask.this.cancel(true);
				}
			});
			dlg.show();
		}
		
		@Override
		public Boolean doInBackground(Uri... data)
		{
			assert data.length > 0;
			
			Uri uri = data[0];
			return HttpScraper.getInstance().cancelMeal(uri);
		}
		
		@Override
		public void onPostExecute(Boolean res)
		{
			cursor.requery();
			dlg.dismiss();
		}
	}

	// Rebuilds the items list from the meals list
	private void refreshList()
	{
		meals_list.clear();
		for(Meal meal: meals)
			addToListItems(meal);
		if(adapter != null)
			adapter.notifyDataSetChanged();
	}
	// Adds a single meal to the items list
	private void addToListItems(final Meal meal)
	{
		HashMap<String, Object> map = new HashMap<String,Object>();
		
		if(meal.spaces == 0)
			map.put(ListItemId.ItemStatus.toString(), drawable.book_full);
		else if(meal.can_cancel)
			map.put(ListItemId.ItemStatus.toString(), drawable.book_cancel);
		else if(meal.can_book)
			map.put(ListItemId.ItemStatus.toString(), drawable.book_allowed);
		else
			map.put(ListItemId.ItemStatus.toString(), drawable.book_forbidden);
		
		map.put(ListItemId.ItemText.toString(), meal.date + " " + meal.time + " " + meal.sitting);
		map.put(ListItemId.ItemMenu.toString(), new Integer(drawable.menu));
		
		/* TODO: Implement guest booking and show the proper icon */
		if(meal.guests > 0 && false)
			map.put(ListItemId.ItemGuest.toString(), new Integer(drawable.guests_added));
		else
			map.put(ListItemId.ItemGuest.toString(), new Integer(drawable.guests_forbidden));
		
		meals_list.add(map);
	}
	
    @Override        
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HttpScraper scraper = HttpScraper.getInstance();
        if(!scraper.isLoggedIn()) finish(); // sanity check
        
        GetMealsTask task = new GetMealsTask();
        task.execute();
        initializeViews();
    }
    
    // Called from GetMealsTask.onPostExecute
    private void initializeViews()
    {
    	Log.d(logtag, "Generating list and setting view");
    	setContentView(R.layout.meals_list);
    	mealsListView = (ListView) findViewById(R.id.MealsListView);
    	
    	registerForContextMenu(mealsListView);
    	mealsListView.setItemsCanFocus(true);
    	mealsListView.setAddStatesFromChildren(true);
    	mealsListView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
    	
		cursor = getContentResolver().query(MealsMetadata.CONTENT_URI,
				null, null, null, null);
		
		startManagingCursor(cursor);
		
		adapter = new SimpleCursorAdapter(this, R.layout.meal_list_item,
				cursor, 
				new String[] { MealsMetadata.CAN_BOOK, MealsMetadata.TITLE, MealsMetadata.MENU,
						MealsMetadata.BOOKED_GUESTS }, 
				new int[] { R.id.bookImage, R.id.title, R.id.menuImage, R.id.guestsImage });
		
		final ViewBinder mealsViewBinder = new ViewBinder() {
			@Override
			public boolean setViewValue(final View view, final Cursor cursor,
					final int columnIndex)
			{
				final String colName = cursor.getColumnName(columnIndex);
				final String date = cursor.getString(cursor.getColumnIndex(MealsMetadata.DATE));
				final String time = cursor.getString(cursor.getColumnIndex(MealsMetadata.TIME));
				final String title = cursor.getString(cursor.getColumnIndex(MealsMetadata.TITLE));
				
				final int id = cursor.getInt(cursor.getColumnIndex(MealsMetadata._ID));
				
				final boolean can_book = cursor.getInt(cursor
						.getColumnIndex(MealsMetadata.CAN_BOOK)) == 1;
				final boolean can_cancel = cursor.getInt(cursor
						.getColumnIndex(MealsMetadata.CAN_CANCEL)) == 1;
				final int spaces = cursor.getInt(cursor.getColumnIndex(MealsMetadata.SPACES));

				final Uri mealUri = Uri.withAppendedPath(
						MealsMetadata.CONTENT_URI, Integer.toString(id));
				
				OnClickListener bookCancelListener = new OnClickListener() {
					@Override
					public void onClick(View v)
					{
						if(can_book && spaces != 0)
							bookItem(mealUri);
						else if (can_cancel)
							cancelItem(mealUri);
						else if(spaces == 0)
							Toast.makeText(ViewMeals.this, R.string.bookFull, Toast.LENGTH_SHORT).show();
						else if (!can_book)
							Toast.makeText(ViewMeals.this, R.string.bookCannot, Toast.LENGTH_SHORT).show();
						
					}};
					
				/* Handle the images */
				if (view instanceof ImageView)
				{
					final ImageView imgView = (ImageView) view;
					
					if(colName.equals(MealsMetadata.MENU) && view instanceof ImageView)
					{
						imgView.setImageResource(R.drawable.menu);
						
						imgView.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v)
							{
								showMenuDialog(mealUri);
							}
						});
					}
					else if (colName.equals(MealsMetadata.BOOKED_GUESTS))
					{
						imgView.setImageResource(R.drawable.guests_forbidden);
					}
					else if (colName.equals(MealsMetadata.CAN_BOOK))
					{
						imgView.setImageResource(R.drawable.guests_forbidden);
						if(spaces == 0)
							imgView.setImageResource(drawable.book_full);
						else if(can_cancel)
							imgView.setImageResource(drawable.book_cancel);
						else if(can_book)
							imgView.setImageResource(drawable.book_allowed);
						else
							imgView.setImageResource(drawable.book_forbidden);
						
						imgView.setOnClickListener(bookCancelListener);
						
					}
					
					return true;
				}
				
				/* Handle the text */
				if (view instanceof TextView)
				{
					StringBuilder strBld = new StringBuilder(date);
					strBld.append(" ");
					strBld.append(time);
					strBld.append("\n");
					strBld.append(title);
					
					((TextView) view).setText(strBld.toString());
					
					view.setOnClickListener(bookCancelListener);
					
					return true;
				}
				
				return false;
			}
			
		};
		adapter.setViewBinder(mealsViewBinder);

    	Button legend = (Button) findViewById(R.id.LegendButton);
    	legend.setOnClickListener(new LegendClickListener());
    	mealsListView.setAdapter(adapter);
    }
    
    // Process the result of the BookMeal activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
    	switch(requestCode)
    	{
    	case MENU_BOOK: 
    	{
    		if(resultCode == BookMeal.BOOK_FAILURE)
    			Toast.makeText(this, R.string.bookFail, Toast.LENGTH_SHORT).show();
    		else if(resultCode == BookMeal.BOOK_SUCCESS)
    			Toast.makeText(this, R.string.bookSuccess, Toast.LENGTH_SHORT).show();
    		
    		break;
    	}
    	}
    }
    
    /* Generate the appropriate context menu for that item 
     * This method relies on finalizeInit registering a stub LongClickListener
     * which always returns false, propagating the event to the ListView */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo)
    {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
    	Meal meal = meals.get(info.position);
    	
    	menu.add(MENU_MENU, info.position, 0, R.string.viewMenu);
   		menu.add(MENU_INFO, info.position, 0, R.string.viewInfo);
   		
   		if(!meal.has_menu)
   			menu.setGroupEnabled(MENU_MENU, false);
   		if(!meal.has_info)
   			menu.setGroupEnabled(MENU_INFO, false);
    	if(meal.can_book)
    		menu.add(MENU_BOOK, meal.id, 0, R.string.book);
    	if(meal.can_change)
    		menu.add(MENU_CHANGE, meal.id, 0, R.string.changeBooking);
    	if(meal.can_cancel)
    		menu.add(MENU_CANCEL, meal.id, 0, R.string.cancel);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
    	super.onContextItemSelected(item);

		Uri mealUri = Uri.withAppendedPath(MealsMetadata.CONTENT_URI,
				Integer.toString(item.getItemId()));
		
    	switch(item.getGroupId())
    	{
    	case MENU_MENU: {
    		showMenuDialog(mealUri);
    		break;
    		}
    	case MENU_INFO: {
    		showInfoDialog(mealUri);
    		break;
    		}
    	case MENU_BOOK: {
    		
    		bookItem(mealUri);
    		break;
    		}
    	case MENU_CANCEL: {
    		cancelItem(mealUri);
    		break;
    	}
    	case MENU_CHANGE: {
    		changeItem(mealUri);
    		break;
    	}
    	
    	}
    	return true;
    }
    
    private void changeItem(Uri mealUri)
    {
		Intent intent = new Intent("name.kratunov.mealbooking.CHANGE_MEAL");
		intent.putExtra("id", Integer.parseInt(mealUri.getLastPathSegment()));
		intent.setData(mealUri);

		startActivityForResult(intent, MENU_CHANGE);
    }
    // Starts the BookMeal activity given an URI to a specific meal
    private void bookItem(Uri mealUri)
    {
		Intent intent = new Intent("name.kratunov.mealbooking.BOOK_MEAL");
		intent.putExtra("id", Integer.parseInt(mealUri.getLastPathSegment()));
		intent.setData(mealUri);
		
		startActivityForResult(intent, MENU_BOOK);		
    }
    // Starts the CancelMealTask background task given an index in the meals list
    private void cancelItem(Uri mealUri)
    {
		CancelMealTask task = new CancelMealTask();
		task.execute(mealUri);
    }
    
    private class GetDetailsTask extends AsyncTask<Uri, Void, String>
    {
    	private int type;
    	private ProgressDialog dlg;
    	
    	public GetDetailsTask(int tasktype)
    	{
    		type = tasktype;
    	}
    	
    	@Override
    	public void onPreExecute()
    	{
    		dlg = ProgressDialog.show(ViewMeals.this, "", 
    				ViewMeals.this.getString(R.string.request), 
    				true, true);
			dlg.setOnCancelListener(new OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					Log.d(logtag, "Cancelling dialog");
					GetDetailsTask.this.cancel(true);
					dlg.dismiss();
				}
			});
			dlg.show();
    	}

		@Override
		protected String doInBackground(Uri... params) {
			assert params.length > 0;
			Uri uri = params[0];
			Uri.Builder bld = uri.buildUpon();
			ContentResolver resolver = getContentResolver();
			switch(type)
			{
			case MENU_MENU:
			{
				bld.fragment(MealsMetadata.FRAGMENT_MENU);
				
				Cursor c = resolver.query(bld.build(), null, null, null, null);
				int colId = c.getColumnIndex(MealsMetadata.MENU);
				
				c.moveToFirst();
				
				if(!c.isAfterLast() && !c.isNull(colId))
					return c.getString(colId);
				else
	    			return null;
			}
			case MENU_INFO:
			{
				bld.fragment(MealsMetadata.FRAGMENT_INFO);
				
				Cursor c = resolver.query(bld.build(), null, null, null, null);
				int colId = c.getColumnIndex(MealsMetadata.EXTRA_INFO);
				
				c.moveToFirst();
				
				if(!c.isAfterLast() && !c.isNull(colId))
					return c.getString(colId);
				else
	    			return null;
			}
			default:
				Log.e(logtag, "Unknown type in GetDetailsTask");
			}
			
			return null;
		}
		
		public void onPostExecute(String res)
		{
			dlg.dismiss();
			if(res != null && res.trim().length() > 0)
			{
				AlertDialog.Builder alert = new AlertDialog.Builder(ViewMeals.this);
				alert.setMessage(res);
				alert.show();
			}
			else
			{
				Toast.makeText(ViewMeals.this, 
						R.string.requestFail, 
						Toast.LENGTH_SHORT).show();
			}
		}
    	
    }
    
    // Shows the menu for the meal at index `id` in the meals list
    private void showMenuDialog(Uri mealUri)
    {
    	GetDetailsTask task = new GetDetailsTask(MENU_MENU);
    	task.execute(mealUri);
    }
    // Shows the info for the meal at index `id` in the meals list
    private void showInfoDialog(Uri mealUri)
    {
    	GetDetailsTask task = new GetDetailsTask(MENU_INFO);
    	task.execute(mealUri);
    }
    
}