import java.util.*;
import java.io.*;
import java.text.*;
import java.util.logging.*;

public class Logging {
    private Logger logger;
    private final String peerIndex;
    private SimpleDateFormat dateTime = null;
    private FileHandler fileHandler;
    public Logging(String peerIndex) {
        this.peerIndex = peerIndex;
        try {
            var logFname = "log_peer_" + this.peerIndex + ".log";
            this.fileHandler = new FileHandler(logFname, false);
            this.dateTime = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a");
            System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s %n");
            this.fileHandler.setFormatter(new SimpleFormatter());

            this.logger = Logger.getLogger("PeerLogs");
            this.logger.setUseParentHandlers(false);
            this.logger.addHandler(this.fileHandler);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void makeLogForPreferredNeighbors(List<String> stringList) {
        var calendar = Calendar.getInstance();
        var peerNeighborList = new StringBuilder();
        for (var s : stringList) {
            peerNeighborList.append(s).append(",");
        }
        peerNeighborList = new StringBuilder(peerNeighborList.substring(0, peerNeighborList.length() - 1));
        this.logger.log(Level.INFO,
                "[" + this.dateTime.format(calendar.getTime()) + "]: Peer [" + this.peerIndex + "] has the preferred neighbors [" + peerNeighborList + "]");
    }

    public synchronized void makeLogForOptUnchokedNeighbor(String pID) {
        var calendar = Calendar.getInstance();
        this.logger.log(Level.INFO, "[" + this.dateTime.format(calendar.getTime()) + "]: Peer [" + this.peerIndex + "] has the optimistically unchoked neighbor [" + pID + "]");
    }

    public synchronized void makeLogForHaveMessage(String pID, int i) {
        var calendar = Calendar.getInstance();
        this.logger.log(Level.INFO, "[" + this.dateTime.format(calendar.getTime()) + "]: Peer [" + this.peerIndex + "] received the ‘have’ message from [" + pID + "] for the piece [" + i + "]");
    }

    public synchronized void makeLogForInterestedMessage(String pID) {
        var calendar = Calendar.getInstance();
        this.logger.log(Level.INFO, "[" + this.dateTime.format(calendar.getTime()) + "]: Peer [" + this.peerIndex + "] received the ‘interested’ message from [" + pID + "]");
    }

    public synchronized void makeLogForUnchokedNeighbors(String pID) {
        var calendar = Calendar.getInstance();
        this.logger.log(Level.INFO, "[" + this.dateTime.format(calendar.getTime()) + "]: Peer [" + this.peerIndex + "] is unchoked by [" + pID + "]");
    }

    public synchronized void makeLogForChokedNeighbors(String pID) {
        var calendar = Calendar.getInstance();
        this.logger.log(Level.INFO, "[" + this.dateTime.format(calendar.getTime()) + "]: Peer [" + this.peerIndex + "] is choked by [" + pID + "]");
    }

    public synchronized void makeLogForTCPConnection(String pID) {
        var calendar = Calendar.getInstance();
        this.logger.log(Level.INFO,
                "[" + this.dateTime.format(calendar.getTime()) + "]: Peer [" + this.peerIndex + "] makes a connection to Peer " + "[" + pID + "]");
    }
    public synchronized void makeLogForDownloadedPiece(String pID, int i, int chunks) {
        var calendar = Calendar.getInstance();
        this.logger.log(Level.INFO, "[" + this.dateTime.format(calendar.getTime()) + "]: Peer [" + this.peerIndex + "] has downloaded the piece [" + i + "] from [" + pID + "]. Now the number of pieces it has is [" + chunks + "]");
    }

    public synchronized void makeLogForDownloadFinish() {
        var calendar = Calendar.getInstance();
        this.logger.log(Level.INFO, "[" + this.dateTime.format(calendar.getTime()) + "]: Peer [" + this.peerIndex + "] has downloaded the complete file");
    }

    public synchronized void makeLogForTCPConnectionServer(String pID) {
        var calendar = Calendar.getInstance();
        this.logger.log(Level.INFO,
                "[" + this.dateTime.format(calendar.getTime()) + "]: Peer [" + this.peerIndex + "] is connected from Peer " + "[" + pID + "]");
    }

    public synchronized void makeLogForNotInterestedMessage(String pID) {
        var calendar = Calendar.getInstance();
        this.logger.log(Level.INFO, "[" + this.dateTime.format(calendar.getTime()) + "]: Peer [" + this.peerIndex + "] received the ‘not interested’ message from [" + pID + "]");
    }

    public void terminateLogging() {
        try {
            if (this.fileHandler != null) {
                this.fileHandler.close();
            }
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}