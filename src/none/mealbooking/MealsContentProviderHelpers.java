package none.mealbooking;

import android.net.Uri;
import android.provider.BaseColumns;

final public class MealsContentProviderHelpers {
	public static final String AUTHORITY = "name.kratunov.mealbooking.mealsprovider";
	
	public static final class MealsMetadata implements BaseColumns {
        // This class cannot be instantiated
        private MealsMetadata() {}
        
        public static final String FRAGMENT_MENU = "menu", FRAGMENT_INFO = "info";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/meals");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of meals.
         */
        public static final String CONTENT_TYPE_MEALS = "vnd.android.cursor.dir/vnd.mealbooking.meal";
        
        /**
         * The MIME type of {@link #CONTENT_URI} providing a single meal.
         */
        public static final String CONTENT_TYPE_MEAL = "vnd.android.cursor.item/vnd.mealbooking.meal";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "_id ASC";

		static public final String _ID = "_id", ID = _ID, DATE = "date", TIME = "time",
				TITLE = "title", SPACES = "spaces",
				MAXIMUM_GUESTS = "max_guests", BOOKED_GUESTS = "booked_guests",
				MENU = "menu", EXTRA_INFO = "info", CAN_BOOK = "can_book", 
				CAN_CHANGE = "can_change", CAN_CANCEL = "can_cancel";
	}
}
