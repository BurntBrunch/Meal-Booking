package name.kratunov.mealbooking;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

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

import android.util.Log;

public class HttpScraper {
	public class Meal {
		int id = 0;
		String date, time, sitting;
		int spaces, guests, booked;
		List<String> menu = null;
		String info;
		public String getMenuString()
		{
			String res = "";
			if(menu != null)
			{
				for(String item: menu)
					res += item + ";";
			}
			return res;
		}
	}

	final private String cookieUrl = "http://meals.keble.ox.ac.uk/login.php";
	final private String loginUrl = "http://meals.keble.ox.ac.uk/login.php?ccsForm=Login";
	final private String mealsUrl = "http://meals.keble.ox.ac.uk/mealbooking.php";
	final private String menuUrl = "http://meals.keble.ox.ac.uk/sittingmenu.php";
	final private String infoUrl = "http://meals.keble.ox.ac.uk/sittinginfo.php";
	final private String logtag = "HTTPScraper";

	final private String userAgent = "MealBooking Android/0.01a";

	private DefaultHttpClient client = new DefaultHttpClient();

	private boolean loggedIn = false;

	public HttpScraper() {
		client.getParams().setParameter("http.protocol.cookie-policy",
				CookiePolicy.BROWSER_COMPATIBILITY);
		client.getParams().setParameter("http.protocol.expect-continue", false);
	}

	private void addCommonHeaders(AbstractHttpMessage request) {
		request.addHeader("Referer", cookieUrl);
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

	private String executeRequest(HttpUriRequest request)
	{
		HttpResponse response = null;
		String content = null;
		try {
			response = client.execute(request);
			Log.v(logtag, response.getStatusLine().toString());
			content = getResponseEntity(response);

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return content;
	}
	// This method uses thnamee default parser and if it fails, resorts to the
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
		Log.v(logtag, "Parsed " + res + " from '" + num + "'");
		return res;
	}

	public boolean login(String username, String password) {
		// Do a normal GET request to accquire a PHP session id
		HttpGet cookierequest = new HttpGet(cookieUrl);
		addCommonHeaders(cookierequest);

		try {
			HttpResponse response = client.execute(cookierequest);
			Header[] cookies = response.getHeaders("Set-Cookie");

			for (int i = 0; i < cookies.length; i++)
				Log.d(logtag, cookies[i].toString());

		} catch (ClientProtocolException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		// Now that we have a session id, try to log us in
		HttpPost request = new HttpPost(loginUrl);
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
		Log.d(logtag, "POST data is: " + loginData);
		String content = executeRequest(request);

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

		HttpGet request = new HttpGet(mealsUrl);
		addCommonHeaders(request);
		String html = null;
		try {
			HttpResponse response = client.execute(request);
			html = getResponseEntity(response);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		Document doc = Jsoup.parse(html);
		Elements rows = doc.select("tr.RowAS1");
		Log.v(logtag, html);
		if (rows.isEmpty()) {
			Log.e(logtag, "Could not get the rows in the table");
			return null;
		} else
			Log.d(logtag, "Got " + rows.size() + " rows");

		List<Meal> meals = new ArrayList<Meal>();

		for (Element row : rows) {
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

			meals.add(meal);
		}
		return meals;
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
	private Meal getMenu(Meal meal)
	{
		HttpPost request = new HttpPost(menuUrl);
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
		String content = executeRequest(request);
		
		Document doc = Jsoup.parse(content);
		Elements rows = doc.select("td[align=center]");
		if(rows.size() == 0)
		{
			Log.e(logtag, "Could not get menu for meal " + meal.id);
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
	private Meal getInfo(Meal meal)
	{
		HttpPost request = new HttpPost(infoUrl);
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
		String content = executeRequest(request);
		
		Document doc = Jsoup.parse(content);
		Elements text = doc.select("td > p[align=center]");
		String info = text.first().text().replace((char) 0xA0, ' ').trim();
		if(info.length() == 0)
		{
			Log.e(logtag, "Could not get info for meal " + meal.id);
			Log.v(logtag, content);
			return meal;
		}
		else
			Log.d(logtag, "Got info for meal " + meal.id);
		meal.info = info;
		
		return meal;
	}
}
