package com.raha.browser.tv;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class IptvActivity extends AppCompatActivity {
    private record Channel(String name,String url,String group){}
    private final List<Channel> channels=new ArrayList<>(); private ArrayAdapter<String> adapter; private EditText source;
    private final ActivityResultLauncher<String[]> filePicker=registerForActivityResult(new ActivityResultContracts.OpenDocument(),this::loadFile);
    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_iptv);source=findViewById(R.id.iptvSource);ListView list=findViewById(R.id.channelList);adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,new ArrayList<>());list.setAdapter(adapter);findViewById(R.id.iptvLoadUrl).setOnClickListener(v->loadUrl(source.getText().toString()));findViewById(R.id.iptvLoadFile).setOnClickListener(v->filePicker.launch(new String[]{"application/x-mpegURL","application/vnd.apple.mpegurl","text/plain","*/*"}));findViewById(R.id.iptvBack).setOnClickListener(v->finish());list.setOnItemClickListener((p,v,pos,id)->play(channels.get(pos)));}
    private void loadUrl(String value){String u=value==null?"":value.trim();if(!(u.startsWith("https://")||u.startsWith("http://"))){Toast.makeText(this,R.string.invalid_url,Toast.LENGTH_SHORT).show();return;}Executors.newSingleThreadExecutor().execute(()->{try{OkHttpClient c=new OkHttpClient.Builder().followRedirects(true).build();Request r=new Request.Builder().url(u).header("User-Agent","RahaTVBrowser/0.6").build();try(Response x=c.newCall(r).execute()){if(!x.isSuccessful()||x.body()==null)throw new Exception("HTTP "+x.code());String text=x.body().string();if(text.length()>8_000_000)throw new Exception("Playlist too large");runOnUiThread(()->parse(text));}}catch(Exception e){runOnUiThread(()->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());}});}
    private void loadFile(Uri uri){if(uri==null)return;Executors.newSingleThreadExecutor().execute(()->{try(BufferedReader br=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)))){StringBuilder s=new StringBuilder();String line;while((line=br.readLine())!=null&&s.length()<8_000_000)s.append(line).append('\n');runOnUiThread(()->parse(s.toString()));}catch(Exception e){runOnUiThread(()->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());}});}
    private void parse(String text){channels.clear();String pending=null,group="";for(String raw:text.split("\\r?\\n")){String l=raw.trim();if(l.startsWith("#EXTINF")){int comma=l.indexOf(',');pending=comma>=0?l.substring(comma+1).trim():getString(R.string.iptv_channel);int gi=l.indexOf("group-title=\"");if(gi>=0){int end=l.indexOf('"',gi+13);if(end>gi)group=l.substring(gi+13,end);}}else if(!l.isEmpty()&&!l.startsWith("#")&&(l.startsWith("http://")||l.startsWith("https://"))){channels.add(new Channel(pending==null?l:pending,l,group));pending=null;}}List<String> names=new ArrayList<>();for(Channel c:channels)names.add((c.group().isBlank()?"":c.group()+" • ")+c.name());adapter.clear();adapter.addAll(names);adapter.notifyDataSetChanged();if(channels.isEmpty())Toast.makeText(this,R.string.no_channels,Toast.LENGTH_SHORT).show();}
    private void play(Channel c){Intent i=new Intent(this,PlayerActivity.class);i.putExtra(PlayerActivity.EXTRA_URL,c.url());i.putExtra(PlayerActivity.EXTRA_TITLE,c.name());startActivity(i);}
}
