package com.example.rfidpetrescue.fragments.admin.scanrfid.info;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.fragments.admin.scanrfid.medicalrecord.AddMedicalRecordFragment;
import com.example.rfidpetrescue.fragments.admin.scanrfid.vaccination.AddVaccinationFragment;
import com.example.rfidpetrescue.models.Pet;
import com.example.rfidpetrescue.fragments.admin.scanrfid.rfid.RFIDPetMapper;
import com.example.rfidpetrescue.utils.BarcodeUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class PetSummaryAdminFragment extends Fragment {

    // ─── Views ────────────────────────────────────────────────────────────────
    private ImageView ivPetHeader, ivBarcode;
    private MaterialCardView btnBack, cardStatusContainer;
    private TextView tvPetNameTitle, tvStatus, tvRfid;
    private TextView tvDetailBreed, tvDetailGender, tvDetailAge, tvDetailWeight, tvDetailColor, tvDetailSterilized;
    private MaterialButton btnAddMedicalRecord, btnAddVaccination, btnViewProfile;

    // ─── Data ─────────────────────────────────────────────────────────────────
    private Pet currentPet = null;
    private String currentPetId = null;
    private String petImageUrl = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pet_summary_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupClickListeners();
        loadPetFromArguments();
    }

    // ─── Init ─────────────────────────────────────────────────────────────────

    private void initViews(View view) {
        ivPetHeader        = view.findViewById(R.id.ivPetHeader);
        ivBarcode          = view.findViewById(R.id.ivBarcode);
        btnBack            = view.findViewById(R.id.btnBack);
        tvPetNameTitle     = view.findViewById(R.id.tvPetNameTitle);
        tvStatus           = view.findViewById(R.id.tvStatus);
        cardStatusContainer = (MaterialCardView) tvStatus.getParent();
        tvRfid             = view.findViewById(R.id.tvRfid);
        tvDetailBreed      = view.findViewById(R.id.tvDetailBreed);
        tvDetailGender     = view.findViewById(R.id.tvDetailGender);
        tvDetailAge        = view.findViewById(R.id.tvDetailAge);
        tvDetailWeight     = view.findViewById(R.id.tvDetailWeight);
        tvDetailColor      = view.findViewById(R.id.tvDetailColor);
        tvDetailSterilized = view.findViewById(R.id.tvDetailSterilized);
        btnAddMedicalRecord = view.findViewById(R.id.btnAddMedicalRecord);
        btnAddVaccination  = view.findViewById(R.id.btnAddVaccination);
        btnViewProfile     = view.findViewById(R.id.btnViewProfile);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        btnAddMedicalRecord.setOnClickListener(v -> {
            if (currentPetId != null) {
                AddMedicalRecordFragment addMedicalFragment = new AddMedicalRecordFragment();
                Bundle bundle = new Bundle();
                bundle.putString("PET_ID", currentPetId);
                bundle.putString("PET_IMAGE_URL", petImageUrl);
                addMedicalFragment.setArguments(bundle);

                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_container, addMedicalFragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                showToast("Đang tải dữ liệu, vui lòng đợi!");
            }
        });

        btnAddVaccination.setOnClickListener(v -> {
            if (currentPetId != null) {
                AddVaccinationFragment addVaccinationFragment = new AddVaccinationFragment();
                Bundle bundle = new Bundle();
                bundle.putString("PET_ID", currentPetId);
                bundle.putString("PET_IMAGE_URL", petImageUrl);
                addVaccinationFragment.setArguments(bundle);

                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_container, addVaccinationFragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                showToast("Đang tải dữ liệu, vui lòng đợi!");
            }
        });

        // NÚT CHI TIẾT → Gói toàn bộ dữ liệu gửi sang màn Info
        btnViewProfile.setOnClickListener(v -> {
            if (currentPet != null) {
                PetInfoAdminFragment infoFragment = new PetInfoAdminFragment();
                Bundle bundle = new Bundle();

                bundle.putString("PET_ID", currentPet.getId());
                bundle.putString("PET_IMAGE_URL", currentPet.getImageUrl());
                bundle.putString("PET_NAME", currentPet.getName());
                bundle.putString("PET_STATUS", currentPet.getStatus());
                bundle.putString("PET_BREED", currentPet.getBreed());
                bundle.putString("PET_GENDER", currentPet.getGender());
                bundle.putString("PET_AGE", currentPet.getAge());
                bundle.putString("PET_WEIGHT", String.valueOf(currentPet.getWeight()));
                bundle.putString("PET_COLOR", currentPet.getColor());

                if (currentPet.isSterilized() != null) {
                    bundle.putString("PET_STERILIZED", currentPet.isSterilized() ? "Đã triệt sản" : "Chưa");
                } else {
                    bundle.putString("PET_STERILIZED", "Chưa rõ");
                }

                bundle.putString("PET_RESCUE_HISTORY", currentPet.getRescueHistory());
                bundle.putString("PET_NOTE", currentPet.getNote());

                infoFragment.setArguments(bundle);

                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_container, infoFragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                showToast("Đang tải dữ liệu, vui lòng đợi!");
            }
        });
    }

    private void loadPetFromArguments() {
        if (getArguments() == null) return;
        String rfidCode = getArguments().getString("SCANNED_RFID");
        if (rfidCode != null) {
            loadPetData(rfidCode);
        }
    }

    // ─── Firebase ─────────────────────────────────────────────────────────────

    private void loadPetData(String rfidCode) {
        RFIDPetMapper.fetchPetByRFID(rfidCode, new RFIDPetMapper.PetFetchListener() {
            @Override
            public void onSuccess(Pet pet) {
                if (!isAdded()) return;
                currentPet = pet; // Lưu lại object
                currentPetId = pet.getId();
                petImageUrl = pet.getImageUrl();
                populateUI(pet);
            }

            @Override
            public void onNotFound() {
                if (!isAdded()) return;
                tvPetNameTitle.setText("Không tìm thấy dữ liệu!");
                showToast("Thẻ RFID này chưa được liên kết với thú cưng nào");
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                tvPetNameTitle.setText("Lỗi kết nối");
                showToast("Lỗi mạng: " + error);
            }
        });
    }

    private void populateUI(Pet pet) {
        String imageUrl = pet.getImageUrl();
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(requireContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_dog_manage)
                    .into(ivPetHeader);
        }

        String petName = pet.getName();
        if (petName != null && !petName.trim().isEmpty()) {
            tvPetNameTitle.setText(petName);
        } else {
            tvPetNameTitle.setText("Chưa cập nhật tên");
        }

        String status = pet.getStatus() != null ? pet.getStatus() : "Chưa có";
        tvStatus.setText("Tình trạng: " + status);
        updateStatusColor(status);

        if (pet.getRfid_tag_id() != null) {
            String rfid = pet.getRfid_tag_id();
            if (rfid.startsWith("tag_")) {
                rfid = rfid.replace("tag_", "");
            }

            Bitmap barcode = BarcodeUtils.generateBarcode(rfid);
            if (barcode != null && ivBarcode != null) {
                ivBarcode.setImageBitmap(barcode);
            }
            tvRfid.setText(rfid);
        } else {
            tvRfid.setText("Chưa có mã");
        }

        tvDetailBreed.setText(nullSafe(pet.getBreed()));
        tvDetailGender.setText(nullSafe(pet.getGender()));
        tvDetailAge.setText((pet.getAge() != null ? pet.getAge() : "Chưa rõ"));
        tvDetailWeight.setText(pet.getWeight() + " kg");
        tvDetailColor.setText(nullSafe(pet.getColor()));

        if (pet.isSterilized() == null) {
            tvDetailSterilized.setText("Chưa rõ");
        } else {
            tvDetailSterilized.setText((pet.isSterilized() ? "Đã triệt sản" : "Chưa"));
        }
    }
    // ─── Lifecycle ────────────────────────────────────────────────────────────

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
    private void updateStatusColor(String status) {
        if (cardStatusContainer == null || tvStatus == null) return;
        int bgColor;
        int textColor = Color.WHITE;

        switch (status) {
            case "Đang điều trị":
                bgColor = Color.parseColor("#C9BDF4");
                break;
            case "Sẵn sàng nhận nuôi":
                bgColor = Color.parseColor("#1D1A9B");
                break;
            case "Đang chờ phỏng vấn":
                bgColor = Color.parseColor("#100E55");
                break;
            case "Đang chờ nhận nuôi":
                bgColor = Color.parseColor("#627B12");
                break;
            case "Đã nhận nuôi":
                bgColor = Color.parseColor("#B2DF20");
                break;
            default:
                bgColor = Color.parseColor("#D9D9D9");
                textColor = Color.DKGRAY;
                break;
        }
        cardStatusContainer.setCardBackgroundColor(bgColor);
        tvStatus.setTextColor(textColor);
    }
    // ─── Helper ───────────────────────────────────────────────────────────────

    private String nullSafe(String value) {
        return (value != null && !value.isEmpty()) ? value : "Chưa rõ";
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}