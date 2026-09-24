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
import com.example.rfidpetrescue.fragments.admin.scanrfid.medicalrecord.MedicalRecordFragment;
import com.example.rfidpetrescue.fragments.admin.scanrfid.vaccination.VaccinationRecordFragment;
import com.example.rfidpetrescue.models.Pet;
import com.example.rfidpetrescue.utils.BarcodeUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PetInfoAdminFragment extends Fragment {

    // ─── Views ────────────────────────────────────────────────────────────────
    private ImageView imgHeader, ivBarcode;
    private MaterialCardView btnBack, btnEdit, cardStatusBannerContainer;
    private TextView tvPetNameTitle, tvRfid;
    private TextView tvBreed, tvGender, tvAge, tvWeight, tvColor, tvSterilized;
    private TextView tvStatusBanner;
    private TextView tvRescueHistory, tvNote;
    private TextView tvTabMedicalRecord, tvTabVaccination;
    
    // Bổ sung view Loại thú cưng
    private TextView tvPetType, tvDetailType;
    
    private Pet currentPet;
    private String petId, petImageUrl;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pet_info_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupClickListeners();

        if (getArguments() != null) {
            petId = getArguments().getString("PET_ID");
            petImageUrl = getArguments().getString("PET_IMAGE_URL");

            populateUIFromBundle(getArguments());

            if (petId != null) {
                loadPetData(petId);
            }
        }
    }

    // ─── Init ─────────────────────────────────────────────────────────────────

    private void initViews(View view) {
        imgHeader      = view.findViewById(R.id.imgHeader);
        ivBarcode      = view.findViewById(R.id.ivBarcode);
        tvRfid         = view.findViewById(R.id.tvRfid);
        btnBack        = view.findViewById(R.id.btnBack);
        btnEdit        = view.findViewById(R.id.btnEdit);
        tvPetNameTitle = view.findViewById(R.id.tvPetNameTitle);
        tvStatusBanner = view.findViewById(R.id.tvStatusBanner);
        cardStatusBannerContainer = (MaterialCardView) tvStatusBanner.getParent();
        tvTabMedicalRecord = view.findViewById(R.id.tvTabMedicalRecord);
        tvTabVaccination = view.findViewById(R.id.tvTabVaccination);

        // Ánh xạ View Loại thú cưng
        tvPetType = view.findViewById(R.id.tvPetType);
        tvDetailType = view.findViewById(R.id.tvDetailType);

        tvBreed = view.findViewById(R.id.tvDetailBreed);
        tvGender = view.findViewById(R.id.tvDetailGender);
        tvAge = view.findViewById(R.id.tvDetailAge);
        tvWeight = view.findViewById(R.id.tvDetailWeight);
        tvColor = view.findViewById(R.id.tvDetailColor);
        tvSterilized = view.findViewById(R.id.tvDetailSterilized);
        tvRescueHistory = view.findViewById(R.id.tvRescueHistory);
        tvNote = view.findViewById(R.id.tvNote);
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                if (petId != null) {
                    PetDetailAdminFragment editFragment = new PetDetailAdminFragment();
                    Bundle bundle = new Bundle();
                    if (currentPet != null) {
                        bundle.putString("PET_ID", currentPet.getId());
                        bundle.putString("PET_IMAGE_URL", currentPet.getImageUrl());
                        bundle.putString("PET_NAME", currentPet.getName());
                        bundle.putString("PET_STATUS", currentPet.getStatus());
                        
                        // Bổ sung: Truyền loại thú cưng sang màn chỉnh sửa, đảm bảo không null
                        bundle.putString("PET_TYPE", currentPet.getSpecies() != null ? currentPet.getSpecies() : "Chưa rõ");
                        
                        bundle.putString("PET_BREED", currentPet.getBreed());
                        bundle.putString("PET_GENDER", currentPet.getGender());
                        bundle.putString("PET_AGE", currentPet.getAge());
                        bundle.putString("PET_WEIGHT", String.valueOf(currentPet.getWeight()));
                        bundle.putString("PET_COLOR", currentPet.getColor());

                        String sterilizedStr = "Chưa rõ";
                        if (currentPet.isSterilized() != null) {
                            sterilizedStr = currentPet.isSterilized() ? "Đã triệt sản" : "Chưa";
                        }
                        bundle.putString("PET_STERILIZED", sterilizedStr);

                        bundle.putString("PET_RESCUE_HISTORY", currentPet.getRescueHistory());
                        bundle.putString("PET_NOTE", currentPet.getNote());
                    } else if (getArguments() != null) {
                        bundle.putAll(getArguments());
                        // Đảm bảo PET_TYPE không bị null khi pass từ arguments
                        if (!bundle.containsKey("PET_TYPE")) {
                            bundle.putString("PET_TYPE", "Chưa rõ");
                        }
                    }

                    editFragment.setArguments(bundle);

                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_container, editFragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        if (tvTabMedicalRecord != null) {
            tvTabMedicalRecord.setOnClickListener(v -> {
                if (petId != null) {
                    MedicalRecordFragment medicalFragment = new MedicalRecordFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("PET_ID", petId);
                    bundle.putString("PET_IMAGE_URL", petImageUrl);
                    medicalFragment.setArguments(bundle);

                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_container, medicalFragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        if (tvTabVaccination != null) {
            tvTabVaccination.setOnClickListener(v -> {
                if (petId != null) {
                    VaccinationRecordFragment vaccinationFragment = new VaccinationRecordFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("PET_ID", petId);
                    bundle.putString("PET_IMAGE_URL", petImageUrl);
                    vaccinationFragment.setArguments(bundle);

                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_container, vaccinationFragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }
    }

    // ─── Hàm hiển thị UI tức thời từ Bundle ──────────────────────────────────
    private void populateUIFromBundle(Bundle bundle) {
        if (!isAdded()) return;

        if (imgHeader != null && petImageUrl != null) {
            Glide.with(this).load(petImageUrl).centerCrop().placeholder(R.drawable.ic_dog_manage).into(imgHeader);
        }

        tvPetNameTitle.setText(bundle.getString("PET_NAME", "Chưa có tên"));

        if (tvStatusBanner != null) {
            String status = bundle.getString("PET_STATUS", "Chưa có");
            tvStatusBanner.setText("Tình trạng: " + status);
            updateStatusColor(status);
        }

        // Hiển thị Loại thú cưng từ Bundle
        String species = bundle.getString("PET_TYPE", "Chưa rõ");
        if (tvPetType != null) tvPetType.setText(species);
        if (tvDetailType != null) tvDetailType.setText(species);

        tvBreed.setText(bundle.getString("PET_BREED", "Chưa rõ"));
        tvGender.setText(bundle.getString("PET_GENDER", "Chưa rõ"));
        tvAge.setText(bundle.getString("PET_AGE", "Chưa rõ"));
        tvWeight.setText(bundle.getString("PET_WEIGHT", "0") + " kg");
        tvColor.setText(bundle.getString("PET_COLOR", "Chưa rõ"));
        tvSterilized.setText(bundle.getString("PET_STERILIZED", "Chưa rõ"));

        if (tvRescueHistory != null) {
            tvRescueHistory.setText(bundle.getString("PET_RESCUE_HISTORY", "Chưa có thông tin cứu hộ"));
        }
        if (tvNote != null) {
            tvNote.setText(bundle.getString("PET_NOTE", "Không có ghi chú"));
        }
    }

    // ─── Firebase ─────────────────────────────────────────────────────────────

    private void loadPetData(String id) {
        // Đã sửa: Sử dụng instance mặc định để đồng bộ với toàn app
        DatabaseReference petRef = FirebaseDatabase
                .getInstance()
                .getReference("Pets")
                .child(id);

        petRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                Pet pet = snapshot.getValue(Pet.class);
                if (pet != null) {
                    pet.setId(snapshot.getKey());
                    currentPet = pet;
                    populateUI(pet);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void populateUI(Pet pet) {
        if (!isAdded() || pet == null) return;

        petImageUrl = pet.getImageUrl();
        if (imgHeader != null && petImageUrl != null && !petImageUrl.isEmpty()) {
            Glide.with(this).load(petImageUrl).centerCrop().placeholder(R.drawable.ic_dog_manage).into(imgHeader);
        }

        String petName = pet.getName();
        if (petName != null && !petName.trim().isEmpty()) {
            tvPetNameTitle.setText(petName);
        } else {
            tvPetNameTitle.setText("Chưa cập nhật tên");
        }

        if (tvStatusBanner != null) {
            String status = pet.getStatus() != null ? pet.getStatus() : "Chưa có";
            tvStatusBanner.setText("Tình trạng: " + status);
            updateStatusColor(status); // Gọi hàm mới
        }

        // Hiển thị Loại thú cưng từ Model
        String species = pet.getSpecies() != null ? pet.getSpecies() : "Chưa rõ";
        if (tvPetType != null) tvPetType.setText(species);
        if (tvDetailType != null) tvDetailType.setText(species);

        // Logic Barcode đồng bộ với màn Summary
        if (pet.getRfid_tag_id() != null) {
            String rfid = pet.getRfid_tag_id();
            if (rfid.startsWith("tag_")) {
                rfid = rfid.replace("tag_", "");
            }

            Bitmap barcode = BarcodeUtils.generateBarcode(rfid);
            if (barcode != null && ivBarcode != null) {
                ivBarcode.setImageBitmap(barcode);
            }
            if (tvRfid != null) {
                tvRfid.setText(rfid);
            }
        } else {
            if (tvRfid != null) tvRfid.setText("Chưa có mã");
        }

        tvBreed.setText(nullSafe(pet.getBreed()));
        tvGender.setText(nullSafe(pet.getGender()));
        tvAge.setText((pet.getAge() != null ? pet.getAge() : "Chưa rõ"));
        tvWeight.setText(pet.getWeight() + " kg");
        tvColor.setText(nullSafe(pet.getColor()));

        String sterilizedStr = "Chưa rõ";
        if (pet.isSterilized() != null) {
            sterilizedStr = pet.isSterilized() ? "Đã triệt sản" : "Chưa";
        }
        tvSterilized.setText(sterilizedStr);

        if (tvRescueHistory != null) {
            tvRescueHistory.setText(pet.getRescueHistory() != null && !pet.getRescueHistory().isEmpty() ? pet.getRescueHistory() : "Chưa có thông tin cứu hộ");
        }
        if (tvNote != null) {
            tvNote.setText(pet.getNote() != null && !pet.getNote().isEmpty() ? pet.getNote() : "Không có ghi chú");
        }
    }

    private void updateStatusColor(String status) {
        if (cardStatusBannerContainer == null || tvStatusBanner == null) return;
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
        cardStatusBannerContainer.setCardBackgroundColor(bgColor);
        tvStatusBanner.setTextColor(textColor);
    }
    private String nullSafe(String value) {
        return (value != null && !value.isEmpty()) ? value : "Chưa rõ";
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }
}