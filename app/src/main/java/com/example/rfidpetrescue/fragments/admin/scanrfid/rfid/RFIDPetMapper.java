package com.example.rfidpetrescue.fragments.admin.scanrfid.rfid;

import androidx.annotation.NonNull;

import com.example.rfidpetrescue.models.Pet;
import com.example.rfidpetrescue.models.RFIDLog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RFIDPetMapper {

    public interface PetFetchListener {
        void onSuccess(Pet pet);
        void onNotFound();
        void onError(String error);
    }

    public static void fetchPetByRFID(String rfidCode, PetFetchListener listener) {
        String tagIdFormat = "tag_" + rfidCode;
        DatabaseReference petsRef = FirebaseDatabase.getInstance().getReference("Pets");
        Query query = petsRef.orderByChild("rfid_tag_id").equalTo(tagIdFormat);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot petSnapshot : snapshot.getChildren()) {
                        Pet pet = petSnapshot.getValue(Pet.class);
                        if (pet != null) {
                            pet.setId(petSnapshot.getKey());
                            // Ghi log thành công
                            saveScanLog(rfidCode, "Tìm thấy: " + pet.getName());
                            listener.onSuccess(pet);
                            return;
                        }
                    }
                } else {
                    // Ghi log không tìm thấy
                    saveScanLog(rfidCode, "Không tìm thấy hồ sơ");
                    listener.onNotFound();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                saveScanLog(rfidCode, "Lỗi truy vấn: " + error.getMessage());
                listener.onError(error.getMessage());
            }
        });
    }

    // Hàm bổ sung để ghi Log vào Firebase
    private static void saveScanLog(String rfidCode, String result) {
        DatabaseReference logsRef = FirebaseDatabase.getInstance().getReference("RFIDLogs");
        String logId = logsRef.push().getKey();
        String currentUid = "Unknown";
        
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        String timeStamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

        RFIDLog log = new RFIDLog();
        log.setId(logId);
        log.setTag_id(rfidCode);
        log.setUser_id(currentUid);
        log.setScan_time(timeStamp);
        log.setResult(result);

        if (logId != null) {
            logsRef.child(logId).setValue(log);
        }
    }
}
