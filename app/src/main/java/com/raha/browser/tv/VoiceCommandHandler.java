package com.raha.browser.tv;

import java.util.Locale;

final class VoiceCommandHandler {
    enum Action { SEARCH, HOME, BACK, FORWARD, RELOAD, NEW_TAB, CLOSE_TAB, PRIVATE, SETTINGS, LIGHT, DARK, DESKTOP, MOBILE, YOUTUBE, GOOGLE, SOUNDCLOUD, CHATGPT, IPTV, FILES, PLAY, PAUSE, FULLSCREEN }
    record Result(Action action, String query) {}
    static Result parse(String phrase) {
        String p = phrase == null ? "" : phrase.trim().toLowerCase(Locale.ROOT);
        if (matches(p,"خانه","home")) return new Result(Action.HOME,"");
        if (matches(p,"برگرد","بازگشت","go back","back")) return new Result(Action.BACK,"");
        if (matches(p,"جلو برو","forward")) return new Result(Action.FORWARD,"");
        if (matches(p,"تازه سازی","تازه‌سازی","رفرش","reload","refresh")) return new Result(Action.RELOAD,"");
        if (matches(p,"تب جدید","new tab")) return new Result(Action.NEW_TAB,"");
        if (matches(p,"بستن تب","close tab")) return new Result(Action.CLOSE_TAB,"");
        if (matches(p,"حالت خصوصی","private mode")) return new Result(Action.PRIVATE,"");
        if (matches(p,"تنظیمات","settings")) return new Result(Action.SETTINGS,"");
        if (matches(p,"حالت روشن","پوسته روشن","light mode")) return new Result(Action.LIGHT,"");
        if (matches(p,"حالت تیره","پوسته تیره","dark mode")) return new Result(Action.DARK,"");
        if (matches(p,"حالت دسکتاپ","desktop mode")) return new Result(Action.DESKTOP,"");
        if (matches(p,"حالت موبایل","mobile mode")) return new Result(Action.MOBILE,"");
        if (p.contains("یوتیوب")||p.contains("youtube")) return new Result(Action.YOUTUBE,"");
        if (p.contains("ساندکلاد")||p.contains("soundcloud")) return new Result(Action.SOUNDCLOUD,"");
        if (p.contains("چت جی پی تی")||p.contains("چت‌جی‌پی‌تی")||p.contains("chatgpt")) return new Result(Action.CHATGPT,"");
        if (p.contains("گوگل")||p.equals("google")) return new Result(Action.GOOGLE,"");
        if (p.contains("آی پی تی وی")||p.contains("تلویزیون اینترنتی")||p.contains("iptv")) return new Result(Action.IPTV,"");
        if (p.contains("فایل")||p.contains("فلش")||p.contains("usb")) return new Result(Action.FILES,"");
        if (matches(p,"پخش","play")) return new Result(Action.PLAY,"");
        if (matches(p,"توقف","مکث","pause")) return new Result(Action.PAUSE,"");
        if (matches(p,"تمام صفحه","تمام‌صفحه","fullscreen")) return new Result(Action.FULLSCREEN,"");
        return new Result(Action.SEARCH, phrase == null ? "" : phrase.trim());
    }
    private static boolean matches(String p,String... values){ for(String v:values) if(p.equals(v)) return true; return false; }
}
