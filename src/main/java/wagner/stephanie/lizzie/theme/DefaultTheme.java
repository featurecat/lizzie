package wagner.stephanie.lizzie.theme;

import java.awt.Image;
import java.io.IOException;

import javax.imageio.ImageIO;


/**
 * DefaultTheme
 */
public class DefaultTheme implements ITheme{
    @Override
    public Image getBlackStone(int[] position) throws IOException {
        return ImageIO.read(DefaultTheme.class.getResource("/assets/black0.png"));
    }
    @Override
    public Image getWhiteStone(int[] position) throws IOException {
        return ImageIO.read(DefaultTheme.class.getResource("/assets/white0.png"));
    }
    @Override
    public Image getBoard() throws IOException {
        return ImageIO.read(DefaultTheme.class.getResource("/assets/board.png"));
    }
    @Override
    public Image getBackground() throws IOException {
        return ImageIO.read(DefaultTheme.class.getResource("/assets/background.jpg"));
    }
}