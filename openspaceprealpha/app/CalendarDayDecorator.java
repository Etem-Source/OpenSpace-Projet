import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.CalendarView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class CalendarDayDecorator {
    private CalendarView calendarView;
    private ArrayList<Date> fullyBookedDates;

    public CalendarDayDecorator(CalendarView calendarView, ArrayList<Date> fullyBookedDates) {
        this.calendarView = calendarView;
        this.fullyBookedDates = fullyBookedDates;
    }

    public void decorate() {
        calendarView.setDateTextAppearance(android.R.style.TextAppearance_Small);
        for (Date date : fullyBookedDates) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendarView.setDate(calendar.getTimeInMillis(), true, true);
            calendarView.setBackground(new ColorDrawable(Color.RED)); // Colorer la date en rouge
        }
    }
}
