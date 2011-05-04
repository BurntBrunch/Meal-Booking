package name.kratunov.mealbooking;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import name.kratunov.mealbooking.MealsContentProviderHelpers.MealsMetadata;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.ContentValues;
import android.util.Log;

public class HttpScraper {
	final private String baseUrl = "http://meals.keble.ox.ac.uk/";
	final private String cookieUrl = "login.php";
	final private String loginUrl = "login.php?ccsForm=Login";
	final private String mealsUrl = "mealbooking.php";
	final private String bookGetUrl = "bookmeal.php";
	final private String bookPostUrl = "custommeal.php?ccsForm=mealToBook";
	final private String changeUrl = "changebooking.php";
	final private String cancelUrl = "cancelbooking.php";
	final private String menuUrl = "sittingmenu.php";
	final private String infoUrl = "sittinginfo.php";
	
	final private String logtag = "HTTPScraper";

	final private String userAgent = "MealBooking Android/0.01a";

	private DefaultHttpClient client = new DefaultHttpClient();

	private boolean loggedIn = false;

	private HttpScraper() {
		client.getParams().setParameter("http.protocol.cookie-policy",
				CookiePolicy.BROWSER_COMPATIBILITY);
		client.getParams().setParameter("http.protocol.expect-continue", false);
	}
	
	static private HttpScraper singleton = null;
	static public HttpScraper getInstance() {
		if(HttpScraper.singleton == null)
			HttpScraper.singleton = new HttpScraper();
		return HttpScraper.singleton;
	}

	private void addCommonHeaders(AbstractHttpMessage request) {
		request.addHeader("Referer", baseUrl+cookieUrl);
		request.addHeader("User-Agent", userAgent);
	}
	private String getResponseEntity(HttpResponse response) {
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			Log.v(logtag, "Entity length: " + entity.getContentLength());
			try {
				String res = EntityUtils.toString(entity);
				return res;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		} else
			return null;
	}
	private String executeRequest(HttpUriRequest request, boolean getResponse)
	{
		HttpResponse response = null;
		String content = null;
		try {
			response = client.execute(request);
			Log.v(logtag, response.getStatusLine().toString());
			if(getResponse)
				content = getResponseEntity(response);

		} catch (ClientProtocolException e) {
			Log.e(logtag, "Could not execute request " + request.toString(), e);
		} catch (IOException e) {
			Log.e(logtag, "IO error" + request.toString(), e);
		}
		
		return content;
	}
	
	// This method uses the default parser and if it fails, resorts to the
	// more inefficient greedy parsing
	private int parseInt(String num) {
		// 0xA0 is non-breaking space
		String tmp = num.replace((char) 0xA0, ' ').trim();
		int res;
		try {
			res = Integer.parseInt(tmp);
		} catch (NumberFormatException e) {
			byte[] bytes = tmp.getBytes();
			int count = 0;
			while (count < bytes.length && Character.isDigit(bytes[count]))
				count++;
			
			if (count > 0)
				res = Integer.parseInt(new String(bytes, 0, count));
			else
				res = 0;
		}
		//Log.v(logtag, "Parsed " + res + " from '" + num + "'");
		return res;
	}

	public boolean login(String username, String password) {
		if(isLoggedIn())
			return true;
		
		// Do a normal GET request to accquire a PHP session id
		HttpGet cookierequest = new HttpGet(baseUrl+cookieUrl);
		addCommonHeaders(cookierequest);

		try {
			HttpResponse response = client.execute(cookierequest);
			Header[] cookies = response.getHeaders("Set-Cookie");

			for (int i = 0; i < cookies.length; i++)
				Log.d(logtag, cookies[i].toString());

		} catch (ClientProtocolException e2) {
			Log.e(logtag, "Protocol exception", e2);
			return false;
		} catch (IOException e2) {
			Log.e(logtag, "IO exception", e2);
			return false;
		}

		// Now that we have a session id, try to log us in
		HttpPost request = new HttpPost(baseUrl+loginUrl);
		addCommonHeaders(request);
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("login", username));
		params.add(new BasicNameValuePair("password", password));
		params.add(new BasicNameValuePair("Button_DoLogin.x", "0"));
		params.add(new BasicNameValuePair("Button_DoLogin.y", "0"));
		params.add(new BasicNameValuePair("Button_DoLogin", "Login"));

