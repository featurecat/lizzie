package featurecat.lizzie.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.mozilla.universalchardet.UniversalDetector;

public class EncodingDetector {

  public static String detect(String fileName) {
    String encoding = "UTF-8";
    try {
      encoding = detect(new FileInputStream(fileName));
    } catch (FileNotFoundException e) {
    }
    return encoding;
  }

  public static String detect(FileInputStream fis) {
    String encoding = "UTF-8";
    try {
      byte[] buf = new byte[4096];

      UniversalDetector detector = new UniversalDetector(null);

      int nread;
      while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
        detector.handleData(buf, 0, nread);
      }
      detector.dataEnd();

      String detect = detector.getDetectedCharset();
      if (detect != null) {
        encoding = detect;
      }

      detector.reset();

    } catch (IOException e) {
    }

    return encoding;
  }
}
