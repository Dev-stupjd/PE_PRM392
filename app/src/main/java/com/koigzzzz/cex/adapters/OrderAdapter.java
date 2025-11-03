package com.koigzzzz.cex.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.koigzzzz.cex.R;
import com.koigzzzz.cex.models.Order;
import com.koigzzzz.cex.utils.FirebaseHelper;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orderList;
    private OnCancelOrderListener cancelListener;

    public interface OnCancelOrderListener {
        void onCancelOrder(Order order);
    }

    public OrderAdapter(List<Order> orderList) {
        this.orderList = orderList;
    }

    public void setOnCancelOrderListener(OnCancelOrderListener listener) {
        this.cancelListener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSymbol, tvOrderTypeAndTime, tvTimestamp, tvFilledAmount, tvProgressPercent, tvPrice, tvStatus;
        private View viewProgressBar, viewProgressBackground;
        private Button btnCancel;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSymbol = itemView.findViewById(R.id.tvSymbol);
            tvOrderTypeAndTime = itemView.findViewById(R.id.tvOrderTypeAndTime);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvFilledAmount = itemView.findViewById(R.id.tvFilledAmount);
            tvProgressPercent = itemView.findViewById(R.id.tvProgressPercent);
            viewProgressBackground = itemView.findViewById(R.id.viewProgressBackground);
            viewProgressBar = itemView.findViewById(R.id.viewProgressBar);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }

        public void bind(Order order) {
            tvSymbol.setText(order.getSymbol() + "/USDT");
            
            // Order type and buy/sell
            String orderTypeText = order.getOrderType() + " / " + order.getType();
            tvOrderTypeAndTime.setText(orderTypeText);
            
            // Color code buy/sell
            if (order.getType().equals(Order.TYPE_BUY)) {
                tvOrderTypeAndTime.setTextColor(Color.parseColor("#0ECB81")); // Green
            } else {
                tvOrderTypeAndTime.setTextColor(Color.parseColor("#F6465D")); // Red
            }

            // Timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            tvTimestamp.setText(sdf.format(new Date(order.getTimestamp())));

            // Filled / Amount
            double filledAmount = 0.0;
            double totalAmount = order.getQuantity();
            if (order.getStatus().equals(Order.STATUS_COMPLETED)) {
                filledAmount = totalAmount;
            }
            
            NumberFormat format = new DecimalFormat("#,##0.0000");
            tvFilledAmount.setText(format.format(filledAmount) + " / " + format.format(totalAmount));

            // Progress bar
            double progressPercent = 0.0;
            if (totalAmount > 0) {
                progressPercent = (filledAmount / totalAmount) * 100.0;
            }
            tvProgressPercent.setText(String.format("%.0f%%", progressPercent));
            
            // Update progress bar width - use post to ensure view is measured
            final double finalProgressPercent = progressPercent; // Make final for lambda
            viewProgressBackground.post(() -> {
                int parentWidth = viewProgressBackground.getWidth();
                if (parentWidth > 0) {
                    ViewGroup.LayoutParams params = viewProgressBar.getLayoutParams();
                    // Progress bar fills based on percentage of available width
                    params.width = (int) (parentWidth * (finalProgressPercent / 100.0));
                    viewProgressBar.setLayoutParams(params);
                }
            });

            // Price
            NumberFormat priceFormat = new DecimalFormat("#,##0.00");
            tvPrice.setText(priceFormat.format(order.getPrice()));

            // Status
            tvStatus.setText(order.getStatus());
            
            // Color code status
            if (order.getStatus().equals(Order.STATUS_COMPLETED)) {
                tvStatus.setTextColor(Color.parseColor("#0ECB81")); // Green
            } else if (order.getStatus().equals(Order.STATUS_PENDING)) {
                tvStatus.setTextColor(Color.parseColor("#F0B90B")); // Yellow
            } else {
                tvStatus.setTextColor(Color.parseColor("#848E9C")); // Gray
            }

            // Cancel button - only show for pending orders
            if (order.getStatus().equals(Order.STATUS_PENDING)) {
                btnCancel.setVisibility(View.VISIBLE);
                btnCancel.setOnClickListener(v -> {
                    if (cancelListener != null) {
                        cancelListener.onCancelOrder(order);
                    }
                });
            } else {
                btnCancel.setVisibility(View.GONE);
            }
        }
    }
}

