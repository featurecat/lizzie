package wagner.stephanie.lizzie.gui;

import wagner.stephanie.lizzie.Lizzie;

import java.awt.event.*;

import static java.awt.event.KeyEvent.*;

public class Input implements MouseListener, KeyListener, MouseWheelListener, MouseMotionListener {
    private boolean controlIsPressed = false;

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

        int movesToAdvance = 1; // number of moves to advance if control is held down
        switch (e.getKeyCode()) {
            case VK_CONTROL:

                controlIsPressed = true;
                break;

            case VK_RIGHT:
                if (controlIsPressed)
                    movesToAdvance = 10;
                for (int i = 0; i < movesToAdvance; i++)
                    Lizzie.board.nextMove();
                break;

            case VK_LEFT:
                if (controlIsPressed)
                    movesToAdvance = 10;
                for (int i = 0; i < movesToAdvance; i++)
                    Lizzie.board.previousMove();
                break;

            case VK_SPACE:
                Lizzie.leelaz.togglePonder();
                break;

            case VK_P:
                Lizzie.board.pass();
                break;

            case VK_M:
                Lizzie.config.toggleShowMoveNumber();
                break;

            case VK_S:
                // Don't ask
                // Stop the ponder when
                Lizzie.leelaz.ponder();
                Lizzie.leelaz.togglePonder();
                Lizzie.frame.saveSgf();
                break;

            case VK_O:
                Lizzie.leelaz.ponder();
                Lizzie.leelaz.togglePonder();
                Lizzie.frame.openSgf();
                break;

            case VK_HOME:
                while (Lizzie.board.previousMove()) ;
                break;

            case VK_END:
                while (Lizzie.board.nextMove()) ;
                break;

            default:
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case VK_CONTROL:
                controlIsPressed = false;
                break;

            default:
        }
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