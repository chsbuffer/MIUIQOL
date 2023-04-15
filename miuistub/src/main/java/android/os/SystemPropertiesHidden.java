package android.os;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(SystemProperties.class)
public class SystemPropertiesHidden {
    public static String get(String key) {
        throw new RuntimeException("Stub!");
    }
}
