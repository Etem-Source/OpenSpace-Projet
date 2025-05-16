import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.openspaceprealpha.R;

import java.util.ArrayList;

public class TimeSlotAdapter extends ArrayAdapter<String> {
    private ArrayList<String> unavailableTimeSlots;

    public TimeSlotAdapter(Context context, int resource, String[] objects, ArrayList<String> unavailableTimeSlots) {
        super(context, resource, objects);
        this.unavailableTimeSlots = unavailableTimeSlots;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        TextView textView = (TextView) view;
        String timeSlot = getItem(position);
        if (unavailableTimeSlots.contains(timeSlot)) {
            textView.setTextColor(getContext().getResources().getColor(R.color.red)); // Mettre en rouge les heures non disponibles
        } else {
            textView.setTextColor(getContext().getResources().getColor(R.color.dark_blue));
        }
        return view;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView textView = (TextView) view;
        String timeSlot = getItem(position);
        if (unavailableTimeSlots.contains(timeSlot)) {
            textView.setTextColor(getContext().getResources().getColor(R.color.red)); // Mettre en rouge les heures non disponibles
        } else {
            textView.setTextColor(getContext().getResources().getColor(R.color.dark_blue));
        }
        return view;
    }
}
