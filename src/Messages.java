/*     */ import java.io.PrintStream;
/*     */ import java.nio.ByteBuffer;
/*     */ 
/*     */ class Messages
/*     */ {
/*     */   private static final char CHOKE = '0';
/*     */   private static final char UNCHOKE = '1';
/*     */   private static final char INTERESTED = '2';
/*     */   private static final char NOT_INTERESTED = '3';
/*     */   private static final char HAVE = '4';
/*     */   private static final char BITFIELD = '5';
/*     */   private static final char REQUEST = '6';
/*     */   private static final char PIECE = '7';
/*     */ 
/*     */   public byte[] makeMessage(int len, char type, byte[] payload)
/*     */   {
/*  96 */     byte msgType = (byte)type;
/*     */     byte[] message;
/*  98 */     switch (type) {
/*     */     case '0':
/*     */     case '1':
/*     */     case '2':
/*     */     case '3':
/* 103 */       byte[] message = new byte[len + 4];
/* 104 */       byte[] length = ByteBuffer.allocate(4).putInt(len).array();
/* 105 */       int counter = 0;
/* 106 */       for (byte x : length) {
/* 107 */         message[counter] = x;
/* 108 */         counter++;
/*     */       }
/* 110 */       message[counter] = msgType;
/* 111 */       break;
/*     */     case '4':
/*     */     case '5':
/*     */     case '6':
/*     */     case '7':
/* 116 */       byte[] message = new byte[len + 4];
/* 117 */       byte[] length = ByteBuffer.allocate(4).putInt(len).array();
/* 118 */       int counter = 0;
/* 119 */       for (byte x : length) {
/* 120 */         message[counter] = x;
/* 121 */         counter++;
/*     */       }
/* 123 */       message[(counter++)] = msgType;
/* 124 */       for (byte x : payload) {
/* 125 */         message[counter] = x;
/* 126 */         counter++;
/*     */       }
/* 128 */       break;
/*     */     default:
/* 130 */       message = new byte[0];
/* 131 */       System.out.println("ERROR in Message: " + type);
/*     */     }
/* 133 */     return message;
/*     */   }
/*     */ 
/*     */   public byte[] getChokeMessage() {
/* 137 */     return makeMessage(1, '0', null);
/*     */   }
/*     */ 
/*     */   public byte[] getUnchokeMessage() {
/* 141 */     return makeMessage(1, '1', null);
/*     */   }
/*     */ 
/*     */   public byte[] getInterestedMessage() {
/* 145 */     return makeMessage(1, '2', null);
/*     */   }
/*     */ 
/*     */   public byte[] getNotInterestedMessage() {
/* 149 */     return makeMessage(1, '3', null);
/*     */   }
/*     */ 
/*     */   public byte[] getHaveMessage(int pieceIndex) {
/* 153 */     byte[] payload = ByteBuffer.allocate(4).putInt(pieceIndex).array();
/* 154 */     return makeMessage(5, '4', payload);
/*     */   }
/*     */ 
/*     */   public byte[] getBitfieldMessage(int[] bitfield) {
/* 158 */     int len = 1 + 4 * bitfield.length;
/* 159 */     byte[] payload = new byte[len - 1];
/* 160 */     int counter = 0;
/* 161 */     for (int bit : bitfield) {
/* 162 */       byte[] bitBytes = ByteBuffer.allocate(4).putInt(bit).array();
/* 163 */       for (byte b : bitBytes) {
/* 164 */         payload[counter] = b;
/* 165 */         counter++;
/*     */       }
/*     */     }
/* 168 */     return makeMessage(len, '5', payload);
/*     */   }
/*     */ 
/*     */   public byte[] getRequestMessage(int pieceIndex) {
/* 172 */     byte[] payload = ByteBuffer.allocate(4).putInt(pieceIndex).array();
/* 173 */     return makeMessage(5, '6', payload);
/*     */   }
/*     */ 
/*     */   public byte[] getPieceMessage(int pieceIndex, byte[] piece) {
/* 177 */     byte[] payload = new byte[4 + piece.length];
/* 178 */     int counter = 0;
/* 179 */     byte[] indexBytes = ByteBuffer.allocate(4).putInt(pieceIndex).array();
/* 180 */     for (byte bit : indexBytes) {
/* 181 */       payload[counter] = bit;
/* 182 */       counter++;
/*     */     }
/* 184 */     for (byte bit : piece) {
/* 185 */       payload[counter] = bit;
/* 186 */       counter++;
/*     */     }
/* 188 */     return makeMessage(5 + piece.length, '7', payload);
/*     */   }
/*     */ 
/*     */   public byte[] getHandshakeMessage(int peerID) {
/* 192 */     byte[] message = new byte[32];
/* 193 */     byte[] header = new String("P2PFILESHARINGPROJ").getBytes();
/* 194 */     byte[] zerobits = new String("0000000000").getBytes();
/* 195 */     byte[] id = ByteBuffer.allocate(4).putInt(peerID).array();
/* 196 */     int counter = 0;
/* 197 */     for (byte b : header) {
/* 198 */       message[counter] = b;
/* 199 */       counter++;
/*     */     }
/* 201 */     for (byte b : zerobits) {
/* 202 */       message[counter] = b;
/* 203 */       counter++;
/*     */     }
/* 205 */     for (byte b : id) {
/* 206 */       message[counter] = b;
/* 207 */       counter++;
/*     */     }
/* 209 */     return message;
/*     */   }
/*     */ }

/* Location:           C:\Users\dell\Downloads\BitTorrent\BitTorrent\peerProcess.jar
 * Qualified Name:     Messages
 * JD-Core Version:    0.6.2
 */