package wagner.stephanie.lizzie.plugin;


import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;

public interface IPlugin {
    public void onInit(Class lizzieClass) throws IOException;

    public void onMousePressed(MouseEvent e);

    public void onMouseReleased(MouseEvent e);

    public void onMouseMoved(MouseEvent e);

    public void onKeyPressed(KeyEvent e);

    public void onKeyReleased(KeyEvent e);

    public boolean onDraw(Graphics2D g);

    public void onShutdown() throws IOException;

    public String getName();
    public String getVersion();
}
