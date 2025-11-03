package com.koigzzzz.cex.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.IMarker;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.koigzzzz.cex.R;
import com.koigzzzz.cex.models.Order;
import com.koigzzzz.cex.models.TokenPrice;
import com.koigzzzz.cex.models.Wallet;
import com.koigzzzz.cex.utils.FirebaseHelper;
import com.koigzzzz.cex.utils.PriceHistoryTracker;
import com.koigzzzz.cex.utils.PriceManager;
import com.koigzzzz.cex.utils.PriceMarker;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TradeFragment extends Fragment {

    private TextView tvSymbol, tvPrice, tvChange24h, tvBalance, tvAvailable, tvMarketPriceIndicator;
    private EditText etPrice, etQuantity;
    private Button btnBuy, btnSell;
    private MaterialButtonToggleGroup toggleOrderType;
    private TextInputLayout tilPrice;
    private LineChart chartPrice;
    private String selectedSymbol;
    private PriceManager priceManager;
    private FirebaseHelper firebaseHelper;
    private Wallet userWallet;
    private String userId;
    private TokenPrice currentTokenPrice;
    private Handler priceRefreshHandler;
    private Runnable priceRefreshRunnable;
    private static final long PRICE_REFRESH_INTERVAL = 2 * 60 * 1000; // 2 minutes (to respect 10k/day limit)

    public static TradeFragment newInstance(String symbol) {
        TradeFragment fragment = new TradeFragment();
        Bundle args = new Bundle();
        args.putString("SYMBOL", symbol);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedSymbol = getArguments().getString("SYMBOL", "BTC");
        } else {
            selectedSymbol = "BTC";
        }

        priceManager = PriceManager.getInstance();
        firebaseHelper = FirebaseHelper.getInstance();
        userId = firebaseHelper.getCurrentUser() != null ? firebaseHelper.getCurrentUser().getUid() : null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trade, container, false);

        if (userId == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return view;
        }

        initializeViews(view);
        loadUserWallet();
        loadTokenPrice();
        setupListeners();
        startPriceRefresh();

        return view;
    }

    private void startPriceRefresh() {
        priceRefreshHandler = new Handler(Looper.getMainLooper());
        priceRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAdded() && getContext() != null) {
                    loadTokenPrice();
                    priceRefreshHandler.postDelayed(this, PRICE_REFRESH_INTERVAL);
                }
            }
        };
        priceRefreshHandler.postDelayed(priceRefreshRunnable, PRICE_REFRESH_INTERVAL);
    }

    private void stopPriceRefresh() {
        if (priceRefreshHandler != null && priceRefreshRunnable != null) {
            priceRefreshHandler.removeCallbacks(priceRefreshRunnable);
        }
    }

    private void updateOrderTypeUI(int checkedId) {
        if (checkedId == R.id.btnMarketOrder) {
            // Market order - hide price input, show market price indicator
            tilPrice.setVisibility(View.GONE);
            tvMarketPriceIndicator.setVisibility(View.VISIBLE);
            if (currentTokenPrice != null) {
                tvMarketPriceIndicator.setText("Using current market price: $" + String.format("%.2f", currentTokenPrice.getPrice()));
            } else {
                tvMarketPriceIndicator.setText("Using current market price shown above");
            }
        } else if (checkedId == R.id.btnLimitOrder) {
            // Limit order - show price input, hide market indicator
            tilPrice.setVisibility(View.VISIBLE);
            tvMarketPriceIndicator.setVisibility(View.GONE);
            if (currentTokenPrice != null) {
                DecimalFormat priceInputFormat = new DecimalFormat("#.####");
                etPrice.setText(priceInputFormat.format(currentTokenPrice.getPrice()));
            }
        }
    }

    private void initializeViews(View view) {
        tvSymbol = view.findViewById(R.id.tvSymbol);
        tvPrice = view.findViewById(R.id.tvPrice);
        tvChange24h = view.findViewById(R.id.tvChange24h);
        tvBalance = view.findViewById(R.id.tvBalance);
        tvAvailable = view.findViewById(R.id.tvAvailable);
        etPrice = view.findViewById(R.id.etPrice);
        etQuantity = view.findViewById(R.id.etQuantity);
        btnBuy = view.findViewById(R.id.btnBuy);
        btnSell = view.findViewById(R.id.btnSell);
        toggleOrderType = view.findViewById(R.id.toggleOrderType);
        tilPrice = view.findViewById(R.id.tilPrice);
        tvMarketPriceIndicator = view.findViewById(R.id.tvMarketPriceIndicator);
        chartPrice = view.findViewById(R.id.chartPrice);

        tvSymbol.setText(selectedSymbol);

        // Set initial UI state for limit order (default checked button)
        // Make sure price input is visible and market indicator is hidden
        tilPrice.setVisibility(View.VISIBLE);
        tvMarketPriceIndicator.setVisibility(View.GONE);

        // Setup chart
        setupChart();

        // Listen to order type toggle changes
        toggleOrderType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateOrderTypeUI(checkedId);
            }
        });
    }
    
    private void setupChart() {
        // Chart styling - Binance dark theme
        chartPrice.setBackgroundColor(Color.TRANSPARENT);
        chartPrice.getDescription().setEnabled(false);
        chartPrice.setTouchEnabled(true);
        chartPrice.setDragEnabled(true);
        chartPrice.setScaleEnabled(true);
        chartPrice.setPinchZoom(true);
        chartPrice.setDoubleTapToZoomEnabled(false); // Disable double-tap zoom
        chartPrice.setDrawGridBackground(false);
        chartPrice.getLegend().setEnabled(false);
        chartPrice.setNoDataText("Loading price data...");
        chartPrice.setNoDataTextColor(Color.parseColor("#848E9C"));
        
        // Enable highlighting to show marker
        chartPrice.setHighlightPerTapEnabled(true);
        chartPrice.setHighlightPerDragEnabled(true);
        
        // Create and set custom marker if context is available
        if (getContext() != null) {
            PriceMarker marker = new PriceMarker(getContext(), R.layout.price_marker);
            chartPrice.setMarker(marker);
        }
        
        // X Axis
        XAxis xAxis = chartPrice.getXAxis();
        xAxis.setEnabled(false);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        
        // Y Axis
        YAxis leftAxis = chartPrice.getAxisLeft();
        leftAxis.setEnabled(false);
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawAxisLine(false);
        
        YAxis rightAxis = chartPrice.getAxisRight();
        rightAxis.setEnabled(false);
        rightAxis.setDrawGridLines(false);
        rightAxis.setDrawAxisLine(false);
        
        // Enable auto scale
        chartPrice.setAutoScaleMinMaxEnabled(true);
    }
    
    private void updateChart(TokenPrice token) {
        if (token == null || chartPrice == null) return;
        
        // Get all available price values
        List<Float> priceValues = PriceHistoryTracker.getInstance().getPriceValues(token.getSymbol());
        
        // Create entries
        ArrayList<Entry> entries = new ArrayList<>();
        
        if (priceValues.isEmpty()) {
            // No history at all: create a mini trend based on 24h change
            float currentPrice = (float) token.getPrice();
            double change24h = token.getChange24h();
            
            // Generate 10 data points showing a trend based on 24h change
            int numPoints = 10;
            // Calculate start price based on 24h change
            float startPrice = (float) (currentPrice / (1.0 + change24h / 100.0));
            
            // Use token symbol hash to create unique variation pattern per token
            int tokenHash = token.getSymbol().hashCode();
            
            // Create trend line from start to current price with unique variation
            for (int i = 0; i < numPoints; i++) {
                float progress = (float) i / (numPoints - 1);
                float price = startPrice + (currentPrice - startPrice) * progress;
                // Add token-specific variation using hash
                float variation = (float) (currentPrice * 0.003 * Math.sin(i * 0.5 + tokenHash % 100));
                entries.add(new Entry(i, price + variation));
            }
        } else if (priceValues.size() == 1) {
            // One point: show a flat line with small token-specific variation
            float price = priceValues.get(0);
            int numPoints = 10;
            int tokenHash = token.getSymbol().hashCode();
            
            for (int i = 0; i < numPoints; i++) {
                // Small variation based on token
                float variation = (float) (price * 0.002 * Math.sin(i * 0.3 + tokenHash % 100));
                entries.add(new Entry(i, price + variation));
            }
        } else {
            // We have 2+ data points, use actual historical data
            for (int i = 0; i < priceValues.size(); i++) {
                entries.add(new Entry(i, priceValues.get(i)));
            }
        }
        
        // Create dataset
        LineDataSet dataSet = new LineDataSet(entries, "Price");
        
        // Determine color based on 24h change
        int lineColor;
        if (token.getChange24h() >= 0) {
            lineColor = Color.parseColor("#0ECB81"); // Binance green
        } else {
            lineColor = Color.parseColor("#F6465D"); // Binance red
        }
        
        dataSet.setColor(lineColor);
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(lineColor);
        dataSet.setFillAlpha(30); // Semi-transparent fill
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth curve
        dataSet.setDrawHorizontalHighlightIndicator(true);
        dataSet.setDrawVerticalHighlightIndicator(true);
        dataSet.setHighlightLineWidth(1.5f);
        dataSet.setHighLightColor(Color.parseColor("#F0B90B")); // Binance yellow for highlight
        
        // Create line data
        LineData lineData = new LineData(dataSet);
        chartPrice.setData(lineData);
        
        // Ensure marker is set (in case it wasn't set during setup)
        if (getContext() != null && chartPrice.getMarker() == null) {
            PriceMarker marker = new PriceMarker(getContext(), R.layout.price_marker);
            chartPrice.setMarker(marker);
        }
        
        // Force refresh and animate
        chartPrice.notifyDataSetChanged();
        chartPrice.invalidate();
        
        // Reset chart view to show all data for the selected time frame
        chartPrice.fitScreen();
    }

    private void loadUserWallet() {
        if (getContext() == null || userId == null) return;

        firebaseHelper.getUserDocument(userId, task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot doc = task.getResult();
                if (doc != null && doc.exists()) {
                    Map<String, Object> walletMap = (Map<String, Object>) doc.get("wallet");
                    if (walletMap != null) {
                        userWallet = new Wallet();
                        userWallet.loadFromMap(walletMap); // Supports dynamic tokens
                        if (isAdded() && getContext() != null) {
                            updateBalanceDisplay();
                        }
                    }
                }
            }
        });
    }

    private void loadTokenPrice() {
        if (getContext() == null) return;

        priceManager.fetchPrice(selectedSymbol, new PriceManager.PriceCallback() {
            @Override
            public void onPriceReceived(TokenPrice tokenPrice) {
                if (!isAdded() || getContext() == null) return;

                currentTokenPrice = tokenPrice;
                NumberFormat priceFormat = new DecimalFormat("#,##0.00");
                tvPrice.setText("$" + priceFormat.format(tokenPrice.getPrice()));

                double change24h = tokenPrice.getChange24h();
                String changeText = String.format("%.2f%%", change24h);
                tvChange24h.setText(changeText);
                tvChange24h.setTextColor(change24h >= 0 ?
                        getResources().getColor(R.color.binance_green, null) :
                        getResources().getColor(R.color.binance_red, null));

                // Update UI based on current order type
                int checkedButtonId = toggleOrderType.getCheckedButtonId();
                if (checkedButtonId == R.id.btnMarketOrder) {
                    // Update market price indicator
                    tvMarketPriceIndicator.setText("Using current market price: $" + priceFormat.format(tokenPrice.getPrice()));
                } else if (checkedButtonId == R.id.btnLimitOrder) {
                    // Auto-fill current price for limit orders (only if empty)
                    if (etPrice.getText().toString().trim().isEmpty()) {
                        DecimalFormat priceInputFormat = new DecimalFormat("#.####");
                        etPrice.setText(priceInputFormat.format(tokenPrice.getPrice()));
                    }
                }
                
                // Update chart with price history
                updateChart(tokenPrice);
                
                // Check and execute pending limit orders for this symbol
                checkAndExecutePendingOrders(tokenPrice.getPrice());
            }

            @Override
            public void onError(String error) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Error loading price: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateBalanceDisplay() {
        if (userWallet != null && isAdded() && getContext() != null) {
            NumberFormat format = new DecimalFormat("#,##0.00");
            double usdtBalance = userWallet.getUsdt();
            double tokenBalance = userWallet.getBalance(selectedSymbol);
            tvBalance.setText("USDT: $" + format.format(usdtBalance));
            tvAvailable.setText(selectedSymbol + ": " + format.format(tokenBalance));
        }
    }

    private void checkAndExecutePendingOrders(double currentMarketPrice) {
        if (getContext() == null || userId == null) return;

        // First reload wallet to get latest balance before checking pending orders
        firebaseHelper.getUserDocument(userId, walletTask -> {
            if (!walletTask.isSuccessful() || walletTask.getResult() == null) return;
            
            com.google.firebase.firestore.DocumentSnapshot walletDoc = walletTask.getResult();
            if (walletDoc.exists()) {
                Map<String, Object> walletMap = (Map<String, Object>) walletDoc.get("wallet");
                if (walletMap != null) {
                    userWallet = new Wallet();
                    userWallet.loadFromMap(walletMap); // Supports dynamic tokens
                }
            }

            // Now check pending orders
            firebaseHelper.getPendingOrders(userId, selectedSymbol, task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    com.google.firebase.firestore.QuerySnapshot querySnapshot = task.getResult();
                    
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            Order order = new Order();
                            order.setOrderId(doc.getString("orderId"));
                            order.setType(doc.getString("type"));
                            order.setSymbol(doc.getString("symbol"));
                            
                            Object qtyObj = doc.get("quantity");
                            Object priceObj = doc.get("price");
                            
                            if (qtyObj != null) order.setQuantity(((Number) qtyObj).doubleValue());
                            if (priceObj != null) order.setPrice(((Number) priceObj).doubleValue());
                            
                            // Check if order can be executed
                            // Both buy and sell limit orders: Execute when market price reaches or exceeds limit price
                            // Example: Limit $1100, Market reaches $1100 or above → Execute
                            boolean canExecute = currentMarketPrice >= order.getPrice();
                            
                            if (canExecute) {
                                executePendingOrder(order, doc.getId());
                            }
                        } catch (Exception e) {
                            android.util.Log.e("TradeFragment", "Error processing pending order: " + doc.getId(), e);
                        }
                    }
                }
            });
        });
    }

    private void executePendingOrder(Order order, String documentId) {
        if (getContext() == null || userId == null || userWallet == null) return;

        double total = order.getPrice() * order.getQuantity();
        
        if (order.getType().equals(Order.TYPE_BUY)) {
            // Complete buy order: tokens already reserved, just mark as completed
            // Wallet was already updated when order was created (USDT deducted, tokens will be added)
            // But since it was reserved, we need to actually add the tokens now
            userWallet.setBalance(order.getSymbol(), userWallet.getBalance(order.getSymbol()) + order.getQuantity());
        } else {
            // Complete sell order: USDT already reserved, just mark as completed
            // Wallet was already updated when order was created (tokens deducted, USDT will be added)
            // But since it was reserved, we need to actually add the USDT now
            userWallet.setUsdt(userWallet.getUsdt() + total);
        }

        // Update order status to completed
        firebaseHelper.updateOrderStatus(order.getOrderId(), Order.STATUS_COMPLETED, updateTask -> {
            if (updateTask.isSuccessful()) {
                // Update wallet
                firebaseHelper.updateWallet(userId, userWallet.toMap(), walletTask -> {
                    if (walletTask.isSuccessful() && isAdded() && getContext() != null) {
                        loadUserWallet();
                        android.widget.Toast.makeText(getContext(),
                                order.getType() + " limit order executed at $" + String.format("%.2f", order.getPrice()),
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void setupListeners() {
        btnBuy.setOnClickListener(v -> executeOrder(Order.TYPE_BUY));
        btnSell.setOnClickListener(v -> executeOrder(Order.TYPE_SELL));
    }

    private void executeOrder(String orderType) {
        if (getContext() == null) return;

        String quantityStr = etQuantity.getText().toString().trim();
        boolean isMarketOrder = toggleOrderType.getCheckedButtonId() == R.id.btnMarketOrder;

        if (TextUtils.isEmpty(quantityStr)) {
            Toast.makeText(getContext(), "Please enter quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double price;
            String orderTypeStr;
            
            if (isMarketOrder) {
                // Market order - use current market price from the top display
                if (currentTokenPrice == null) {
                    Toast.makeText(getContext(), "Please wait for price to load", Toast.LENGTH_SHORT).show();
                    return;
                }
                price = currentTokenPrice.getPrice();
                orderTypeStr = Order.ORDER_TYPE_MARKET;
            } else {
                // Limit order - use entered price
                // IMPORTANT: For limit orders, we MUST have current price loaded to check execution
                if (currentTokenPrice == null) {
                    Toast.makeText(getContext(), "Please wait for price to load before placing limit order", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String priceStr = etPrice.getText().toString().trim();
                if (TextUtils.isEmpty(priceStr)) {
                    Toast.makeText(getContext(), "Please enter price for limit order", Toast.LENGTH_SHORT).show();
                    return;
                }
                price = Double.parseDouble(priceStr);
                orderTypeStr = Order.ORDER_TYPE_LIMIT;
                
                if (price <= 0) {
                    Toast.makeText(getContext(), "Price must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            double quantity = Double.parseDouble(quantityStr);
            double total = price * quantity;

            if (quantity <= 0) {
                Toast.makeText(getContext(), "Quantity must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create order
            String orderId = UUID.randomUUID().toString();
            Order order = new Order(orderId, userId, orderType, orderTypeStr, selectedSymbol, quantity, price);
            
            // For limit orders, check if price is favorable for immediate execution
            // IMPORTANT: Limit orders should ONLY execute if current price is available AND condition is met
            boolean shouldExecuteImmediately = false;
            
            if (orderTypeStr.equals(Order.ORDER_TYPE_MARKET)) {
                // Market orders always execute immediately
                shouldExecuteImmediately = true;
            } else if (orderTypeStr.equals(Order.ORDER_TYPE_LIMIT)) {
                // Limit orders: REQUIRED to have current price loaded
                // We already checked above that currentTokenPrice is not null, but double-check here
                if (currentTokenPrice == null) {
                    // This should never happen due to check above, but safety check
                    Toast.makeText(getContext(), "Cannot place limit order: price not loaded", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                double marketPrice = currentTokenPrice.getPrice();
                // Both buy and sell limit orders: Wait until price reaches limit
                // Buy limit: Wait until market price rises to limit (or above)
                // Sell limit: Wait until market price rises to limit (or above) - same logic
                // Example: Market $1080, Limit $1100 → Wait (pending)
                //          Market $1100, Limit $1100 → Execute
                //          Market $1110, Limit $1100 → Execute (price reached/passed limit)
                shouldExecuteImmediately = marketPrice >= price;
                
                // Log for debugging
                android.util.Log.d("TradeFragment", String.format(
                    "Limit %s order: Market=%.2f, Limit=%.2f, Execute=%s",
                    orderType, marketPrice, price, shouldExecuteImmediately
                ));
            }

            // Execute immediately for market orders or favorable limit orders
            // Save as pending for limit orders that can't execute yet
            if (shouldExecuteImmediately) {
                // Execute immediately
                if (orderType.equals(Order.TYPE_BUY)) {
                    // Check if user has enough USDT
                    if (userWallet.getUsdt() < total) {
                        Toast.makeText(getContext(), "Insufficient USDT balance", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Update wallet: deduct USDT, add token
                    userWallet.setUsdt(userWallet.getUsdt() - total);
                    userWallet.setBalance(selectedSymbol, userWallet.getBalance(selectedSymbol) + quantity);
                } else {
                    // Check if user has enough tokens
                    if (userWallet.getBalance(selectedSymbol) < quantity) {
                        Toast.makeText(getContext(), "Insufficient " + selectedSymbol + " balance", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Update wallet: deduct token, add USDT
                    userWallet.setBalance(selectedSymbol, userWallet.getBalance(selectedSymbol) - quantity);
                    userWallet.setUsdt(userWallet.getUsdt() + total);
                }
                order.setStatus(Order.STATUS_COMPLETED);
            } else {
                // Limit order not yet fillable - save as pending
                // Reserve funds for pending orders
                if (orderType.equals(Order.TYPE_BUY)) {
                    // Check if user has enough USDT
                    if (userWallet.getUsdt() < total) {
                        Toast.makeText(getContext(), "Insufficient USDT balance", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Reserve USDT for pending buy order (deduct from available balance)
                    userWallet.setUsdt(userWallet.getUsdt() - total);
                } else {
                    // Check if user has enough tokens
                    if (userWallet.getBalance(selectedSymbol) < quantity) {
                        Toast.makeText(getContext(), "Insufficient " + selectedSymbol + " balance", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Reserve tokens for pending sell order (deduct from available balance)
                    userWallet.setBalance(selectedSymbol, userWallet.getBalance(selectedSymbol) - quantity);
                }
                order.setStatus(Order.STATUS_PENDING);
            }

            firebaseHelper.createOrder(order, createTask -> {
                if (createTask.isSuccessful()) {
                    // Update wallet in Firestore
                    firebaseHelper.updateWallet(userId, userWallet.toMap(), updateTask -> {
                        if (isAdded() && getContext() != null) {
                            if (updateTask.isSuccessful()) {
                                if (order.getStatus().equals(Order.STATUS_COMPLETED)) {
                                    Toast.makeText(getContext(),
                                            orderType + " order executed successfully!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getContext(),
                                            "Limit order placed. Will execute when price reaches " + String.format("%.2f", price),
                                            Toast.LENGTH_LONG).show();
                                }
                                loadUserWallet();
                                etQuantity.setText("");
                                if (orderTypeStr.equals(Order.ORDER_TYPE_LIMIT)) {
                                    etPrice.setText("");
                                }
                            } else {
                                Toast.makeText(getContext(),
                                        "Error updating wallet: " + updateTask.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } else {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(),
                                "Error creating order: " + createTask.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                }
            });

        } catch (NumberFormatException e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Invalid number format", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPriceRefresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        startPriceRefresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPriceRefresh();
    }
}

