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
            undo();
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

    private void undo() {
        int movesToAdvance = 1;
        if (controlIsPressed)
            movesToAdvance = 10;

        for (int i = 0; i < movesToAdvance; i++)
            Lizzie.board.previousMove();
    }

    private void redo() {
        int movesToAdvance = 1;
        if (controlIsPressed)
            movesToAdvance = 10;

        for (int i = 0; i < movesToAdvance; i++)
            Lizzie.board.nextMove();
    }

    @Override
    public void keyPressed(KeyEvent e) {

        int movesToAdvance = 1; // number of moves to advance if control is held down
        switch (e.getKeyCode()) {
            case VK_CONTROL:

                controlIsPressed = true;
                break;

            case VK_RIGHT:
                redo();
                break;

            case VK_LEFT:
                undo();
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
                // stop the ponder
                if (Lizzie.leelaz.isPondering())
                    Lizzie.leelaz.togglePonder();
                Lizzie.frame.saveSgf();
                break;

            case VK_O:
                if (Lizzie.leelaz.isPondering())
                    Lizzie.leelaz.togglePonder();
                Lizzie.frame.openSgf();
                break;

            case VK_HOME:
                while (Lizzie.board.previousMove()) ;
                break;

            case VK_END:
                while (Lizzie.board.nextMove()) ;
                break;

            case VK_X:
                if (!Lizzie.frame.showControls) {
                    if (Lizzie.leelaz.isPondering()) {
                        wasPonderingWhenControlsShown = true;
                        Lizzie.leelaz.togglePonder();
                    } else {
                        wasPonderingWhenControlsShown = false;
                    }
                    Lizzie.frame.drawControls();
                }
                Lizzie.frame.showControls = true;
                break;

            case VK_C:
                Lizzie.frame.toggleCoordinates();
                break;

            default:
        }
    }

    boolean wasPonderingWhenControlsShown = false;
    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case VK_CONTROL:
                controlIsPressed = false;
                break;

            case VK_X:
                if (wasPonderingWhenControlsShown)
                    Lizzie.leelaz.togglePonder();
                Lizzie.frame.showControls = false;
                break;

            default:
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getWheelRotation() > 0) {
            redo();
        } else if (e.getWheelRotation() < 0) {
            undo();
        }
    }
}