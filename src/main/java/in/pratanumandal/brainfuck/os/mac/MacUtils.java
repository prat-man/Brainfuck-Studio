package in.pratanumandal.brainfuck.os.mac;

import com.sun.jna.Platform;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

public class MacUtils {

    private static void setMenuStyle(Menu menu) {
        for (MenuItem item : menu.getItems()) {
            item.setGraphic(null);
            if (item instanceof Menu) {
                setMenuStyle((Menu) item);
            }
        }
    }

    public static void setMenuStyle(MenuBar menuBar) {
        if (Platform.getOSType() == Platform.MAC) {
            // use system menu bar
            menuBar.setUseSystemMenuBar(true);

            // remove icons
            for (Menu menu : menuBar.getMenus()) {
                setMenuStyle(menu);
            }
        }
    }

}
