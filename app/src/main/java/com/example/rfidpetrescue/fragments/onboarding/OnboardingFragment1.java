package com.example.rfidpetrescue.fragments.onboarding;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.OnboardingActivity;

public class OnboardingFragment1 extends Fragment {

    private Handler handler;
    private Runnable autoNext;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_onboarding1, container, false);

        handler = new Handler(Looper.getMainLooper());

        autoNext = () -> {
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).goToNextPage();
            }
        };

        //  ĐỢI 1 GIÂY RỒI NHẢY
        handler.postDelayed(autoNext, 1000);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null && autoNext != null) {
            handler.removeCallbacks(autoNext);
        }
    }
}


//package com.example.rfidpetrescue.fragments;
//
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//
//import com.example.rfidpetrescue.R;
//import com.example.rfidpetrescue.interfaces.OnboardingNavigationListener;
//
//public class OnboardingFragment1 extends Fragment {
//
//    private OnboardingNavigationListener listener;
//    private final Handler handler = new Handler(Looper.getMainLooper());
//
//    @Override
//    public void onAttach(@NonNull android.content.Context context) {
//        super.onAttach(context);
//        if (context instanceof OnboardingNavigationListener) {
//            listener = (OnboardingNavigationListener) context;
//        }
//    }
//
//    @Nullable
//    @Override
//    public View onCreateView(
//            @NonNull LayoutInflater inflater,
//            @Nullable ViewGroup container,
//            @Nullable Bundle savedInstanceState
//    ) {
//        View view = inflater.inflate(R.layout.fragment_onboarding1, container, false);
//
//        //  Sau 1 giây → chuyển sang trang tiếp theo
//        handler.postDelayed(() -> {
//            if (listener != null && isAdded()) {
//                listener.goToNextPage();
//            }
//        }, 100);
//
//        return view;
//    }
//    @Override
//    public void onDetach() {
//        super.onDetach();
//        handler.removeCallbacksAndMessages(null);
//        listener = null;
//    }
//}