		String loginData = URLEncodedUtils.format(params, "UTF-8");
		StringEntity loginEntity = null;
		try {
			loginEntity = new StringEntity(loginData);
		} catch (UnsupportedEncodingException e1) {
			Log.d(logtag, "UnsupportedEncoding", e1);
			e1.printStackTrace();
		}

		request.setEntity(loginEntity);
		Log.d(logtag, "Login POST data is: " + loginData);
		String content = executeRequest(request, true);

		if (content.toLowerCase().contains("error")) {
			Log.d(logtag, "Received an error");
			Log.v(logtag, content);
			return false;
		}

		/*
		 * HTTPClient follows redirects automatically, so by now we should be at
		 * the nojavascript.htm page. Since I can't figure out how to get the
		 * current page's URL without writing a custom RedirectHandler, we
		 * search for a string to do the sanity check.
		 */
		if (!content.contains("window.location = \"mealbooking.php\""))
			Log.e(logtag, "Not at nojavascript.htm page! No idea where we are!");

		// We have successfully logged in.
		Log.i(logtag, "Login was successful");
		loggedIn = true;

		return true;
	}
	public boolean isLoggedIn() {
		return loggedIn;
	}
	
	public List<Meal> getMeals(boolean details) {
		if (!isLoggedIn()) {
			Log.e(logtag, "Must be logged in to get meal list");
			return null;
		}

		HttpGet request = new HttpGet(baseUrl+mealsUrl);
		addCommonHeaders(request);
		String html = null;
		try {
			HttpResponse response = client.execute(request);
			html = getResponseEntity(response);
		} catch (ClientProtocolException e) {
			Log.e(logtag, "Could not get a session id", e);
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			Log.e(logtag, "Could not get a session id", e);
			e.printStackTrace();
			return null;
		}

		Document doc = Jsoup.parse(html);
		Elements rows = doc.select("table > tr.RowAS1");
		//Log.v(logtag, html);
		if (rows.isEmpty()) {
			Log.e(logtag, "Could not get the rows in the table");
			return null;
		} else
			Log.d(logtag, "Got " + rows.size() + " rows");

		List<Meal> meals = new ArrayList<Meal>();
		Application.getContext().getContentResolver().delete(MealsMetadata.CONTENT_URI, "1=1", null);
		
		for (Element row: rows) {
			Meal meal = new Meal();

			// The id is hidden in the input fields
			Elements ids = row.select("input[name=sitUniq]");
			if (ids.isEmpty()) {
				Log.w(logtag, "Could not retrieve id. Moving on.");
				continue;
			}
			meal.id = parseInt(ids.first().attr("value"));

			// This is a hack. The HTML doesn't allow for anything sensible to
			// be done here.
			Elements info = row.select("[align]");

			// The info cells are in the following order:
			// Date, Time, Spaces, Max Guests, Booked
			int count = 1;
			for (Element elem : info) {
				if (!elem.hasText())
					continue;
				switch (count) {
				case 1:
					meal.date = elem.text().trim();
					count++;
					break;
				case 2:
					meal.time = elem.text().trim();
					count++;
					break;
				case 3:
					meal.spaces = parseInt(elem.text());
					count++;
					break;
				case 4:
					meal.guests = parseInt(elem.text());
					count++;
					break;
				case 5:
					meal.booked = parseInt(elem.text());
					count++;
					break;
				default:
					Log.e(logtag,
							"Non-empty info cell which we can't deal with!");
					return meals; // return what we have so far
				}
			}

			String title = row.select("th").get(1).text();
			
			// 0xA0 is the Unicode non-breaking space character, which jsoup 
			// likes to use for &nbsp;
			title = title.replace(" )", ")").replace((char)0xA0, ' ').replace("  ", " ").trim();
			meal.sitting = title;
			
			if(details)
				meal = getDetails(meal);
			
			// Check the forms to get the capabilities
			if(row.select("form[action="+changeUrl+"] > input[type=submit]").size() > 0)
				meal.can_change = true;
			if(row.select("form[action="+cancelUrl+"] > input[type=submit]").size() > 0)
				meal.can_cancel = true;
			if(row.select("form[action="+bookGetUrl+"] > input[type=submit][value=Book]").size() > 0)
				meal.can_book = true;
			if(row.select("form[action="+menuUrl+"] > input[type=submit]").size() > 0)
				meal.has_menu = true;
			if(row.select("form[action="+infoUrl+"] > input[type=submit]").size() > 0)
				meal.has_info = true;
			
			meals.add(meal);
			addToContentProvider(meal);
		}
		return meals;
	}
	private void addToContentProvider(Meal meal)
	{
		ContentValues vals = new ContentValues();
		vals.put(MealsMetadata.ID, meal.id);
		vals.put(MealsMetadata.DATE, meal.date);
		vals.put(MealsMetadata.TIME, meal.time);
		vals.put(MealsMetadata.TITLE, meal.sitting);
		vals.put(MealsMetadata.SPACES, meal.spaces);
		vals.put(MealsMetadata.MAXIMUM_GUESTS, meal.guests);
		vals.put(MealsMetadata.BOOKED_GUESTS, meal.booked);
		vals.put(MealsMetadata.MENU, meal.getMenuString(true));
		vals.put(MealsMetadata.EXTRA_INFO, meal.info);
		vals.put(MealsMetadata.CAN_BOOK, meal.can_book ? 1 : 0);
		vals.put(MealsMetadata.CAN_CANCEL, meal.can_cancel ? 1 : 0);
		vals.put(MealsMetadata.CAN_CHANGE, meal.can_change ? 1 : 0);
		
		Application.getContext().getContentResolver().insert(MealsMetadata.CONTENT_URI, vals);
	}
	private Meal getDetails(Meal meal)
	{
		if (meal.id == 0) {
			Log.e(logtag,"We need the id to fill in the details");
			return meal;
		}
		meal = getMenu(meal);
		meal = getInfo(meal);
		
		return meal;
	}
	public Meal getMenu(Meal meal)
	{
		HttpPost request = new HttpPost(baseUrl+menuUrl);
		addCommonHeaders(request);
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("sitUniq", ""+meal.id));
		params.add(new BasicNameValuePair("btnMenu", "Menu"));

		String menuData = URLEncodedUtils.format(params, "UTF-8");
		StringEntity menuEntity = null;
		try {
			menuEntity = new StringEntity(menuData);
		} catch (UnsupportedEncodingException e1) {
			Log.d(logtag, "UnsupportedEncoding", e1);
			e1.printStackTrace();
		}

		request.setEntity(menuEntity);
		String content = executeRequest(request, true);
		
		Document doc = Jsoup.parse(content);
		Elements rows = doc.select("td[align=center]");
		if(rows.size() == 0)
		{
			Log.d(logtag, "Could not get menu for meal " + meal.id);
			Log.v(logtag, content);
			return meal;
		}
		else
			Log.d(logtag, "Got menu for meal " + meal.id + ": ~" + rows.size() +
					" items");
		
		if(meal.menu == null)
			meal.menu = new ArrayList<String>();
		
		for(Element elem: rows)
		{
			if(!elem.hasText())
				continue;
			
			meal.menu.add(elem.text().replace((char)0xA0, ' ').trim());
		}
		
		return meal;
	}
	public Meal getInfo(Meal meal)
	{
		HttpPost request = new HttpPost(baseUrl+infoUrl);
		addCommonHeaders(request);
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("sitUniq", ""+meal.id));
		params.add(new BasicNameValuePair("btnInfo", "Info"));

		String infoData = URLEncodedUtils.format(params, "UTF-8");
		StringEntity infoEntity = null;
		try {
			infoEntity = new StringEntity(infoData);
		} catch (UnsupportedEncodingException e1) {
			Log.d(logtag, "UnsupportedEncoding", e1);
			e1.printStackTrace();
		}

		request.setEntity(infoEntity);
		String content = executeRequest(request, true);
		
		Document doc = Jsoup.parse(content);
		Elements text = doc.select("td > p[align=center]");
		String info = text.first().text().replace((char) 0xA0, ' ').trim();
		if(info.length() == 0)
		{
			Log.d(logtag, "Could not get info for meal " + meal.id);
			Log.v(logtag, content);
			return meal;
		}
		else
			Log.d(logtag, "Got info for meal " + meal.id);
		meal.info = info;
		
		return meal;
	}
	protected BookingInfo getBookingInfo(final Meal meal, boolean useChangeUrl)
	{
		HttpPost request;
		if(useChangeUrl)
			request = new HttpPost(baseUrl+changeUrl);
		else
			request = new HttpPost(baseUrl+bookGetUrl);
		addCommonHeaders(request);
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("sitUniq", ""+meal.id));
		params.add(new BasicNameValuePair("btnBook", "Book"));

		String bookData = URLEncodedUtils.format(params, "UTF-8");
		StringEntity bookEntity = null;
		try {
			bookEntity = new StringEntity(bookData);
		} catch (UnsupportedEncodingException e1) {
			Log.d(logtag, "UnsupportedEncoding", e1);
			e1.printStackTrace();
		}

		request.setEntity(bookEntity);
		String content = executeRequest(request, true);
		
		Document doc = Jsoup.parse(content);
		Elements meals_elems = doc.select("select[name=lstMeals] > option");
		Map<String, String> meals = new LinkedHashMap<String, String>();
		
		for(Element elem: meals_elems)
			meals.put(elem.text(), elem.attr("value"));
		
		Elements diet_elems = doc.select("select[name=lstSpecDiet] > option");
		Map<String, String> diets = new LinkedHashMap<String, String>();
		for(Element elem: diet_elems)
		{
			Log.v(logtag, "Meal: " + elem.text().trim() + " = " + elem.attr("value"));
			diets.put(elem.text().trim(), elem.attr("value"));
		}
		
		BookingInfo res = new BookingInfo();
		
		if(useChangeUrl)
		{
			Map<String, String> extra = null;
			Elements hidden = doc.select("input[name=hidBk_Uniq]");
			if(hidden.size() > 0)
			{
				extra = new LinkedHashMap<String, String>();
				Element elem = hidden.first();
				if(elem.attr("value").trim().length() > 0)
					extra.put(elem.attr("name"), elem.attr("value"));
			}
			res.secrets = extra;
		}
		
		res.dietary_requirements = diets;
		res.meals = meals;
		res.meal = meal;
		
		return res;
	}
	public boolean bookMeal(final BookingInfo info)
	{
		HttpPost request = new HttpPost(baseUrl+bookPostUrl);
		
		addCommonHeaders(request);
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("lstMeals", info.meals.get(info.meal_choice)));
		params.add(new BasicNameValuePair("lstSpecDiet", info.dietary_requirements.get(info.diet_choice)));
		params.add(new BasicNameValuePair("txtSpecDietInfo", info.additional_dietary));
		params.add(new BasicNameValuePair("txtXtraInfo", info.additional_info));
		params.add(new BasicNameValuePair("hidXtra", ""));
		params.add(new BasicNameValuePair("hidSit_Uniq", ""+info.meal.id));
		if(info.secrets != null)
			for(Entry<String, String> entry: info.secrets.entrySet())
				params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		
		String bookData = URLEncodedUtils.format(params, "UTF-8");
		StringEntity bookEntity = null;
		try {
			bookEntity = new StringEntity(bookData);
		} catch (UnsupportedEncodingException e1) {
			Log.d(logtag, "UnsupportedEncoding", e1);
			e1.printStackTrace();
		}

		request.setEntity(bookEntity);
		
		String response = executeRequest(request, false);
		
		// TODO: Handle fail conditions
		return true;
	}
	public boolean cancelMeal(final Meal meal)
	{
		assert meal != null && meal.id > 0;
		HttpPost request = new HttpPost(baseUrl+cancelUrl);
		
		addCommonHeaders(request);
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("sitUniq", ""+meal.id));
		params.add(new BasicNameValuePair("btnCancel", "Cancel"));

		String cancelData = URLEncodedUtils.format(params, "UTF-8");
		StringEntity cancelEntity = null;
		try {
			cancelEntity = new StringEntity(cancelData);
		} catch (UnsupportedEncodingException e1) {
			Log.d(logtag, "UnsupportedEncoding", e1);
			e1.printStackTrace();
		}

		request.setEntity(cancelEntity);
		
		String response = executeRequest(request, false);
		
		// TODO: Handle fail conditions
		return true;
	}
}
