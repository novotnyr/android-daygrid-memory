package sk.upjs.ics.novotnyr.daygrid;

import static android.content.ContentResolver.CURSOR_ITEM_BASE_TYPE;
import static android.content.ContentResolver.SCHEME_CONTENT;
import static sk.upjs.ics.android.util.Defaults.NO_CONTENT_OBSERVER;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class DayColorContentProvider extends ContentProvider {
	private static final String AUTHORITY = "sk.upjs.ics.novotnyr.daygrid.provider";

	public static final Uri YEAR_MONTH_DAY_COLOR = new Uri.Builder().scheme(SCHEME_CONTENT).authority(AUTHORITY).appendPath(Database.DayColor.TABLE_NAME).build();

	private static final String MIME_TYPE_DAYCOLOR_SINGLE_ROW = CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + "." + Database.DayColor.TABLE_NAME;
	
	private static final int YEAR_MONTH_DAY_COLOR_CODE = 0;
	
	private static String[] DAYCOLOR_COLUMNS_ALL = { Database.DayColor._ID, Database.DayColor.TIMESTAMP, Database.DayColor.COLOR };
	
	private UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

	private Map<DayMonthYear, DayColor> colors = new ConcurrentHashMap<DayMonthYear, DayColor>();
	
	@Override
	public boolean onCreate() {
		uriMatcher.addURI(AUTHORITY, Database.DayColor.TABLE_NAME + "/*/*/*", YEAR_MONTH_DAY_COLOR_CODE);
		return true;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		switch(uriMatcher.match(uri)) {
		case YEAR_MONTH_DAY_COLOR_CODE:
			return queryDayColorForYearAndMonth(uri);
		default:
			return null;
		}
	}
	
	private Cursor queryDayColorForYearAndMonth(Uri uri) {
		MatrixCursor cursor = new MatrixCursor(DAYCOLOR_COLUMNS_ALL);;
		
		DayMonthYear filterEntry = toDMY(uri);
		DayColor savedColor = this.colors.get(filterEntry);
		if(savedColor != null) {
			cursor.addRow(new Object[] { 1, filterEntry.toUnixTimestamp(), savedColor.getColor() });
		}
		return cursor;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		switch(uriMatcher.match(uri)) {
		case YEAR_MONTH_DAY_COLOR_CODE:

			DayMonthYear filterEntry = toDMY(uri);
			DayColor savedColor = this.colors.get(filterEntry);
			if(savedColor != null) {
				if(this.colors.remove(filterEntry) != null) {
					getContext().getContentResolver().notifyChange(uri, NO_CONTENT_OBSERVER);
					return 1;
				}
			}
			return 0;
		default:
			return 0;
		}		
	}

	@Override
	public String getType(Uri uri) {
		switch(uriMatcher.match(uri)) {
		case YEAR_MONTH_DAY_COLOR_CODE:
			return MIME_TYPE_DAYCOLOR_SINGLE_ROW;
		default:
			return null;
		}		
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		switch(uriMatcher.match(uri)) {
		case YEAR_MONTH_DAY_COLOR_CODE:
			setColorForYearMonthAndDay(uri, values);
			return uri;
		default:
			return null;
		}
	}

	private void setColorForYearMonthAndDay(Uri uri, ContentValues values) {
		if(!values.containsKey(Database.DayColor.COLOR)) {
			throw new IllegalArgumentException("Content values do not contain value for column " + Database.DayColor.COLOR);
		}
		
		DayMonthYear date = toDMY(uri);
		colors.put(date, DayColor.fromColor(values.getAsInteger(Database.DayColor.COLOR)));
		
		getContext().getContentResolver().notifyChange(uri, NO_CONTENT_OBSERVER);
	}

	private DayMonthYear toDMY(Uri uri) {
		List<String> pathSegments = uri.getPathSegments();
		// "daycolor" = pathSegments.get(0)
		int year = Integer.parseInt(pathSegments.get(1));
		int month = Integer.parseInt(pathSegments.get(2));
		int day = Integer.parseInt(pathSegments.get(3));
		DayMonthYear date = new DayMonthYear(year, month, day);

		return date;
	}


	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
