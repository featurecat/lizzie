package featurecat.lizzie.gui;

import static java.awt.event.KeyEvent.*;

import featurecat.lizzie.Lizzie;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;

public class Input implements MouseListener, KeyListener, MouseWheelListener, MouseMotionListener {
  @Override
  public void mouseClicked(MouseEvent e) {}

  @Override
  public void mousePressed(MouseEvent e) {
    if (e.getButton() == MouseEvent.BUTTON1) { // left click
      if (e.getClickCount() == 2) { // TODO: Maybe need to delay check
        Lizzie.frame.onDoubleClicked(e.getX(), e.getY());
      } else {
        Lizzie.frame.onClicked(e.getX(), e.getY());
      }
    } else if (e.getButton() == MouseEvent.BUTTON3) // right click
    undo();
  }

  @Override
  public void mouseReleased(MouseEvent e) {}

  @Override
  public void mouseEntered(MouseEvent e) {}

  @Override
  public void mouseExited(MouseEvent e) {}

  @Override
  public void mouseDragged(MouseEvent e) {
    Lizzie.frame.onMouseDragged(e.getX(), e.getY());
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    Lizzie.frame.onMouseMoved(e.getX(), e.getY());
  }

  @Override
  public void keyTyped(KeyEvent e) {}

  public static void undo() {
    undo(1);
  }

  public static void undo(int movesToAdvance) {
    if (Lizzie.board.inAnalysisMode()) Lizzie.board.toggleAnalysis();
    if (Lizzie.frame.isPlayingAgainstLeelaz) {
      Lizzie.frame.isPlayingAgainstLeelaz = false;
    }
    if (Lizzie.frame.incrementDisplayedBranchLength(-movesToAdvance)) {
      return;
    }

    for (int i = 0; i < movesToAdvance; i++) Lizzie.board.previousMove();
  }

  private void undoToChildOfPreviousWithVariation() {
    // Undo until the position just after the junction position.
    // If we are already on such a position, we go to
    // the junction position for convenience.
    // Use cases:
    // [Delete branch] Call this function and then deleteMove.
    // [Go to junction] Call this function twice.
    if (!Lizzie.board.undoToChildOfPreviousWithVariation()) Lizzie.board.previousMove();
  }

  private void undoToFirstParentWithVariations() {
    if (Lizzie.board.undoToChildOfPreviousWithVariation()) {
      Lizzie.board.previousMove();
    }
  }

  private void goCommentNode(boolean moveForward) {
    if (moveForward) {
      redo(Lizzie.board.getHistory().getCurrentHistoryNode().goToNextNodeWithComment());
    } else {
      undo(Lizzie.board.getHistory().getCurrentHistoryNode().goToPreviousNodeWithComment());
    }
  }

  private void redo() {
    redo(1);
  }

  private void redo(int movesToAdvance) {
    if (Lizzie.board.inAnalysisMode()) Lizzie.board.toggleAnalysis();
    if (Lizzie.frame.isPlayingAgainstLeelaz) {
      Lizzie.frame.isPlayingAgainstLeelaz = false;
    }
    if (Lizzie.frame.incrementDisplayedBranchLength(movesToAdvance)) {
      return;
    }

    for (int i = 0; i < movesToAdvance; i++) Lizzie.board.nextMove();
  }

  private void startTemporaryBoard() {
    if (Lizzie.config.showBestMoves) {
      startRawBoard();
    } else {
      Lizzie.config.showBestMovesTemporarily = true;
    }
  }

  private void startRawBoard() {
    if (!Lizzie.config.showRawBoard) {
      Lizzie.frame.startRawBoard();
    }
    Lizzie.config.showRawBoard = true;
  }

  private void stopRawBoard() {
    Lizzie.frame.stopRawBoard();
    Lizzie.config.showRawBoard = false;
  }

  private void stopTemporaryBoard() {
    stopRawBoard();
    Lizzie.config.showBestMovesTemporarily = false;
  }

  private void toggleHints() {
    Lizzie.config.toggleShowBranch();
    Lizzie.config.showSubBoard =
        Lizzie.config.showNextMoves = Lizzie.config.showBestMoves = Lizzie.config.showBranch;
  }

