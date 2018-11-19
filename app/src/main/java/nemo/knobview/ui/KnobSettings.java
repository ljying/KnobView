package nemo.knobview.ui;


public class KnobSettings {

    private final static String FILE_NAME = "KnobSettings";

    private final static String HANDLE_KEY = "is_handle_movement";

    private KnobSettings() {
    }


    public static boolean hasHandleMovementShown() {
//        return sKeeper.get(HANDLE_KEY,false);
        return false;
    }

    public static void setHandleMovementShown(boolean wasShown) {
        //TODO
    }

}
