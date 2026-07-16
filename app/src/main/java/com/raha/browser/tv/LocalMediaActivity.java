package com.raha.browser.tv;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LocalMediaActivity extends AppCompatActivity {
    private record MediaEntry(String name, Uri uri, File file, boolean directory, String mime) {}

    private final List<MediaEntry> entries = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private TextView status;
    private File currentDirectory;

    private final ActivityResultLauncher<Uri> folderPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(), this::onFolderSelected);
    private final ActivityResultLauncher<String[]> filePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::openUri);
    private final ActivityResultLauncher<String[]> permissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> showStorageRoots());

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_media);
        FontManager.apply(this, findViewById(android.R.id.content));

        status = findViewById(R.id.localMediaStatus);
        ListView list = findViewById(R.id.mediaList);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        list.setAdapter(adapter);

        findViewById(R.id.localMediaClose).setOnClickListener(v -> finish());
        findViewById(R.id.localMediaUp).setOnClickListener(v -> goUp());
        findViewById(R.id.selectFolderButton).setOnClickListener(v -> launchFolderPicker());
        findViewById(R.id.selectSingleFileButton).setOnClickListener(v -> launchFilePicker());
        findViewById(R.id.scanStorageButton).setOnClickListener(v -> requestAndScan());
        list.setOnItemClickListener((parent, view, position, id) -> open(entries.get(position)));

        requestAndScan();
    }

    private void launchFolderPicker() {
        try {
            folderPicker.launch(null);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.file_picker_unavailable, Toast.LENGTH_LONG).show();
            requestAndScan();
        }
    }

    private void launchFilePicker() {
        try {
            filePicker.launch(new String[]{"video/*", "audio/*", "image/*"});
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.file_picker_unavailable, Toast.LENGTH_LONG).show();
            requestAndScan();
        }
    }

    private void requestAndScan() {
        List<String> needed = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33) {
            addIfMissing(needed, Manifest.permission.READ_MEDIA_VIDEO);
            addIfMissing(needed, Manifest.permission.READ_MEDIA_AUDIO);
            addIfMissing(needed, Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            addIfMissing(needed, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (needed.isEmpty()) showStorageRoots();
        else permissions.launch(needed.toArray(new String[0]));
    }

    private void addIfMissing(List<String> needed, String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) needed.add(permission);
    }

    private void showStorageRoots() {
        currentDirectory = null;
        entries.clear();
        Set<String> seen = new LinkedHashSet<>();
        addRoot(Environment.getExternalStorageDirectory(), seen);
        File[] appExternal = getExternalFilesDirs(null);
        if (appExternal != null) {
            for (File dir : appExternal) {
                if (dir == null) continue;
                File root = storageRootOf(dir);
                addRoot(root, seen);
            }
        }
        addChildrenOf(new File("/storage"), seen);
        addChildrenOf(new File("/mnt/media_rw"), seen);
        entries.sort(Comparator.comparing(e -> e.name().toLowerCase(Locale.ROOT)));
        refreshList();
        status.setText(entries.isEmpty() ? R.string.no_storage_found : R.string.storage_roots);
    }

    private void addChildrenOf(File parent, Set<String> seen) {
        File[] children;
        try { children = parent.listFiles(); } catch (Exception e) { return; }
        if (children == null) return;
        for (File child : children) if (child.isDirectory()) addRoot(child, seen);
    }

    private void addRoot(File root, Set<String> seen) {
        if (root == null) return;
        try {
            String key = root.getCanonicalPath();
            if (!seen.add(key) || !root.exists() || !root.isDirectory() || !root.canRead()) return;
            String name = root.getName().isBlank() ? key : root.getName();
            entries.add(new MediaEntry("▣ " + name, null, root, true, null));
        } catch (Exception ignored) {}
    }

    private File storageRootOf(File file) {
        File current = file;
        while (current != null && current.getParentFile() != null) {
            File parent = current.getParentFile();
            if ("Android".equals(current.getName())) return parent;
            current = parent;
        }
        return file;
    }

    private void browse(File directory) {
        currentDirectory = directory;
        entries.clear();
        File[] children;
        try { children = directory.listFiles(); } catch (Exception e) { children = null; }
        if (children != null) {
            for (File file : children) {
                if (!file.canRead() || file.isHidden()) continue;
                if (file.isDirectory()) entries.add(new MediaEntry("▣ " + file.getName(), null, file, true, null));
                else if (supported(file.getName(), null)) entries.add(new MediaEntry(file.getName(), Uri.fromFile(file), file, false, guessMime(file.getName())));
            }
        }
        entries.sort(Comparator.comparing((MediaEntry e) -> !e.directory()).thenComparing(e -> e.name().toLowerCase(Locale.ROOT)));
        refreshList();
        status.setText(directory.getAbsolutePath());
    }

    private void onFolderSelected(Uri uri) {
        if (uri == null) return;
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}
        DocumentFile root = DocumentFile.fromTreeUri(this, uri);
        entries.clear();
        if (root != null) collectDocument(root, 0);
        entries.sort(Comparator.comparing(e -> e.name().toLowerCase(Locale.ROOT)));
        refreshList();
        status.setText(uri.toString());
        if (entries.isEmpty()) Toast.makeText(this, R.string.no_media, Toast.LENGTH_SHORT).show();
    }

    private void collectDocument(DocumentFile folder, int depth) {
        if (depth > 5 || entries.size() >= 1500) return;
        DocumentFile[] children;
        try { children = folder.listFiles(); } catch (Exception e) { return; }
        for (DocumentFile child : children) {
            if (child.isDirectory()) collectDocument(child, depth + 1);
            else if (child.isFile() && supported(child.getName(), child.getType())) {
                String name = child.getName() == null ? getString(R.string.unnamed_file) : child.getName();
                entries.add(new MediaEntry(name, child.getUri(), null, false, child.getType()));
            }
        }
    }

    private void refreshList() {
        List<String> names = new ArrayList<>();
        for (MediaEntry entry : entries) names.add(entry.name());
        adapter.clear();
        adapter.addAll(names);
        adapter.notifyDataSetChanged();
    }

    private void goUp() {
        if (currentDirectory == null) { showStorageRoots(); return; }
        File parent = currentDirectory.getParentFile();
        if (parent == null || "/".equals(parent.getAbsolutePath())) showStorageRoots();
        else browse(parent);
    }

    private void open(MediaEntry entry) {
        if (entry.directory() && entry.file() != null) { browse(entry.file()); return; }
        openUri(entry.uri(), entry.mime());
    }

    private void openUri(Uri uri) { openUri(uri, null); }

    private void openUri(Uri uri, String mime) {
        if (uri == null) return;
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_URL, uri.toString());
        if (mime != null) intent.putExtra(PlayerActivity.EXTRA_MIME_TYPE, mime);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private boolean supported(String name, String mime) {
        if (mime != null && (mime.startsWith("video/") || mime.startsWith("audio/") || mime.startsWith("image/"))) return true;
        if (name == null) return false;
        return name.toLowerCase(Locale.ROOT).matches(".*\\.(mp4|mkv|webm|avi|mov|ts|m2ts|mp3|aac|m4a|flac|ogg|wav|jpg|jpeg|png|webp|gif|bmp)$");
    }

    private String guessMime(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.matches(".*\\.(jpg|jpeg|png|webp|gif|bmp)$")) return "image/*";
        if (n.matches(".*\\.(mp3|aac|m4a|flac|ogg|wav)$")) return "audio/*";
        return "video/*";
    }
}
