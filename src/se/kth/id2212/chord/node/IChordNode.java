package se.kth.id2212.chord.node;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import se.kth.id2212.chord.dht.IKey;

public interface IChordNode extends Remote {
    public IChordNode findSuccessor(IKey id) throws RemoteException;

    public void put(String key, String value) throws RemoteException;

    public String get(String key) throws RemoteException;

    public void notify(IChordNode node) throws RemoteException;

    public void notify(IChordNode node, boolean isNewNode) throws RemoteException;

    public IChordNode getPredecessor() throws RemoteException;

    public IChordNode getSuccessor() throws RemoteException;

    public List<IChordNode> getSuccessorList() throws RemoteException;

    public boolean isActive() throws RemoteException;

    public IKey getId() throws RemoteException;

    // remove the node
    public void remove() throws RemoteException;

    // set the key-value pair in local hash table
    public void store(String key, String value) throws RemoteException;

    // get the key-value pair from local hash table
    public String retrieve(String key) throws RemoteException;

    /* Interfaces below are used by GUI dashboard to get the internal info of the node */

    public String getRmiUrl() throws RemoteException;

    public IChordNode[] getFingers() throws RemoteException;

    public Map<String, String> getLocalHashTable() throws RemoteException;
}
