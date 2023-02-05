/*     */ class CommonInfo
/*     */ {
/*     */   private int numberOfPreferredNeighbors;
/*     */   private int unchokingInterval;
/*     */   private int optimisticUnchokingInterval;
/*     */   private String fileName;
/*     */   private int fileSize;
/*     */   private int pieceSize;
/*     */ 
/*     */   public void setNumberOfPreferredNeighbors(int k)
/*     */   {
/* 224 */     this.numberOfPreferredNeighbors = k;
/*     */   }
/*     */ 
/*     */   public int getNumberOfPreferredNeighbors()
/*     */   {
/* 229 */     return this.numberOfPreferredNeighbors;
/*     */   }
/*     */ 
/*     */   public void setUnchokingInterval(int u)
/*     */   {
/* 234 */     this.unchokingInterval = u;
/*     */   }
/*     */ 
/*     */   public int getUnchokingInterval()
/*     */   {
/* 239 */     return this.unchokingInterval;
/*     */   }
/*     */ 
/*     */   public void setOptimisticUnchokingInterval(int o) {
/* 243 */     this.optimisticUnchokingInterval = o;
/*     */   }
/*     */ 
/*     */   public int getOptimisticUnchokingInterval()
/*     */   {
/* 248 */     return this.optimisticUnchokingInterval;
/*     */   }
/*     */ 
/*     */   public void setFileName(String f)
/*     */   {
/* 253 */     this.fileName = f;
/*     */   }
/*     */ 
/*     */   public String getFileName()
/*     */   {
/* 258 */     return this.fileName;
/*     */   }
/*     */ 
/*     */   public void setFileSize(int size) {
/* 262 */     this.fileSize = size;
/*     */   }
/*     */ 
/*     */   public int getFileSize()
/*     */   {
/* 267 */     return this.fileSize;
/*     */   }
/*     */ 
/*     */   public void setPieceSize(int psize)
/*     */   {
/* 272 */     this.pieceSize = psize;
/*     */   }
/*     */ 
/*     */   public int getPieceSize() {
/* 276 */     return this.pieceSize;
/*     */   }
/*     */ }

/* Location:           C:\Users\dell\Downloads\BitTorrent\BitTorrent\peerProcess.jar
 * Qualified Name:     CommonInfo
 * JD-Core Version:    0.6.2
 */