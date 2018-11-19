package nemo.knobview.ui;

import android.content.Context;
import android.graphics.Typeface;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: 表盘时间字体加载
 *
 * @author Li Jianying
 * @version 2.0
 * @since 2018/7/31
 */
public class FontType {
    private static Typeface defaultFont = null;
    private static final Map<Type, Typeface> fontCache = new HashMap<>();

    public enum Type {
        Regular("fonts/Lato-Reg.ttf"),
        Light("fonts/Lato-Reg.ttf"),
        Bold("fonts/Lato-Reg.ttf");
        
        protected final String res;

        Type(String res) {
            this.res = res;
        }
    }

    public static Typeface getTypeFace(Context context,Type fontType) {
        if (fontType == null && defaultFont != null) {
            return defaultFont;
        }
        Typeface tf = fontCache.get(fontType);
        if (tf != null) {
            return tf;
        }
        assert fontType != null;
        tf = Typeface.createFromAsset(context.getAssets(), fontType.res);
        fontCache.put(fontType, tf);
        return tf;
    }
}
