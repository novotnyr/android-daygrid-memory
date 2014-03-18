package sk.upjs.ics.novotnyr.daygrid;

import static sk.upjs.ics.android.util.Defaults.*;


import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.MonthDisplayHelper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String BUNDLE_KEY_CURRENT_CALENDAR = "currentCalendar";
	private static final int WEEKDAY_COUNT = 7;
	private static final int WEEK_COUNT = 6;
	
	private DayMonthYear currentCalendar = new DayMonthYear();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(savedInstanceState != null) {
			currentCalendar = (DayMonthYear) savedInstanceState.get(BUNDLE_KEY_CURRENT_CALENDAR);
		}
		
		prepareLayout();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(BUNDLE_KEY_CURRENT_CALENDAR, currentCalendar);
		super.onSaveInstanceState(outState);
	}

	private void prepareLayout() {
		MonthDisplayHelper monthDisplay = currentCalendar.toMonthDisplayHelper();
		
		TableLayout layout = new TableLayout(this);
		layout.setStretchAllColumns(true);
		
		for(int weekIndex = 0; weekIndex < WEEK_COUNT; weekIndex++) {
			TableRow row = new TableRow(this);
			
			for (int dayIndex = 0; dayIndex < WEEKDAY_COUNT; dayIndex++) {
				final TextView textView = new TextView(this);
				
				int padding = (int) getResources().getDimension(R.dimen.day_padding);
				textView.setPadding(padding, padding, padding, padding);
				textView.setGravity(Gravity.CENTER);
				textView.setTextSize(getResources().getDimension(R.dimen.text_size));
				
				int day = monthDisplay.getDayAt(weekIndex, dayIndex);
				textView.setText(Integer.toString(day));
				DayMonthYear dmy = new DayMonthYear(currentCalendar, day);
				textView.setTag(dmy);
				textView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						onDayClick(textView);
					}
				});
				if(!monthDisplay.isWithinCurrentMonth(weekIndex, dayIndex)) {
					textView.setEnabled(false);
				} else {
					textView.setBackgroundColor(getColor(dmy).getColor());					
				}
				
				row.addView(textView);
			}
			layout.addView(row);
		}
		
		setContentView(layout);
		
		setTitle("" + currentCalendar.getYear() + " / " + currentCalendar.getMonth());
	}
	
	public void onDayClick(TextView dayTextView) {
		DayMonthYear date = (DayMonthYear) dayTextView.getTag();
		if(isColored(date)) {
			clearColors(date);
		} else {
			pickColorForDate(date);
		}
	}
	
	private boolean isColored(DayMonthYear date) {
		return getColor(date) != DayColor.TRANSPARENT;
	}

	private void clearColors(final DayMonthYear date) {
		Uri uri = getDayColorUri(date);

		getContentResolver().delete(uri, NO_SELECTION, NO_SELECTION_ARGS);
		prepareLayout();
	}

	private void pickColorForDate(final DayMonthYear date) {
			new AlertDialog.Builder(this)
				.setTitle("Vyberte farbu")
				.setItems(R.array.colors_array,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								colorize(date, DayColor.values()[which]);
								prepareLayout();
							}

						})
				.show();
	}
	
	private void colorize(DayMonthYear date, DayColor color) {
		Uri uri = getDayColorUri(date);
		
		ContentValues contentValues = new ContentValues();
		contentValues.put(Database.DayColor.COLOR, color.getColor());
		getContentResolver().insert(uri, contentValues);
	}

	private Uri getDayColorUri(DayMonthYear date) {
		Uri uri = Uri.withAppendedPath(DayColorContentProvider.YEAR_MONTH_DAY_COLOR, String.valueOf(date.getYear()));
		uri = Uri.withAppendedPath(uri, String.valueOf(date.getMonth()));
		uri = Uri.withAppendedPath(uri, String.valueOf(date.getDay()));
		return uri;		
	}

	private DayColor getColor(DayMonthYear date) {
		Uri uri = getDayColorUri(date);
		
		Cursor cursor = getContentResolver().query(uri, NO_PROJECTION, NO_SELECTION, NO_SELECTION_ARGS, NO_SORT_ORDER);
		DayColor color = DayColor.TRANSPARENT;
		if(cursor.moveToNext()) {
			int c = cursor.getInt(cursor.getColumnIndex(Database.DayColor.COLOR));
			color = DayColor.fromColor(c);
		}
		cursor.close();
		return color;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.action_previous_month:
			currentCalendar = currentCalendar.previousMonth();
			prepareLayout();	
			break;
		case R.id.action_next_month:
			currentCalendar = currentCalendar.nextMonth();
			prepareLayout();
			break;
		}
		return super.onOptionsItemSelected(item);		
	}

}
