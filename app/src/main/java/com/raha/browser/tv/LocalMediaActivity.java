package com.raha.browser.tv;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class LocalMediaActivity extends AppCompatActivity {
    private final List<DocumentFile> files = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private final ActivityResultLauncher<Uri> folderPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(), this::onFolderSelected);
    private final ActivityResultLauncher<String[]> filePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> { if (uri != null) { Intent i=new Intent(this,PlayerActivity.class); i.putExtra(PlayerActivity.EXTRA_URL,uri.toString()); startActivity(i); } });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_media);
        Button select = findViewById(R.id.selectFolderButton);
        ListView list = findViewById(R.id.mediaList);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        list.setAdapter(adapter);
        select.setOnClickListener(v -> folderPicker.launch(null));
        findViewById(R.id.selectSingleFileButton).setOnClickListener(v -> filePicker.launch(new String[]{"video/*","audio/*","image/*"}));
        list.setOnItemClickListener((p,v,pos,id) -> open(files.get(pos)));
    }

    private void onFolderSelected(Uri uri) {
        if (uri == null) return;
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}
        DocumentFile root = DocumentFile.fromTreeUri(this, uri);
        files.clear();
        if (root != null) collect(root, 0);
        files.sort(Comparator.comparing(f -> f.getName() == null ? "" : f.getName().toLowerCase(Locale.ROOT)));
        List<String> names = new ArrayList<>();
        for (DocumentFile f : files) names.add(f.getName());
        adapter.clear(); adapter.addAll(names); adapter.notifyDataSetChanged();
        if (files.isEmpty()) Toast.makeText(this, R.string.no_media, Toast.LENGTH_SHORT).show();
    }

    private void collect(DocumentFile folder, int depth) {
        if (depth > 4 || files.size() > 1000) return;
        for (DocumentFile f : folder.listFiles()) {
            if (f.isDirectory()) collect(f, depth + 1);
            else if (f.isFile() && supported(f.getName(), f.getType())) files.add(f);
        }
    }

    private boolean supported(String name, String mime) {
        if (mime != null && (mime.startsWith("video/") || mime.startsWith("audio/") || mime.startsWith("image/"))) return true;
        if (name == null) return false;
        String n = name.toLowerCase(Locale.ROOT);
        return n.matches(".*\\.(mp4|mkv|webm|avi|mov|ts|m2ts|mp3|aac|m4a|flac|ogg|wav|jpg|jpeg|png|webp|gif)$");
    }

    private void open(DocumentFile file) {
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra(PlayerActivity.EXTRA_URL, file.getUri().toString());
        startActivity(i);
    }
}
