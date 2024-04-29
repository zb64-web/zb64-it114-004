package Project.Common;

/**
 * For chatroom projects, you can call this "User"
 */
public class Player {
    private boolean isReady;


    public boolean isReady() {
        return isReady;
    }

    private String choice;
    private boolean isRemoved;


    public String getChoice() {
        return choice;
    }

    public void setChoice(String choice) {
        this.choice = choice;
    }
    //zb64 4/10/24

    public boolean getRemoved() {
        return isRemoved;
    }

    public void setRemoved(boolean isRemoved) {
        this.isRemoved = isRemoved;
    }

    //zb64 4/18/24

    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    private boolean takenTurn;

    public boolean didTakeTurn() {
        return takenTurn;
    }

    public void setTakenTurn(boolean takenTurn) {
        this.takenTurn = takenTurn;
    }

    private boolean isMyTurn;

    public boolean isMyTurn() {
        return isMyTurn;
    }

    public void setMyTurn(boolean isMyTurn) {
        this.isMyTurn = isMyTurn;
    }

    public long getClientId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getClientId'");
    }

}
