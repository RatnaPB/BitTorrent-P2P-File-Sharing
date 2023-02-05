/*     */ import java.io.BufferedWriter;
/*     */ import java.io.IOException;
/*     */ import java.text.DateFormat;
/*     */ import java.text.SimpleDateFormat;
/*     */ import java.util.Date;
/*     */ 
/*     */ class Logs
/*     */ {
/* 281 */   private DateFormat timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
/* 282 */   private Date time = new Date();
/*     */   private BufferedWriter writer;
/*     */ 
/*     */   public Logs(BufferedWriter writer)
/*     */     throws IOException
/*     */   {
/* 286 */     this.timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
/* 287 */     this.writer = writer;
/*     */   }
/*     */ 
/*     */   public void connectionTo(int id1, int id2) {
/* 291 */     this.time = new Date();
/* 292 */     StringBuffer log = new StringBuffer();
/* 293 */     log.append(this.timeFormat.format(this.time));
/* 294 */     log.append(':');
/* 295 */     log.append(" Peer ");
/* 296 */     log.append(id1);
/* 297 */     log.append(" makes a connection to Peer ");
/* 298 */     log.append(id2);
/* 299 */     log.append('.');
/*     */     try {
/* 301 */       this.writer.write(log.toString());
/* 302 */       this.writer.newLine();
/* 303 */       this.writer.flush();
/*     */     }
/*     */     catch (Exception localException)
/*     */     {
/*     */     }
/*     */   }
/*     */ 
/*     */   public void connectionFrom(int id1, int id2) {
/* 311 */     this.time = new Date();
/* 312 */     StringBuffer log = new StringBuffer();
/* 313 */     log.append(this.timeFormat.format(this.time));
/* 314 */     log.append(':');
/* 315 */     log.append(" Peer ");
/* 316 */     log.append(id1);
/* 317 */     log.append(" is connected from Peer ");
/* 318 */     log.append(id2);
/* 319 */     log.append('.');
/*     */     try {
/* 321 */       this.writer.write(log.toString());
/* 322 */       this.writer.newLine();
/* 323 */       this.writer.flush();
/*     */     }
/*     */     catch (Exception localException)
/*     */     {
/*     */     }
/*     */   }
/*     */ 
/*     */   public void changePreferredNeighbors(int id1, int[] ids) {
/* 331 */     this.time = new Date();
/* 332 */     StringBuffer log = new StringBuffer();
/* 333 */     log.append(this.timeFormat.format(this.time));
/* 334 */     log.append(':');
/* 335 */     log.append(" Peer ");
/* 336 */     log.append(id1);
/* 337 */     log.append(" has the preferred neighbors ");
/* 338 */     for (int id : ids) {
/* 339 */       log.append(id);
/* 340 */       log.append(',');
/*     */     }
/* 342 */     log.deleteCharAt(log.length() - 1);
/* 343 */     log.append('.');
/*     */     try {
/* 345 */       this.writer.write(log.toString());
/* 346 */       this.writer.newLine();
/* 347 */       this.writer.flush();
/*     */     }
/*     */     catch (Exception localException1)
/*     */     {
/*     */     }
/*     */   }
/*     */ 
/*     */   public void changeOptimisticallyUnchokedNeighbor(int id1, int id2) {
/* 355 */     this.time = new Date();
/* 356 */     StringBuffer log = new StringBuffer();
/* 357 */     log.append(this.timeFormat.format(this.time));
/* 358 */     log.append(':');
/* 359 */     log.append(" Peer ");
/* 360 */     log.append(id1);
/* 361 */     log.append(" has the optimistically unchoked neighbor ");
/* 362 */     log.append(id2);
/* 363 */     log.append('.');
/*     */     try {
/* 365 */       this.writer.write(log.toString());
/* 366 */       this.writer.newLine();
/* 367 */       this.writer.flush();
/*     */     }
/*     */     catch (Exception localException)
/*     */     {
/*     */     }
/*     */   }
/*     */ 
/*     */   public void unchoked(int id1, int id2) {
/* 375 */     this.time = new Date();
/* 376 */     StringBuffer log = new StringBuffer();
/* 377 */     log.append(this.timeFormat.format(this.time));
/* 378 */     log.append(':');
/* 379 */     log.append(" Peer ");
/* 380 */     log.append(id1);
/* 381 */     log.append(" is unchoked by ");
/* 382 */     log.append(id2);
/* 383 */     log.append('.');
/*     */     try {
/* 385 */       this.writer.write(log.toString());
/* 386 */       this.writer.newLine();
/* 387 */       this.writer.flush();
/*     */     }
/*     */     catch (Exception localException)
/*     */     {
/*     */     }
/*     */   }
/*     */ 
/*     */   public void choked(int id1, int id2) {
/* 395 */     this.time = new Date();
/* 396 */     StringBuffer log = new StringBuffer();
/* 397 */     log.append(this.timeFormat.format(this.time));
/* 398 */     log.append(':');
/* 399 */     log.append(" Peer ");
/* 400 */     log.append(id1);
/* 401 */     log.append(" is choked by ");
/* 402 */     log.append(id2);
/* 403 */     log.append('.');
/*     */     try {
/* 405 */       this.writer.write(log.toString());
/* 406 */       this.writer.newLine();
/* 407 */       this.writer.flush();
/*     */     }
/*     */     catch (Exception localException)
/*     */     {
/*     */     }
/*     */   }
/*     */ 
/*     */   public void receiveHave(int id1, int id2, int index) {
/* 415 */     this.time = new Date();
/* 416 */     StringBuffer log = new StringBuffer();
/* 417 */     log.append(this.timeFormat.format(this.time));
/* 418 */     log.append(':');
/* 419 */     log.append(" Peer ");
/* 420 */     log.append(id1);
/* 421 */     log.append(" received the 'have' message from ");
/* 422 */     log.append(id2);
/* 423 */     log.append(" for the piece ");
/* 424 */     log.append(index);
/* 425 */     log.append('.');
/*     */     try {
/* 427 */       this.writer.write(log.toString());
/* 428 */       this.writer.newLine();
/* 429 */       this.writer.flush();
/*     */     }
/*     */     catch (Exception localException)
/*     */     {
/*     */     }
/*     */   }
/*     */ 
/*     */   public void receiveInterested(int id1, int id2) {
/* 437 */     this.time = new Date();
/* 438 */     StringBuffer log = new StringBuffer();
/* 439 */     log.append(this.timeFormat.format(this.time));
/* 440 */     log.append(':');
/* 441 */     log.append(" Peer ");
/* 442 */     log.append(id1);
/* 443 */     log.append(" received the 'interested' message from ");
/* 444 */     log.append(id2);
/* 445 */     log.append('.');
/*     */     try {
/* 447 */       this.writer.write(log.toString());
/* 448 */       this.writer.newLine();
/* 449 */       this.writer.flush();
/*     */     }
/*     */     catch (Exception localException)
/*     */     {
/*     */     }
/*     */   }
/*     */ 
/*     */   public void receiveNotInterested(int id1, int id2) {
/* 457 */     this.time = new Date();
/* 458 */     StringBuffer log = new StringBuffer();
/* 459 */     log.append(this.timeFormat.format(this.time));
/* 460 */     log.append(':');
/* 461 */     log.append(" Peer ");
/* 462 */     log.append(id1);
/* 463 */     log.append(" received the 'not interested' message from ");
/* 464 */     log.append(id2);
/* 465 */     log.append('.');
/*     */     try {
/* 467 */       this.writer.write(log.toString());
/* 468 */       this.writer.newLine();
/* 469 */       this.writer.flush();
/*     */     }
/*     */     catch (Exception localException)
/*     */     {
/*     */     }
/*     */   }
/*     */ 
/*     */   public void downloadingPiece(int id1, int id2, int index, int numOfPieces) {
/* 477 */     this.time = new Date();
/* 478 */     StringBuffer log = new StringBuffer();
/* 479 */     log.append(this.timeFormat.format(this.time));
/* 480 */     log.append(':');
/* 481 */     log.append(" Peer ");
/* 482 */     log.append(id1);
/* 483 */     log.append(" has downloaded the piece ");
/* 484 */     log.append(index);
/* 485 */     log.append(" from ");
/* 486 */     log.append(id2);
/* 487 */     log.append(".\n");
/* 488 */     log.append("Now the number of pieces it has is ");
/* 489 */     log.append(numOfPieces);
/* 490 */     log.append('.');
/*     */     try {
/* 492 */       this.writer.write(log.toString());
/* 493 */       this.writer.newLine();
/* 494 */       this.writer.flush();
/*     */     }
/*     */     catch (Exception localException)
/*     */     {
/*     */     }
/*     */   }
/*     */ 
/*     */   public void downloadCompleted(int id1) {
/* 502 */     this.time = new Date();
/* 503 */     StringBuffer log = new StringBuffer();
/* 504 */     log.append(this.timeFormat.format(this.time));
/* 505 */     log.append(':');
/* 506 */     log.append(" Peer ");
/* 507 */     log.append(id1);
/* 508 */     log.append(" has downloaded the complete file ");
/*     */     try {
/* 510 */       this.writer.write(log.toString());
/* 511 */       this.writer.newLine();
/* 512 */       this.writer.flush();
/*     */     }
/*     */     catch (Exception localException)
/*     */     {
/*     */     }
/*     */   }
/*     */ }

/* Location:           C:\Users\dell\Downloads\BitTorrent\BitTorrent\peerProcess.jar
 * Qualified Name:     Logs
 * JD-Core Version:    0.6.2
 */