package wagner.stephanie.lizzie.gui;

import wagner.stephanie.lizzie.Lizzie;

import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.event.*;

public class Input implements MouseListener, KeyListener, MouseWheelListener {
    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        if (e.getButton() == MouseEvent.BUTTON1) // left mouse click
            Lizzie.frame.onClicked(x, y);
        else if (e.getButton() == MouseEvent.BUTTON3) // right mouse click
            Lizzie.board.previousMove(); // interpret as undo
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            Lizzie.board.nextMove();
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            Lizzie.board.previousMove();
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            Lizzie.leelaz.togglePonder();
        } else if (e.getKeyCode() == KeyEvent.VK_P) {
            Lizzie.board.pass();
        } else if (e.getKeyCode() == KeyEvent.VK_S) {
            Lizzie.frame.toggleShowMoveNumver();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
//        for (int i= 0; i < Math.abs(e.getWheelRotation()); i++) {
            if (e.getWheelRotation() > 0) {
                Lizzie.board.nextMove();
            } else if (e.getWheelRotation() < 0) {
                Lizzie.board.previousMove();
            }
//        }
    }
}
