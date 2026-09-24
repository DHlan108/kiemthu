package com.example.rfidpetrescue.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.rfidpetrescue.fragments.onboarding.OnboardingFragment1;
import com.example.rfidpetrescue.fragments.onboarding.OnboardingFragment2;
import com.example.rfidpetrescue.fragments.onboarding.OnboardingFragment3;
import com.example.rfidpetrescue.fragments.onboarding.OnboardingFragment4;
import com.example.rfidpetrescue.fragments.onboarding.OnboardingFragment5;

public class OnboardingPagerAdapter extends FragmentStateAdapter {

    public OnboardingPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new OnboardingFragment1();
            case 1: return new OnboardingFragment2();
            case 2: return new OnboardingFragment3();
            case 3: return new OnboardingFragment4();
            case 4: return new OnboardingFragment5();
            default: return new OnboardingFragment1();
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
