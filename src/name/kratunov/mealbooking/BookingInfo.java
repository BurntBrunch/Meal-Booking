package name.kratunov.mealbooking;

import java.util.Map;

public class BookingInfo {
	Meal meal;
	
	// A Name - Value mapping
	// Read-only values
	Map<String, String> meals, dietary_requirements;
	
	// Read-write values
	String additional_dietary, additional_info, meal_choice, diet_choice;
	
	// Store additional `secret` values
	Map<String, String> secrets;
	
}
