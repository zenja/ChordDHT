package se.kth.id2212.chord.node;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;

public class ChordNodeLauncher {
    private static final String CHORD = "Chord";
    private static final List<IChordNode> launchedNodes = new ArrayList<IChordNode>();
    private IChordNode node;

    public ChordNodeLauncher(String chordName, IChordNode knownPeer) {
        try {
            String rmiURL = "rmi://localhost:12345/";
            IChordNode chordobj = new ChordNodeImpl(knownPeer, rmiURL, chordName);
            String id = chordobj.getId().toDecString();
            rmiURL = rmiURL + id;
            // Register the newly created object at rmiregistry.
            // LocateRegistry.createRegistry(2020);
            java.rmi.Naming.rebind(rmiURL, chordobj);
            node = chordobj;
            launchedNodes.add(chordobj);

            System.out.println(id + " is ready.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public IChordNode getNode() {
        return this.node;
    }

    public static List<IChordNode> getLaunchedNodes() {
        return ChordNodeLauncher.launchedNodes;
    }

    public static IChordNode getFirstActiveNode() throws NoActiveNodeException {
        for (IChordNode node : launchedNodes) {
            try {
                if (node.isActive()) {
                    return node;
                }
            } catch (RemoteException e) {
                // do nothing, because dead node is permitted
                e.printStackTrace();
            }
        }
        throw new NoActiveNodeException();
    }

    public static class NoActiveNodeException extends Exception {
        private static final long serialVersionUID = -4358323959277205347L;
    }

    public static void main(String[] args) throws RemoteException {
        LocateRegistry.createRegistry(12345);
        ChordNodeLauncher root = new ChordNodeLauncher(CHORD + '1', null);
        // connect to the known peer
        try {
            root = new ChordNodeLauncher(CHORD + '2', root.getNode());
            root = new ChordNodeLauncher(CHORD + '3', root.getNode());
            root = new ChordNodeLauncher(CHORD + '4', root.getNode());
            root = new ChordNodeLauncher(CHORD + '5', root.getNode());
            root = new ChordNodeLauncher(CHORD + '6', root.getNode());

            // Thread.sleep(100000);
            // ((ChordNodeImpl) launchedNodes.get(4)).leaveGroup();

        } catch (Exception e) {
            e.printStackTrace();
            // System.exit(-1);
        }
    }
}
