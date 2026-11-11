package com.ddvader44.officebuddy.excel;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.ddvader44.officebuddy.BuildConfig;
import com.ddvader44.officebuddy.FileScanner;
import com.ddvader44.officebuddy.R;

import java.io.File;
import java.util.ArrayList;

public class excelActivity extends AppCompatActivity {

    private ListView listView;
    private excelAdapter obj_adapter;
    private final ArrayList<File> fileList = new ArrayList<>();   // instance, not static
    private static final int REQUEST_PERMISSION = 1;
    private Thread scanThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excel);
        listView = findViewById(R.id.listView_excel);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File file = fileList.get(position);               // use the model, not toString()
                openWithViewer(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            }
        });

        checkPermissionAndLoad();
    }

    private void checkPermissionAndLoad() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        } else {
            loadFiles();
        }
    }

    /**
     * Scanning the whole storage tree is slow, so it runs on a background thread.
     * Only the adapter update touches the UI thread.
     */
    private void loadFiles() {
        if (scanThread != null && scanThread.isAlive()) return;    // avoid parallel scans
        final File dir = android.os.Environment.getExternalStorageDirectory();
        final ArrayList<File> result = new ArrayList<>();
        scanThread = new Thread(new Runnable() {
            @Override
            public void run() {
                result.addAll(FileScanner.scan(dir,
                        f -> FileScanner.hasExtension(f, ".xlsx", ".xls")));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fileList.clear();
                        fileList.addAll(result);
                        obj_adapter = new excelAdapter(getApplicationContext(), fileList);
                        listView.setAdapter(obj_adapter);
                        if (fileList.isEmpty()) {
                            Toast.makeText(excelActivity.this, "No documents found", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }, "file-scan");
        scanThread.start();
    }

    private void openWithViewer(File file, String mime) {
        Uri uri = FileProvider.getUriForFile(getApplicationContext(),
                BuildConfig.APPLICATION_ID + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mime);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app installed that can open this file", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanThread != null) scanThread.interrupt();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFiles();
            } else {
                Toast.makeText(this, "Storage permission is required to list documents",
                        Toast.LENGTH_LONG).show();
                finish();                                          // don't leave a dead empty screen
            }
        }
    }
}
