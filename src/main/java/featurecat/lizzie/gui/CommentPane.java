package featurecat.lizzie.gui;

import static java.lang.Math.min;

import featurecat.lizzie.Lizzie;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

/** The window used to display the game. */
public class CommentPane extends LizziePane {

  //  private final BufferStrategy bs;

  // Display Comment
  private HTMLDocument htmlDoc;
  private HTMLEditorKit htmlKit;
  private StyleSheet htmlStyle;
  public JScrollPane scrollPane;
  private JTextPane commentPane;
  private JLabel dragPane = new JLabel("Drag out");
  private MouseMotionListener[] mouseMotionListeners;
  private MouseMotionAdapter mouseMotionAdapter;

  /** Creates a window */
  public CommentPane(LizzieMain owner) {
    super(owner);
    setLayout(new BorderLayout(0, 0));

    htmlKit = new HTMLEditorKit();
    htmlDoc = (HTMLDocument) htmlKit.createDefaultDocument();
    htmlStyle = htmlKit.getStyleSheet();
    String style =
        ".comment {background:#"
            + String.format(
                "%02x%02x%02x",
                Lizzie.config.commentBackgroundColor.getRed(),
                Lizzie.config.commentBackgroundColor.getGreen(),
                Lizzie.config.commentBackgroundColor.getBlue())
            + "; color:#"
            + String.format(
                "%02x%02x%02x",
                Lizzie.config.commentFontColor.getRed(),
                Lizzie.config.commentFontColor.getGreen(),
                Lizzie.config.commentFontColor.getBlue())
            + ";}";
    htmlStyle.addRule(style);

    commentPane = new JTextPane();
    commentPane.setBorder(BorderFactory.createEmptyBorder());
    commentPane.setEditorKit(htmlKit);
    commentPane.setDocument(htmlDoc);
    commentPane.setText("");
    commentPane.setEditable(false);
    commentPane.setFocusable(false);
    scrollPane = new JScrollPane();
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setVerticalScrollBarPolicy(
        javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    add(scrollPane);
    scrollPane.setViewportView(commentPane);
    setVisible(false);

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
        addText("<span class=\"comment\">" + comment + "</span>");
      }
    }
  }

  private void addText(String text) {
    try {
      htmlDoc.remove(0, htmlDoc.getLength());
      htmlKit.insertHTML(htmlDoc, htmlDoc.getLength(), text, 0, 0, null);
      commentPane.setCaretPosition(htmlDoc.getLength());
    } catch (BadLocationException | IOException e) {
      e.printStackTrace();
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
      this.commentPane.setVisible(false);
      this.scrollPane.setVisible(false);
      dragPane.setVisible(true);
    } else {
      remove(dragPane);
      add(scrollPane);
      this.commentPane.setVisible(true);
      this.scrollPane.setVisible(true);
      dragPane.setVisible(false);
    }
    super.setDesignMode(mode);
    repaint();
  }

  public void setCommentBounds(int x, int y, int width, int height) {
    this.setBounds(x, y, width, height);
    if (scrollPane != null) {
      scrollPane.setSize(width, height);
    }
  }
}
