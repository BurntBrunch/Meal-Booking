package name.kratunov.mealbooking;

import java.util.List;

public class Meal {
	int id = 0;
	String date, time, sitting, info;
	int spaces = -1, guests = -1, booked = -1;
	List<String> menu = null;
	
	boolean has_info = false, has_menu = false, can_book = false,
			can_cancel = false, can_change = false;
	
	public String getMenuString(boolean oneLine)
	{
		String res = "";
		if(menu != null)
		{
			for(String item: menu)
				if(oneLine)
					res += item + "; ";
				else
					res += item + "\n";
		}
		return res;
	}
}