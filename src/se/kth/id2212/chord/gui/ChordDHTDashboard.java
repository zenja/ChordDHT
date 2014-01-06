package se.kth.id2212.chord.gui;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import se.kth.id2212.chord.dht.HashUtil;
import se.kth.id2212.chord.dht.IKey;
import se.kth.id2212.chord.dht.KeyImpl;
import se.kth.id2212.chord.node.ChordNodeLauncher;
import se.kth.id2212.chord.node.ChordNodeLauncher.NoActiveNodeException;
import se.kth.id2212.chord.node.IChordNode;

public class ChordDHTDashboard extends JFrame {

    private static final long serialVersionUID = -6763636103536477576L;

    private static final String CHORD = "Chord";

    private JPanel contentPane;
    private JList listNodes;
    private JList listFingers;
    private JList listSuccessors;
    private JLabel lblNodeDetails;
    private JLabel lblPredecessor;
    private JLabel lblSuccessor;
    private JLabel lblPredecessorContent;
    private JLabel lblSuccessorContent;
    private JLabel lblIsActiveContent;
    private JButton btnNodeLeave;
    private JButton btnNewNode;
    private JLabel lblFingerTable;
    private JLabel lblSuccessorList;
    private JScrollPane scrollPane_1;
    private JScrollPane scrollPane_2;
    private JTextField textFieldKey;
    private JTextField textFieldValue;
    private JButton btnRefreshNodes;
    private JLabel lblRmiUri;
    private JList listKeyValue;
    private JTextField txtRmiUriContent;

