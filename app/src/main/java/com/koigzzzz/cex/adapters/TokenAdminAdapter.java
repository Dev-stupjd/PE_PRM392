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
import com.koigzzzz.cex.models.Token;

import java.util.List;

public class TokenAdminAdapter extends RecyclerView.Adapter<TokenAdminAdapter.TokenViewHolder> {

    private List<Token> tokenList;
    private OnTokenActionListener listener;

    public interface OnTokenActionListener {
        void onEdit(Token token);
        void onDelete(Token token);
    }

    public TokenAdminAdapter(List<Token> tokenList, OnTokenActionListener listener) {
        this.tokenList = tokenList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TokenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_token_admin, parent, false);
        return new TokenViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TokenViewHolder holder, int position) {
        Token token = tokenList.get(position);
        holder.bind(token, listener);
    }

    @Override
    public int getItemCount() {
        return tokenList.size();
    }

    static class TokenViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSymbol, tvName, tvStatus;
        private Button btnEdit, btnDelete;

        public TokenViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSymbol = itemView.findViewById(R.id.tvSymbol);
            tvName = itemView.findViewById(R.id.tvName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(Token token, OnTokenActionListener listener) {
            tvSymbol.setText(token.getSymbol());
            tvName.setText(token.getName());
            
            if (token.isEnabled()) {
                tvStatus.setText("ENABLED");
                tvStatus.setTextColor(Color.parseColor("#0ECB81")); // Green
            } else {
                tvStatus.setText("DISABLED");
                tvStatus.setTextColor(Color.parseColor("#F6465D")); // Red
            }

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEdit(token);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(token);
                }
            });
        }
    }
}

