package com.example.skillswapproject.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.skillswapproject.MainActivity;
import com.example.skillswapproject.databinding.ActivitySwapSuccessBinding;

public class SwapSuccessActivity extends AppCompatActivity {

    private ActivitySwapSuccessBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySwapSuccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String targetName = getIntent().getStringExtra("TARGET_USER_NAME");
        if (targetName != null && !targetName.isEmpty()) {
            binding.tvSuccessSubtitle.setText("Your request has been sent to " + targetName + ". You'll be notified once they respond.");
        }

        binding.btnCloseSuccess.setOnClickListener(v -> finish());

        binding.btnBackHome.setOnClickListener(v -> {
            Intent intent = new Intent(SwapSuccessActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("EXTRA_NAVIGATE_TO", com.example.skillswapproject.R.id.homeFragment);
            startActivity(intent);
            finish();
        });
    }
}
