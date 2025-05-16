package com.example.openspaceprealpha;

import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.content.Context;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.Collection;
import java.util.HashSet;

public class DayColorDecorator implements DayViewDecorator {
    private final HashSet<CalendarDay> days;
    private final Drawable drawable;

    public DayColorDecorator(Collection<CalendarDay> days, int color, Context context) {
        this.days = new HashSet<>(days);

        // Créer un ShapeDrawable rond
        ShapeDrawable circle = new ShapeDrawable(new OvalShape());
        circle.getPaint().setColor(ContextCompat.getColor(context, color));
        this.drawable = circle;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return days.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.setBackgroundDrawable(drawable); // Appliquer le Drawable rond
    }
}
