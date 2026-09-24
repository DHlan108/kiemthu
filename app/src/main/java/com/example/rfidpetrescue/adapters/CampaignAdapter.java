package com.example.rfidpetrescue.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.models.Campaign;
import java.text.DecimalFormat;
import java.util.List;

public class CampaignAdapter extends RecyclerView.Adapter<CampaignAdapter.ViewHolder> {
    private Context context;
    private List<Campaign> list;
    private OnItemClickListener listener;
    private boolean isAdmin;
    private boolean isFullWidth; // --- THÊM BIẾN NÀY ---

    public interface OnItemClickListener {
        void onItemClick(Campaign campaign);
    }

    // 1. Constructor mặc định (dùng cho các nơi cũ gọi đến, không Admin, không FullWidth)
    public CampaignAdapter(Context context, List<Campaign> list, OnItemClickListener listener) {
        this(context, list, false, false, listener);
    }

    // 2. Constructor cũ của bạn (giữ nguyên để phần Admin không bị lỗi)
    // Mặc định gọi bên Admin thì sẽ là FullWidth luôn (chiếm trọn 1 dòng)
    public CampaignAdapter(Context context, List<Campaign> list, boolean isAdmin, OnItemClickListener listener) {
        this(context, list, isAdmin, isAdmin, listener);
    }

    // 3. Constructor MỚI (Cho phép bật/tắt chính xác Admin và FullWidth)
    public CampaignAdapter(Context context, List<Campaign> list, boolean isAdmin, boolean isFullWidth, OnItemClickListener listener) {
        this.context = context;
        this.list = list;
        this.isAdmin = isAdmin;
        this.isFullWidth = isFullWidth;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isAdmin ? R.layout.item_donate_campaign_admin : R.layout.item_urgent_need;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);

        // --- BẮT ĐẦU LOGIC ÉP KÍCH THƯỚC (Chỉ can thiệp giao diện User) ---
        if (!isAdmin) {
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) view.getLayoutParams();
            if (isFullWidth) {
                // Trang "Tất cả": Kéo tràn viền màn hình
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            } else {
                // Trang chủ cuộn ngang: Cố định kích thước nhỏ
                layoutParams.width = (int) (310 * context.getResources().getDisplayMetrics().density);
            }
            view.setLayoutParams(layoutParams);
        }
        // --- KẾT THÚC LOGIC ---

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Campaign item = list.get(position);

        holder.tvTitle.setText(item.getTitle());

        boolean isEnded = ("Ended".equalsIgnoreCase(item.getStatus())) ||
                (item.getTarget_amount() > 0 && item.getCurrent_amount() >= item.getTarget_amount());

        if (holder.tvCampaignDesc != null) {
            holder.tvCampaignDesc.setText(item.getSub_title());
        }

        // Đảm bảo text ngày tháng luôn là màu trắng
        if (holder.tvStartDate != null) {
            holder.tvStartDate.setText(item.getStart_date());
            holder.tvStartDate.setTextColor(Color.WHITE);
        }
        if (holder.tvEndDate != null) {
            holder.tvEndDate.setText(item.getEnd_date());
            holder.tvEndDate.setTextColor(Color.WHITE);
        }

        // Logic đổi màu nền layout ngày tháng thành xám khi kết thúc
        if (holder.layoutTimeRange != null) {
            if (isEnded) {
                holder.layoutTimeRange.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D9D9D9")));
            } else {
                holder.layoutTimeRange.setBackgroundTintList(null); // Trả lại màu mặc định của XML
            }
        }

        DecimalFormat df = new DecimalFormat("#,###");
        String current = df.format(item.getCurrent_amount()).replace(",", ".");
        String target = df.format(item.getTarget_amount()).replace(",", ".");

        holder.tvAmountProgress.setText("Tiến độ: " + current + " / " + target);

        if (holder.progressBar != null && item.getTarget_amount() > 0) {
            int progress = (int) ((item.getCurrent_amount() * 100) / item.getTarget_amount());
            if (progress > 100) progress = 100;
            holder.progressBar.setProgress(progress);
            if (holder.tvPercent != null) {
                holder.tvPercent.setText(progress + "%");
            }
        }

        if (holder.imgCampaign != null) {
            Glide.with(context)
                    .load(item.getImage_url())
                    .placeholder(R.drawable.img_happy)
                    .into(holder.imgCampaign);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPercent, tvAmountProgress, tvCampaignDesc, tvStartDate, tvEndDate;
        ImageView imgCampaign;
        ProgressBar progressBar;
        LinearLayout layoutTimeRange;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvCampaignTitle);
            tvCampaignDesc = itemView.findViewById(R.id.tvCampaignDesc);
            tvStartDate = itemView.findViewById(R.id.tvStartDate);
            tvEndDate = itemView.findViewById(R.id.tvEndDate);

            // Ánh xạ layoutTimeRange để đổi màu nền
            layoutTimeRange = itemView.findViewById(R.id.layoutTimeRange);

            imgCampaign = itemView.findViewById(R.id.imgCampaignUrgent);
            if (imgCampaign == null) {
                imgCampaign = itemView.findViewById(R.id.ivCampaignImage);
            }
            tvPercent = itemView.findViewById(R.id.tvCampaignPercent);
            if (tvPercent == null) {
                tvPercent = itemView.findViewById(R.id.tvPercent);
            }
            tvAmountProgress = itemView.findViewById(R.id.tvProgressAmount);

            progressBar = itemView.findViewById(R.id.pbCampaignProgress);
            if (progressBar == null) {
                progressBar = itemView.findViewById(R.id.pbCampaignProgressAd);
            }
        }
    }
}