package com.example.skillswapproject.ui.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skillswapproject.data.model.CoinTransaction;
import com.example.skillswapproject.databinding.ItemCoinTransactionBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CoinTransactionAdapter extends RecyclerView.Adapter<CoinTransactionAdapter.TransactionViewHolder> {

    private final List<CoinTransaction> transactionList = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault());

    public void setTransactions(List<CoinTransaction> list) {
        transactionList.clear();
        if (list != null) {
            transactionList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCoinTransactionBinding binding = ItemCoinTransactionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new TransactionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        holder.bind(transactionList.get(position));
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ItemCoinTransactionBinding binding;

        public TransactionViewHolder(ItemCoinTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CoinTransaction tx) {
            binding.tvTxReason.setText(tx.getReason());

            if (tx.getTimestamp() != null) {
                binding.tvTxDate.setText(dateFormat.format(tx.getTimestamp()));
            } else {
                binding.tvTxDate.setText("Recent");
            }

            int amount = tx.getAmount();
            if (amount > 0) {
                binding.tvTxAmount.setText("+" + amount + " Coins");
                binding.tvTxAmount.setTextColor(Color.parseColor("#047857")); // Deep Green
                binding.layoutTxIconContainer.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            } else {
                binding.tvTxAmount.setText(amount + " Coins");
                binding.tvTxAmount.setTextColor(Color.parseColor("#EF4444")); // Red
                binding.layoutTxIconContainer.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
            }

            binding.tvTxBalanceAfter.setText("Bal: " + tx.getBalanceAfter());
        }
    }
}
