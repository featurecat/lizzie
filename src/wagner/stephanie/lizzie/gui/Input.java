package wagner.stephanie.lizzie.gui;

import wagner.stephanie.lizzie.Lizzie;

import java.awt.event.*;

public class Input implements MouseListener, KeyListener, MouseWheelListener, MouseMotionListener {
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
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        
        Lizzie.frame.onMouseMoved(x, y);
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
        } else if (e.getKeyCode() == KeyEvent.VK_M) {
            Lizzie.config.toggleShowMoveNumber();
        } else if (e.getKeyCode() == KeyEvent.VK_S) {
            // Don 't ask
            // Stop the ponder when 
            Lizzie.leelaz.ponder();
            Lizzie.leelaz.togglePonder();
            Lizzie.frame.saveSgf();
        } else if (e.getKeyCode() == KeyEvent.VK_O) {
            Lizzie.leelaz.ponder();
            Lizzie.leelaz.togglePonder();
            Lizzie.frame.openSgf();
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