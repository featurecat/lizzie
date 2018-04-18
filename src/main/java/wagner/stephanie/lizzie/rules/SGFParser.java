 package wagner.stephanie.lizzie.rules;
 
 import wagner.stephanie.lizzie.Lizzie;
import wagner.stephanie.lizzie.analysis.GameInfo;
 
 import java.io.*;
import java.text.SimpleDateFormat;
 
 public class SGFParser {
    private static final SimpleDateFormat SGF_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

     public static boolean load(String filename) throws IOException {
         // Clear the board
         Lizzie.board.clear();

         if (value.charAt(value.length() - 2) == ')') {
             isMultiGo = true;
         }

        String whitePlayer = "";
        String blackPlayer = "";
 
         for (byte b : value.getBytes()) {
             // Check unicode charactors (UTF-8)

                     } else {
                         Lizzie.board.place(move[0], move[1], Stone.WHITE);
                     }
                } else if (tag.equals("PW")) {
                    whitePlayer = tagContent;
                } else if (tag.equals("PB")) {
                    blackPlayer = tagContent;
                 }
                 break;
             case ';':

             }
         }
 
        Lizzie.frame.setPlayers(whitePlayer, blackPlayer);

         // Rewind to game start
         while (Lizzie.board.previousMove());
 
         return true;
     }
 
    public static boolean save(Board board, String filename) throws IOException {
         FileOutputStream fp = new FileOutputStream(filename);
         OutputStreamWriter writer = new OutputStreamWriter(fp);
 
         try
         {
            // collect game info
            BoardHistoryList history = board.getHistory();
            GameInfo gameInfo = history.getGameInfo();
            String playerBlack = gameInfo.getPlayerBlack();
            String playerWhite = gameInfo.getPlayerWhite();
            Double komi = gameInfo.getKomi();
            Integer handicap = gameInfo.getHandicap();
            String date = SGF_DATE_FORMAT.format(gameInfo.getDate());

             // add SGF header
            StringBuilder builder = new StringBuilder("(;");
            if (handicap != 0) builder.append(String.format("HA[%s]", handicap));
            builder.append(String.format("KM[%s]PW[%s]PB[%s]DT[%s]AP[Lizzie: %s]",
                    komi, playerWhite, playerBlack, date, Lizzie.lizzieVersion));
//            StringBuilder builder = new StringBuilder(String.format("(;KM[7.5]AP[Lizzie: %s]", Lizzie.lizzieVersion));
 
             // move to the first move
            history.toStart();

            // add handicap stones to SGF
            if (handicap != 0)
            {
                builder.append("AB");
                Stone[] stones = history.getStones();
                for (int i = 0; i < stones.length; i++) {
                    Stone stone = stones[i];
                    if (stone.isBlack())
                    {
                        // i = x * Board.BOARD_SIZE + y;
                        int corY = i % Board.BOARD_SIZE;
                        int corX = (i - corY) / Board.BOARD_SIZE;

                        char x = (char)(corX + 'a');
                        char y = (char)(corY + 'a');
                        builder.append(String.format("[%c%c]", x, y));
                    }
                }
            }
//            BoardHistoryList history = Lizzie.board.getHistory();
//            while (history.previous() != null);
 
             // replay moves, and convert them to tags.
             // *  format: ";B[xy]" or ";W[xy]"
         }
         return true;
     }
}