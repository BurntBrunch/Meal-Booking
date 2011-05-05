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
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
		
		@Override
		public void onClick(View v) 	
		{
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
			
			LinearLayout layout = new LinearLayout(ViewMeals.this);
			layout.setOrientation(LinearLayout.VERTICAL);
			
			TextView text = new TextView(ViewMeals.this);
			text.setText(msg);
			
			layout.addView(text, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			
			final CheckBox box = new CheckBox(ViewMeals.this);
			box.setText(R.string.legendDontShow);
			box.setChecked(true);
			
			layout.addView(box, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			layout.setBackgroundColor(Color.WHITE);
			
			builder.setView(layout).setOnCancelListener(new DialogInterface.OnCancelListener(){
				@Override
				public void onCancel(DialogInterface dialog) {
					dialog.dismiss();
					Editor editor = getSharedPreferences("gui", 0).edit();
					editor.putBoolean("legendShow", !box.isChecked());
					editor.commit();
					
					syncLegendButton();
				}
			}).setCancelable(true).setNegativeButton(R.string.legendClose, 
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.cancel();
						}
					});
			AlertDialog dlg = builder.create();
			dlg.setCanceledOnTouchOutside(true);
			
			dlg.show();
		}
	}
	
	// Background task to acquire the list of meals
	private class GetMealsTask extends AsyncTask<Void,Void,Void>
	{
		@Override
		public void onPreExecute()
		{
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
			cursor.requery();
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
    	
    	syncLegendButton();
    }
    
    public void syncLegendButton()
    {
    	boolean legendShow = getSharedPreferences("gui", 0).getBoolean("legendShow", true);
    	
    	if(!legendShow)
    		findViewById(R.id.LegendButton).setVisibility(View.GONE);
    	else
    		findViewById(R.id.LegendButton).setVisibility(View.VISIBLE);
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