  private void nextBranch() {
    if (Lizzie.frame.isPlayingAgainstLeelaz) {
      Lizzie.frame.isPlayingAgainstLeelaz = false;
    }
    Lizzie.board.nextBranch();
  }

  private void previousBranch() {
    if (Lizzie.frame.isPlayingAgainstLeelaz) {
      Lizzie.frame.isPlayingAgainstLeelaz = false;
    }
    Lizzie.board.previousBranch();
  }

  private void moveBranchUp() {
    Lizzie.board.moveBranchUp();
  }

  private void moveBranchDown() {
    Lizzie.board.moveBranchDown();
  }

  private void deleteMove() {
    Lizzie.board.deleteMove();
  }

  private void deleteBranch() {
    Lizzie.board.deleteBranch();
  }

  private boolean controlIsPressed(KeyEvent e) {
    boolean mac = System.getProperty("os.name", "").toUpperCase().startsWith("MAC");
    return e.isControlDown() || (mac && e.isMetaDown());
  }

  private void toggleShowDynamicKomi() {
    Lizzie.config.showDynamicKomi = !Lizzie.config.showDynamicKomi;
  }

  @Override
  public void keyPressed(KeyEvent e) {
    // If any controls key is pressed, let's disable analysis mode.
    // This is probably the user attempting to exit analysis mode.
    boolean shouldDisableAnalysis = true;
    int refreshType = 1;

    switch (e.getKeyCode()) {
      case VK_E:
        Lizzie.frame.toggleGtpConsole();
        break;
      case VK_RIGHT:
        if (e.isShiftDown()) {
          moveBranchDown();
        } else {
          nextBranch();
        }
        break;

      case VK_LEFT:
        if (e.isShiftDown()) {
          moveBranchUp();
        } else if (controlIsPressed(e)) {
          undoToFirstParentWithVariations();
        } else {
          previousBranch();
        }
        break;

      case VK_UP:
        if (controlIsPressed(e) && e.isShiftDown()) {
          goCommentNode(false);
        } else if (e.isShiftDown()) {
          undoToChildOfPreviousWithVariation();
        } else if (controlIsPressed(e)) {
          undo(10);
        } else {
          if (Lizzie.frame.isMouseOver) {
            Lizzie.frame.doBranch(-1);
          } else {
            undo();
          }
        }
        break;

      case VK_PAGE_DOWN:
        if (controlIsPressed(e) && e.isShiftDown()) {
          Lizzie.frame.increaseMaxAlpha(-5);
        } else {
          redo(10);
        }
        break;

      case VK_DOWN:
        if (controlIsPressed(e) && e.isShiftDown()) {
          goCommentNode(true);
        } else if (controlIsPressed(e)) {
          redo(10);
        } else {
          if (Lizzie.frame.isMouseOver) {
            Lizzie.frame.doBranch(1);
          } else {
            redo();
          }
        }
        break;

      case VK_N:
        // stop the ponder
        if (Lizzie.leelaz.isPondering()) Lizzie.leelaz.togglePonder();
        Lizzie.frame.startGame();
        break;
      case VK_SPACE:
        if (Lizzie.frame.isPlayingAgainstLeelaz) {
          Lizzie.frame.isPlayingAgainstLeelaz = false;
          Lizzie.leelaz.isThinking = false;
        }
        Lizzie.leelaz.togglePonder();
        refreshType = 2;
        break;

      case VK_P:
        Lizzie.board.pass();
        break;

      case VK_COMMA:
        if (!Lizzie.frame.playCurrentVariation()) Lizzie.frame.playBestMove();
        break;

      case VK_M:
        if (e.isAltDown()) {
          Lizzie.frame.openChangeMoveDialog();
        } else {
          Lizzie.config.toggleShowMoveNumber();
        }
        break;

      case VK_Q:
        Lizzie.frame.openOnlineDialog();
        break;

      case VK_F:
        Lizzie.config.toggleShowNextMoves();
        break;

      case VK_H:
        Lizzie.config.toggleHandicapInsteadOfWinrate();
        break;

      case VK_PAGE_UP:
        if (controlIsPressed(e) && e.isShiftDown()) {
          Lizzie.frame.increaseMaxAlpha(5);
        } else {
          undo(10);
        }
        break;

      case VK_I:
        // stop the ponder
        boolean isPondering = Lizzie.leelaz.isPondering();
        if (isPondering) Lizzie.leelaz.togglePonder();
        Lizzie.frame.editGameInfo();
        if (isPondering) Lizzie.leelaz.togglePonder();
        break;

      case VK_S:
        if (e.isAltDown()) {
          Lizzie.frame.saveImage();
        } else {
          // stop the ponder
          if (Lizzie.leelaz.isPondering()) Lizzie.leelaz.togglePonder();
          Lizzie.frame.saveFile();
        }
        break;

      case VK_O:
        if (Lizzie.leelaz.isPondering()) Lizzie.leelaz.togglePonder();
        Lizzie.frame.openFile();
        break;

      case VK_B:
        if (controlIsPressed(e)) {
          Lizzie.watcher.setFilePath(null);
        } else {
          File file = Lizzie.frame.chooseFile();
          if (file != null) {
            Lizzie.watcher.setFilePath(file.getPath());
          }
        }
        break;

      case VK_V:
        if (controlIsPressed(e)) {
          Lizzie.frame.pasteSgf();
        } else {
          Lizzie.config.toggleShowBranch();
        }
        break;

      case VK_HOME:
        if (controlIsPressed(e)) {
          Lizzie.board.clear();
        } else {
          while (Lizzie.board.previousMove()) ;
        }
        break;

      case VK_END:
        while (Lizzie.board.nextMove()) ;
        break;

      case VK_X:
        if (controlIsPressed(e)) {
          Lizzie.frame.openConfigDialog();
        } else {
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
        }
        break;

      case VK_W:
        if (controlIsPressed(e)) {
          Lizzie.config.toggleLargeWinrate();
          refreshType = 2;
        } else if (e.isAltDown()) {
          Lizzie.frame.toggleDesignMode();
        } else {
          Lizzie.config.toggleShowWinrate();
          refreshType = 2;
        }
        break;

      case VK_L:
        Lizzie.config.toggleShowLcbWinrate();
        break;

      case VK_G:
        Lizzie.config.toggleShowVariationGraph();
        refreshType = 2;
        break;

      case VK_T:
        if (controlIsPressed(e)) {
          Lizzie.config.toggleShowCommentNodeColor();
        } else {
          Lizzie.config.toggleShowComment();
          refreshType = 2;
        }
        break;

      case VK_Y:
        Lizzie.config.toggleNodeColorMode();
        break;

      case VK_C:
        if (controlIsPressed(e)) {
          Lizzie.frame.copySgf();
        } else {
          Lizzie.config.toggleCoordinates();
          refreshType = 2;
        }
        break;

      case VK_ENTER:
        if (!Lizzie.leelaz.isThinking) {
          Lizzie.leelaz.sendCommand(
              "time_settings 0 "
                  + Lizzie.config
                      .config
                      .getJSONObject("leelaz")
                      .getInt("max-game-thinking-time-seconds")
                  + " 1");
          Lizzie.frame.playerIsBlack = !Lizzie.board.getData().blackToPlay;
          Lizzie.frame.isPlayingAgainstLeelaz = true;
          Lizzie.leelaz.genmove((Lizzie.board.getData().blackToPlay ? "B" : "W"));
        }
        break;

      case VK_DELETE:
      case VK_BACK_SPACE:
        if (e.isShiftDown()) {
          deleteBranch();
        } else {
          deleteMove();
        }
        break;

      case VK_Z:
        if (e.isShiftDown()) {
          toggleHints();
        } else if (e.isAltDown()) {
          Lizzie.config.toggleShowSubBoard();
        } else {
          startTemporaryBoard();
        }
        break;

      case VK_A:
        shouldDisableAnalysis = false;
        Lizzie.board.toggleAnalysis();
        break;

      case VK_PERIOD:
        if (Lizzie.leelaz.isKataGo) {
          if (e.isAltDown()) {
            Lizzie.frame.estimateByZen();
          } else {
            Lizzie.config.showKataGoEstimate = !Lizzie.config.showKataGoEstimate;
            Lizzie.leelaz.ponder();
            if (!Lizzie.config.showKataGoEstimate) {
              Lizzie.frame.removeEstimateRect();
            }
          }
        } else Lizzie.frame.estimateByZen();
        // if (!Lizzie.board.getHistory().getNext().isPresent()) {
        // Lizzie.board.setScoreMode(!Lizzie.board.inScoreMode());}
        break;

      case VK_D:
        if (Lizzie.leelaz.isKataGo) {
          if (Lizzie.config.showKataGoScoreMean && Lizzie.config.kataGoNotShowWinrate) {
            Lizzie.config.showKataGoScoreMean = false;
            Lizzie.config.kataGoNotShowWinrate = false;
            break;
          }
          if (Lizzie.config.showKataGoScoreMean && !Lizzie.config.kataGoNotShowWinrate) {
            Lizzie.config.kataGoNotShowWinrate = true;
            break;
          }
          if (Lizzie.config.showKataGoScoreMean) {
            Lizzie.config.showKataGoScoreMean = false;
            break;
          }
          if (!Lizzie.config.showKataGoScoreMean) {
            Lizzie.config.showKataGoScoreMean = true;
            Lizzie.config.kataGoNotShowWinrate = false;
          }
        } else {
          toggleShowDynamicKomi();
        }
        break;

      case VK_R:
        Lizzie.frame.replayBranch(e.isAltDown());
        break;

      case VK_OPEN_BRACKET:
        if (Lizzie.frame.boardPositionProportion > 0) {
          Lizzie.frame.boardPositionProportion--;
          refreshType = 2;
        }
        break;

      case VK_CLOSE_BRACKET:
        if (Lizzie.frame.boardPositionProportion < 8) {
          Lizzie.frame.boardPositionProportion++;
          refreshType = 2;
        }
        break;

      case VK_K:
        Lizzie.config.toggleEvaluationColoring();
        break;

        // Use Ctrl+Num to switching multiple engine
      case VK_0:
      case VK_1:
      case VK_2:
      case VK_3:
      case VK_4:
      case VK_5:
      case VK_6:
      case VK_7:
      case VK_8:
      case VK_9:
        if (controlIsPressed(e)) {
          Lizzie.engineManager.switchEngine(e.getKeyCode() - VK_0);
          refreshType = 0;
        }
        break;
      default:
        shouldDisableAnalysis = false;
    }

    if (shouldDisableAnalysis && Lizzie.board.inAnalysisMode()) Lizzie.board.toggleAnalysis();

    Lizzie.frame.refresh(refreshType);
  }

