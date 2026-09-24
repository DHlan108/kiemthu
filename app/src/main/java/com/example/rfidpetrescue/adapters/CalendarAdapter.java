package com.example.rfidpetrescue.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.models.CalendarDateModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private List<CalendarDateModel> dateList;
    private OnDateClickListener onDateClickListener;

    public interface OnDateClickListener {
        void onDateClick(Date date);
    }

    public void setOnDateClickListener(OnDateClickListener listener) {
        this.onDateClickListener = listener;
    }

    public CalendarAdapter(List<CalendarDateModel> dateList) {
        this.dateList = dateList;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        CalendarDateModel model = dateList.get(position);

        // --- BẮT ĐẦU ĐOẠN ĐÃ SỬA: CHUYỂN SANG THỨ TIẾNG VIỆT ---
        Calendar cal = Calendar.getInstance();
        cal.setTime(model.getDate());
        int dayOfWeekInt = cal.get(Calendar.DAY_OF_WEEK);

        String dayName = "";
        switch (dayOfWeekInt) {
            case Calendar.SUNDAY: dayName = "CN"; break;
            case Calendar.MONDAY: dayName = "T2"; break;
            case Calendar.TUESDAY: dayName = "T3"; break;
            case Calendar.WEDNESDAY: dayName = "T4"; break;
            case Calendar.THURSDAY: dayName = "T5"; break;
            case Calendar.FRIDAY: dayName = "T6"; break;
            case Calendar.SATURDAY: dayName = "T7"; break;
        }
        holder.tvDayOfWeek.setText(dayName);
        // --- KẾT THÚC ĐOẠN ĐÃ SỬA ---

        SimpleDateFormat sdfDate = new SimpleDateFormat("dd", Locale.getDefault());
        holder.tvDateNumber.setText(sdfDate.format(model.getDate()));

        if (model.isToday()) {
            holder.layoutContainer.setBackgroundResource(R.drawable.bg_calendar_today);
            holder.tvDayOfWeek.setTextColor(Color.WHITE);
            holder.tvDateNumber.setTextColor(Color.WHITE);
            holder.dotIndicator.setVisibility(View.INVISIBLE);
        } else {
            holder.layoutContainer.setBackground(null);
            holder.tvDayOfWeek.setTextColor(Color.parseColor("#AAAAAA"));
            holder.tvDateNumber.setTextColor(Color.parseColor("#222222"));

            if (model.isPast()) {
                holder.dotIndicator.setVisibility(View.VISIBLE);
            } else {
                holder.dotIndicator.setVisibility(View.INVISIBLE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            // Cập nhật trạng thái isToday để hiển thị highlight
            for (CalendarDateModel d : dateList) {
                d.setToday(false);
            }
            model.setToday(true);
            notifyDataSetChanged();

            if (onDateClickListener != null) {
                onDateClickListener.onDateClick(model.getDate());
            }
        });
    }

    @Override
    public int getItemCount() {
        return dateList != null ? dateList.size() : 0;
    }

    public static class CalendarViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutContainer;
        TextView tvDayOfWeek, tvDateNumber;
        View dotIndicator;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutContainer = itemView.findViewById(R.id.layoutContainer);
            tvDayOfWeek = itemView.findViewById(R.id.tvDayOfWeek);
            tvDateNumber = itemView.findViewById(R.id.tvDateNumber);
            dotIndicator = itemView.findViewById(R.id.dotIndicator);
        }
    }
}