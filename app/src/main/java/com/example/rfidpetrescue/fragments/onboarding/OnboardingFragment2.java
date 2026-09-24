package com.example.rfidpetrescue.fragments.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.OnboardingActivity;

public class OnboardingFragment2 extends Fragment {

    public OnboardingFragment2() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate layout
        View view = inflater.inflate(R.layout.fragment_onboarding2, container, false);


        view.setOnClickListener(v -> {
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).goToNextPage();
            }
        });

        return view;
    }
}
