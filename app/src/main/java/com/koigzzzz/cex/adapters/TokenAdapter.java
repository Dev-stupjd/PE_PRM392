package com.koigzzzz.cex.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.koigzzzz.cex.R;
import com.koigzzzz.cex.models.TokenPrice;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TokenAdapter extends RecyclerView.Adapter<TokenAdapter.TokenViewHolder> {

    private List<TokenPrice> tokenList;
    private OnTokenClickListener listener;

    public interface OnTokenClickListener {
        void onTokenClick(TokenPrice tokenPrice);
    }

    public TokenAdapter(List<TokenPrice> tokenList, OnTokenClickListener listener) {
        this.tokenList = tokenList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TokenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_token, parent, false);
        return new TokenViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TokenViewHolder holder, int position) {
        TokenPrice token = tokenList.get(position);
        holder.bind(token, listener);
    }

    @Override
    public int getItemCount() {
        return tokenList.size();
    }

    static class TokenViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSymbol, tvName, tvPrice, tvChange24h, tvVolume;

        public TokenViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSymbol = itemView.findViewById(R.id.tvSymbol);
            tvName = itemView.findViewById(R.id.tvName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvChange24h = itemView.findViewById(R.id.tvChange24h);
            tvVolume = itemView.findViewById(R.id.tvVolume);
        }

        public void bind(TokenPrice token, OnTokenClickListener listener) {
            tvSymbol.setText(token.getSymbol());
            tvName.setText(token.getName());

            NumberFormat priceFormat = new DecimalFormat("#,##0.00");
            tvPrice.setText("$" + priceFormat.format(token.getPrice()));

            double change24h = token.getChange24h();
            String changeText = String.format(Locale.US, "%.2f%%", change24h);
            tvChange24h.setText(changeText);

            if (change24h >= 0) {
                tvChange24h.setTextColor(Color.parseColor("#0ECB81")); // Green
            } else {
                tvChange24h.setTextColor(Color.parseColor("#F6465D")); // Red
            }

            NumberFormat volumeFormat = new DecimalFormat("#,##0.00");
            String volumeText = "$" + volumeFormat.format(token.getVolume24h() / 1000000) + "M";
            tvVolume.setText(volumeText);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTokenClick(token);
                }
            });
        }
    }
}

