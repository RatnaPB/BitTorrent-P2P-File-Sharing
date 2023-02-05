/*    */ import java.io.PrintStream;
/*    */ 
/*    */ class PeerInfo extends Thread
/*    */ {
/*    */   private int peerID;
/*    */   private String hostName;
/*    */   private int portNumber;
/*    */   private int haveFile;
/*    */   private int[] bitfield;
/* 21 */   private int numOfPieces = 0;
/*    */ 
/*    */   public void printBitfield() {
/* 24 */     for (int bit : this.bitfield)
/* 25 */       System.out.print(bit);
/*    */   }
/*    */ 
/*    */   public int getNumOfPieces() {
/* 29 */     return this.numOfPieces;
/*    */   }
/*    */ 
/*    */   public void updateNumOfPieces() {
/* 33 */     this.numOfPieces += 1;
/* 34 */     if (this.numOfPieces == this.bitfield.length)
/* 35 */       this.haveFile = 1;
/*    */   }
/*    */ 
/*    */   public int getPeerID() {
/* 39 */     return this.peerID;
/*    */   }
/*    */ 
/*    */   public void setPeerID(int peerID) {
/* 43 */     this.peerID = peerID;
/*    */   }
/*    */ 
/*    */   public String getHostName() {
/* 47 */     return this.hostName;
/*    */   }
/*    */ 
/*    */   public void setHostName(String hostName) {
/* 51 */     this.hostName = hostName;
/*    */   }
/*    */ 
/*    */   public int getPortNumber() {
/* 55 */     return this.portNumber;
/*    */   }
/*    */ 
/*    */   public void setPortNumber(int portNumber) {
/* 59 */     this.portNumber = portNumber;
/*    */   }
/*    */ 
/*    */   public int getHaveFile() {
/* 63 */     return this.haveFile;
/*    */   }
/*    */ 
/*    */   public void setHaveFile(int haveFile) {
/* 67 */     this.haveFile = haveFile;
/*    */   }
/*    */ 
/*    */   public int[] getBitfield() {
/* 71 */     return this.bitfield;
/*    */   }
/*    */ 
/*    */   public void setBitfield(int[] bitfield) {
/* 75 */     this.bitfield = bitfield;
/*    */   }
/*    */ 
/*    */   public void updateBitfield(int index) {
/* 79 */     this.bitfield[index] = 1;
/*    */   }
/*    */ }

/* Location:           C:\Users\dell\Downloads\BitTorrent\BitTorrent\peerProcess.jar
 * Qualified Name:     PeerInfo
 * JD-Core Version:    0.6.2
 */