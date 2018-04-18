package wagner.stephanie.lizzie.theme;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;


/**
 * DefaultTheme
 */
public class DefaultTheme implements ITheme{
    @Override
    public BufferedImage getBlackStone(int[] position) throws IOException {
        BufferedImage ret = ImageIO.read(DefaultTheme.class.getResource("/assets/black0.png"));
        return ret;
    }
    @Override
    public BufferedImage getWhiteStone(int[] position) throws IOException {
        BufferedImage ret = ImageIO.read(DefaultTheme.class.getResource("/assets/white0.png"));
        return ret;
    }
    @Override
    public BufferedImage getBoard() throws IOException {
        BufferedImage ret = ImageIO.read(DefaultTheme.class.getResource("/assets/board.png"));
        return ret;
    }
    @Override
    public BufferedImage getBackground() throws IOException {
        BufferedImage ret = ImageIO.read(DefaultTheme.class.getResource("/assets/background.jpg"));
        return ret;
    }
}