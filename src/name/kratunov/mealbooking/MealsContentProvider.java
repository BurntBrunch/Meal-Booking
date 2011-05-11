package name.kratunov.mealbooking;

import java.util.HashMap;
import java.util.Map;

import name.kratunov.mealbooking.MealsContentProviderHelpers.MealsMetadata;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class MealsContentProvider extends ContentProvider {
	
	private class MealsDatabase extends SQLiteOpenHelper {
		private static final int DB_VERSION = 1;
		private static final String DB_NAME = "meals.db";
		private static final String DB_MEALS_TABLE = "Meals";		
		
		
		public MealsDatabase(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + DB_MEALS_TABLE + " (" +
					MealsMetadata.ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
					MealsMetadata.DATE + " STRING NOT NULL," +
					MealsMetadata.TIME + " STRING," +
					MealsMetadata.TITLE + " STRING NOT NULL," +
					MealsMetadata.SPACES + " INTEGER NOT NULL," +
					MealsMetadata.MAXIMUM_GUESTS + " INTEGER," +
					MealsMetadata.BOOKED_GUESTS + " INTEGER NOT NULL," +
					MealsMetadata.MENU + " STRING," +
					MealsMetadata.EXTRA_INFO + " STRING," +
					MealsMetadata.CAN_BOOK + " INTEGER," + 
					MealsMetadata.CAN_CANCEL + " INTEGER," + 
					MealsMetadata.CAN_CHANGE + " INTEGER);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			throw new UnsupportedOperationException("Upgrading between db versions is not available yet");
		}
	}
	
	private MealsDatabase mDatabase;
	static private final int PATH_MEALS = 1, PATH_MEALS_ID = 2;
	static private UriMatcher sUriMatcher;
	static private Map<String, String> sDefaultProjectionMap;
	
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(MealsContentProviderHelpers.AUTHORITY, "meals", PATH_MEALS);
		sUriMatcher.addURI(MealsContentProviderHelpers.AUTHORITY, "meals/#", PATH_MEALS_ID);
		
		sDefaultProjectionMap = new HashMap<String,String>();
		
		sDefaultProjectionMap.put(MealsMetadata.ID, MealsMetadata.ID);
		sDefaultProjectionMap.put(MealsMetadata.TIME, MealsMetadata.TIME);
		sDefaultProjectionMap.put(MealsMetadata.DATE, MealsMetadata.DATE);
		sDefaultProjectionMap.put(MealsMetadata.SPACES, MealsMetadata.SPACES);
		sDefaultProjectionMap.put(MealsMetadata.TITLE, MealsMetadata.TITLE);
		sDefaultProjectionMap.put(MealsMetadata.MAXIMUM_GUESTS, MealsMetadata.MAXIMUM_GUESTS);
		sDefaultProjectionMap.put(MealsMetadata.BOOKED_GUESTS, MealsMetadata.BOOKED_GUESTS);
		sDefaultProjectionMap.put(MealsMetadata.MENU, MealsMetadata.MENU);
		sDefaultProjectionMap.put(MealsMetadata.EXTRA_INFO, MealsMetadata.EXTRA_INFO);
		sDefaultProjectionMap.put(MealsMetadata.CAN_BOOK, MealsMetadata.CAN_BOOK);
		sDefaultProjectionMap.put(MealsMetadata.CAN_CANCEL, MealsMetadata.CAN_CANCEL);
		sDefaultProjectionMap.put(MealsMetadata.CAN_CHANGE, MealsMetadata.CAN_CHANGE);
	}
	
	@Override
	public boolean onCreate() {
		mDatabase = new MealsDatabase(getContext());
		return true;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
		SQLiteDatabase db = mDatabase.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case PATH_MEALS:
            count = db.delete(MealsDatabase.DB_MEALS_TABLE, selection, selectionArgs);
            break;

        case PATH_MEALS_ID:
            String mealId = uri.getPathSegments().get(1);
            count = db.delete(MealsDatabase.DB_MEALS_TABLE, MealsMetadata._ID + "=" + mealId
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}

	@Override
	public String getType(Uri uri)
	{
		switch(sUriMatcher.match(uri)) {
			case PATH_MEALS:
				return MealsMetadata.CONTENT_TYPE_MEALS;
			case PATH_MEALS_ID:
				return MealsMetadata.CONTENT_TYPE_MEAL;
			default:
				throw new IllegalArgumentException("The Uri could not be parsed: " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		if(values == null)
			throw new IllegalArgumentException("Cannot create empty row");
		if (!values.containsKey(MealsMetadata.TITLE))
			values.put(MealsMetadata.TITLE, "Title-less meal");
		
		if (!values.containsKey(MealsMetadata.DATE))
			values.put(MealsMetadata.DATE, "Unspecified date");
		
		if (!values.containsKey(MealsMetadata.SPACES))
			values.put(MealsMetadata.SPACES, 0);
		
		if (!values.containsKey(MealsMetadata.BOOKED_GUESTS))
			values.put(MealsMetadata.BOOKED_GUESTS, 0);

		long id = mDatabase.getWritableDatabase().insert(
				MealsDatabase.DB_MEALS_TABLE, null, values);
		
		if(id != -1)
		{
			Uri ret = ContentUris.withAppendedId(MealsMetadata.CONTENT_URI, id);
			getContext().getContentResolver().notifyChange(ret, null);
			return ret;
		}
		throw new SQLiteException("Could not insert row into " + uri);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder)
	{	
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(MealsDatabase.DB_MEALS_TABLE);
		
		switch(sUriMatcher.match(uri))
		{
		case PATH_MEALS:
			queryBuilder.setProjectionMap(sDefaultProjectionMap);
			break;
		case PATH_MEALS_ID:
			queryBuilder.setProjectionMap(sDefaultProjectionMap);
			queryBuilder.appendWhere(MealsMetadata.ID + "=" + uri.getPathSegments().get(1));
			break; 
		default:
			throw new IllegalArgumentException("The Uri could not be parsed: " + uri);
		}
		
		String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = MealsMetadata.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

		Cursor ret = queryBuilder.query(mDatabase.getReadableDatabase(),
				projection, selection, selectionArgs, null, null, orderBy);
		
		/* When you use the _MENU and _INFO fragments, you're guaranteed to have 
		 * the appropriate field in the cursor */
		String fragment = uri.getFragment();
		if(fragment != null)
		{
			int colIdx = -1;
			if(fragment.equals(MealsMetadata.FRAGMENT_MENU))
				colIdx = ret.getColumnIndexOrThrow(MealsMetadata.MENU);
			else if(fragment.equals(MealsMetadata.FRAGMENT_INFO))
				colIdx = ret.getColumnIndexOrThrow(MealsMetadata.EXTRA_INFO);
			else return ret;
			
			ret.moveToFirst();
			
			while(!ret.isAfterLast())
			{
				boolean fetch = false;
				
				if(!ret.isNull(colIdx))
					fetch = TextUtils.isEmpty(ret.getString(colIdx));
				
				if(fetch)
				{
					String[] menu = HttpScraper.getInstance().getMenu(uri);
					ContentValues vals = new ContentValues();
					String menuStr = Utils.concatStringArray(menu, '\n');
					
					vals.put(MealsMetadata.MENU, menuStr);
					update(uri, vals, null, null);
				}
				
				ret.moveToNext();
			}
			
			// requery the database
			ret = queryBuilder.query(mDatabase.getReadableDatabase(),
				projection, selection, selectionArgs, null, null, orderBy);
		}

		return ret;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs)
	{
		SQLiteDatabase db = mDatabase.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case PATH_MEALS:
            count = db.update(MealsDatabase.DB_MEALS_TABLE, values, selection, selectionArgs);
            break;
        case PATH_MEALS_ID:
            String mealId = uri.getPathSegments().get(1);
            count = db.update(MealsDatabase.DB_MEALS_TABLE, values, MealsMetadata._ID + "=" + mealId
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}

}
