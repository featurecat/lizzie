package wagner.stephanie.lizzie.theme;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;


/**
 * DefaultTheme
 */
public class DefaultTheme implements ITheme{
    @Override
    public BufferedImage getBlackStone(int[] position) throws IOException {
        return ImageIO.read(new File("assets/black0.png"));
    }
    @Override
    public BufferedImage getWhiteStone(int[] position) throws IOException {
        return ImageIO.read(new File("assets/white0.png"));
    }
    @Override
    public BufferedImage getBoard() throws IOException {
        return ImageIO.read(new File("assets/board.png"));
    }
    @Override
    public BufferedImage getBackground() throws IOException {
        return ImageIO.read(new File("assets/background.jpg"));
    }
}