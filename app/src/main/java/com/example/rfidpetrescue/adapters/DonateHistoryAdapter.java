package com.example.rfidpetrescue.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.models.Donate;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DonateHistoryAdapter extends RecyclerView.Adapter<DonateHistoryAdapter.DonateViewHolder> {

    private List<Donate> donateList;

    public DonateHistoryAdapter(List<Donate> donateList) {
        this.donateList = donateList;
    }

    public void submitList(List<Donate> newList) {
        this.donateList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DonateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_donate_history_admin, parent, false);
        return new DonateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DonateViewHolder holder, int position) {
        Donate donate = donateList.get(position);

        // 1. Hiển thị nội dung hoặc ID giao dịch (Nếu có)
        holder.tvTransactionId.setText("Nội dung: " + donate.getContent());

        // 2. Hiển thị thời gian
        holder.tvDateTime.setText(donate.getTimestamp());

        // 3. Hiển thị tên người tặng (Sử dụng hàm getUserName() đã sửa ở Model)
        holder.tvDonatorName.setText(donate.getUserName());

        // 4. Định dạng và hiển thị số tiền (+ 50.000 vnđ)
        DecimalFormat formatter = new DecimalFormat("#,###");
        String formattedAmount = "+ " + formatter.format(donate.getAmount()).replace(",", ".") + " vnđ";
        holder.tvAmount.setText(formattedAmount);

        String rawDate = donate.getTimestamp();

        if (rawDate != null && !rawDate.isEmpty()) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault());

                Date parsedDate = inputFormat.parse(rawDate);
                holder.tvDateTime.setText(outputFormat.format(parsedDate));

            } catch (ParseException e) {
                holder.tvDateTime.setText(rawDate);
                e.printStackTrace();
            }
        } else {
            holder.tvDateTime.setText("N/A");
        }
    }

    @Override
    public int getItemCount() {
        return donateList != null ? donateList.size() : 0;
    }

    public static class DonateViewHolder extends RecyclerView.ViewHolder {
        // Đã cập nhật đúng ID theo XML mới của bạn
        TextView tvTransactionId, tvDateTime, tvDonatorName, tvAmount;

        public DonateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTransactionId = itemView.findViewById(R.id.tvTransactionId);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvDonatorName = itemView.findViewById(R.id.tvDonatorName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}