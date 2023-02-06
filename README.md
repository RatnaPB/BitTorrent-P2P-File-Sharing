# **BitTorrent - P2P File Distribution**

## **Project Members:**

| Name                    |   UFID   |
| ----------------------- | :------: |
| Mohit Prasad Chandorkar | 98983327 |
| Ratna Prabha Bhairagond | 88274983 |
| Varad Rajeev Sanpurkar  | 17829883 |

#

## **Description:**

### _BitTorrent_ is a popular _Peer-to-Peer_ file sharing software. We are asked to implement some of it's features like _choking, unchoking_, _handshake_ etc using Java Multithreading.

#

## **Demo Link:**

### UF OneDrive Link:

[Project Demo UF OneDrive Link](https://uflorida-my.sharepoint.com/:v:/g/personal/vsanpurkar_ufl_edu/EZXhO4lyMa5EgOnMTIGp1qABKyp3tbig84zsmA7iRfj6lw?e=89S5JD)

### Google Drive Link:

[Project Demo Google Drive Link](https://drive.google.com/file/d/1clXIL_Hq7TpxufPlgFsXJv819JfbXFQ7/view?usp=sharing)

#

## **Steps to run on Local Host:**

### 1) Change the _PeerInfo.cfg_ file with the _hostname_ to be the _localhost_ on local machine

    1001 localhost 9001 1
    1002 localhost 9002 0
    1003 localhost 9003 0
    1004 localhost 9004 0
    1005 localhost 9005 0

### Change the _PeerInfo.cfg_ file with the _hostname_ to be the respective _linuxhost_

    1001 lin114-00.cise.ufl.edu 9001 1
    1002 lin114-01.cise.ufl.edu 9002 0
    1003 lin114-02.cise.ufl.edu 9003 0
    1004 lin114-03.cise.ufl.edu 9004 0
    1005 lin114-04.cise.ufl.edu 9005 0

### 2) Compile all the Java files to get the executable class files:

    javac *.java

### 3) Run PeerProcess for every PeerID on different command promts:

    java PeerProcess 1001
    java PeerProcess 1002
    java PeerProcess 1003
    java PeerProcess 1004
    java PeerProcess 1005

#

## **Task Distribution:**

|          Name           |                                      Responsibilities                                       |
| :---------------------: | :-----------------------------------------------------------------------------------------: |
| Mohit Prasad Chandorkar |      Configuration Parsing, Message Recpetion and Handling, Choke and Unchoke Handler       |
| Ratna Prabha Bhairagond |                Connection Establishment, Logger, Optimistic Unchoke Handler                 |
| Varad Rajeev Sanpurkar  | Component Integration, Interfaces Design, PeerProcess Main Module, Code Architecture Design |

#

## **Input Files:**

### In the project, there are two configuration files which the peer must read. The common properties utilized by all the peers are specified in the file named by _Common.cfg_

### **a) Common.cfg:**

    NumberOfPreferredNeighbors 2
    UnchokingInterval 5
    OptimisticUnchokingInterval 15
    FileName TheFile.dat
    FileSize 10000232
    PieceSize 32768

### The unit of _UnchokingInterval_ and _OptimisticUnchokingInterval_ is in seconds. The _FileName_ property specifies the name of a file in which all peers are interested. _FileSize_ specifies the size of the file in bytes. _PieceSize_ specifies the size of a piece in bytes. In the above example, the file size is 10,000,232 bytes and the piece size is 32,768 bytes. Then the number of pieces of this file is 306. Note that the size of the last piece is only 5,992 bytes.

### **b) PeerInfo.cfg:**

    1001 lin114-00.cise.ufl.edu 6008 1
    1002 lin114-01.cise.ufl.edu 6008 0
    1003 lin114-02.cise.ufl.edu 6008 0
    1004 lin114-03.cise.ufl.edu 6008 0
    1005 lin114-04.cise.ufl.edu 6008 0
    1006 lin114-05.cise.ufl.edu 6008 0

### The peer information is specified in the file _PeerInfo.cfg_ in the following format:

    [peer ID] [host name] [listening port] [has file or not]

### Each line in file _PeerInfo.cfg_ represents a peer. The first column is the peer ID, which is a positive integer number. The second column is the host name where the peer is. The third column is the port number at which the peer listens. The port numbers of the peers may be different from each other. The fourth column specifies whether it has the file or not. We only have two options here. ‘1’ means that the peer has the complete file and ‘0’ means that the peer does not have the file. We do not consider the case where a peer has only some pieces of the file.

#

## **Code Structure:**

    ->Logging.java - Creates log file for each peer and updates the file with new logs generated.

    ->PeerInfoFileParser.java - Reads PeerInfo.cng file, pareses and stores the config parameters into class member variables.

    ->CommonConfigFileParser.java - Reads Common.cng file, pareses and stores the config parameters into class member variables.

    ->PeerProcess.java - Main entry to the program, which expectes peerId as a command line argument.

    ->PeerMessagePassing.java - Handles message creation and passing of each type of messages between peers.

    -> PeerFunctionsManager.java - Handles intialization, connection creation, choking, unchoking and termination of each peer. Also, handles creation of peer folder, read and write from the file.

#
