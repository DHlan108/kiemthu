package com.example.rfidpetrescue

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rfidpetrescue.ui.theme.RfidpetrescueTheme
import com.google.firebase.database.FirebaseDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Kiểm tra kết nối ngay khi mở app
        checkFirebaseConnection()

        setContent {
            RfidpetrescueTheme {
                // State để lưu danh sách tên thú cưng hoặc thông tin chi tiết
                var petInfo by remember { mutableStateOf("Chưa có dữ liệu quét") }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "MÀN HÌNH QUẢN LÝ THÚ CƯNG",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Nút giả lập quét thẻ
                        Button(
                            onClick = { testReadDatabase { result -> petInfo = result } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("GIẢ LẬP QUÉT THẺ (TEST)")
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Hiển thị kết quả dưới dạng Card cho chuyên nghiệp
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Kết quả quét RFID:",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = petInfo, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkFirebaseConnection() {
        val db = FirebaseDatabase.getInstance().reference
        Log.d("PET_DATA", "--- ĐANG THỬ KẾT NỐI ---")

        db.child("Pets").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("PET_DATA", "Kết nối thành công! Data: ${task.result.value}")
            } else {
                // Nếu hiện "Permission denied", hãy sửa Rules trên Firebase Console thành true
                Log.e("PET_DATA", "LỖI KẾT NỐI: ${task.exception?.message}")
            }
        }
    }

    private fun testReadDatabase(onResult: (String) -> Unit) {
        val db = FirebaseDatabase.getInstance().reference
        Log.d("PET_DATA", "Đang đọc dữ liệu khi quét thẻ...")

        db.child("Pets").get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val sb = StringBuilder()
                for (petSnap in snapshot.children) {
                    // Lấy nhiều trường thông tin cùng lúc để hiển thị chi tiết
                    val name = petSnap.child("name").value.toString()
                    val breed = petSnap.child("breed").value.toString()
                    val status = petSnap.child("status").value.toString()

                    sb.append("• Tên: $name\n  Giống: $breed\n  TT: $status\n\n")
                    Log.d("PET_DATA", "Tìm thấy: $name")
                }
                onResult(sb.toString())
                Toast.makeText(this, "Cập nhật dữ liệu thành công!", Toast.LENGTH_SHORT).show()
            } else {
                onResult("Không tìm thấy thú cưng nào trong hệ thống.")
            }
        }.addOnFailureListener {
            onResult("Lỗi: ${it.message}")
        }
    }
}