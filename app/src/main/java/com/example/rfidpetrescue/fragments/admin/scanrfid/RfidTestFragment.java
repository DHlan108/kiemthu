package com.example.rfidpetrescue.fragments.admin.scanrfid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.fragments.admin.scanrfid.rfid.RFIDListener;
import com.example.rfidpetrescue.fragments.admin.scanrfid.rfid.RFIDPetMapper;
import com.example.rfidpetrescue.models.Pet;

public class RfidTestFragment extends Fragment implements RFIDListener {

    private ImageButton btnBack, btnInfo;
    private AppCompatButton btnInputId, btnScanRFID;
    private TextView tvTitle, tvSubtitle;
    private ImageView ivScannerFrame;
    private Animation rotateAnimation;

    // BIẾN CỜ ĐỂ PHÂN BIỆT MỤC ĐÍCH QUÉT
    private boolean isScanningToAdd = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_rfid_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnBack = view.findViewById(R.id.btnBack);
        btnInfo = view.findViewById(R.id.btnInfo);
        btnInputId = view.findViewById(R.id.btnInputId);
        btnScanRFID = view.findViewById(R.id.btnScanRFID);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvSubtitle = view.findViewById(R.id.tvSubtitle);
        ivScannerFrame = view.findViewById(R.id.ivScannerFrame);

        rotateAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_loading);

        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        btnInfo.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new RfidGuideFragment())
                        .addToBackStack(null).commit();
            }
        });

        // NÚT 1: QUÉT ĐỂ TRA CỨU ĐỊNH DANH BÌNH THƯỜNG
        btnScanRFID.setOnClickListener(v -> {
            isScanningToAdd = false;
            startScanningUI();
        });

        // NÚT 2: QUÉT ĐỂ LẤY MÃ THÊM MỚI
        btnInputId.setOnClickListener(v -> {
            isScanningToAdd = true;
            startScanningUI();
        });
    }

    // Hàm phụ trợ gộp chung UI khi bắt đầu quét
    private void startScanningUI() {
        ivScannerFrame.setImageResource(R.drawable.ic_loading);
        ivScannerFrame.startAnimation(rotateAnimation);

        if (isScanningToAdd) {
            tvTitle.setText("Đang quét thẻ RFID mới");
            tvSubtitle.setText("Đưa thẻ chưa đăng ký vào máy quét");
        } else {
            tvTitle.setText("Đang tiến hành định danh");
            tvSubtitle.setText("Máy quét RFID đang hoạt động");
        }
        tvTitle.setAllCaps(false);

        btnScanRFID.setVisibility(View.GONE);
        btnInputId.setVisibility(View.GONE);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setRFIDListener(this);
        }
    }

    @Override
    public void onTagScanned(String rfidCode) {
        if (getActivity() == null || !isAdded()) return;

        String cleanRfid = rfidCode.replaceAll("[^a-zA-Z0-9]", "").trim();

        if (cleanRfid.isEmpty() || cleanRfid.length() != 15) {
            getActivity().runOnUiThread(() -> {
                resetToIdleState();
                if (isScanningToAdd) {
                    Toast.makeText(getContext(), "Mã RFID không hợp lệ (sai chuẩn 15 ký tự)!", Toast.LENGTH_SHORT).show();
                } else {
                    IdFailedFragment failedFragment = new IdFailedFragment();
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_container, failedFragment).addToBackStack(null).commit();
                }
            });
            return;
        }

        getActivity().runOnUiThread(() -> {
            tvTitle.setText("Đang kiểm tra dữ liệu...");
            tvSubtitle.setText("Vui lòng đợi trong giây lát");
        });

        RFIDPetMapper.fetchPetByRFID(cleanRfid, new RFIDPetMapper.PetFetchListener() {
            @Override
            public void onSuccess(Pet pet) {
                if (!isAdded() || getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    resetToIdleState();
                    if (isScanningToAdd) {
                        // KỊCH BẢN THÊM MỚI MÀ LẠI TÌM THẤY -> BÁO LỖI, NGỪNG QUÉT
                        Toast.makeText(getContext(),
                                "Mã này đã tồn tại! Vui lòng dùng tính năng Quét định danh để xem thông tin.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        // KỊCH BẢN TRA CỨU MÀ TÌM THẤY -> THÀNH CÔNG
                        IdSuccessFragment successFragment = new IdSuccessFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("SCANNED_RFID", cleanRfid);
                        successFragment.setArguments(bundle);
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.main_container, successFragment).addToBackStack(null).commit();
                    }
                });
            }

            @Override
            public void onNotFound() {
                if (!isAdded() || getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    resetToIdleState();
                    if (isScanningToAdd) {
                        // KỊCH BẢN THÊM MỚI VÀ CHƯA CÓ TRONG DATA -> ĐẨY THẲNG SANG MÀN THÊM MỚI
                        AddIdFragment addIdFragment = new AddIdFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("NEW_RFID", cleanRfid);
                        addIdFragment.setArguments(bundle);
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.main_container, addIdFragment).addToBackStack(null).commit();
                    } else {
                        // KỊCH BẢN TRA CỨU MÀ KHÔNG CÓ -> MỞ MÀN NOT FOUND
                        IdNotFoundFragment notFoundFragment = new IdNotFoundFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("SCANNED_RFID", cleanRfid);
                        notFoundFragment.setArguments(bundle);
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.main_container, notFoundFragment).addToBackStack(null).commit();
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    resetToIdleState();
                    if (isScanningToAdd) {
                        Toast.makeText(getContext(), "Lỗi mạng hoặc lỗi hệ thống!", Toast.LENGTH_SHORT).show();
                    } else {
                        IdFailedFragment failedFragment = new IdFailedFragment();
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.main_container, failedFragment).addToBackStack(null).commit();
                    }
                });
            }
        });
    }

    private void resetToIdleState() {
        ivScannerFrame.clearAnimation();
        ivScannerFrame.setImageResource(R.drawable.ic_rfid_scan_frame);
        tvTitle.setText("QUÉT RFID NHẬN DẠNG THÚ CƯNG");
        tvTitle.setAllCaps(true);
        tvTitle.setTextColor(getResources().getColor(android.R.color.white));
        tvSubtitle.setText("Kết nối thiết bị để định danh thú cưng.");
        btnScanRFID.setVisibility(View.VISIBLE);
        btnInputId.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }
}