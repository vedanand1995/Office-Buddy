package com.ddvader44.officebuddy.pdf;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ddvader44.officebuddy.R;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;

import java.io.File;

public class viewPdfActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_pdf);
        PDFView pdfView = findViewById(R.id.pdfView);

        String path = getIntent().getStringExtra("path");
        // Backwards compatibility with the old index-based extra
        if (path == null) {
            int position = getIntent().getIntExtra("position", -1);
            if (position >= 0 && position < pdfActivity.fileList.size()) {
                path = pdfActivity.fileList.get(position).getAbsolutePath();
            }
        }

        File file = path != null ? new File(path) : null;
        if (file == null || !file.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        pdfView.fromFile(file)
                .enableSwipe(true)
                .enableAnnotationRendering(true)
                .scrollHandle(new DefaultScrollHandle(this))
                .load();
    }
}
