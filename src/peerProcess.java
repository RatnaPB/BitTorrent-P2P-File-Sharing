/*      */ import java.io.BufferedInputStream;
/*      */ import java.io.BufferedOutputStream;
/*      */ import java.io.BufferedReader;
/*      */ import java.io.BufferedWriter;
/*      */ import java.io.DataInputStream;
/*      */ import java.io.DataOutputStream;
/*      */ import java.io.File;
/*      */ import java.io.FileInputStream;
/*      */ import java.io.FileOutputStream;
/*      */ import java.io.FileReader;
/*      */ import java.io.FileWriter;
/*      */ import java.io.IOException;
/*      */ import java.io.PrintStream;
/*      */ import java.net.ServerSocket;
/*      */ import java.net.Socket;
/*      */ import java.nio.ByteBuffer;
/*      */ import java.util.ArrayList;
/*      */ import java.util.Arrays;
/*      */ import java.util.Iterator;
/*      */ import java.util.LinkedHashMap;
/*      */ import java.util.Random;
/*      */ import java.util.Set;
/*      */ import java.util.concurrent.ConcurrentHashMap;
/*      */ import java.util.concurrent.ConcurrentHashMap.KeySetView;
/*      */ import java.util.stream.Stream;
/*      */ 
/*      */ public class peerProcess
/*      */ {
/*      */   private static final char CHOKE = '0';
/*      */   private static final char UNCHOKE = '1';
/*      */   private static final char INTERESTED = '2';
/*      */   private static final char NOT_INTERESTED = '3';
/*      */   private static final char HAVE = '4';
/*      */   private static final char BITFIELD = '5';
/*      */   private static final char REQUEST = '6';
/*      */   private static final char PIECE = '7';
/*      */   private static int hostID;
/*      */   private static LinkedHashMap<Integer, PeerInfo> peers;
/*      */   private static byte[][] filePieces;
/*  533 */   private static Messages msg = new Messages();
/*      */   private static File log_file;
/*      */   private static Logs logs;
/*      */   private static ConcurrentHashMap<Integer, peerProcess.PeerConnection> peerConnections;
/*      */   private static PeerInfo thisPeer;
/*      */   private static CommonInfo common;
/*  539 */   private static int completedPeers = 0;
/*      */   private static File directory;
/*      */ 
/*      */   public static void main(String[] args)
/*      */   {
/*  543 */     hostID = Integer.parseInt(args[0]);
/*      */     try
/*      */     {
/*  548 */       directory = new File("peer_" + hostID);
/*  549 */       if (!directory.exists()) {
/*  550 */         directory.mkdir();
/*      */       }
/*  552 */       log_file = new File(System.getProperty("user.dir") + "/log_peer_" + hostID + ".log");
/*  553 */       if (!log_file.exists())
/*  554 */         log_file.createNewFile();
/*  555 */       BufferedWriter writer = new BufferedWriter(new FileWriter(log_file.getAbsolutePath(), true));
/*  556 */       writer.flush();
/*  557 */       logs = new Logs(writer);
/*      */ 
/*  565 */       BufferedReader peerInfo = new BufferedReader(new FileReader("PeerInfo.cfg"));
/*  566 */       peers = new LinkedHashMap();
/*  567 */       for (Object line : peerInfo.lines().toArray()) {
/*  568 */         String[] parts = ((String)line).split(" ");
/*  569 */         PeerInfo peer = new PeerInfo();
/*  570 */         peer.setPeerID(Integer.parseInt(parts[0]));
/*  571 */         peer.setHostName(parts[1]);
/*  572 */         peer.setPortNumber(Integer.parseInt(parts[2]));
/*  573 */         peer.setHaveFile(Integer.parseInt(parts[3]));
/*  574 */         peers.put(Integer.valueOf(peer.getPeerID()), peer);
/*      */       }
/*  576 */       peerInfo.close();
/*      */ 
/*  586 */       BufferedReader commonInfo = new BufferedReader(new FileReader("Common.cfg"));
/*  587 */       common = new CommonInfo();
/*  588 */       Object[] commonInfoLines = commonInfo.lines().toArray();
/*  589 */       common.setNumberOfPreferredNeighbors(Integer.parseInt(((String)commonInfoLines[0]).split(" ")[1]));
/*  590 */       common.setUnchokingInterval(Integer.parseInt(((String)commonInfoLines[1]).split(" ")[1]));
/*  591 */       common.setOptimisticUnchokingInterval(Integer.parseInt(((String)commonInfoLines[2]).split(" ")[1]));
/*  592 */       common.setFileName(((String)commonInfoLines[3]).split(" ")[1]);
/*  593 */       common.setFileSize(Integer.parseInt(((String)commonInfoLines[4]).split(" ")[1]));
/*  594 */       common.setPieceSize(Integer.parseInt(((String)commonInfoLines[5]).split(" ")[1]));
/*  595 */       commonInfo.close();
/*      */ 
/*  606 */       thisPeer = (PeerInfo)peers.get(Integer.valueOf(hostID));
/*  607 */       int fileSize = common.getFileSize();
/*  608 */       int pieceSize = common.getPieceSize();
/*  609 */       int numOfPieces = (int)Math.ceil(fileSize / pieceSize);
/*  610 */       filePieces = new byte[numOfPieces][];
/*  611 */       int bitfieldSize = numOfPieces;
/*  612 */       int[] bitfield = new int[bitfieldSize];
/*  613 */       if (thisPeer.getHaveFile() == 1) {
/*  614 */         completedPeers += 1;
/*  615 */         Arrays.fill(bitfield, 1);
/*  616 */         thisPeer.setBitfield(bitfield);
/*      */ 
/*  618 */         BufferedInputStream file = new BufferedInputStream(new FileInputStream(directory.getAbsolutePath() + "/" + common.getFileName()));
/*  619 */         byte[] fileBytes = new byte[fileSize];
/*  620 */         file.read(fileBytes);
/*  621 */         file.close();
/*  622 */         int part = 0;
/*      */ 
/*  624 */         for (int counter = 0; counter < fileSize; counter += pieceSize)
/*      */         {
/*  626 */           if (counter + pieceSize <= fileSize)
/*  627 */             filePieces[part] = Arrays.copyOfRange(fileBytes, counter, counter + pieceSize);
/*      */           else {
/*  629 */             filePieces[part] = Arrays.copyOfRange(fileBytes, counter, fileSize);
/*      */           }
/*      */ 
/*  633 */           part++;
/*  634 */           thisPeer.updateNumOfPieces();
/*      */         }
/*      */       } else {
/*  637 */         Arrays.fill(bitfield, 0);
/*  638 */         thisPeer.setBitfield(bitfield);
/*      */       }
/*      */ 
/*  649 */       peerConnections = new ConcurrentHashMap();
/*      */ 
/*  651 */       peerProcess.SendConnections sendConnections = new peerProcess.SendConnections(null);
/*  652 */       sendConnections.start();
/*  653 */       peerProcess.ReceiveConnections receiveConnections = new peerProcess.ReceiveConnections(null);
/*  654 */       receiveConnections.start();
/*  655 */       peerProcess.UnchokePeers unchokePeers = new peerProcess.UnchokePeers(null);
/*  656 */       unchokePeers.start();
/*  657 */       peerProcess.OptimisticUnchokePeer optimisticUnchokePeer = new peerProcess.OptimisticUnchokePeer(null);
/*  658 */       optimisticUnchokePeer.start();
/*      */     } catch (Exception e) {
/*  660 */       e.printStackTrace();
/*      */     }
/*      */   }
/*      */ 
/*      */   private static class PeerConnection
/*      */   {
/*      */     private Socket connection;
/*      */     private int peerID;
/*  867 */     private boolean interested = false;
/*  868 */     private boolean choked = true;
/*  869 */     private boolean optimisticallyUnchoked = false;
/*  870 */     private double downloadRate = 0.0D;
/*      */ 
/*      */     public double getDownloadRate() {
/*  873 */       return this.downloadRate;
/*      */     }
/*      */ 
/*      */     public void setDownloadRate(double rate) {
/*  877 */       this.downloadRate = rate;
/*      */     }
/*      */ 
/*      */     public boolean isOptimisticallyUnchoked() {
/*  881 */       return this.optimisticallyUnchoked;
/*      */     }
/*      */ 
/*      */     public void optimisticallyUnchoke() {
/*  885 */       this.optimisticallyUnchoked = true;
/*      */     }
/*      */     public void optimisticallyChoke() {
/*  888 */       this.optimisticallyUnchoked = false;
/*      */     }
/*      */ 
/*      */     public boolean isInterested() {
/*  892 */       return this.interested;
/*      */     }
/*      */ 
/*      */     public void setInterested() {
/*  896 */       this.interested = true;
/*      */     }
/*      */     public void setNotInterested() {
/*  899 */       this.interested = false;
/*      */     }
/*      */ 
/*      */     public boolean isChoked() {
/*  903 */       return this.choked;
/*      */     }
/*      */ 
/*      */     public void choke() {
/*  907 */       this.choked = true;
/*      */     }
/*      */     public void unchoke() {
/*  910 */       this.choked = false;
/*      */     }
/*      */ 
/*      */     public int getPeerID() {
/*  914 */       return this.peerID;
/*      */     }
/*      */ 
/*      */     public Socket getConnection() {
/*  918 */       return this.connection;
/*      */     }
/*      */ 
/*      */     public PeerConnection(Socket conn, int id) {
/*  922 */       this.connection = conn;
/*  923 */       this.peerID = id;
/*  924 */       new peerProcess.PeerConnection.ReaderThread(this).start();
/*      */     }
/*      */ 
/*      */     public void sendMessage(char type) {
/*      */       try {
/*  929 */         DataOutputStream dataOutputStream = new DataOutputStream(this.connection.getOutputStream());
/*  930 */         dataOutputStream.flush();
/*  931 */         switch (type) {
/*      */         case '0':
/*  933 */           dataOutputStream.write(peerProcess.msg.getChokeMessage());
/*  934 */           break;
/*      */         case '1':
/*  936 */           dataOutputStream.write(peerProcess.msg.getUnchokeMessage());
/*  937 */           break;
/*      */         case '2':
/*  939 */           dataOutputStream.write(peerProcess.msg.getInterestedMessage());
/*  940 */           break;
/*      */         case '3':
/*  942 */           dataOutputStream.write(peerProcess.msg.getNotInterestedMessage());
/*  943 */           break;
/*      */         case '5':
/*  945 */           dataOutputStream.write(peerProcess.msg.getBitfieldMessage(peerProcess.thisPeer.getBitfield()));
/*  946 */           break;
/*      */         case '4':
/*      */         }
/*      */ 
/*  950 */         dataOutputStream.flush();
/*      */       }
/*      */       catch (Exception e) {
/*  953 */         e.printStackTrace();
/*      */       }
/*      */     }
/*      */ 
/*      */     public void sendMessage(char type, int index) {
/*      */       try {
/*  959 */         DataOutputStream dataOutputStream = new DataOutputStream(this.connection.getOutputStream());
/*  960 */         dataOutputStream.flush();
/*  961 */         switch (type) {
/*      */         case '4':
/*  963 */           dataOutputStream.write(peerProcess.msg.getHaveMessage(index));
/*  964 */           break;
/*      */         case '6':
/*  966 */           dataOutputStream.write(peerProcess.msg.getRequestMessage(index));
/*  967 */           break;
/*      */         case '7':
/*  969 */           dataOutputStream.write(peerProcess.msg.getPieceMessage(index, peerProcess.filePieces[index]));
/*  970 */           break;
/*      */         case '5':
/*      */         }
/*      */ 
/*  974 */         dataOutputStream.flush();
/*      */       }
/*      */       catch (Exception e) {
/*  977 */         e.printStackTrace();
/*      */       }
/*      */     }
/*      */ 
/*      */     public void compareBitfield(int[] thisPeerBitfield, int[] connectedPeerBitfield, int len)
/*      */     {
/*  983 */       for (int i = 0; i < len; i++) {
/*  984 */         if ((thisPeerBitfield[i] == 0) && (connectedPeerBitfield[i] == 1)) {
/*  985 */           sendMessage('2');
/*  986 */           break;
/*      */         }
/*      */       }
/*  989 */       if (i == len)
/*  990 */         sendMessage('3');
/*      */     }
/*      */ 
/*      */     public void getPieceIndex(int[] thisPeerBitfield, int[] connectedPeerBitfield, int len) {
/*  994 */       ArrayList indices = new ArrayList();
/*      */ 
/*  996 */       for (int i = 0; i < len; i++) {
/*  997 */         if ((thisPeerBitfield[i] == 0) && (connectedPeerBitfield[i] == 1)) {
/*  998 */           indices.add(Integer.valueOf(i));
/*      */         }
/*      */       }
/* 1001 */       Random r = new Random();
/* 1002 */       if (indices.size() > 0) {
/* 1003 */         int index = ((Integer)indices.get(Math.abs(r.nextInt() % indices.size()))).intValue();
/* 1004 */         sendMessage('6', index);
/*      */       }
/*      */     }
/*      */ 
/*      */     public void checkCompleted() {
/* 1009 */       int counter = 0;
/*      */       int bit;
/* 1010 */       for (bit : peerProcess.thisPeer.getBitfield()) {
/* 1011 */         if (bit == 1)
/* 1012 */           counter++;
/*      */       }
/* 1014 */       if (counter == peerProcess.thisPeer.getBitfield().length) {
/* 1015 */         peerProcess.logs.downloadCompleted(peerProcess.thisPeer.getPeerID());
/* 1016 */         counter = 0;
/* 1017 */         byte[] merge = new byte[peerProcess.common.getFileSize()];
/* 1018 */         for (byte[] piece : peerProcess.filePieces)
/* 1019 */           for (byte b : piece) {
/* 1020 */             merge[counter] = b;
/* 1021 */             counter++;
/*      */           }
/*      */         try
/*      */         {
/* 1025 */           FileOutputStream file = new FileOutputStream(peerProcess.directory.getAbsolutePath() + "/" + peerProcess.common.getFileName());
/* 1026 */           BufferedOutputStream bos = new BufferedOutputStream(file);
/* 1027 */           bos.write(merge);
/* 1028 */           bos.close();
/* 1029 */           file.close();
/* 1030 */           System.out.println("File Download Completed.");
/* 1031 */           peerProcess.thisPeer.setHaveFile(1);
/* 1032 */           peerProcess.access$1008();
/*      */         } catch (IOException e) {
/* 1034 */           e.printStackTrace();
/*      */         }
/*      */       }
/*      */     }
/*      */ 
/*      */     private static class ReaderThread extends Thread {
/*      */       private peerProcess.PeerConnection peer;
/*      */ 
/*      */       public ReaderThread(peerProcess.PeerConnection peer) {
/* 1043 */         this.peer = peer;
/*      */       }
/*      */ 
/*      */       public void run()
/*      */       {
/* 1050 */         synchronized (this)
/*      */         {
/*      */           try {
/* 1053 */             DataInputStream dataInputStream = new DataInputStream(this.peer.getConnection().getInputStream());
/* 1054 */             this.peer.sendMessage('5');
/* 1055 */             while (peerProcess.completedPeers < peerProcess.peers.size()) {
/* 1056 */               int msgLength = dataInputStream.readInt();
/* 1057 */               byte[] buffer = new byte[msgLength];
/* 1058 */               double startTime = System.nanoTime() / 100000000.0D;
/* 1059 */               dataInputStream.readFully(buffer);
/* 1060 */               double endTime = System.nanoTime() / 100000000.0D;
/* 1061 */               char msgType = (char)buffer[0];
/* 1062 */               byte[] msg = new byte[msgLength - 1];
/* 1063 */               int counter = 0;
/* 1064 */               for (int i = 1; i < msgLength; i++) {
/* 1065 */                 msg[counter] = buffer[i];
/* 1066 */                 counter++;
/*      */               }
/*      */               int x;
/* 1070 */               switch (msgType) {
/*      */               case '0':
/* 1072 */                 peerProcess.logs.choked(peerProcess.thisPeer.getPeerID(), this.peer.peerID);
/* 1073 */                 this.peer.choke();
/* 1074 */                 break;
/*      */               case '1':
/* 1076 */                 this.peer.unchoke();
/* 1077 */                 peerProcess.logs.unchoked(peerProcess.thisPeer.getPeerID(), this.peer.peerID);
/* 1078 */                 this.peer.getPieceIndex(peerProcess.thisPeer.getBitfield(), ((PeerInfo)peerProcess.peers.get(Integer.valueOf(this.peer.peerID))).getBitfield(), peerProcess.thisPeer.getBitfield().length);
/* 1079 */                 break;
/*      */               case '2':
/* 1081 */                 peerProcess.logs.receiveInterested(peerProcess.thisPeer.getPeerID(), this.peer.peerID);
/* 1082 */                 this.peer.setInterested();
/* 1083 */                 break;
/*      */               case '3':
/* 1085 */                 peerProcess.logs.receiveNotInterested(peerProcess.thisPeer.getPeerID(), this.peer.peerID);
/* 1086 */                 this.peer.setNotInterested();
/* 1087 */                 if (!this.peer.isChoked()) {
/* 1088 */                   this.peer.choke();
/* 1089 */                   this.peer.sendMessage('0'); } break;
/*      */               case '4':
/* 1093 */                 int index = ByteBuffer.wrap(msg).getInt();
/* 1094 */                 ((PeerInfo)peerProcess.peers.get(Integer.valueOf(this.peer.getPeerID()))).updateBitfield(index);
/* 1095 */                 int bits = 0;
/* 1096 */                 for (x : ((PeerInfo)peerProcess.peers.get(Integer.valueOf(this.peer.getPeerID()))).getBitfield()) {
/* 1097 */                   if (x == 1)
/* 1098 */                     bits++;
/*      */                 }
/* 1100 */                 if (bits == peerProcess.thisPeer.getBitfield().length) {
/* 1101 */                   ((PeerInfo)peerProcess.peers.get(Integer.valueOf(this.peer.getPeerID()))).setHaveFile(1);
/* 1102 */                   peerProcess.access$1008();
/*      */                 }
/* 1104 */                 this.peer.compareBitfield(peerProcess.thisPeer.getBitfield(), ((PeerInfo)peerProcess.peers.get(Integer.valueOf(this.peer.getPeerID()))).getBitfield(), peerProcess.thisPeer.getBitfield().length);
/* 1105 */                 peerProcess.logs.receiveHave(peerProcess.thisPeer.getPeerID(), this.peer.getPeerID(), index);
/* 1106 */                 break;
/*      */               case '5':
/* 1108 */                 int[] bitfield = new int[msg.length / 4];
/* 1109 */                 counter = 0;
/* 1110 */                 for (int i = 0; i < msg.length; i += 4) {
/* 1111 */                   bitfield[counter] = ByteBuffer.wrap(Arrays.copyOfRange(msg, i, i + 4)).getInt();
/* 1112 */                   counter++;
/*      */                 }
/* 1114 */                 ((PeerInfo)peerProcess.peers.get(Integer.valueOf(this.peer.peerID))).setBitfield(bitfield);
/* 1115 */                 int bits = 0;
/* 1116 */                 for (int x : ((PeerInfo)peerProcess.peers.get(Integer.valueOf(this.peer.getPeerID()))).getBitfield()) {
/* 1117 */                   if (x == 1)
/* 1118 */                     bits++;
/*      */                 }
/* 1120 */                 if (bits == peerProcess.thisPeer.getBitfield().length) {
/* 1121 */                   ((PeerInfo)peerProcess.peers.get(Integer.valueOf(this.peer.getPeerID()))).setHaveFile(1);
/* 1122 */                   peerProcess.access$1008();
/*      */                 }
/*      */                 else {
/* 1125 */                   ((PeerInfo)peerProcess.peers.get(Integer.valueOf(this.peer.getPeerID()))).setHaveFile(0);
/*      */                 }
/* 1127 */                 this.peer.compareBitfield(peerProcess.thisPeer.getBitfield(), bitfield, bitfield.length);
/* 1128 */                 break;
/*      */               case '6':
/* 1130 */                 this.peer.sendMessage('7', ByteBuffer.wrap(msg).getInt());
/* 1131 */                 break;
/*      */               case '7':
/* 1133 */                 int index = ByteBuffer.wrap(Arrays.copyOfRange(msg, 0, 4)).getInt();
/* 1134 */                 counter = 0;
/* 1135 */                 peerProcess.filePieces[index] = new byte[msg.length - 4];
/* 1136 */                 for (int i = 4; i < msg.length; i++) {
/* 1137 */                   peerProcess.filePieces[index][counter] = msg[i];
/* 1138 */                   counter++;
/*      */                 }
/* 1140 */                 peerProcess.thisPeer.updateBitfield(index);
/* 1141 */                 peerProcess.thisPeer.updateNumOfPieces();
/* 1142 */                 if (!this.peer.isChoked()) {
/* 1143 */                   this.peer.getPieceIndex(peerProcess.thisPeer.getBitfield(), ((PeerInfo)peerProcess.peers.get(Integer.valueOf(this.peer.peerID))).getBitfield(), peerProcess.thisPeer.getBitfield().length);
/*      */                 }
/* 1145 */                 double rate = (msg.length + 5) / (endTime - startTime);
/* 1146 */                 if (((PeerInfo)peerProcess.peers.get(Integer.valueOf(this.peer.getPeerID()))).getHaveFile() == 1) {
/* 1147 */                   this.peer.setDownloadRate(-1.0D);
/*      */                 }
/*      */                 else {
/* 1150 */                   this.peer.setDownloadRate(rate);
/*      */                 }
/* 1152 */                 peerProcess.logs.downloadingPiece(peerProcess.thisPeer.getPeerID(), this.peer.getPeerID(), index, peerProcess.thisPeer.getNumOfPieces());
/* 1153 */                 int downloaded = peerProcess.thisPeer.getNumOfPieces() * 100 / (int)Math.ceil(peerProcess.common.getFileSize() / peerProcess.common.getPieceSize());
/* 1154 */                 StringBuffer sb = new StringBuffer();
/* 1155 */                 sb.append("\r").append("Downloaded: ");
/* 1156 */                 sb.append(downloaded).append("%").append(" Number of Pieces: ").append(peerProcess.thisPeer.getNumOfPieces());
/* 1157 */                 System.out.print(sb);
/* 1158 */                 this.peer.checkCompleted();
/* 1159 */                 for (Iterator localIterator = peerProcess.peerConnections.keySet().iterator(); localIterator.hasNext(); ) { int connection = ((Integer)localIterator.next()).intValue();
/* 1160 */                   ((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(connection))).sendMessage('4', index);
/*      */                 }
/*      */ 
/*      */               }
/*      */ 
/*      */             }
/*      */ 
/* 1167 */             Thread.sleep(5000L);
/* 1168 */             System.exit(0);
/*      */           }
/*      */           catch (Exception e) {
/* 1171 */             e.printStackTrace();
/*      */           }
/*      */         }
/*      */       }
/*      */     }
/*      */   }
/*      */ 
/*      */   private static class OptimisticUnchokePeer extends Thread
/*      */   {
/*      */     public void run()
/*      */     {
/*  829 */       while (peerProcess.completedPeers < peerProcess.peers.size()) {
/*  830 */         ArrayList connections = new ArrayList(peerProcess.peerConnections.keySet());
/*  831 */         ArrayList interested = new ArrayList();
/*  832 */         for (Iterator localIterator = connections.iterator(); localIterator.hasNext(); ) { int connection = ((Integer)localIterator.next()).intValue();
/*  833 */           if (((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(connection))).isInterested()) {
/*  834 */             interested.add(Integer.valueOf(connection));
/*      */           }
/*      */         }
/*  837 */         if (interested.size() > 0) {
/*  838 */           Random r = new Random();
/*  839 */           int randomNumber = Math.abs(r.nextInt() % interested.size());
/*  840 */           int connection = ((Integer)interested.get(randomNumber)).intValue();
/*  841 */           ((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(connection))).unchoke();
/*  842 */           ((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(connection))).sendMessage('1');
/*  843 */           ((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(connection))).optimisticallyUnchoke();
/*  844 */           peerProcess.logs.changeOptimisticallyUnchokedNeighbor(peerProcess.thisPeer.getPeerID(), ((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(connection))).getPeerID());
/*      */           try {
/*  846 */             Thread.sleep(peerProcess.common.getOptimisticUnchokingInterval() * 1000);
/*  847 */             ((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(connection))).optimisticallyChoke();
/*      */           }
/*      */           catch (Exception e) {
/*  850 */             e.printStackTrace();
/*      */           }
/*      */         }
/*      */       }
/*      */       try {
/*  855 */         Thread.sleep(5000L);
/*      */       }
/*      */       catch (Exception e) {
/*  858 */         e.printStackTrace();
/*      */       }
/*  860 */       System.exit(0);
/*      */     }
/*      */   }
/*      */ 
/*      */   private static class UnchokePeers extends Thread
/*      */   {
/*      */     public void run()
/*      */     {
/*  732 */       while (peerProcess.completedPeers < peerProcess.peers.size()) {
/*  733 */         ArrayList connections = new ArrayList(peerProcess.peerConnections.keySet());
/*  734 */         int[] preferredNeighbors = new int[peerProcess.common.getNumberOfPreferredNeighbors()];
/*      */         int i;
/*      */         int i;
/*  735 */         if (peerProcess.thisPeer.getHaveFile() == 1) {
/*  736 */           ArrayList interestedPeers = new ArrayList();
/*  737 */           for (Iterator localIterator = connections.iterator(); localIterator.hasNext(); ) { int peer = ((Integer)localIterator.next()).intValue();
/*  738 */             if (((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(peer))).isInterested())
/*  739 */               interestedPeers.add(Integer.valueOf(peer));
/*      */           }
/*  741 */           if (interestedPeers.size() > 0) {
/*  742 */             if (interestedPeers.size() <= peerProcess.common.getNumberOfPreferredNeighbors()) {
/*  743 */               for (Integer peer : interestedPeers)
/*  744 */                 if (((peerProcess.PeerConnection)peerProcess.peerConnections.get(peer)).isChoked()) {
/*  745 */                   ((peerProcess.PeerConnection)peerProcess.peerConnections.get(peer)).unchoke();
/*  746 */                   ((peerProcess.PeerConnection)peerProcess.peerConnections.get(peer)).sendMessage('1');
/*      */                 }
/*      */             }
/*      */             else {
/*  750 */               Random r = new Random();
/*  751 */               for (i = 0; i < peerProcess.common.getNumberOfPreferredNeighbors(); i++) {
/*  752 */                 preferredNeighbors[i] = ((Integer)interestedPeers.remove(Math.abs(r.nextInt() % interestedPeers.size()))).intValue();
/*      */               }
/*  754 */               for (int peer : preferredNeighbors) {
/*  755 */                 if (((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(peer))).isChoked()) {
/*  756 */                   ((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(peer))).unchoke();
/*  757 */                   ((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(peer))).sendMessage('1');
/*      */                 }
/*      */               }
/*  760 */               for (Integer peer : interestedPeers)
/*  761 */                 if ((!((peerProcess.PeerConnection)peerProcess.peerConnections.get(peer)).isChoked()) && (!((peerProcess.PeerConnection)peerProcess.peerConnections.get(peer)).isOptimisticallyUnchoked())) {
/*  762 */                   ((peerProcess.PeerConnection)peerProcess.peerConnections.get(peer)).choke();
/*  763 */                   ((peerProcess.PeerConnection)peerProcess.peerConnections.get(peer)).sendMessage('0');
/*      */                 }
/*      */             }
/*      */           }
/*      */         }
/*      */         else
/*      */         {
/*  770 */           ArrayList interestedPeers = new ArrayList();
/*  771 */           int counter = 0;
/*  772 */           for (i = connections.iterator(); i.hasNext(); ) { int peer = ((Integer)i.next()).intValue();
/*  773 */             if ((((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(peer))).isInterested()) && (((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(peer))).getDownloadRate() >= 0.0D))
/*  774 */               interestedPeers.add(Integer.valueOf(peer));
/*      */           }
/*  776 */           if (interestedPeers.size() <= peerProcess.common.getNumberOfPreferredNeighbors()) {
/*  777 */             for (i = interestedPeers.iterator(); i.hasNext(); ) { int peer = ((Integer)i.next()).intValue();
/*  778 */               preferredNeighbors[(counter++)] = peer;
/*  779 */               if (((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(peer))).isChoked()) {
/*  780 */                 ((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(peer))).unchoke();
/*  781 */                 ((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(peer))).sendMessage('1');
/*      */               } }
/*      */           }
/*      */           else
/*      */           {
/*  786 */             for (i = 0; i < peerProcess.common.getNumberOfPreferredNeighbors(); i++) {
/*  787 */               int max = ((Integer)interestedPeers.get(0)).intValue();
/*  788 */               for (int j = 1; j < interestedPeers.size(); j++) {
/*  789 */                 if (((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(max))).getDownloadRate() <= ((peerProcess.PeerConnection)peerProcess.peerConnections.get(interestedPeers.get(j))).getDownloadRate()) {
/*  790 */                   max = ((Integer)interestedPeers.get(j)).intValue();
/*      */                 }
/*      */               }
/*  793 */               if (((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(max))).isChoked()) {
/*  794 */                 ((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(max))).unchoke();
/*  795 */                 ((peerProcess.PeerConnection)peerProcess.peerConnections.get(Integer.valueOf(max))).sendMessage('1');
/*      */               }
/*  797 */               preferredNeighbors[i] = max;
/*  798 */               interestedPeers.remove(Integer.valueOf(max));
/*      */             }
/*  800 */             for (Integer peer : interestedPeers) {
/*  801 */               if ((!((peerProcess.PeerConnection)peerProcess.peerConnections.get(peer)).isChoked()) && (!((peerProcess.PeerConnection)peerProcess.peerConnections.get(peer)).isOptimisticallyUnchoked())) {
/*  802 */                 ((peerProcess.PeerConnection)peerProcess.peerConnections.get(peer)).choke();
/*  803 */                 ((peerProcess.PeerConnection)peerProcess.peerConnections.get(peer)).sendMessage('0');
/*      */               }
/*      */             }
/*      */           }
/*      */         }
/*  808 */         peerProcess.logs.changePreferredNeighbors(peerProcess.thisPeer.getPeerID(), preferredNeighbors);
/*      */         try {
/*  810 */           Thread.sleep(peerProcess.common.getUnchokingInterval() * 1000);
/*      */         }
/*      */         catch (Exception e) {
/*  813 */           e.printStackTrace();
/*      */         }
/*      */       }
/*      */       try {
/*  817 */         Thread.sleep(5000L);
/*      */       }
/*      */       catch (Exception localException1)
/*      */       {
/*      */       }
/*  822 */       System.exit(0);
/*      */     }
/*      */   }
/*      */ 
/*      */   private static class ReceiveConnections extends Thread
/*      */   {
/*      */     public void run()
/*      */     {
/*  704 */       byte[] buffer = new byte[32];
/*      */       try {
/*  706 */         ServerSocket serverSocket = new ServerSocket(peerProcess.thisPeer.getPortNumber());
/*  707 */         while (peerProcess.peerConnections.size() < peerProcess.peers.size() - 1) {
/*  708 */           Socket connection = serverSocket.accept();
/*  709 */           DataInputStream dataInputStream = new DataInputStream(connection.getInputStream());
/*  710 */           dataInputStream.readFully(buffer);
/*  711 */           int peerID = ByteBuffer.wrap(Arrays.copyOfRange(buffer, 28, 32)).getInt();
/*  712 */           peerProcess.logs.connectionFrom(peerProcess.hostID, peerID);
/*  713 */           StringBuilder handshakeMsg = new StringBuilder();
/*  714 */           handshakeMsg.append(new String(Arrays.copyOfRange(buffer, 0, 28)));
/*  715 */           handshakeMsg.append(peerID);
/*  716 */           System.out.println(handshakeMsg);
/*  717 */           peerProcess.peerConnections.put(Integer.valueOf(peerID), new peerProcess.PeerConnection(connection, peerID));
/*  718 */           DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
/*  719 */           dataOutputStream.flush();
/*  720 */           dataOutputStream.write(peerProcess.msg.getHandshakeMessage(peerProcess.hostID));
/*      */         }
/*      */       }
/*      */       catch (Exception e) {
/*  724 */         e.printStackTrace();
/*      */       }
/*      */     }
/*      */   }
/*      */ 
/*      */   private static class SendConnections extends Thread
/*      */   {
/*      */     public void run()
/*      */     {
/*  667 */       byte[] buffer = new byte[32];
/*      */       try {
/*  669 */         for (localIterator = peerProcess.peers.keySet().iterator(); localIterator.hasNext(); ) { int id = ((Integer)localIterator.next()).intValue();
/*  670 */           if (id == peerProcess.hostID) {
/*      */             break;
/*      */           }
/*  673 */           PeerInfo connPeer = (PeerInfo)peerProcess.peers.get(Integer.valueOf(id));
/*  674 */           Socket connection = new Socket(connPeer.getHostName(), connPeer.getPortNumber());
/*  675 */           DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
/*  676 */           dataOutputStream.flush();
/*  677 */           dataOutputStream.write(peerProcess.msg.getHandshakeMessage(peerProcess.hostID));
/*  678 */           dataOutputStream.flush();
/*  679 */           DataInputStream dataInputStream = new DataInputStream(connection.getInputStream());
/*  680 */           dataInputStream.readFully(buffer);
/*  681 */           int peerID = ByteBuffer.wrap(Arrays.copyOfRange(buffer, 28, 32)).getInt();
/*  682 */           if (peerID != id) {
/*  683 */             connection.close();
/*      */           } else {
/*  685 */             peerProcess.logs.connectionTo(peerProcess.hostID, id);
/*  686 */             StringBuilder handshakeMsg = new StringBuilder();
/*  687 */             handshakeMsg.append(new String(Arrays.copyOfRange(buffer, 0, 28)));
/*  688 */             handshakeMsg.append(peerID);
/*  689 */             System.out.println(handshakeMsg);
/*  690 */             peerProcess.peerConnections.put(Integer.valueOf(id), new peerProcess.PeerConnection(connection, id));
/*      */           }
/*      */         }
/*      */       }
/*      */       catch (Exception e)
/*      */       {
/*      */         Iterator localIterator;
/*  696 */         e.printStackTrace();
/*      */       }
/*      */     }
/*      */   }
/*      */ }

/* Location:           C:\Users\dell\Downloads\BitTorrent\BitTorrent\peerProcess.jar
 * Qualified Name:     peerProcess
 * JD-Core Version:    0.6.2
 */