  private boolean wasPonderingWhenControlsShown = false;

  @Override
  public void keyReleased(KeyEvent e) {
    switch (e.getKeyCode()) {
      case VK_X:
        if (wasPonderingWhenControlsShown) Lizzie.leelaz.togglePonder();
        Lizzie.frame.showControls = false;
        Lizzie.frame.refresh(1);
        break;

      case VK_Z:
        stopTemporaryBoard();
        Lizzie.frame.refresh(1);
        break;

      default:
    }
  }

  private long wheelWhen;

  @Override
  public void mouseWheelMoved(MouseWheelEvent e) {
    if (Lizzie.frame.processCommentMouseWheelMoved(e)) {
      return;
    }
    if (e.getWhen() - wheelWhen > 0) {
      wheelWhen = e.getWhen();
      if (Lizzie.board.inAnalysisMode()) Lizzie.board.toggleAnalysis();
      if (e.getWheelRotation() > 0) {
        if (Lizzie.frame.isMouseOver) {
          Lizzie.frame.doBranch(1);
        } else {
          redo();
        }
      } else if (e.getWheelRotation() < 0) {
        if (Lizzie.frame.isMouseOver) {
          Lizzie.frame.doBranch(-1);
        } else {
          undo();
        }
      }
      Lizzie.frame.refresh();
    }
  }
}
