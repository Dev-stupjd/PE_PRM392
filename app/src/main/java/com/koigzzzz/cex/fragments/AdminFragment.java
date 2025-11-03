package com.koigzzzz.cex.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.koigzzzz.cex.R;
import com.koigzzzz.cex.adapters.TokenAdminAdapter;
import com.koigzzzz.cex.models.Token;
import com.koigzzzz.cex.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class AdminFragment extends Fragment {

    private RecyclerView recyclerView;
    private TokenAdminAdapter adapter;
    private List<Token> tokenList;
    private FirebaseHelper firebaseHelper;
    private Button btnAddToken;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = FirebaseHelper.getInstance();
        
        // Check if user is admin
        if (!firebaseHelper.isAdmin()) {
            // Not admin, redirect or show error
            if (getContext() != null) {
                Toast.makeText(getContext(), "Access denied. Admin only.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin, container, false);

        if (!firebaseHelper.isAdmin()) {
            return view;
        }

        recyclerView = view.findViewById(R.id.recyclerViewTokens);
        btnAddToken = view.findViewById(R.id.btnAddToken);

        tokenList = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new TokenAdminAdapter(tokenList, new TokenAdminAdapter.OnTokenActionListener() {
            @Override
            public void onEdit(Token token) {
                showEditTokenDialog(token);
            }

            @Override
            public void onDelete(Token token) {
                showDeleteConfirmDialog(token);
            }
        });
        recyclerView.setAdapter(adapter);

        btnAddToken.setOnClickListener(v -> showAddTokenDialog());

        loadTokens();
        seedDefaultTokensIfNeeded();

        return view;
    }

    /**
     * Seed default tokens (BTC, ETH, SOL, BNB) if no tokens exist in database
     */
    private void seedDefaultTokensIfNeeded() {
        if (getContext() == null) return;

        firebaseHelper.getAllTokens(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot.isEmpty()) {
                    // No tokens exist, create default ones
                    Token btc = new Token("BTC", "Bitcoin", true);
                    Token eth = new Token("ETH", "Ethereum", true);
                    Token sol = new Token("SOL", "Solana", true);
                    Token bnb = new Token("BNB", "Binance Coin", true);

                    firebaseHelper.createToken(btc, createBtcTask -> {
                        if (createBtcTask.isSuccessful()) {
                            firebaseHelper.createToken(eth, null);
                            firebaseHelper.createToken(sol, null);
                            firebaseHelper.createToken(bnb, createBnbTask -> {
                                // Reload tokens after seeding
                                loadTokens();
                            });
                        }
                    });
                }
            }
        });
    }

    private void loadTokens() {
        if (getContext() == null) return;

        firebaseHelper.getAllTokens(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                QuerySnapshot querySnapshot = task.getResult();
                tokenList.clear();
                
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    try {
                        Token token = new Token();
                        token.setTokenId(doc.getId());
                        token.setSymbol(doc.getString("symbol"));
                        token.setName(doc.getString("name"));
                        Boolean enabled = doc.getBoolean("enabled");
                        token.setEnabled(enabled != null && enabled);
                        
                        Object createdAtObj = doc.get("createdAt");
                        Object updatedAtObj = doc.get("updatedAt");
                        if (createdAtObj != null) token.setCreatedAt(((Number) createdAtObj).longValue());
                        if (updatedAtObj != null) token.setUpdatedAt(((Number) updatedAtObj).longValue());
                        
                        tokenList.add(token);
                    } catch (Exception e) {
                        android.util.Log.e("AdminFragment", "Error parsing token: " + doc.getId(), e);
                    }
                }
                
                if (isAdded() && getContext() != null) {
                    adapter.notifyDataSetChanged();
                }
            } else {
                if (isAdded() && getContext() != null) {
                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                    Toast.makeText(getContext(), "Error loading tokens: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showAddTokenDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_token_edit, null);
        builder.setView(dialogView);
        builder.setTitle("Add New Token");

        EditText etSymbol = dialogView.findViewById(R.id.etSymbol);
        EditText etName = dialogView.findViewById(R.id.etName);
        Switch switchEnabled = dialogView.findViewById(R.id.switchEnabled);

        builder.setPositiveButton("Add", null);
        builder.setNegativeButton("Cancel", null);
        
        AlertDialog dialog = builder.create();
        
        dialog.setOnShowListener(d -> {
            // Style the buttons for better visibility
            if (getContext() != null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getContext(), R.color.binance_green));
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(getContext(), R.color.binance_text_primary));
            }
            
            // Set button click listeners after dialog is shown
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String symbol = etSymbol.getText().toString().trim();
            String name = etName.getText().toString().trim();
            boolean enabled = switchEnabled.isChecked();

            if (TextUtils.isEmpty(symbol)) {
                Toast.makeText(getContext(), "Symbol is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(getContext(), "Name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            Token token = new Token(symbol, name, enabled);
            firebaseHelper.createToken(token, createTask -> {
                if (createTask.isSuccessful()) {
                    Toast.makeText(getContext(), "Token added successfully", Toast.LENGTH_SHORT).show();
                    loadTokens();
                    dialog.dismiss();
                } else {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error adding token: " + createTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            });
        });
        
        dialog.show();
    }

    private void showEditTokenDialog(Token token) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_token_edit, null);
        builder.setView(dialogView);
        builder.setTitle("Edit Token");

        EditText etSymbol = dialogView.findViewById(R.id.etSymbol);
        EditText etName = dialogView.findViewById(R.id.etName);
        Switch switchEnabled = dialogView.findViewById(R.id.switchEnabled);

        etSymbol.setText(token.getSymbol());
        etName.setText(token.getName());
        switchEnabled.setChecked(token.isEnabled());

        builder.setPositiveButton("Update", null);
        builder.setNegativeButton("Cancel", null);
        
        AlertDialog dialog = builder.create();
        
        dialog.setOnShowListener(d -> {
            // Style the buttons for better visibility
            if (getContext() != null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getContext(), R.color.binance_green));
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(getContext(), R.color.binance_text_primary));
            }
            
            // Override positive button to prevent auto-dismiss
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String symbol = etSymbol.getText().toString().trim();
                String name = etName.getText().toString().trim();
                boolean enabled = switchEnabled.isChecked();

                if (TextUtils.isEmpty(symbol)) {
                    Toast.makeText(getContext(), "Symbol is required", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(getContext(), "Name is required", Toast.LENGTH_SHORT).show();
                    return;
                }

                token.setSymbol(symbol);
                token.setName(name);
                token.setEnabled(enabled);

                firebaseHelper.updateToken(token.getTokenId(), token, updateTask -> {
                    if (updateTask.isSuccessful()) {
                        Toast.makeText(getContext(), "Token updated successfully", Toast.LENGTH_SHORT).show();
                        loadTokens();
                        dialog.dismiss();
                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error updating token: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            });
        });
        
        dialog.show();
    }

    private void showDeleteConfirmDialog(Token token) {
        if (getContext() == null) return;

        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme)
                .setTitle("Delete Token")
                .setMessage("Are you sure you want to delete " + token.getSymbol() + "?")
                .setPositiveButton("Delete", (d, which) -> {
                    firebaseHelper.deleteToken(token.getTokenId(), deleteTask -> {
                        if (deleteTask.isSuccessful()) {
                            Toast.makeText(getContext(), "Token deleted successfully", Toast.LENGTH_SHORT).show();
                            loadTokens();
                        } else {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Error deleting token: " + deleteTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .create();
        
        dialog.setOnShowListener(d -> {
            // Style the buttons for better visibility
            if (getContext() != null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getContext(), R.color.binance_red));
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(getContext(), R.color.binance_text_primary));
            }
        });
        
        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (firebaseHelper.isAdmin()) {
            loadTokens();
        }
    }
}

