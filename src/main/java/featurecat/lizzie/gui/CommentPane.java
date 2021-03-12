package featurecat.lizzie.gui;

import static java.lang.Math.min;

import featurecat.lizzie.Lizzie;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** The window used to display the game. */
public class CommentPane extends LizziePane {

  //  private final BufferStrategy bs;

  // Display Comment
  public JScrollPane scrollPane;
  private JTextArea commentPane;
  private JLabel dragPane = new JLabel("Drag out");
  private MouseMotionListener[] mouseMotionListeners;
  private MouseMotionAdapter mouseMotionAdapter;

  /** Creates a window */
  public CommentPane(LizzieMain owner) {
    super(owner);
    setLayout(new BorderLayout(0, 0));

    commentPane = new JTextArea();
    commentPane.setBorder(BorderFactory.createEmptyBorder());
    commentPane.setText("");
    commentPane.setEditable(false);
    commentPane.setFocusable(false);
    commentPane.setWrapStyleWord(true);
    commentPane.setLineWrap(true);
    // drop alpha for backward compatibility with Lizze 0.7.4
    commentPane.setBackground(new Color(Lizzie.config.commentBackgroundColor.getRGB()));
    commentPane.setForeground(new Color(Lizzie.config.commentFontColor.getRGB()));
    commentPane.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            Lizzie.frame.getFocus();
            Lizzie.frame.checkRightClick(e);
          }
        });
    scrollPane = new JScrollPane();
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setVerticalScrollBarPolicy(
        javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    add(scrollPane);
    scrollPane.setViewportView(commentPane);
    setVisible(false);

    commentPane.addMouseMotionListener(
        new MouseMotionListener() {
          @Override
          public void mouseMoved(MouseEvent e) {
            if (Lizzie.config.showSubBoard) {
              Lizzie.frame.clearIsMouseOverSub();
            }
          }

          @Override
          public void mouseDragged(MouseEvent e) {
            // TODO Auto-generated method stub
          }
        });
    //    mouseMotionAdapter = new MouseMotionAdapter() {
    //      @Override
    //      public void mouseDragged(MouseEvent e) {
    //        System.out.println("Mouse Dragged");
    //        owner.dispatchEvent(e);
    //      }
    //    };
    //    commentPane.addMouseMotionListener(mouseMotionAdapter);
  }

  /** Draw the Comment of the Sgf file */
  public void drawComment() {
    if (Lizzie.leelaz != null && Lizzie.leelaz.isLoaded()) {
      if (Lizzie.config.showComment) {
        setVisible(true);
        String comment = Lizzie.board.getHistory().getData().comment;
        int fontSize = (int) (min(getWidth(), getHeight()) * 0.0294);
        if (Lizzie.config.commentFontSize > 0) {
          fontSize = Lizzie.config.commentFontSize;
        } else if (fontSize < 16) {
          fontSize = 16;
        }
        Font font = new Font(Lizzie.config.fontName, Font.PLAIN, fontSize);
        commentPane.setFont(font);
        commentPane.setText(comment);
        commentPane.setCaretPosition(0);
      }
    }
  }

  public void setDesignMode(boolean mode) {
    //    if (mode) {
    //      mouseMotionListeners = commentPane.getMouseMotionListeners();
    //      if (mouseMotionListeners != null) {
    //        for (MouseMotionListener l : mouseMotionListeners) {
    //          commentPane.removeMouseMotionListener(l);
    //        }
    //      }
    //    } else {
    //      if (mouseMotionListeners != null) {
    //        for (MouseMotionListener l : mouseMotionListeners) {
    //          commentPane.addMouseMotionListener(l);
    //        }
    //      }
    //    }
    if (mode) {
      remove(scrollPane);
      add(dragPane);
      commentPane.setVisible(false);
      scrollPane.setVisible(false);
      dragPane.setVisible(true);
    } else {
      remove(dragPane);
      add(scrollPane);
      commentPane.setVisible(true);
      scrollPane.setVisible(true);
      dragPane.setVisible(false);
    }
    super.setDesignMode(mode);
    revalidate();
    repaint();
  }

  public void setCommentBounds(int x, int y, int width, int height) {
    this.setBounds(x, y, width, height);
    if (scrollPane != null) {
      scrollPane.setSize(width, height);
    }
  }
}
