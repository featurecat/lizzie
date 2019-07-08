package featurecat.lizzie.util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

  public static String toString(InputStream is) {
    String encoding = "UTF-8";
    try {
      byte[] buf = new byte[4096];
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      UniversalDetector detector = new UniversalDetector(null);

      int nread;
      while ((nread = is.read(buf)) > 0) {
        output.write(buf, 0, nread);
      }
      is.close();
      if (output.size() > 0) {
        byte[] data = output.toByteArray();
        detector.handleData(data, 0, data.length);
        detector.dataEnd();

        String detect = detector.getDetectedCharset();
        if (detect != null) {
          encoding = detect;
        }
        detector.reset();

        return new String(data, encoding);
      }

    } catch (IOException e) {
    }

    return "";
  }
}
