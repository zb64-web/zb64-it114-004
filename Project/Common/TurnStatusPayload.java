package Project.Common;

public class TurnStatusPayload extends Payload {
    private boolean didTakeTurn;
    private String choice;

    public TurnStatusPayload() {
        setPayloadType(PayloadType.TURN);
    }

    public boolean didTakeTurn() {
        return didTakeTurn;
    }

    public void setDidTakeTurn(boolean didTakeTurn) {
        this.didTakeTurn = didTakeTurn;
    }

    public String pickChoice() {
        return choice;
    }

    public void setChoice(String choice) {
        this.choice = choice;
    }
}

