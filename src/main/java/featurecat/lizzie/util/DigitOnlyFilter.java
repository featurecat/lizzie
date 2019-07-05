package featurecat.lizzie.util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class DigitOnlyFilter extends DocumentFilter {
  private String pattern = "\\D++";

  public DigitOnlyFilter() {}

  public DigitOnlyFilter(String pattern) {
    this.pattern = pattern;
  }

  @Override
  public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
      throws BadLocationException {
    String newStr = string != null ? string.replaceAll(pattern, "") : "";
    if (!newStr.isEmpty()) {
      fb.insertString(offset, newStr, attr);
    }
  }

  @Override
  public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
      throws BadLocationException {
    String newStr = text != null ? text.replaceAll(pattern, "") : "";
    if (!newStr.isEmpty()) {
      fb.replace(offset, length, newStr, attrs);
    }
  }
}
