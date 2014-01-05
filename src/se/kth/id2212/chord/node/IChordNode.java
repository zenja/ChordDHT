package se.kth.id2212.chord.node;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import se.kth.id2212.chord.dht.IKey;

public interface IChordNode extends Remote {

    public IChordNode findSuccessor(IKey id) throws RemoteException;

    public void put(IKey id, String value) throws RemoteException;

    public String get(IKey id) throws RemoteException;

    public void notify(IChordNode node) throws RemoteException;

    public IChordNode getPredecessor() throws RemoteException;

    public IChordNode getSuccessor() throws RemoteException;

    public List<IChordNode> getSuccessorList() throws RemoteException;

    public boolean isActive() throws RemoteException;

    public IKey getId() throws RemoteException;
}
