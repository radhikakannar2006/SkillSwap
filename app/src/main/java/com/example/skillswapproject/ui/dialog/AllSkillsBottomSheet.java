package com.example.skillswapproject.ui.dialog;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.skillswapproject.R;
import com.example.skillswapproject.databinding.BottomSheetAllSkillsBinding;
import com.example.skillswapproject.util.SkillConstants;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class AllSkillsBottomSheet extends BottomSheetDialogFragment {

    public interface OnSkillSelectedListener {
        void onSkillSelected(String skillName);
    }

    private BottomSheetAllSkillsBinding binding;
    private OnSkillSelectedListener listener;
    private String currentlySelected = "All";

    public static AllSkillsBottomSheet newInstance(String selectedSkill) {
        AllSkillsBottomSheet sheet = new AllSkillsBottomSheet();
        Bundle args = new Bundle();
        args.putString("SELECTED_SKILL", selectedSkill != null ? selectedSkill : "All");
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnSkillSelectedListener(OnSkillSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentlySelected = getArguments().getString("SELECTED_SKILL", "All");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAllSkillsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnCloseAllSkills.setOnClickListener(v -> dismiss());

        populateChips("");

        binding.etFilterSkills.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                populateChips(s != null ? s.toString().trim() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void populateChips(String query) {
        if (binding == null || getContext() == null) return;
        binding.chipGroupAllSkills.removeAllViews();

        List<String> all = new ArrayList<>();
        all.add("All");
        for (String s : SkillConstants.PREDEFINED_SKILLS) {
            all.add(s);
        }

        String queryLower = query.toLowerCase();

        for (String skill : all) {
            if (!query.isEmpty() && !skill.equalsIgnoreCase("All") && !skill.toLowerCase().contains(queryLower)) {
                continue;
            }

            Chip chip = new Chip(requireContext());
            chip.setText(skill);
            chip.setCheckable(true);
            boolean isSelected = skill.equalsIgnoreCase(currentlySelected);
            chip.setChecked(isSelected);

            if (isSelected) {
                chip.setChipBackgroundColorResource(R.color.primary_green);
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            }

            chip.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSkillSelected(skill);
                }
                dismiss();
            });

            binding.chipGroupAllSkills.addView(chip);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
