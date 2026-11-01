package com.example.skillswapproject.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.skillswapproject.databinding.DialogMatchBreakdownBinding;
import com.example.skillswapproject.util.CompatibilityMatcher;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class MatchBreakdownBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_MATCH_RESULT = "ARG_MATCH_RESULT";
    private static final String ARG_PARTNER_NAME = "ARG_PARTNER_NAME";

    private DialogMatchBreakdownBinding binding;
    private CompatibilityMatcher.MatchResult matchResult;
    private String partnerName = "Partner";

    public static MatchBreakdownBottomSheet newInstance(CompatibilityMatcher.MatchResult result, String partnerName) {
        MatchBreakdownBottomSheet fragment = new MatchBreakdownBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MATCH_RESULT, result);
        args.putString(ARG_PARTNER_NAME, partnerName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            matchResult = (CompatibilityMatcher.MatchResult) getArguments().getSerializable(ARG_MATCH_RESULT);
            partnerName = getArguments().getString(ARG_PARTNER_NAME, "Partner");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogMatchBreakdownBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (matchResult == null) {
            dismiss();
            return;
        }

        binding.tvMatchTitle.setText("AI Compatibility with " + partnerName);
        binding.tvMatchScoreBadge.setText(matchResult.getOverallScore() + "% Match ✨");

        binding.tvAiSummaryText.setText(matchResult.getAiSummary());
        binding.tvMatchedSkillsDetail.setText(matchResult.getMatchedSkillsSummary());

        // Progress 1: Skill Match (out of 40)
        int skillVal = matchResult.getSkillScore();
        binding.tvBar1Val.setText(skillVal + " / 40");
        binding.progressSkillMatch.setProgress((skillVal * 100) / 40);

        // Progress 2: Mentor Experience (out of 20)
        int mentorVal = matchResult.getMentorScore();
        binding.tvBar2Val.setText(mentorVal + " / 20");
        binding.progressMentorMatch.setProgress((mentorVal * 100) / 20);

        // Progress 3: Synergy (out of 20)
        int synergyVal = matchResult.getSynergyScore();
        binding.tvBar3Val.setText(synergyVal + " / 20");
        binding.progressSynergyMatch.setProgress((synergyVal * 100) / 20);

        // Progress 4: Community Trust (out of 20)
        int trustVal = matchResult.getTrustScore();
        binding.tvBar4Val.setText(trustVal + " / 20");
        binding.progressTrustMatch.setProgress((trustVal * 100) / 20);

        binding.btnCloseBreakdown.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
