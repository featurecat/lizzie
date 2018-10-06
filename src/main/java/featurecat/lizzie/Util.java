package featurecat.lizzie;

import java.awt.FontMetrics;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class Util {
  /**
   * @param val the value we want to clamp
   * @param min the minimum value that will be returned
   * @param max the maximum value that will be returned
   * @return the closest number to val within the range [min, max]
   */
  public static <T extends Comparable<T>> T clamp(T val, T min, T max) {
    if (val.compareTo(min) < 0) return min;
    else if (val.compareTo(max) > 0) return max;
    else return val;
  }

  /** @return the sha 256 checksum of decompressed contents from a GZIPed file */
  public static String getSha256Sum(File file) {
    try (InputStream inputStream = new GZIPInputStream(new FileInputStream(file))) {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      DigestInputStream digestInputStream = new DigestInputStream(inputStream, messageDigest);

      // we have to read the file. additionally, using a buffer is efficient.
      // 8192 was tested as the best value among 4096, 8192, 16384, and 32768.
      byte[] buffer = new byte[8192];
      while (digestInputStream.read(buffer) != -1) ;

      MessageDigest digest = digestInputStream.getMessageDigest();
      digestInputStream.close();

      byte[] sha256 = digest.digest();
      StringBuilder sb = new StringBuilder();
      for (byte b : sha256) {
        sb.append(String.format("%02X", b));
      }
      return sb.toString().toLowerCase();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (EOFException e) {
      // do nothing, just means we need to download a new one
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  /** @return the url's contents, downloaded as a string */
  public static String downloadAsString(URL url) {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
      return br.lines().collect(Collectors.joining("\n"));
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  /** Downloads the contents of the url, and saves them in a file. */
  public static void saveAsFile(URL url, File file) {
    try {
      ReadableByteChannel rbc = Channels.newChannel(url.openStream());
      FileOutputStream fos = new FileOutputStream(file);
      fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Truncate text that is too long for the given width
   *
   * @param line
   * @param fm
   * @param fitWidth
   * @return fitted
   */
  public static String truncateStringByWidth(String line, FontMetrics fm, int fitWidth) {
    if (line == null || line.length() == 0) {
      return "";
    }
    int width = fm.stringWidth(line);
    if (width > fitWidth) {
      int guess = line.length() * fitWidth / width;
      String before = line.substring(0, guess).trim();
      width = fm.stringWidth(before);
      if (width > fitWidth) {
        int diff = width - fitWidth;
        int i = 0;
        for (; (diff > 0 && i < 5); i++) {
          diff = diff - fm.stringWidth(line.substring(guess - i - 1, guess - i));
        }
        return line.substring(0, guess - i).trim();
      } else {
        return before;
      }
    } else {
      return line;
    }
  }
}
