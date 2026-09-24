package com.example.rfidpetrescue.fragments.admin.scanrfid;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.fragments.admin.scanrfid.rfid.RFIDPetMapper;
import com.example.rfidpetrescue.fragments.admin.scanrfid.rfid.RFIDListener;
import com.example.rfidpetrescue.models.Pet;

import java.util.Set;

public class BluetoothRfidFragment extends Fragment implements RFIDListener {

    // ─── Views ───────────────────────────────────────────────────────────────
    private ImageButton btnBack, btnInfo;
    private AppCompatButton btnInputId, btnScanRFID;
    private TextView tvTitle, tvSubtitle;
    private ImageView ivScannerFrame;

    // ─── Bluetooth ────────────────────────────────────────────────────────────
    private BluetoothAdapter bluetoothAdapter;
    private boolean isBluetoothDeviceConnected = false;

    // ─── Animation ───────────────────────────────────────────────────────────
    private Animation rotateAnimation;

    // ─── States ──────────────────────────────────────────────────────────────
    private enum ScanState {IDLE, SCANNING, SUCCESS}

    private ScanState currentState = ScanState.IDLE;
    private boolean isScanningToAdd = false; // Biến cờ phân biệt mục đích quét

    // ─── Permission launcher ─────────────────────────────────────────────────
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    checkBluetoothAndProceed();
                } else {
                    showBluetoothRequiredDialog("Ứng dụng cần quyền Bluetooth để kết nối với máy quét RFID.");
                }
            });

    // ─── Enable Bluetooth launcher ───────────────────────────────────────────
    private final ActivityResultLauncher<Intent> enableBluetoothLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                    checkBluetoothDeviceConnected();
                } else {
                    showBluetoothRequiredDialog("Bluetooth chưa được bật. Vui lòng bật Bluetooth để tiếp tục.");
                }
            });

    // ─── Bluetooth state receiver ─────────────────────────────────────────────
    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    checkBluetoothDeviceConnected();
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    isBluetoothDeviceConnected = false;
                    if (currentState == ScanState.SCANNING) {
                        resetToIdleState();
                        showToast("Bluetooth đã bị tắt. Vui lòng bật lại để tiếp tục.");
                    }
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                isBluetoothDeviceConnected = true;
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                isBluetoothDeviceConnected = false;
                if (currentState == ScanState.SCANNING) {
                    resetToIdleState();
                    showToast("Thiết bị Bluetooth đã ngắt kết nối.");
                }
            }
        }
    };

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_rfid_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        initBluetooth();
        setupClickListeners();
        registerBluetoothReceiver();
    }

    // ─── Init ─────────────────────────────────────────────────────────────────

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        btnInfo = view.findViewById(R.id.btnInfo);
        btnInputId = view.findViewById(R.id.btnInputId);
        btnScanRFID = view.findViewById(R.id.btnScanRFID);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvSubtitle = view.findViewById(R.id.tvSubtitle);
        ivScannerFrame = view.findViewById(R.id.ivScannerFrame);

        rotateAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_loading);
    }

    private void initBluetooth() {
        if (getContext() == null) return;
        BluetoothManager manager = (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null) {
            bluetoothAdapter = manager.getAdapter();
        }
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            checkBluetoothDeviceConnected();
        }
    }

    private void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        if (getContext() != null) {
            getContext().registerReceiver(bluetoothStateReceiver, filter);
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
        });

        btnInfo.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new RfidGuideFragment()).addToBackStack(null).commit();
            }
        });

        // NÚT 1: KHI BẤM QUÉT ĐỊNH DANH BÌNH THƯỜNG
        btnScanRFID.setOnClickListener(v -> {
            if (currentState != ScanState.IDLE) return;
            isScanningToAdd = false;
            requestBluetoothPermissionsAndScan();
        });

        // NÚT 2: KHI BẤM QUÉT ĐỂ THÊM MỚI (NÚT INPUT ID)
        btnInputId.setOnClickListener(v -> {
            if (currentState != ScanState.IDLE) return;
            isScanningToAdd = true;
            requestBluetoothPermissionsAndScan();
        });
    }

    // ─── Bluetooth permission & state check ──────────────────────────────────

    private void requestBluetoothPermissionsAndScan() {
        if (getContext() == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean scanGranted = ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean connectGranted = ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;

            if (!scanGranted || !connectGranted) {
                permissionLauncher.launch(new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                });
                return;
            }
        }
        checkBluetoothAndProceed();
    }

    private void checkBluetoothAndProceed() {
        if (bluetoothAdapter == null) {
            showBluetoothRequiredDialog("Thiết bị của bạn không hỗ trợ Bluetooth.");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            showEnableBluetoothDialog();
            return;
        }

        checkBluetoothDeviceConnected();

        if (!isBluetoothDeviceConnected) {
            showBluetoothRequiredDialog(
                    "Chưa có thiết bị Bluetooth nào được kết nối.\n\n" +
                            "Vui lòng ghép đôi và kết nối máy quét RFID trước khi quét.");
            return;
        }

        startScanning();
    }

    private void checkBluetoothDeviceConnected() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            isBluetoothDeviceConnected = false;
            return;
        }
        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            isBluetoothDeviceConnected = (pairedDevices != null && !pairedDevices.isEmpty());
        } catch (SecurityException e) {
            isBluetoothDeviceConnected = false;
        }
    }

    // ─── Dialogs ─────────────────────────────────────────────────────────────

    private void showEnableBluetoothDialog() {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Bluetooth chưa được bật")
                .setMessage("Vui lòng bật Bluetooth để kết nối với máy quét RFID.")
                .setPositiveButton("Bật Bluetooth", (dialog, which) -> {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    enableBluetoothLauncher.launch(enableIntent);
                })
                .setNegativeButton("Hủy", null)
                .setCancelable(false)
                .show();
    }

    private void showBluetoothRequiredDialog(String message) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Yêu cầu kết nối Bluetooth")
                .setMessage(message)
                .setPositiveButton("Mở Cài đặt Bluetooth", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Hủy", null)
                .setCancelable(false)
                .show();
    }

    // ─── Scan states ──────────────────────────────────────────────────────────

    private void startScanning() {
        currentState = ScanState.SCANNING;

        ivScannerFrame.setImageResource(R.drawable.ic_loading);
        ivScannerFrame.startAnimation(rotateAnimation);

        // ĐỔI TEXT DỰA VÀO BIẾN CỜ
        if (isScanningToAdd) {
            tvTitle.setText("Đang quét thẻ RFID mới");
            tvSubtitle.setText("Vui lòng đưa thẻ chưa đăng ký vào máy quét");
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

    private void resetToIdleState() {
        FragmentActivity activity = getActivity();
        if (activity == null || !isAdded()) return;

        activity.runOnUiThread(() -> {
            if (!isAdded()) return;
            currentState = ScanState.IDLE;

            ivScannerFrame.clearAnimation();
            ivScannerFrame.setImageResource(R.drawable.ic_rfid_scan_frame);

            tvTitle.setText("QUÉT RFID NHẬN DẠNG THÚ CƯNG");
            tvTitle.setAllCaps(true);
            tvTitle.setTextColor(getResources().getColor(android.R.color.white));
            tvSubtitle.setText("Kết nối Bluetooth với máy quét RFID để định danh thú cưng.");

            btnScanRFID.setVisibility(View.VISIBLE);
            btnInputId.setVisibility(View.VISIBLE);
        });
    }

    // ─── RFIDListener ────────────────────────────────────────────────────────

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
                        bundle.putString("PET_ID", pet.getId());
                        successFragment.setArguments(bundle);
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.main_container, successFragment)
                                .addToBackStack(null)
                                .commit();
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
            ((MainActivity) getActivity()).setRFIDListener(null);
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }

        try {
            if (getContext() != null) {
                getContext().unregisterReceiver(bluetoothStateReceiver);
            }
        } catch (IllegalArgumentException ignored) {}
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}