    /**
     * Launch the application.
     * @throws RemoteException
     */
    public static void main(String[] args) throws RemoteException {
        // rmi registration
        LocateRegistry.createRegistry(12345);

        // create 6 nodes at first
        ChordNodeLauncher root = new ChordNodeLauncher(CHORD + '0', null);
        try {
            root = new ChordNodeLauncher(CHORD + '1', root.getNode());
            root = new ChordNodeLauncher(CHORD + '2', root.getNode());
            root = new ChordNodeLauncher(CHORD + '3', root.getNode());
            root = new ChordNodeLauncher(CHORD + '4', root.getNode());
            root = new ChordNodeLauncher(CHORD + '5', root.getNode());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // invoke GUI frame
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    ChordDHTDashboard frame = new ChordDHTDashboard();
                    frame.setVisible(true);

                    frame.updateNodeList();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public ChordDHTDashboard() {
        setResizable(false);
        setTitle("DHT Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 480, 570);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        JLabel lblAvailableNodes = new JLabel("Available nodes:");
        lblAvailableNodes.setBounds(35, 34, 131, 16);
        contentPane.add(lblAvailableNodes);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(35, 62, 131, 101);
        contentPane.add(scrollPane);

        listNodes = new JList();
        listNodes.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                try {
                    int index = listNodes.getSelectedIndex();
                    if (index >= 0) {
                        boolean isDead = ((String) listNodes.getSelectedValue()).toLowerCase()
                                .contains("dead");
                        if (!isDead) {
                            IChordNode selectedNode = ChordNodeLauncher.getLaunchedNodes().get(index);
                            lblNodeDetails.setText("Node #" + selectedNode.getId().toDecString()
                                    + " details");

                            IChordNode predecessor = selectedNode.getPredecessor();
                            if (predecessor != null) {
                                lblPredecessorContent.setText(selectedNode.getPredecessor().getId().toDecString());
                            } else {
                                lblPredecessorContent.setText("null");
                            }

                            IChordNode successor = selectedNode.getSuccessor();
                            if (successor != null) {
                                lblSuccessorContent.setText(selectedNode.getSuccessor().getId().toDecString());
                            } else {
                                lblSuccessorContent.setText("null");
                            }

                            txtRmiUriContent.setText(selectedNode.getRmiUrl());

                            if (selectedNode.isActive()) {
                                lblIsActiveContent.setText("True");
                            } else {
                                lblIsActiveContent.setText("False");
                            }
                        } else {
                            lblPredecessorContent.setText("--");
                            lblSuccessorContent.setText("--");
                            txtRmiUriContent.setText("--");
                            lblIsActiveContent.setText("False");
                        }
                    }

                    updateFingerList();
                    updateSuccessorList();
                    updateKeyValueList();
                } catch (RemoteException e) {
                    e.printStackTrace();
                    alertInfo("RemoteException", "in valueChanged(ListSelectionEvent event)");
                }
            }

            private void updateSuccessorList() throws RemoteException {
                DefaultListModel listModelSuccessors = new DefaultListModel();
                int selectedIndex = listNodes.getSelectedIndex();
                if (selectedIndex >= 0) {
                    IChordNode selectedNode = ChordNodeLauncher.getLaunchedNodes().get(selectedIndex);
                    for (IChordNode node : selectedNode.getSuccessorList()) {
                        listModelSuccessors.addElement((node == null ? "null" : node.getId()
                                .toDecString()) + " ");
                    }
                }
                listSuccessors.setModel(listModelSuccessors);
            }

            private void updateFingerList() throws RemoteException {
                DefaultListModel listModelFingers = new DefaultListModel();
                int selectedIndex = listNodes.getSelectedIndex();
                if (selectedIndex >= 0) {
                    IChordNode selectedNode = ChordNodeLauncher.getLaunchedNodes().get(
                            selectedIndex);
                    IChordNode[] fingers = selectedNode.getFingers();
                    for (int i = 1; i <= KeyImpl.KEY_LENGTH; i++) {
                        listModelFingers.addElement("Finger " + i + ":"
                                + (fingers[i] == null ? "null" : fingers[i].getId().toDecString()));
                    }
                }
                listFingers.setModel(listModelFingers);
            }

            private void updateKeyValueList() throws RemoteException {
                DefaultListModel listModelKeyValues = new DefaultListModel();
                int selectedIndex = listNodes.getSelectedIndex();
                if (selectedIndex == -1) {
                    return;
                }
                IChordNode selectedNode = ChordNodeLauncher.getLaunchedNodes().get(selectedIndex);
                Map<String, String> hashTable = selectedNode.getLocalHashTable();
                for (Map.Entry<String, String> entry : hashTable.entrySet()) {
                    IKey hashKey = null;
                    try {
                        hashKey = HashUtil.hash(entry.getKey().getBytes(), HashUtil.SHA_ALGO,
                                KeyImpl.KEY_MAX);
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                        alertInfo("NoSuchAlgorithmException",
                                "in valueChanged(ListSelectionEvent event)");
                    }
                    listModelKeyValues.addElement(entry.getKey() + "(" + hashKey.toDecString()
                            + "): " + entry.getValue());
                }
                listKeyValue.setModel(listModelKeyValues);
            }
        });
        scrollPane.setViewportView(listNodes);

        lblNodeDetails = new JLabel("Node Details:");
        lblNodeDetails.setBounds(210, 34, 116, 16);
        contentPane.add(lblNodeDetails);

        lblPredecessor = new JLabel("Predecessor:");
        lblPredecessor.setBounds(210, 62, 89, 16);
        contentPane.add(lblPredecessor);

        lblSuccessor = new JLabel("Successor: ");
        lblSuccessor.setBounds(210, 90, 89, 16);
        contentPane.add(lblSuccessor);

        lblPredecessorContent = new JLabel("-");
        lblPredecessorContent.setBounds(311, 62, 122, 16);
        contentPane.add(lblPredecessorContent);

        lblSuccessorContent = new JLabel("-");
        lblSuccessorContent.setBounds(311, 90, 122, 16);
        contentPane.add(lblSuccessorContent);

        JLabel lblIsActive = new JLabel("Is Active:");
        lblIsActive.setBounds(210, 118, 89, 16);
        contentPane.add(lblIsActive);

        lblIsActiveContent = new JLabel("-");
        lblIsActiveContent.setBounds(311, 118, 122, 16);
        contentPane.add(lblIsActiveContent);

        btnNodeLeave = new JButton("Node Leave");
        btnNodeLeave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                int index = listNodes.getSelectedIndex();
                if (index == -1) {
                    alertInfo("Error", "You must select a node first");
                    return;
                }
                IChordNode node = ChordNodeLauncher.getLaunchedNodes().get(index);
                try {
                    node.remove();
                    updateNodeList();
                } catch (RemoteException e) {
                    e.printStackTrace();
                    alertInfo("RemoteException", "node.remove(); or updateNodeList();");
                }
            }
        });
        btnNodeLeave.setBounds(209, 203, 117, 29);
        contentPane.add(btnNodeLeave);

        btnNewNode = new JButton("New Node");
        btnNewNode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    new ChordNodeLauncher(CHORD + ChordNodeLauncher.getLaunchedNodes().size(),
                            ChordNodeLauncher.getFirstActiveNode());
                    updateNodeList();
                } catch (NoActiveNodeException e) {
                    e.printStackTrace();
                    alertInfo("NoActiveNodeException", "ChordNodeLauncher.getFirstActiveNode()");
                } catch (RemoteException e) {
                    e.printStackTrace();
                    alertInfo("RemoteException", "updateNodeList();");
                }
            }
        });
        btnNewNode.setBounds(339, 203, 117, 29);
        contentPane.add(btnNewNode);

        lblFingerTable = new JLabel("Finger Table:");
        lblFingerTable.setBounds(35, 244, 131, 16);
        contentPane.add(lblFingerTable);

        lblSuccessorList = new JLabel("Successor List:");
        lblSuccessorList.setBounds(210, 244, 131, 16);
        contentPane.add(lblSuccessorList);

        scrollPane_1 = new JScrollPane();
        scrollPane_1.setBounds(35, 274, 131, 82);
        contentPane.add(scrollPane_1);

        listFingers = new JList();
        scrollPane_1.setViewportView(listFingers);

        scrollPane_2 = new JScrollPane();
        scrollPane_2.setBounds(210, 274, 131, 82);
        contentPane.add(scrollPane_2);

        listSuccessors = new JList();
        scrollPane_2.setViewportView(listSuccessors);

        JLabel lblKeyValues = new JLabel("Key/Values in this node:");
        lblKeyValues.setBounds(35, 379, 176, 16);
        contentPane.add(lblKeyValues);

        JScrollPane scrollPane_3 = new JScrollPane();
        scrollPane_3.setBounds(35, 407, 306, 61);
        contentPane.add(scrollPane_3);

        listKeyValue = new JList();
        scrollPane_3.setViewportView(listKeyValue);

        JButton btnRefreshKeyValue = new JButton("Refresh");
        btnRefreshKeyValue.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        btnRefreshKeyValue.setBounds(353, 439, 117, 29);
        contentPane.add(btnRefreshKeyValue);

        JButton btnPut = new JButton("Put");
        btnPut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                String key = textFieldKey.getText();
                String value = textFieldValue.getText();
                if (key.isEmpty() || value.isEmpty()) {
                    alertInfo("Invalid Input", "Key and value should not be null!");
                    return;
                }
                try {
                    ChordNodeLauncher.getLaunchedNodes().get(0).put(key, value);
                    alertInfo(
                            "Info",
                            "Stored. The id (hash) of the key is: "
                                    + HashUtil.hash(key.getBytes(), HashUtil.SHA_ALGO,
                                            KeyImpl.KEY_MAX).toDecString());
                } catch (RemoteException e) {
                    e.printStackTrace();
                    alertInfo("RemoteException",
                            "ChordNodeLauncher.getLaunchedNodes().get(0).put(key, value);");
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    alertInfo("NoSuchAlgorithmException", "HashUtil.hash(...);");
                }
            }
        });
        btnPut.setBounds(234, 513, 105, 29);
        contentPane.add(btnPut);

        JLabel lblKey = new JLabel("Key:");
        lblKey.setBounds(33, 485, 39, 16);
        contentPane.add(lblKey);

        textFieldKey = new JTextField();
        textFieldKey.setBounds(73, 480, 99, 28);
        contentPane.add(textFieldKey);
        textFieldKey.setColumns(10);

        JLabel lblValue = new JLabel("Value:");
        lblValue.setBounds(184, 485, 50, 16);
        contentPane.add(lblValue);

        textFieldValue = new JTextField();
        textFieldValue.setBounds(234, 479, 107, 28);
        contentPane.add(textFieldValue);
        textFieldValue.setColumns(10);

        JButton btnNewButton = new JButton("Get");
        btnNewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                String key = textFieldKey.getText();
                if (key.isEmpty()) {
                    alertInfo("Invalid Input", "Key should not be null!");
                    return;
                }
                try {
                    String value = ChordNodeLauncher.getFirstActiveNode().get(key);
                    alertInfo("Result", "The value is: " + value);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    alertInfo("RemoteException", "ChordNodeLauncher.getFirstActiveNode().get(key);");
                } catch (NoActiveNodeException e) {
                    e.printStackTrace();
                    alertInfo("NoActiveNodeException",
                            "ChordNodeLauncher.getFirstActiveNode().get(key);");
                }
            }
        });
        btnNewButton.setBounds(73, 513, 99, 29);
        contentPane.add(btnNewButton);

        btnRefreshNodes = new JButton("Refresh");
        btnRefreshNodes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    updateNodeList();
                } catch (RemoteException ex) {
                    alertInfo("RemoteException", "updateNodeList()");
                    ex.printStackTrace();
                }
            }
        });
        btnRefreshNodes.setBounds(35, 203, 117, 29);
        contentPane.add(btnRefreshNodes);

        lblRmiUri = new JLabel("RMI URI:");
        lblRmiUri.setBounds(35, 175, 89, 16);
        contentPane.add(lblRmiUri);

        txtRmiUriContent = new JTextField();
        txtRmiUriContent.setEditable(false);
        txtRmiUriContent.setBounds(103, 169, 353, 28);
        contentPane.add(txtRmiUriContent);
        txtRmiUriContent.setColumns(10);
    }

    public void updateNodeList() throws RemoteException {
        DefaultListModel listModel = new DefaultListModel();
        for (IChordNode node : ChordNodeLauncher.getLaunchedNodes()) {
            if (node.isActive()) {
                listModel.addElement(node.getId().toDecString());
            } else {
                listModel.addElement(node.getId().toDecString() + " (Dead)");
            }
        }
        listNodes.setModel(listModel);
    }

    public synchronized void alertInfo(final String title, final String content)   {
        final JFrame frame = this;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(frame, content, title,
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }
}
