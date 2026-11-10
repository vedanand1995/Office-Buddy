package com.ddvader44.officebuddy.excel;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excel);
        listView = findViewById(R.id.listView_excel);

        listView.setOnItemClickListener((AdapterView<?> parent, android.view.View view, int position, long id) -> {
            File file = fileList.get(position);                   // use the model, not toString()
            if ("excel".equals("pdf")) {
                Intent intent = new Intent(this, viewPdfActivity.class);
                intent.putExtra("path", file.getAbsolutePath());  // pass the path, not the index
                startActivity(intent);
            } else {
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

    private void loadFiles() {
        fileList.clear();                                          // never accumulate duplicates
        fileList.addAll(FileScanner.scan(
                android.os.Environment.getExternalStorageDirectory(),
                f -> FileScanner.hasExtension(f, ".xlsx", ".xls")));
        obj_adapter = new excelAdapter(getApplicationContext(), fileList);
        listView.setAdapter(obj_adapter);
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
