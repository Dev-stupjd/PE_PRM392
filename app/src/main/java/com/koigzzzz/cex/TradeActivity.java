package com.koigzzzz.cex;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.koigzzzz.cex.models.Order;
import com.koigzzzz.cex.models.TokenPrice;
import com.koigzzzz.cex.models.Wallet;
import com.koigzzzz.cex.utils.FirebaseHelper;
import com.koigzzzz.cex.utils.PriceManager;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TradeActivity extends AppCompatActivity {

    private TextView tvSymbol, tvPrice, tvChange24h, tvBalance, tvAvailable;
    private EditText etPrice, etQuantity;
    private Button btnBuy, btnSell;
    private MaterialButtonToggleGroup toggleOrderType;
    private TextInputLayout tilPrice;
    private String selectedSymbol;
    private PriceManager priceManager;
    private FirebaseHelper firebaseHelper;
    private Wallet userWallet;
    private String userId;
    private TokenPrice currentTokenPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_trade2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        selectedSymbol = getIntent().getStringExtra("SYMBOL");
        if (selectedSymbol == null) {
            selectedSymbol = "BTC";
        }

        priceManager = PriceManager.getInstance();
        firebaseHelper = FirebaseHelper.getInstance();

        userId = firebaseHelper.getCurrentUser() != null ? firebaseHelper.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        loadUserWallet();
        loadTokenPrice();
        setupListeners();
    }

    private void initializeViews() {
        tvSymbol = findViewById(R.id.tvSymbol);
        tvPrice = findViewById(R.id.tvPrice);
        tvChange24h = findViewById(R.id.tvChange24h);
        tvBalance = findViewById(R.id.tvBalance);
        tvAvailable = findViewById(R.id.tvAvailable);
        etPrice = findViewById(R.id.etPrice);
        etQuantity = findViewById(R.id.etQuantity);
        btnBuy = findViewById(R.id.btnBuy);
        btnSell = findViewById(R.id.btnSell);
        toggleOrderType = findViewById(R.id.toggleOrderType);
        tilPrice = findViewById(R.id.tilPrice);

        tvSymbol.setText(selectedSymbol);

        // Listen to order type toggle changes
        toggleOrderType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnMarketOrder) {
                    // Market order - disable price input and set to current market price
                    tilPrice.setEnabled(false);
                    etPrice.setEnabled(false);
                    if (currentTokenPrice != null) {
                        etPrice.setText(String.valueOf(currentTokenPrice.getPrice()));
                    }
                } else {
                    // Limit order - enable price input
                    tilPrice.setEnabled(true);
                    etPrice.setEnabled(true);
                }
            }
        });
    }

    private void loadUserWallet() {
        firebaseHelper.getUserDocument(userId, task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot doc = task.getResult();
                if (doc != null && doc.exists()) {
                    Map<String, Object> walletMap = (Map<String, Object>) doc.get("wallet");
                    if (walletMap != null) {
                        userWallet = new Wallet();
                        userWallet.setUsdt(((Number) walletMap.get("usdt")).doubleValue());
                        userWallet.setBtc(((Number) walletMap.get("btc")).doubleValue());
                        userWallet.setEth(((Number) walletMap.get("eth")).doubleValue());
                        userWallet.setSol(((Number) walletMap.get("sol")).doubleValue());
                        userWallet.setBnb(((Number) walletMap.get("bnb")).doubleValue());
                        updateBalanceDisplay();
                    }
                }
            }
        });
    }

    private void loadTokenPrice() {
        priceManager.fetchPrice(selectedSymbol, new PriceManager.PriceCallback() {
            @Override
            public void onPriceReceived(TokenPrice tokenPrice) {
                currentTokenPrice = tokenPrice;
                NumberFormat priceFormat = new DecimalFormat("#,##0.00");
                tvPrice.setText("$" + priceFormat.format(tokenPrice.getPrice()));

                double change24h = tokenPrice.getChange24h();
                String changeText = String.format("%.2f%%", change24h);
                tvChange24h.setText(changeText);
                tvChange24h.setTextColor(change24h >= 0 ?
                        getResources().getColor(R.color.binance_green, null) :
                        getResources().getColor(R.color.binance_red, null));

                // Auto-fill current price for limit orders
                etPrice.setText(String.valueOf(tokenPrice.getPrice()));
            }

            @Override
            public void onError(String error) {
                Toast.makeText(TradeActivity.this, "Error loading price: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateBalanceDisplay() {
        if (userWallet != null) {
            NumberFormat format = new DecimalFormat("#,##0.00");
            double usdtBalance = userWallet.getUsdt();
            double tokenBalance = userWallet.getBalance(selectedSymbol);
            tvBalance.setText("USDT: $" + format.format(usdtBalance));
            tvAvailable.setText(selectedSymbol + ": " + format.format(tokenBalance));
        }
    }

    private void setupListeners() {
        btnBuy.setOnClickListener(v -> executeOrder(Order.TYPE_BUY));
        btnSell.setOnClickListener(v -> executeOrder(Order.TYPE_SELL));
    }

    private void executeOrder(String orderType) {
        String quantityStr = etQuantity.getText().toString().trim();
        boolean isMarketOrder = toggleOrderType.getCheckedButtonId() == R.id.btnMarketOrder;

        if (TextUtils.isEmpty(quantityStr)) {
            Toast.makeText(this, "Please enter quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double price;
            String orderTypeStr;
            
            if (isMarketOrder) {
                // Market order - use current market price
                if (currentTokenPrice == null) {
                    Toast.makeText(this, "Please wait for price to load", Toast.LENGTH_SHORT).show();
                    return;
                }
                price = currentTokenPrice.getPrice();
                orderTypeStr = Order.ORDER_TYPE_MARKET;
            } else {
                // Limit order - use entered price
                String priceStr = etPrice.getText().toString().trim();
                if (TextUtils.isEmpty(priceStr)) {
                    Toast.makeText(this, "Please enter price", Toast.LENGTH_SHORT).show();
                    return;
                }
                price = Double.parseDouble(priceStr);
                orderTypeStr = Order.ORDER_TYPE_LIMIT;
                
                if (price <= 0) {
                    Toast.makeText(this, "Price must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            double quantity = Double.parseDouble(quantityStr);
            double total = price * quantity;

            if (quantity <= 0) {
                Toast.makeText(this, "Quantity must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }

            if (orderType.equals(Order.TYPE_BUY)) {
                // Check if user has enough USDT
                if (userWallet.getUsdt() < total) {
                    Toast.makeText(this, "Insufficient USDT balance", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update wallet: deduct USDT, add token
                userWallet.setUsdt(userWallet.getUsdt() - total);
                userWallet.setBalance(selectedSymbol, userWallet.getBalance(selectedSymbol) + quantity);
            } else {
                // Check if user has enough tokens
                if (userWallet.getBalance(selectedSymbol) < quantity) {
                    Toast.makeText(this, "Insufficient " + selectedSymbol + " balance", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update wallet: deduct token, add USDT
                userWallet.setBalance(selectedSymbol, userWallet.getBalance(selectedSymbol) - quantity);
                userWallet.setUsdt(userWallet.getUsdt() + total);
            }

            // Create order
            String orderId = UUID.randomUUID().toString();
            Order order = new Order(orderId, userId, orderType, orderTypeStr, selectedSymbol, quantity, price);
            order.setStatus(Order.STATUS_COMPLETED);

            firebaseHelper.createOrder(order, createTask -> {
                if (createTask.isSuccessful()) {
                    // Update wallet in Firestore
                    firebaseHelper.updateWallet(userId, userWallet.toMap(), updateTask -> {
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(TradeActivity.this,
                                    orderType + " order executed successfully!", Toast.LENGTH_SHORT).show();
                            loadUserWallet();
                            etQuantity.setText("");
                        } else {
                            Toast.makeText(TradeActivity.this,
                                    "Error updating wallet: " + updateTask.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    Toast.makeText(TradeActivity.this,
                            "Error creating order: " + createTask.getException().getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
        }
    }
}
