package name.kratunov.mealbooking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.kratunov.mealbooking.R.drawable;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
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
import android.view.View.OnLongClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class ViewMeals extends Activity {
	final private String logtag = "ViewMeals";
	final public int MENU_MENU = 0, MENU_INFO = 1, MENU_BOOK = 2, MENU_CANCEL = 3,
		MENU_CHANGE = 4, MENU_ITEMS = 5;
	
	private enum ListItemId {ItemStatus, ItemText, ItemMenu, ItemGuest};
	
	private List<Meal> meals;
	private List<Map<String, Object>> meals_list = 
		new ArrayList<Map<String, Object>>();
	private DecapsulatedSimpleAdapter adapter;
	
	private ListView mealsListView;
	
	private class LegendClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v) {
			AlertDialog.Builder dlg = new AlertDialog.Builder(ViewMeals.this);
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
			
			
			dlg.setMessage(msg);
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
			Log.d(logtag, "Showing progress dialog");
			dlg = ProgressDialog.show(ViewMeals.this, "", "Updating meals", true, true);
			dlg.setOnCancelListener(new OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					Log.d(logtag, "Cancelling dialog");
					GetMealsTask.this.cancel(true);
					ViewMeals.this.finish();
				}
			});
			dlg.show();
		}
		
		@Override
		public Void doInBackground(Void... data)
		{
	        HttpScraper scraper = HttpScraper.getInstance();
			meals = scraper.getMeals(false);
			
			return null;
		}
		
		@Override
		public void onPostExecute(Void res)
		{
			Log.d(logtag, "Dismissing progress dialog");
			dlg.dismiss();
			ViewMeals.this.finalizeInit();
		}
	}
	
	// Background task to cancel a meal
	private class CancelMealTask extends AsyncTask<Meal,Void,Boolean>
	{
		private Meal meal;
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
		public Boolean doInBackground(Meal... data)
		{
			assert data.length > 0;
			
			meal = data[0];
	        HttpScraper scraper = HttpScraper.getInstance();
			return scraper.cancelMeal(meal);
		}
		
		@Override
		public void onPostExecute(Boolean res)
		{
			Log.d(logtag, "Dismissing progress dialog");
			dlg.dismiss();
			if(res)
			{
				meal.can_book = true;
				meal.can_change = false;
				meal.can_cancel = false;
				meal.booked = 0;
				ViewMeals.this.refreshList();
			}
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
    }
    
    // Called from GetMealsTask.onPostExecute
    private void finalizeInit()
    {
    	Log.d(logtag, "Generating list and setting view");
    	setContentView(R.layout.meals_list);
    	mealsListView = (ListView) findViewById(R.id.MealsListView);
    	refreshList();
    	
    	registerForContextMenu(mealsListView);
    	mealsListView.setItemsCanFocus(true);
    	
    	adapter = new DecapsulatedSimpleAdapter(this, meals_list, R.layout.meal_list_item,
    			new String[] {ListItemId.ItemStatus.toString(),
    						  ListItemId.ItemText.toString(), 
    						  ListItemId.ItemMenu.toString(),
    						  ListItemId.ItemGuest.toString()},
    			new int[] {R.id.bookImage, R.id.title, R.id.menuImage, R.id.guestsImage});
    	adapter.setOnClickListener(new ItemButtonsClickListener());
    	
    	adapter.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				return false;
			}
		});
    	
    	Button legend = (Button) findViewById(R.id.LegendButton);
    	legend.setOnClickListener(new LegendClickListener());
    	mealsListView.setAdapter(adapter);
    }
    
    // Hacky way of getting click events from the actual views in the items
    private class ItemButtonsClickListener implements OnClickListener
    {

		@Override
		public void onClick(View v) {
			int pos = mealsListView.getPositionForView(v);
			Log.d(logtag, "Inferred position " + pos);
			switch(v.getId())
			{
			case R.id.title:
			case R.id.bookImage:
				Log.d(logtag, "Got click event from book image " + v.getId());
				Meal meal = meals.get(pos);
				if(meal.can_book && meal.spaces != 0)
					bookItem(pos);
				else if (meal.can_cancel)
					cancelItem(pos);
				else if(meal.spaces == 0)
					Toast.makeText(ViewMeals.this, R.string.bookFull, Toast.LENGTH_SHORT).show();
				else if (!meal.can_book)
					Toast.makeText(ViewMeals.this, R.string.bookCannot, Toast.LENGTH_SHORT).show();
				break;
			case R.id.menuImage:
				showMenuDialog(pos);
				break; 
			case R.id.guestsImage:
				Toast.makeText(ViewMeals.this, R.string.guestsNotImplemented, Toast.LENGTH_LONG).show();
				break;
			case R.id.LinearLayout01:
				Log.d(logtag, "Got click event from layout " + v.getId());
				break;
			}
		}
    	
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
    		{
    			Toast.makeText(this, R.string.bookFail, Toast.LENGTH_SHORT).show();
    		}
    		else if(resultCode == BookMeal.BOOK_SUCCESS)
    		{
    			int pos = intent.getIntExtra("arrayIdx", -1);
    			if(pos!=-1)
    			{
    				// Update the list
    				Meal meal = meals.get(pos);
    				meal.can_book = false;
    				meal.can_cancel = true;
    				meal.can_change = true;
    				
    				refreshList();
    			}
    			Toast.makeText(this, R.string.bookSuccess, Toast.LENGTH_SHORT).show();
    		}
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
    		menu.add(MENU_BOOK, info.position, 0, R.string.book);
    	if(meal.can_change)
    		menu.add(MENU_CHANGE, info.position, 0, R.string.changeBooking);
    	if(meal.can_cancel)
    		menu.add(MENU_CANCEL, info.position, 0, R.string.cancel);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
    	super.onContextItemSelected(item);
    	switch(item.getGroupId())
    	{
    	case MENU_MENU: {
    		showMenuDialog(item.getItemId());
    		break;
    		}
    	case MENU_INFO: {
    		showInfoDialog(item.getItemId());
    		break;
    		}
    	case MENU_BOOK: {
    		bookItem(item.getItemId());
    		break;
    		}
    	case MENU_CANCEL: {
    		cancelItem(item.getItemId());
    		break;
    	}
    	case MENU_CHANGE: {
    		changeItem(item.getItemId());
    		break;
    	}
    	
    	}
    	return true;
    }
    
    private void changeItem(int pos)
    {
    	Meal meal = meals.get(pos);
    	if(meal.can_change)
    	{
    		Intent intent = new Intent(this, BookMeal.class);
    		intent.putExtra("id", meal.id);
    		intent.putExtra("arrayIdx", pos);
    		intent.putExtra("change", true);
    		
    		startActivityForResult(intent, MENU_CHANGE);
    	}
    	else if(meal.spaces == 0)
    		Toast.makeText(this, R.string.bookFull, Toast.LENGTH_SHORT).show();
    	else
    		Toast.makeText(this, R.string.bookCannot, Toast.LENGTH_SHORT).show();
    }
    // Starts the BookMeal activity given an index in the meals list
    private void bookItem(int pos)
    {
    	Meal meal = meals.get(pos);
    	if(meal.can_book && meal.spaces > 0)
    	{
    		Intent intent = new Intent(this, BookMeal.class);
    		intent.putExtra("id", meal.id);
    		intent.putExtra("arrayIdx", pos);
    		
    		startActivityForResult(intent, MENU_BOOK);
    	}
    	else if(meal.spaces == 0)
    		Toast.makeText(this, R.string.bookFull, Toast.LENGTH_SHORT).show();
    	else
    		Toast.makeText(this, R.string.bookCannot, Toast.LENGTH_SHORT).show();		
    }
    // Starts the CancelMealTask background task given an index in the meals list
    private void cancelItem(int pos)
    {
    	Meal meal = meals.get(pos);
    	if(meal.can_cancel)
    	{
    		CancelMealTask task = new CancelMealTask();
    		task.execute(meal);
    	}
    	else
    	{
    		Log.e(logtag, "This meal cannot be cancelled");
    	}
    }
    
    private class GetDetailsTask extends AsyncTask<Integer, Void, String>
    {
    	private int id, type;
    	private ProgressDialog dlg;
    	
    	@Override
    	public void onPreExecute()
    	{
    		dlg = ProgressDialog.show(ViewMeals.this, "", 
    				ViewMeals.this.getResources().getString(R.string.request), 
    				true, true);
			dlg.setOnCancelListener(new OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					Log.d(logtag, "Cancelling dialog");
					GetDetailsTask.this.cancel(true);
				}
			});
			dlg.show();
    	}

		@Override
		protected String doInBackground(Integer... params) {
			assert params.length > 0;
			type = params[0];
			id = params[1];
			Meal meal = meals.get(id);
			
			HttpScraper scraper = HttpScraper.getInstance();
			switch(type)
			{
			case MENU_MENU:
	    		meal = scraper.getMenu(meal); 
	    		return meal.getMenuString(false); 
			case MENU_INFO:
				meal = scraper.getInfo(meal);
				return meal.info;
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
    private void showMenuDialog(int id)
    {
    	GetDetailsTask task = new GetDetailsTask();
    	task.execute(MENU_MENU, id);
    }
    // Shows the info for the meal at index `id` in the meals list
    private void showInfoDialog(int id)
    {
    	GetDetailsTask task = new GetDetailsTask();
    	task.execute(MENU_INFO, id);
    }
    
}