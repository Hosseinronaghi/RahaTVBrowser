package com.raha.browser.tv;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public class SettingsActivity extends AppCompatActivity {
    private AppSettings settings;
    private Button themeButton, languageButton, directionButton, voiceLanguageButton, qualityButton;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_settings); FontManager.apply(this, findViewById(android.R.id.content)); settings=new AppSettings(this);
        themeButton=findViewById(R.id.themeSetting); languageButton=findViewById(R.id.languageSetting); directionButton=findViewById(R.id.directionSetting); voiceLanguageButton=findViewById(R.id.voiceLanguageSetting); qualityButton=findViewById(R.id.qualitySetting);
        findViewById(R.id.backSettings).setOnClickListener(v->finish());
        themeButton.setOnClickListener(v->chooseTheme()); languageButton.setOnClickListener(v->chooseLanguage()); directionButton.setOnClickListener(v->chooseDirection()); voiceLanguageButton.setOnClickListener(v->chooseVoiceLanguage()); qualityButton.setOnClickListener(v->chooseQuality());
        findViewById(R.id.clearHistorySetting).setOnClickListener(v->{new BrowserStore(this).clearHistory(); android.widget.Toast.makeText(this,R.string.history_cleared,android.widget.Toast.LENGTH_SHORT).show();});
        android.widget.Switch voice=findViewById(R.id.voiceCommandsSetting); voice.setChecked(settings.voiceCommands()); voice.setOnCheckedChangeListener((b,c)->settings.put("voice_commands",c));
        SeekBar speed=findViewById(R.id.pointerSpeedSetting); speed.setProgress(Math.max(1,Math.min(60,(int)settings.pointerSpeed()))); speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){settings.put("pointer_speed",(float)Math.max(8,p));}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        refreshLabels();
    }
    private void chooseTheme(){String[] labels={getString(R.string.theme_dark),getString(R.string.theme_light),getString(R.string.theme_system)};String[] vals={"dark","light","system"};new AlertDialog.Builder(this).setTitle(R.string.theme).setItems(labels,(d,w)->{settings.put("theme",vals[w]);int mode=w==1?AppCompatDelegate.MODE_NIGHT_NO:w==2?AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:AppCompatDelegate.MODE_NIGHT_YES;AppCompatDelegate.setDefaultNightMode(mode);recreate();}).show();}
    private void chooseLanguage(){String[] labels={getString(R.string.language_auto),"فارسی","English"};String[] vals={"auto","fa","en"};new AlertDialog.Builder(this).setTitle(R.string.language).setItems(labels,(d,w)->{settings.put("language",vals[w]);AppCompatDelegate.setApplicationLocales("auto".equals(vals[w])?LocaleListCompat.getEmptyLocaleList():LocaleListCompat.forLanguageTags(vals[w]));recreate();}).show();}
    private void chooseDirection(){String[] labels={getString(R.string.direction_auto),getString(R.string.direction_rtl),getString(R.string.direction_ltr)};String[] vals={"auto","rtl","ltr"};new AlertDialog.Builder(this).setTitle(R.string.direction).setItems(labels,(d,w)->{settings.put("direction",vals[w]);recreate();}).show();}
    private void chooseVoiceLanguage(){String[] labels={getString(R.string.language_auto),"فارسی","English"};String[] vals={"auto","fa-IR","en-US"};new AlertDialog.Builder(this).setTitle(R.string.voice_language).setItems(labels,(d,w)->{settings.put("voice_language",vals[w]);refreshLabels();}).show();}
    private void chooseQuality(){String[] labels={getString(R.string.quality_auto),"720p","1080p","4K"};int[] vals={0,720,1080,2160};new AlertDialog.Builder(this).setTitle(R.string.preferred_quality).setItems(labels,(d,w)->{settings.put("preferred_height",vals[w]);refreshLabels();}).show();}
    private void refreshLabels(){themeButton.setText(getString(R.string.theme)+": "+localizedTheme(settings.theme()));languageButton.setText(getString(R.string.language)+": "+localizedLanguage(settings.language()));directionButton.setText(getString(R.string.direction)+": "+localizedDirection(settings.direction()));voiceLanguageButton.setText(getString(R.string.voice_language)+": "+localizedVoice(settings.voiceLanguage()));qualityButton.setText(getString(R.string.preferred_quality)+": "+(settings.preferredHeight()==0?getString(R.string.quality_auto):settings.preferredHeight()==2160?"4K":settings.preferredHeight()+"p"));}
    private String localizedTheme(String v){return "light".equals(v)?getString(R.string.theme_light):"system".equals(v)?getString(R.string.theme_system):getString(R.string.theme_dark);}
    private String localizedLanguage(String v){return "fa".equals(v)?"فارسی":"en".equals(v)?"English":getString(R.string.language_auto);}
    private String localizedDirection(String v){return "rtl".equals(v)?getString(R.string.direction_rtl):"ltr".equals(v)?getString(R.string.direction_ltr):getString(R.string.direction_auto);}
    private String localizedVoice(String v){return "fa-IR".equals(v)?"فارسی":"en-US".equals(v)?"English":getString(R.string.language_auto);}
}
