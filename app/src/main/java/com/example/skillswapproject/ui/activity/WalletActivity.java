package com.example.skillswapproject.ui.activity;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.skillswapproject.data.model.CoinTransaction;
import com.example.skillswapproject.data.model.FirestoreUser;
import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.example.skillswapproject.databinding.ActivityWalletBinding;
import com.example.skillswapproject.ui.adapter.CoinTransactionAdapter;
import com.example.skillswapproject.util.MentorRankHelper;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class WalletActivity extends AppCompatActivity {

    private ActivityWalletBinding binding;
    private CoinTransactionAdapter adapter;
    private ListenerRegistration userRegistration;
    private ListenerRegistration transactionsRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWalletBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Please log in to access your wallet", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        loadWalletData(currentUserId);
    }

    private void setupUI() {
        binding.btnBackWallet.setOnClickListener(v -> finish());

        adapter = new CoinTransactionAdapter();
        binding.rvCoinTransactions.setAdapter(adapter);
    }

    private void loadWalletData(String userId) {
        binding.pbTransactionsLoading.setVisibility(View.VISIBLE);

        // 1. Listen to real-time user profile for balance and mentor rank
        userRegistration = FirestoreRepository.getInstance().listenToUserProfile(userId, new FirestoreRepository.OnUserProfileListener() {
            @Override
            public void onUserProfileLoaded(FirestoreUser user) {
                if (user != null && binding != null) {

                    int coins = user.getSkillCoins();
                    binding.tvWalletCoinsCount.setText(coins + (coins == 1 ? " Coin" : " Coins"));

                    // Mentor Rank calculation & UI binding
                    String rankTitle = MentorRankHelper.getRankTitle(coins);
                    int badgeColor = MentorRankHelper.getRankBadgeColor(coins);
                    int bgColor = MentorRankHelper.getRankBgColor(coins);

                    binding.tvMentorRankBadge.setText(rankTitle);
                    binding.tvMentorRankBadge.setTextColor(badgeColor);
                    binding.tvMentorRankBadge.setBackgroundTintList(ColorStateList.valueOf(bgColor));

                    binding.tvRankDesc.setText(MentorRankHelper.getRankDescription(coins));

                    int progress = MentorRankHelper.getProgressPercentage(coins);
                    binding.progressRank.setProgress(progress);
                    binding.progressRank.setIndicatorColor(badgeColor);

                    int coinsNeeded = MentorRankHelper.getCoinsToNextRank(coins);
                    String nextRank = MentorRankHelper.getNextRankTitle(coins);
                    if (coinsNeeded > 0) {
                        binding.tvRankProgressHint.setText(coinsNeeded + " more " + (coinsNeeded == 1 ? "coin" : "coins") + " needed to reach " + nextRank + " rank");
                    } else {
                        binding.tvRankProgressHint.setText("Maximum rank achieved! You are a Master educator.");
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                // Keep defaults if failed
            }
        });

        // 2. Listen to real-time coin transactions
        transactionsRegistration = FirestoreRepository.getInstance().listenToCoinTransactions(userId, new FirestoreRepository.OnTransactionsLoadedListener() {
            @Override
            public void onTransactionsLoaded(List<CoinTransaction> transactions) {
                if (binding == null) return;
                binding.pbTransactionsLoading.setVisibility(View.GONE);

                if (transactions == null || transactions.isEmpty()) {
                    binding.layoutEmptyTransactions.setVisibility(View.VISIBLE);
                    binding.rvCoinTransactions.setVisibility(View.GONE);
                    binding.tvWalletCoinsEarned.setText("+0 Coins");
                    binding.tvWalletCoinsSpent.setText("0 Coins");
                } else {
                    binding.layoutEmptyTransactions.setVisibility(View.GONE);
                    binding.rvCoinTransactions.setVisibility(View.VISIBLE);
                    adapter.setTransactions(transactions);

                    int totalEarned = 0;
                    int totalSpent = 0;
                    for (CoinTransaction tx : transactions) {
                        if (tx.getAmount() > 0) {
                            totalEarned += tx.getAmount();
                        } else if (tx.getAmount() < 0) {
                            totalSpent += Math.abs(tx.getAmount());
                        }
                    }
                    binding.tvWalletCoinsEarned.setText("+" + totalEarned + (totalEarned == 1 ? " Coin" : " Coins"));
                    binding.tvWalletCoinsSpent.setText("-" + totalSpent + (totalSpent == 1 ? " Coin" : " Coins"));
                }
            }

            @Override
            public void onError(Exception e) {
                if (binding == null) return;
                binding.pbTransactionsLoading.setVisibility(View.GONE);
                android.util.Log.w("WalletActivity", "Could not load coin history: " + (e != null ? e.getMessage() : "Unknown"));
                binding.layoutEmptyTransactions.setVisibility(View.VISIBLE);
                binding.rvCoinTransactions.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userRegistration != null) {
            userRegistration.remove();
            userRegistration = null;
        }
        if (transactionsRegistration != null) {
            transactionsRegistration.remove();
            transactionsRegistration = null;
        }
        binding = null;
    }
}
