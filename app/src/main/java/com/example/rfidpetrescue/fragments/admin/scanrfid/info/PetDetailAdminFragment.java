package com.example.rfidpetrescue.fragments.admin.scanrfid.info;

import static com.example.rfidpetrescue.utils.BarcodeUtils.generateBarcode;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.utils.DropdownUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class PetDetailAdminFragment extends Fragment {

    private ImageButton btnBack;
    private ImageView imgPetAvatar, ivBarcode;
    private TextView tvRfid;
    private MaterialCardView cardStatus, btnChangeAvatar, cardPetType;
    private TextView tvStatusValue, tvPetTypeValue, tvDetailType;

    // Đã đổi edtDetailGender và edtDetailSterilized thành TextView
    private TextView tvDetailGender, tvDetailSterilized;

    private MaterialButton btnSavePet, btnDeleteRecord;

    private EditText edtPetName, edtRescueHistory, edtNote;
    private EditText edtDetailBreed, edtDetailAge, edtDetailWeight, edtDetailColor;

    private String petId;
    private String petImageUrl;
    private Uri newImageUri = null;

    // Các biến lưu trữ dữ liệu GỐC để đem ra so sánh
    private String origName = "", origStatus = "", origType = "", origBreed = "", origGender = "";
    private String origAge = "", origWeight = "", origColor = "", origSterilized = "";
    private String origHistory = "", origNote = "";

    // Bộ lắng nghe sự thay đổi chữ (TextWatcher)
    private final TextWatcher textChangeListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            checkForChanges();
        }
    };

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    newImageUri = uri;
                    imgPetAvatar.setImageURI(uri);
                    checkForChanges();
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pet_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupClickListeners();

        if (getArguments() != null) {
            petId = getArguments().getString("PET_ID");
            petImageUrl = getArguments().getString("PET_IMAGE_URL");
            populateUIFromBundle(getArguments());

            // Tải mã vạch thật từ Firebase để tránh bị hiển thị nhầm ID
            if (petId != null) {
                loadRealBarcode(petId);
            }
        }

        checkForChanges();
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        imgPetAvatar = view.findViewById(R.id.imgPetAvatar);
        btnChangeAvatar = view.findViewById(R.id.btnChangeAvatar);
        ivBarcode = view.findViewById(R.id.ivBarcode);
        tvRfid = view.findViewById(R.id.tvRfid);
        cardStatus = view.findViewById(R.id.cardStatus);
        tvStatusValue = view.findViewById(R.id.tvStatusValue);

        cardPetType = view.findViewById(R.id.cardPetType);
        tvPetTypeValue = view.findViewById(R.id.tvPetTypeValue);
        tvDetailType = view.findViewById(R.id.tvDetailType);

        btnSavePet = view.findViewById(R.id.btnSavePet);
        btnDeleteRecord = view.findViewById(R.id.btnDeleteRecord);

        edtPetName = view.findViewById(R.id.edtPetName);
        edtRescueHistory = view.findViewById(R.id.edtRescueHistory);
        edtNote = view.findViewById(R.id.edtNote);

        edtDetailBreed = view.findViewById(R.id.edtDetailBreed);
        edtDetailAge = view.findViewById(R.id.edtDetailAge);
        edtDetailWeight = view.findViewById(R.id.edtDetailWeight);
        edtDetailColor = view.findViewById(R.id.edtDetailColor);

        // Map ID cho TextView giới tính và triệt sản (Nhớ sửa ID trong file XML)
        tvDetailGender = view.findViewById(R.id.tvDetailGender);
        tvDetailSterilized = view.findViewById(R.id.tvDetailSterilized);

        // Gắn TextWatcher vào tất cả các ô nhập liệu (Chỉ giữ lại cho EditText)
        edtPetName.addTextChangedListener(textChangeListener);
        edtRescueHistory.addTextChangedListener(textChangeListener);
        edtNote.addTextChangedListener(textChangeListener);
        edtDetailBreed.addTextChangedListener(textChangeListener);
        edtDetailAge.addTextChangedListener(textChangeListener);
        edtDetailWeight.addTextChangedListener(textChangeListener);
        edtDetailColor.addTextChangedListener(textChangeListener);
    }

    private void populateUIFromBundle(Bundle bundle) {
        if (imgPetAvatar != null && petImageUrl != null) {
            Glide.with(this).load(petImageUrl).centerCrop().placeholder(R.drawable.ic_dog_manage).into(imgPetAvatar);
        }

        origName = safeString(bundle.getString("PET_NAME"), "");
        origStatus = safeString(bundle.getString("PET_STATUS"), "Chưa có");
        origType = safeString(bundle.getString("PET_TYPE"), "Chó");
        origBreed = safeString(bundle.getString("PET_BREED"), "Chưa rõ");
        origGender = safeString(bundle.getString("PET_GENDER"), "Chưa rõ");
        origAge = safeString(bundle.getString("PET_AGE"), "Chưa rõ");
        origWeight = safeString(bundle.getString("PET_WEIGHT"), "0");
        origColor = safeString(bundle.getString("PET_COLOR"), "Chưa rõ");

        // Fix dữ liệu triệt sản cho khớp với dạng Dropdown
        String rawSterilized = safeString(bundle.getString("PET_STERILIZED"), "Chưa rõ");
        if (rawSterilized.equals("true") || rawSterilized.toLowerCase().contains("đã")) {
            origSterilized = "Đã triệt sản";
        } else if (rawSterilized.equals("false") || rawSterilized.toLowerCase().contains("chưa")) {
            origSterilized = "Chưa triệt sản";
        } else {
            origSterilized = rawSterilized;
        }

        origHistory = safeString(bundle.getString("PET_RESCUE_HISTORY"), "");
        origNote = safeString(bundle.getString("PET_NOTE"), "");

        edtPetName.setText(origName);
        tvStatusValue.setText(origStatus);

        if (tvPetTypeValue != null) tvPetTypeValue.setText(origType);
        if (tvDetailType != null) tvDetailType.setText(origType);

        edtDetailBreed.setText(origBreed);
        edtDetailAge.setText(origAge);
        edtDetailWeight.setText(origWeight);
        edtDetailColor.setText(origColor);
        edtRescueHistory.setText(origHistory);
        edtNote.setText(origNote);

        // Gán cho TextView
        tvDetailGender.setText(origGender);
        tvDetailSterilized.setText(origSterilized);

        tvRfid.setText("Đang tải...");
    }

    private void loadRealBarcode(String id) {
        FirebaseDatabase.getInstance().getReference("Pets").child(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        if (snapshot.exists()) {
                            String realRfid = snapshot.child("rfid_tag_id").getValue(String.class);
                            if (realRfid != null && !realRfid.isEmpty()) {
                                if (realRfid.startsWith("tag_")) realRfid = realRfid.replace("tag_", "");
                                Bitmap barcode = generateBarcode(realRfid);
                                if (barcode != null) ivBarcode.setImageBitmap(barcode);
                                tvRfid.setText(realRfid);
                            } else {
                                tvRfid.setText("Chưa có mã");
                            }

                            String species = snapshot.child("species").getValue(String.class);
                            if (species != null) {
                                origType = species;
                                if (tvPetTypeValue != null) tvPetTypeValue.setText(species);
                                if (tvDetailType != null) tvDetailType.setText(species);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private String safeString(String value, String defValue) {
        return (value != null) ? value : defValue;
    }

    private void checkForChanges() {
        boolean hasChanged = false;

        if (!edtPetName.getText().toString().trim().equals(origName)) hasChanged = true;
        if (!tvStatusValue.getText().toString().trim().equals(origStatus)) hasChanged = true;

        if (tvPetTypeValue != null && !tvPetTypeValue.getText().toString().trim().equals(origType)) hasChanged = true;

        if (!edtDetailBreed.getText().toString().trim().equals(origBreed)) hasChanged = true;
        if (!edtDetailAge.getText().toString().trim().equals(origAge)) hasChanged = true;
        if (!edtDetailWeight.getText().toString().trim().equals(origWeight)) hasChanged = true;
        if (!edtDetailColor.getText().toString().trim().equals(origColor)) hasChanged = true;
        if (!edtRescueHistory.getText().toString().trim().equals(origHistory)) hasChanged = true;
        if (!edtNote.getText().toString().trim().equals(origNote)) hasChanged = true;

        // Kiểm tra TextView
        if (!tvDetailGender.getText().toString().trim().equals(origGender)) hasChanged = true;
        if (!tvDetailSterilized.getText().toString().trim().equals(origSterilized)) hasChanged = true;

        if (newImageUri != null) hasChanged = true;

        btnSavePet.setEnabled(hasChanged);
        if (hasChanged) {
            btnSavePet.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1D1A9B")));
            btnSavePet.setTextColor(Color.WHITE);
        } else {
            btnSavePet.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D9D9D9")));
            btnSavePet.setTextColor(Color.parseColor("#888888"));
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        btnChangeAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Đổi Status thành Dropdown giống AddId
        cardStatus.setOnClickListener(v ->
                DropdownUtils.showRoundedDropdown(requireContext(), cardStatus,
                        new String[]{"Đang điều trị", "Sẵn sàng nhận nuôi", "Đã nhận nuôi", "Đang chờ phỏng vấn", "Đang chờ nhận nuôi"},
                        selectedItem -> {
                            tvStatusValue.setText(selectedItem);
                            checkForChanges();
                        })
        );

        if (cardPetType != null) {
            cardPetType.setOnClickListener(v -> {
                DropdownUtils.showRoundedDropdown(requireContext(), cardPetType, new String[]{"Chó", "Mèo"}, s -> {
                    tvPetTypeValue.setText(s);
                    if (tvDetailType != null) tvDetailType.setText(s);
                    checkForChanges();
                });
            });
        }

        // Bổ sung: Dropdown cho Giới tính
        tvDetailGender.setOnClickListener(v ->
                DropdownUtils.showRoundedDropdown(requireContext(), tvDetailGender, new String[]{"Đực", "Cái", "Chưa rõ"}, selectedItem -> {
                    tvDetailGender.setText(selectedItem);
                    checkForChanges();
                })
        );

        // Bổ sung: Dropdown cho Triệt sản
        tvDetailSterilized.setOnClickListener(v ->
                DropdownUtils.showRoundedDropdown(requireContext(), tvDetailSterilized, new String[]{"Đã triệt sản", "Chưa triệt sản", "Chưa rõ"}, selectedItem -> {
                    tvDetailSterilized.setText(selectedItem);
                    checkForChanges();
                })
        );

        btnSavePet.setOnClickListener(v -> processSaving());
        btnDeleteRecord.setOnClickListener(v -> confirmDeletePet());
    }

    private void processSaving() {
        if (petId == null) return;
        String newName = edtPetName.getText().toString().trim();
        if (newName.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập tên thú cưng", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSavePet.setEnabled(false);
        btnSavePet.setText("Đang lưu...");

        if (newImageUri != null) {
            uploadImageToCloudinary();
        } else {
            saveDataToDatabase(petImageUrl);
        }
    }

    private void uploadImageToCloudinary() {
        MediaManager.get().upload(newImageUri).callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {}

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {}

            @Override
            public void onSuccess(String requestId, Map resultData) {
                String secureUrl = (String) resultData.get("secure_url");
                saveDataToDatabase(secureUrl);
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                Toast.makeText(getContext(), "Lỗi tải ảnh lên: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                btnSavePet.setEnabled(true);
                btnSavePet.setText("Lưu chỉnh sửa");
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) { }
        }).dispatch();
    }

    private void saveDataToDatabase(String imageUrl) {
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("name", edtPetName.getText().toString().trim());
        updates.put("status", tvStatusValue.getText().toString().trim());

        if (tvPetTypeValue != null) {
            updates.put("species", tvPetTypeValue.getText().toString().trim());
        }

        updates.put("breed", edtDetailBreed.getText().toString().trim());
        updates.put("age", edtDetailAge.getText().toString().trim());
        updates.put("color", edtDetailColor.getText().toString().trim());

        // Lưu giá trị từ TextView Giới tính
        updates.put("gender", tvDetailGender.getText().toString().trim());

        updates.put("rescueHistory", edtRescueHistory.getText().toString().trim());
        updates.put("note", edtNote.getText().toString().trim());
        updates.put("imageUrl", imageUrl);

        try {
            String weightStr = edtDetailWeight.getText().toString().trim().replace(",", ".");
            updates.put("weight", Double.parseDouble(weightStr));
        } catch (Exception e) {
            updates.put("weight", 0.0);
        }

        // Lưu boolean cho triệt sản dựa vào text của Dropdown
        String sterilizedStr = tvDetailSterilized.getText().toString().trim();
        if (sterilizedStr.equals("Đã triệt sản")) {
            updates.put("sterilized", true);
        } else if (sterilizedStr.equals("Chưa triệt sản")) {
            updates.put("sterilized", false);
        }

        FirebaseDatabase.getInstance().getReference("Pets").child(petId)
                .updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    } else {
                        Toast.makeText(getContext(), "Lỗi cập nhật dữ liệu", Toast.LENGTH_SHORT).show();
                        btnSavePet.setEnabled(true);
                        btnSavePet.setText("Lưu chỉnh sửa");
                    }
                });
    }

    private void confirmDeletePet() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xoá thú cưng")
                .setMessage("Bạn có chắc chắn muốn xoá hồ sơ bé này không?")
                .setPositiveButton("Xoá", (dialog, which) -> deletePetRecord())
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void deletePetRecord() {
        if (petId == null) return;
        FirebaseDatabase.getInstance().getReference("Pets").child(petId)
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Đã xoá thú cưng", Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    } else {
                        Toast.makeText(getContext(), "Lỗi khi xoá", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity)
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainActivity)
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
    }
}