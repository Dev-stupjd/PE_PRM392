package com.koigzzzz.cex.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.koigzzzz.cex.R;
import com.koigzzzz.cex.models.Order;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orderList;

    public OrderAdapter(List<Order> orderList) {
        this.orderList = orderList;
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

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSymbol, tvType, tvQuantity, tvPrice, tvTotal, tvStatus, tvTime;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSymbol = itemView.findViewById(R.id.tvSymbol);
            tvType = itemView.findViewById(R.id.tvType);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        public void bind(Order order) {
            tvSymbol.setText(order.getSymbol());
            tvType.setText(order.getType());

            // Color code buy/sell
            if (order.getType().equals(Order.TYPE_BUY)) {
                tvType.setTextColor(Color.parseColor("#0ECB81")); // Green
            } else {
                tvType.setTextColor(Color.parseColor("#F6465D")); // Red
            }

            NumberFormat format = new DecimalFormat("#,##0.00000000");
            tvQuantity.setText(format.format(order.getQuantity()));

            NumberFormat priceFormat = new DecimalFormat("#,##0.00");
            tvPrice.setText("$" + priceFormat.format(order.getPrice()));
            tvTotal.setText("$" + priceFormat.format(order.getTotal()));

            tvStatus.setText(order.getStatus());

            // Color code status
            if (order.getStatus().equals(Order.STATUS_COMPLETED)) {
                tvStatus.setTextColor(Color.parseColor("#0ECB81")); // Green
            } else if (order.getStatus().equals(Order.STATUS_PENDING)) {
                tvStatus.setTextColor(Color.parseColor("#F0B90B")); // Yellow
            } else {
                tvStatus.setTextColor(Color.parseColor("#848E9C")); // Gray
            }

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
            tvTime.setText(sdf.format(new Date(order.getTimestamp())));
        }
    }
}

