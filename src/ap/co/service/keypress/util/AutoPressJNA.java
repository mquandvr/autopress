package ap.co.service.keypress.util;

import ap.co.service.keypress.AutoPressConstants;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import javafx.scene.input.KeyCode;

public class AutoPressJNA {

    private final User32 user32 = User32.INSTANCE;

    private static WinDef.HWND window = null;

    private static AutoPressJNA autoPressJNA = null;

    public static AutoPressJNA getInstance() {
        if (autoPressJNA == null) {
            autoPressJNA = new AutoPressJNA();
        }
        return autoPressJNA;
    }

    public boolean checkWindowExist(String windowName) {
        return !user32.EnumWindows((hwnd, pointer) -> {
            char[] windowText = new char[512];
            user32.GetWindowText(hwnd, windowText, 512);
            String wText = Native.toString(windowText).trim();

            if (!wText.isEmpty() && wText.toLowerCase().contains(windowName.toLowerCase())) {
                window = hwnd;
                return false;
            }
            return true;
        }, null);
    }

    public void postMessage(String key) {
        user32.ShowWindow(window, WinUser.SW_SHOWNOACTIVATE);
        user32.PostMessage(window, WinUser.WM_KEYDOWN, new WinDef.WPARAM(KeyCode.getKeyCode(key).getCode()), new WinDef.LPARAM());
    }

}