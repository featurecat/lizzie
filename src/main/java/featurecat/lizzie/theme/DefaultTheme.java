package featurecat.lizzie.theme;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;


/**
 * DefaultTheme
 */
public class DefaultTheme implements ITheme {
    BufferedImage blackStoneCached = null;
    BufferedImage whiteStoneCached = null;
    BufferedImage boardCached = null;
    BufferedImage backgroundCached = null;

    @Override
    public BufferedImage getBlackStone(int[] position) {
        if (blackStoneCached == null) {
            try {
                blackStoneCached = ImageIO.read(getClass().getResourceAsStream("/assets/black0.png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return blackStoneCached;
    }

    @Override
    public BufferedImage getWhiteStone(int[] position) {
        if (whiteStoneCached == null) {
            try {
                whiteStoneCached = ImageIO.read(getClass().getResourceAsStream("/assets/white0.png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return whiteStoneCached;
    }

    @Override
    public BufferedImage getBoard() {
        if (boardCached == null) {
            try {
                boardCached = ImageIO.read(getClass().getResourceAsStream("/assets/board.png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return boardCached;
    }

    @Override
    public BufferedImage getBackground() {
        if (backgroundCached == null) {
            try {
                backgroundCached = ImageIO.read(getClass().getResourceAsStream("/assets/background.jpg"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return backgroundCached;
    }
}