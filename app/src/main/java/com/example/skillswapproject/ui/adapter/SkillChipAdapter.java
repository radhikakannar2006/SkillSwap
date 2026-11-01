package com.example.skillswapproject.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.skillswapproject.data.model.Skill;
import com.example.skillswapproject.databinding.ItemSkillChipBinding;
import java.util.ArrayList;
import java.util.List;

public class SkillChipAdapter extends RecyclerView.Adapter<SkillChipAdapter.SkillViewHolder> {

    private final List<Skill> skillList = new ArrayList<>();
    private String selectedSkill = "All";
    private OnSkillClickListener listener;

    public interface OnSkillClickListener {
        void onSkillClick(Skill skill);
    }

    public void setOnSkillClickListener(OnSkillClickListener listener) {
        this.listener = listener;
    }

    public void setSelectedSkill(String skillName) {
        this.selectedSkill = (skillName != null) ? skillName : "All";
        notifyDataSetChanged();
    }

    public String getSelectedSkill() {
        return selectedSkill;
    }

    public void submitList(List<Skill> skills) {
        skillList.clear();
        if (skills != null) {
            skillList.addAll(skills);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SkillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSkillChipBinding binding = ItemSkillChipBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new SkillViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SkillViewHolder holder, int position) {
        Skill skill = skillList.get(position);
        boolean isSelected = skill.getName() != null && skill.getName().equalsIgnoreCase(selectedSkill);
        holder.bind(skill, isSelected, listener);
    }

    @Override
    public int getItemCount() {
        return skillList.size();
    }

    static class SkillViewHolder extends RecyclerView.ViewHolder {
        private final ItemSkillChipBinding binding;

        public SkillViewHolder(ItemSkillChipBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Skill skill, boolean isSelected, OnSkillClickListener listener) {
            binding.tvSkillName.setText(skill.getName());
            if (isSelected) {
                binding.tvSkillName.setBackgroundResource(com.example.skillswapproject.R.drawable.bg_skill_chip_selected);
                binding.tvSkillName.setTextColor(androidx.core.content.ContextCompat.getColor(
                        binding.getRoot().getContext(), com.example.skillswapproject.R.color.white));
            } else {
                binding.tvSkillName.setBackgroundResource(com.example.skillswapproject.R.drawable.bg_skill_chip);
                binding.tvSkillName.setTextColor(androidx.core.content.ContextCompat.getColor(
                        binding.getRoot().getContext(), com.example.skillswapproject.R.color.dark_text));
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSkillClick(skill);
                }
            });
        }
    }
}
