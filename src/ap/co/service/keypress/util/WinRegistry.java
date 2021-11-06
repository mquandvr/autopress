package ap.co.service.keypress.util;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

public class WinRegistry {

    private static Preferences pref = null;

    private static WinRegistry winRegistry = null;

    public static WinRegistry getInstance() {
        if (winRegistry == null) {
            winRegistry = new WinRegistry();
        }
        if (pref == null) {
            pref = Preferences.userRoot();
        }
        return winRegistry;
    }

    public void writeRegistry(String key, String value) {
        pref.put(key, value);
    }

    public String readRegistry(String key) {
        return pref.get(key, StringUtils.EMPTY);
    }
}