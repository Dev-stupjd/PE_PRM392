package com.koigzzzz.cex.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.koigzzzz.cex.R;
import com.koigzzzz.cex.models.Wallet;
import com.koigzzzz.cex.utils.FirebaseHelper;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView tvUsername, tvEmail;
    private Button btnSendTokens;
    private FirebaseHelper firebaseHelper;
    private String userId;
    private Wallet userWallet;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = FirebaseHelper.getInstance();
        userId = firebaseHelper.getCurrentUser() != null ? firebaseHelper.getCurrentUser().getUid() : null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        if (userId == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return view;
        }

        tvUsername = view.findViewById(R.id.tvUsername);
        tvEmail = view.findViewById(R.id.tvEmail);
        btnSendTokens = view.findViewById(R.id.btnSendTokens);

        btnSendTokens.setOnClickListener(v -> showSendTokenDialog());

        loadUserProfile();

        return view;
    }

    private void loadUserProfile() {
        if (getContext() == null || userId == null) return;

        firebaseHelper.getUserDocument(userId, task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot doc = task.getResult();
                if (doc != null && doc.exists()) {
                    String username = doc.getString("username");
                    String email = doc.getString("email");
                    Map<String, Object> walletMap = (Map<String, Object>) doc.get("wallet");

                    if (username != null) {
                        tvUsername.setText(username);
                    }
                    if (email != null) {
                        tvEmail.setText(email);
                    }

                    if (walletMap != null) {
                        userWallet = new Wallet();
                        userWallet.loadFromMap(walletMap); // Supports dynamic tokens
                    }
                }
            }
        });
    }

    private void showSendTokenDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_send_token, null);
        builder.setView(dialogView);
        builder.setTitle("Send Tokens");

        EditText etRecipientUsername = dialogView.findViewById(R.id.etRecipientUsername);
        Spinner spinnerToken = dialogView.findViewById(R.id.spinnerToken);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        TextView tvAvailableBalance = dialogView.findViewById(R.id.tvAvailableBalance);

        // Set available balance for selected token
        spinnerToken.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String[] tokens = getResources().getStringArray(R.array.token_symbols);
                String selectedToken = tokens[position];
                if (userWallet != null) {
                    double balance = userWallet.getBalance(selectedToken);
                    NumberFormat format = new DecimalFormat("#,##0.00000000");
                    tvAvailableBalance.setText("Available: " + format.format(balance) + " " + selectedToken);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        // Initialize balance display
        if (userWallet != null && spinnerToken.getSelectedItemPosition() >= 0) {
            String[] tokens = getResources().getStringArray(R.array.token_symbols);
            String selectedToken = tokens[spinnerToken.getSelectedItemPosition()];
            double balance = userWallet.getBalance(selectedToken);
            NumberFormat format = new DecimalFormat("#,##0.00000000");
            tvAvailableBalance.setText("Available: " + format.format(balance) + " " + selectedToken);
        }

        builder.setPositiveButton("Send", null);
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
                String recipientUsername = etRecipientUsername.getText().toString().trim();
                String selectedToken = getResources().getStringArray(R.array.token_symbols)[spinnerToken.getSelectedItemPosition()];
                String amountStr = etAmount.getText().toString().trim();

                if (TextUtils.isEmpty(recipientUsername)) {
                    Toast.makeText(getContext(), "Please enter recipient username", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(amountStr)) {
                    Toast.makeText(getContext(), "Please enter amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amountStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (amount <= 0) {
                    Toast.makeText(getContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if user has enough balance
                if (userWallet == null) {
                    Toast.makeText(getContext(), "Error loading wallet", Toast.LENGTH_SHORT).show();
                    return;
                }

                double balance = userWallet.getBalance(selectedToken);
                if (balance < amount) {
                    Toast.makeText(getContext(), "Insufficient balance", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Prevent sending to yourself
                if (recipientUsername.equalsIgnoreCase(tvUsername.getText().toString())) {
                    Toast.makeText(getContext(), "You cannot send tokens to yourself", Toast.LENGTH_SHORT).show();
                    return;
                }

                sendTokens(recipientUsername, selectedToken, amount, dialog);
            });
        });
        
        dialog.show();
    }

    private void sendTokens(String recipientUsername, String tokenSymbol, double amount, AlertDialog dialog) {
        if (getContext() == null || userId == null) return;

        // Find recipient user by username
        firebaseHelper.findUserByUsername(recipientUsername, findTask -> {
            if (findTask.isSuccessful() && findTask.getResult() != null) {
                QuerySnapshot querySnapshot = findTask.getResult();
                if (querySnapshot.isEmpty()) {
                    Toast.makeText(getContext(), "User not found: " + recipientUsername, Toast.LENGTH_SHORT).show();
                    return;
                }

                DocumentSnapshot recipientDoc = querySnapshot.getDocuments().get(0);
                String recipientUserId = recipientDoc.getString("userId");
                
                if (recipientUserId == null || recipientUserId.equals(userId)) {
                    Toast.makeText(getContext(), "Invalid recipient", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Perform transfer
                firebaseHelper.transferTokens(userId, recipientUserId, tokenSymbol, amount, transferTask -> {
                    if (transferTask.isSuccessful()) {
                        Toast.makeText(getContext(), "Tokens sent successfully!", Toast.LENGTH_SHORT).show();
                        // Reload user profile to update balance
                        loadUserProfile();
                        // Dismiss dialog
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    } else {
                        String error = transferTask.getException() != null ? 
                                transferTask.getException().getMessage() : "Transfer failed";
                        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(getContext(), "Error finding user: " + recipientUsername, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
    }
}
