package com.ddvader44.officebuddy.pdf;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ddvader44.officebuddy.FileScanner;
import com.ddvader44.officebuddy.R;

import java.io.File;
import java.util.ArrayList;

public class pdfActivity extends AppCompatActivity {

    private ListView pdfListView;
    private pdfAdapter obj_adapter;
    private final ArrayList<File> fileList = new ArrayList<>();   // instance, not static
    private static final int REQUEST_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);
        pdfListView = findViewById(R.id.listView_pdf);

        pdfListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File file = fileList.get(position);               // use the model, not toString()
                Intent intent = new Intent(pdfActivity.this, viewPdfActivity.class);
                intent.putExtra("path", file.getAbsolutePath());  // pass the path, not the index
                startActivity(intent);
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
                f -> FileScanner.hasExtension(f, ".pdf")));
        obj_adapter = new pdfAdapter(getApplicationContext(), fileList);
        pdfListView.setAdapter(obj_adapter);
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
