package Project.Common;

/**
 * For chatroom projects, you can call this "User"
 */
public class Player {
private boolean isAway;

    public boolean isAway() {
        return isAway;
    }

    public void setAway(boolean isAway) {
        this.isAway = isAway;
    }

    private boolean isReady;

public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int changePoints(int points) {
        this.points += points;
        return this.points;
    }

    private int points;


    public boolean isReady() {
        return isReady;
    }

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

    String lastChoice;

    public String getPreviousChoice() {
        return lastChoice;
    }

    public void setPreviousChoice(String lastChoice) {
        this.lastChoice = lastChoice;
    }
}

