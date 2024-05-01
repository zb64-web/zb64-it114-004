package Project.Client.Views;

import java.awt.CardLayout;
import java.awt.Component;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JPanel;

import Project.Client.CardView;
import Project.Client.Client;
import Project.Client.ICardControls;
import Project.Client.IGameEvents;
import Project.Common.Constants;
import Project.Common.Phase;

public class GamePanel extends JPanel implements IGameEvents {
    private JPanel gridPanel;
    private CardLayout cardLayout;

    private final List<String> moves = new ArrayList<>();

    public GamePanel(ICardControls controls) {
        super(new CardLayout());
        cardLayout = (CardLayout) this.getLayout();
        this.setName(CardView.GAME_SCREEN.name());
        Client.INSTANCE.addCallback(this);

        createReadyPanel();
        gridPanel = new JPanel();
        gridPanel.setName("GRID");
        //zb64 4/30/24
        JButton rButton = new JButton();
        rButton.setText("Rock");
        rButton.addActionListener(l -> {
            try {
                Client.INSTANCE.sendTakeTurn("R");
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        });
        gridPanel.add(rButton);
        this.add(gridPanel);
        JButton pButton = new JButton();
        pButton.setText("Paper");
        pButton.addActionListener(l -> {
            try {
                Client.INSTANCE.sendTakeTurn("P");
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        });
        gridPanel.add(pButton);
        this.add(gridPanel);
        JButton sButton = new JButton();
        sButton.setText("Scissors");
        sButton.addActionListener(l -> {
            try {
                Client.INSTANCE.sendTakeTurn("S");
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        });
        gridPanel.add(sButton);
        this.add(gridPanel);
        JButton skipButton = new JButton();
        skipButton.setText("Skio");
        skipButton.addActionListener(l -> {
            try {
                Client.INSTANCE.sendTakeTurn("skip");
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        });
        gridPanel.add(skipButton);
        this.add(gridPanel);
        JButton lButton = new JButton();
        lButton.setText("Lizard");
        lButton.addActionListener(l -> {
            try {
                Client.INSTANCE.sendTakeTurn("lizard");
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        });
        gridPanel.add(lButton);
        this.add(gridPanel);
        JButton spockButton = new JButton();
        spockButton.setText("Spock");
        spockButton.addActionListener(l -> {
            try {
                Client.INSTANCE.sendTakeTurn("spock");
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        });
        gridPanel.add(spockButton);
        this.add(gridPanel);
        JButton fButton = new JButton();
        fButton.setText("Fire");
        fButton.addActionListener(l -> {
            try {
                Client.INSTANCE.sendTakeTurn("fire");
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        });
        gridPanel.add(fButton);
        this.add(gridPanel);
        add("GRID", gridPanel);
        setVisible(false);
        // don't need to add this to ClientUI as this isn't a primary panel(it's nested
        // in ChatGamePanel)
        // controls.addPanel(Card.GAME_SCREEN.name(), this);
    }

    private void createReadyPanel() {
        JPanel readyPanel = new JPanel();
        JButton readyButton = new JButton();
        readyButton.setText("Ready");
        readyButton.addActionListener(l -> {
            try {
                Client.INSTANCE.sendReadyCheck();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        readyPanel.add(readyButton);
        this.add(readyPanel);
    }

    /*private void resetView() {
        if (gridPanel == null) {
            return;
        }
        if (gridPanel.getLayout() != null) {
            gridPanel.setLayout(null);
        }
        cells = null;
        gridPanel.removeAll();
        gridPanel.revalidate();
        gridPanel.repaint();
    }*/

   /*  private void makeGrid(int rows, int columns) {
        resetView();
        cells = new CellPanel[rows][columns];
        gridPanel.setLayout(new GridLayout(rows, columns));
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                cells[i][j] = new CellPanel();
                cells[i][j].setType(CellType.NONE, i, j);
                gridPanel.add(cells[i][j]);
            }
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    } */

    @Override
    public void onClientConnect(long id, String clientName, String message) {
    }

    @Override
    public void onClientDisconnect(long id, String clientName, String message) {
    }

    @Override
    public void onMessageReceive(long id, String message) {
    }

    @Override
    public void onReceiveClientId(long id) {
    }

    @Override
    public void onSyncClient(long id, String clientName) {
    }

    @Override
    public void onResetUserList() {
    }

    @Override
    public void onRoomJoin(String roomName) {
        if (Constants.LOBBY.equals(roomName)) {
            setVisible(false);// TODO along the way to hide game view when you leave
        }
    }

    @Override
    public void onReceiveRoomList(List<String> rooms, String message) {

    }

    @Override
    public void onReceivePhase(Phase phase) {
        // I'll temporarily do next(), but there may be scenarios where the screen can
        // be inaccurate
        System.out.println("Received phase: " + phase.name());
        if (phase == Phase.READY) {
            if (!isVisible()) {
                setVisible(true);
                this.getParent().revalidate();
                this.getParent().repaint();
                System.out.println("GamePanel visible");
            } else {
                cardLayout.next(this);
            }
        } else if (phase == Phase.TURN) {
            cardLayout.show(this, "GRID");
        }
    }

    @Override
    public void onReceiveReady(long clientId, boolean isReady){

    }

   /*  @Override
     public void onReceiveCell(List<CellData> cells) {
        for (CellData c : cells) {
            CellPanel target = this.cells[c.getX()][c.getY()];
            target.setType(c.getCellType(), c.getX(), c.getY());
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    @Override
    public void onReceiveGrid(int rows, int columns) {
        resetView();
        if (rows > 0 && columns > 0) {
            makeGrid(rows, columns);
        }
    } */


}