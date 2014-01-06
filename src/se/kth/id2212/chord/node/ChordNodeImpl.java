package se.kth.id2212.chord.node;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import se.kth.id2212.chord.dht.HashUtil;
import se.kth.id2212.chord.dht.IKey;
import se.kth.id2212.chord.dht.KeyImpl;

public class ChordNodeImpl extends UnicastRemoteObject implements IChordNode {

    private static final long serialVersionUID = 7356823435790024489L;

    // the hash table of this node
    Map<String, String> hashTable = new HashMap<String, String>();

    // the length of successors list (r should be log(N))
    private static final int r = 3;

    // static int port = 12345;
    private boolean isActive;

    // finger table, size = m = KEY_LENGTH + 1
    private IChordNode[] fingers = new IChordNode[KeyImpl.KEY_LENGTH + 1];

    // successor list for handling node leaving/failure
    private List<IChordNode> successorList = new ArrayList<IChordNode>();

    private IChordNode predecessor;

    // the index of the next finger to fix, used by fixFingers()
    private int nextFinger;

    public final String rmiURL;

    // the key/identifier of the node
    public final IKey chordId;

    // Scheduler
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public ChordNodeImpl(IChordNode knownPeer, String _rmiURL, String _chordName)
            throws RemoteException, NoSuchAlgorithmException {
        super();

        isActive = false;
        predecessor = null;
        nextFinger = 0;

        String tmpURL = _rmiURL + _chordName;
        chordId = HashUtil.hash(tmpURL.getBytes(), HashUtil.SHA_ALGO, KeyImpl.KEY_MAX);
        this.rmiURL = _rmiURL + chordId;

        if (knownPeer == null) {
            createGroup();
        } else {
            joinGroup(knownPeer);
        }

        isActive = true;

        // Runnable nested class for stablizing
        Runnable stabalizer = new Runnable() {
            @Override
            public void run() {
                try {
                    stablize();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        // Runnable nested class for fixing fingers
        Runnable fingerFixer = new Runnable() {
            @Override
            public void run() {
                try {
                    fixFingers();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        // Runnable nested class for checking predecessor
        Runnable predecessorChecker = new Runnable() {
            @Override
            public void run() {
                try {
                    checkPredecessor();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        // Runnable nested class for updating successor list
        Runnable successorListUpdator = new Runnable() {
            @Override
            public void run() {
                try {
                    updateSuccessorList(getSuccessor().getSuccessorList());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        scheduler.scheduleAtFixedRate(stabalizer, 1, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(fingerFixer, 1, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(predecessorChecker, 1, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(successorListUpdator, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public IKey getId() {
        return chordId;
    }

    @Override
    public IChordNode getPredecessor() throws RemoteException {
        return predecessor;
    }

    @Override
    public IChordNode getSuccessor() {
        if (successorList.size() > 0)
            return this.successorList.get(0);
        else
            return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see id2212.chord.node.IChordNode#findSuccessor(id2212.chord.dht.IKey)
     * 
     * Pseudo Code:
     * 
     * // ask node n to find the successor of id
     * procedure n.findSuccessor(id) {
     *   if (predecessor != nil and id in (predecessor, n]) then
     *     return n
     *   else if (id in (n, successor]) then
     *     return successor
     *   else {
     *     m := closestPrecedingNode(id) return
     *     m.findSuccessor(id)
     *   }
     * }
     */
    @Override
    public IChordNode findSuccessor(IKey id) throws RemoteException {
        System.out.println("Find successor of id: " + id.toDecString());

        if (predecessor != null && id.isBetween(predecessor.getId(), this.getId())) {
            return this;
        } else if (id.isBetween(getId(), getSuccessor().getId())) {
            return getSuccessor();
        } else {
            // forward the query around the circle
            IChordNode m = closetPrecedingNode(id);
            return m.findSuccessor(id);
        }
    }

    /*
     * Find the closet preceding node given the id of the node
     *
     * Pseudo Code:
     *
     * // search locally for the highest predecessor of id
     * procedure closestPrecedingNode(id) {
     *   for i = m downto 1 do {
     *     if (finger[i] in (n, id)) then
     *       return finger[i]
     *   }
     *   return n
     * }
     */
    private IChordNode closetPrecedingNode(IKey id) {
        for (int i = KeyImpl.KEY_LENGTH; i > 0; i--) {
            try {
                if (fingers[i] != null && fingers[i].isActive()) {
                    if (fingers[i].getId().isBetween(getId(), id.decreaseOne())) {
                        return fingers[i];
                    }
                } else {
                    fingers[i] = null;
                }
            } catch (RemoteException ex) {
                // Fingers i is now dead, set it to null and wait fix fingers to fix it
                fingers[i] = null;
            }
        }
        return this;
    }

    @Override
    public void put(String key, String value) throws RemoteException {
        try {
            IKey id = HashUtil.hash(key.getBytes(), HashUtil.SHA_ALGO, KeyImpl.KEY_MAX);
            IChordNode nodeToStore = findSuccessor(id);
            nodeToStore.store(key, value);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @Override
    public String get(String key) throws RemoteException {
        IChordNode nodeToFetch = null;
        try {
            IKey id = HashUtil.hash(key.getBytes(), HashUtil.SHA_ALGO, KeyImpl.KEY_MAX);
            nodeToFetch = findSuccessor(id);
            return nodeToFetch.retrieve(key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * @see id2212.chord.node.IChordNode#notify(id2212.chord.node.IChordNode)
     * 
     * Update predecessor
     * 
     * Core Pseudo Code:
     * 
     * // When receiving notify(p) at n:
     * if (pred = nil or p in (pred, n]) then set pred := p
     */
    @Override
    public void notify(IChordNode node) throws RemoteException {
        notify(node, false);
    }

    @Override
    public void notify(IChordNode notifyingNode, boolean isNewNode) throws RemoteException {
        // if is new node, we may need to transfer key - value pairs to the new node
        // now the state is:
        // predecessor  ------> this <-|
        //    ^                  |     |
        //    |-------------------     |
        //                             |
        //            new node ---------
        if (isNewNode) {
            // transfer some key-value pairs to the new predecessor
            // whose id in (predecessor.predecessor, predecessor]
            Iterator<Map.Entry<String, String>> entryIterator = hashTable.entrySet().iterator();
            while (entryIterator.hasNext()) {
                Map.Entry<String, String> entry = entryIterator.next();
                String key = entry.getKey();
                String value = entry.getValue();
                IKey id = null;
                try {
                    id = HashUtil.hash(key.getBytes(), HashUtil.SHA_ALGO, KeyImpl.KEY_MAX);
                } catch (NoSuchAlgorithmException e) {
                    // Do nothing
                    e.printStackTrace();
                }
                // if id(key) in (predecessor, new node], transfer the key/value pair to new node
                if (id.isBetween(predecessor.getId(), notifyingNode.getId())) {
                    notifyingNode.store(key, value);
                    entryIterator.remove();
                }
            }
        }

        // the id of the node that notifies FROM (not to!)
        IKey nodeId = notifyingNode.getId();

        // set predecessor to null if it is not alive
        checkPredecessor();

        if (predecessor == null || nodeId.isBetween(predecessor.getId(), getId())) {
            System.out.println("now " + nodeId.toDecString() + " => " + getId().toDecString());

            predecessor = notifyingNode;
            this.printInfo();
        }
    }

    @Override
    public List<IChordNode> getSuccessorList() throws RemoteException {
        return this.successorList;
    }

    // Set predecessor and successor to itself
    private void createGroup() {
        this.predecessor = this;
        this.setSuccessor(this);
    }

    // Make knownPeer to find the successor of the node
    private void joinGroup(IChordNode knownPeer) throws RemoteException {
        setSuccessor(knownPeer.findSuccessor(chordId));
        getSuccessor().notify(this, true);
        updateSuccessorList(getSuccessor().getSuccessorList());
    }

    public void leaveGroup() {
        System.out.println("Node " + getId().toDecString() + " left");

        this.setIsActive(false);
        this.predecessor = null;
        this.successorList.clear();
        this.hashTable.clear();
        scheduler.shutdown();
    }

    /*
     *  Periodic stabilization is used to make pointers eventually correct.
     *  1. Try pointing successor to closest alive successor.
     *  2. Try pointing predecessor to closest alive predecessor.
     */
    private void stablize() {
        try {
            // If successor is not alive anymore, replace it by one in the successor list
            if (!getSuccessor().isActive()) {
                if (replaceDeadSuccessor()) {
                    updateSuccessorList(getSuccessor().getSuccessorList());
                } else {
                    // Change state to dead now
                    setIsActive(false);
                }
            }

            /*
             *  Core pseudo code of this part:
             *
             *  v := succ.pred
             *  if (v != nil and v in (n,succ]) then
             *      set succ := v
             *  send a notify(n) to succ
             */
            // Successor is alive, get its predecessor
            IChordNode v = getSuccessor().getPredecessor();
            // Check validity of successor's predecessor
            if (v != null && v.isActive()) {
                // Check if v's id is between node's id and successor's id,
                // if yes, we need to change successor and notify it
                IKey vId = v.getId();
                if (vId.isBetween(getId(), getSuccessor().getId())) {
                    setSuccessor(v);
                    getSuccessor().notify(this);
                    updateSuccessorList(getSuccessor().getSuccessorList());

                    // print debug info
                    printInfo();
                }
            }
        } catch (RemoteException ex) {
            // Could not connect to successor, do the replacement
            if (replaceDeadSuccessor()) {
                try {
                    updateSuccessorList(getSuccessor().getSuccessorList());
                } catch (RemoteException ex1) {
                    Logger.getLogger(ChordNodeImpl.class.getName()).log(Level.SEVERE, null, ex1);
                }
            } else {
                // Change state to dead now
                setIsActive(false);
            }
        }
    }

    // Periodically refresh finger table entries, and store the index of the next finger to fix.
    private void fixFingers() throws RemoteException {
        nextFinger += 1;
        if (nextFinger > KeyImpl.KEY_LENGTH) {
            nextFinger = 1;
        }

        BigInteger step = KeyImpl.LOG_BASE.pow(nextFinger - 1);
        fingers[nextFinger] = this.findSuccessor(getId().increase(step));

        this.printInfo(); // print debug info
    }

    private void setSuccessor(IChordNode _successor) {
        if (this.successorList.size() > 0)
            this.successorList.set(0, _successor);
        else
            this.successorList.add(_successor);
        fingers[1] = getSuccessor();
    }

    private void updateSuccessorList(List<IChordNode> _newList) {
        successorList.clear();
        successorList.add(fingers[1]);
        successorList.addAll(_newList);
        if (successorList.size() > r) {
            for (int i = r; i < successorList.size(); i++) {
                successorList.remove(i);
            }
        }
    }

    // for debugging
    private void printInfo() throws RemoteException {
        System.out.println("---------------Node " + chordId.toDecString() + " --------------");
        System.out.println("Predecessor: "
                + (predecessor == null ? "null" : predecessor.getId().toDecString()));
        System.out.print("Successor: ");
        for (IChordNode node : getSuccessorList())
            System.out.print((node == null ? "null" : node.getId().toDecString()) + " ");
        System.out.println(" ");
        System.out.println("Finger tables: ");
        for (int i = 1; i <= KeyImpl.KEY_LENGTH; i++) {
            System.out.println("Finger " + i + ":"
                    + (fingers[i] == null ? "null" : fingers[i].getId().toDecString()));
        }
        System.out.println("-------------------------------------");
    }

    // check if predecessor live, if not, set it to null
    private void checkPredecessor() {
        try {
            if (predecessor != null && !predecessor.isActive()) {
                System.out.println("Predecessor of node " + chordId.toDecString() + " fails.");
                predecessor = null;
            }
        } catch (RemoteException e) {
            System.out.println("Predecessor of node " + chordId.toDecString() + " fails.");
            predecessor = null;
        }
    }

    @Override
    public boolean isActive() throws RemoteException {
        return isActive;
    }

    private void setIsActive(boolean _isActive) {
        isActive = _isActive;
    }

    // find a new live successor to replace the current dead successor
    private boolean replaceDeadSuccessor() {
        // In panic now, checking all successor in the list first
        for (IChordNode node : successorList) {
            try {
                // If there is one alive, replace the successor to this and notify the successor
                if (node.isActive()) {
                    this.setSuccessor(node);
                    getSuccessor().notify(this);
                    return true;
                }
            } catch (RemoteException ex) {
                // Nothing bad happened, just check if successor is alive
            }
        }

        // there is no live successor in the successor list
        return false;
    }

    @Override
    public String getRmiUrl() throws RemoteException {
        return rmiURL;
    }

    @Override
    public IChordNode[] getFingers() throws RemoteException {
        return fingers;
    }

    /**
     * Remove this node
     */
    @Override
    public void remove() throws RemoteException {
        // transfer key-value pairs to its successor
        for (Map.Entry<String, String> entry : hashTable.entrySet()) {
            getSuccessor().store(entry.getKey(), entry.getValue());
        }
        // leave the group
        leaveGroup();
    }

    @Override
    public void store(String key, String value) throws RemoteException {
        hashTable.put(key, value);
    }

    @Override
    public String retrieve(String key) throws RemoteException {
        return hashTable.get(key);
    }

    @Override
    public Map<String, String> getLocalHashTable() throws RemoteException {
        return hashTable;
    }

}
