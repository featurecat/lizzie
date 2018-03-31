package wagner.stephanie.lizzie.plugin;


import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.io.IOException;
import org.w3c.dom.events.MouseEvent;

public interface IPlugin {
    public void init(Class lizzieClass) throws IOException;
    public void onKeyPressed(KeyEvent e);
    public void onketReleased(KeyEvent e);
    public void onDraw(Graphics2D g0);
}
