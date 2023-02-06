import java.io.*;
import java.util.*;

public class CommonConfigFileParser {
    public int PreferredNumberOfUsers;
    public int Interval;
    public int OptInterval;
    public String FName;
    public int FSize;
    public int ChunkSize;

    public void parseLoadCommonConfig() {
        try {
            var fileReader = new FileReader("Common.cfg");
            var scanner = new Scanner(fileReader);
            while (scanner.hasNextLine()) {
                var nxtLine = scanner.nextLine();
                var piecesArray = nxtLine.split(" ");
                switch (piecesArray[0]) {
                    case "NumberOfPreferredNeighbors" : this.PreferredNumberOfUsers = Integer.parseInt(piecesArray[1]); break;
                    case "PieceSize" : this.ChunkSize = Integer.parseInt(piecesArray[1]); break;
                    case "UnchokingInterval" : this.Interval = Integer.parseInt(piecesArray[1]); break;
                    case "FileName" : this.FName = piecesArray[1]; break;
                    case "FileSize" : this.FSize = Integer.parseInt(piecesArray[1]); break;
                    case "OptimisticUnchokingInterval" : this.OptInterval = Integer.parseInt(piecesArray[1]); break;
                }
            }
            scanner.close();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
    }
}
