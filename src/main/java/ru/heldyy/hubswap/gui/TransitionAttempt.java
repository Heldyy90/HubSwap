package ru.heldyy.hubswap.gui;

public class TransitionAttempt {
    public enum Stage {
        WAITING_HUB,
        OPENING_MENU,
        CLICKING,
        CONFIRM_TRANSFER
    }

    private final TransitionMode mode;
    private final int targetNumber;
    private final long startedAt;
    private final int hubDelay;
    private final int clickDelay;
    private final int confirmDelay;

    private boolean finished;
    private boolean hubSeen;
    private boolean targetClicked;
    private Stage stage;
    private long stageStartedAt;
    private long lastClickAt;

    public TransitionAttempt(TransitionMode mode, int targetNumber, int hubDelay, int clickDelay, int confirmDelay) {
        this.mode = mode;
        this.targetNumber = targetNumber;
        this.startedAt = System.currentTimeMillis();
        this.hubDelay = hubDelay;
        this.clickDelay = clickDelay;
        this.confirmDelay = confirmDelay;
        this.finished = false;
        this.hubSeen = false;
        this.targetClicked = false;
        this.stage = Stage.WAITING_HUB;
        this.stageStartedAt = this.startedAt;
        this.lastClickAt = 0L;
    }

    public TransitionMode getMode() {
        return mode;
    }

    public int getTargetNumber() {
        return targetNumber;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public int getHubDelay() {
        return hubDelay;
    }

    public int getClickDelay() {
        return clickDelay;
    }

    public int getConfirmDelay() {
        return confirmDelay;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean hasHubBeenSeen() {
        return hubSeen;
    }

    public void markHubSeen() {
        this.hubSeen = true;
    }

    public Stage getStage() {
        return stage;
    }

    public long getStageStartedAt() {
        return stageStartedAt;
    }

    public boolean hasTargetClicked() {
        return targetClicked;
    }

    public long getLastClickAt() {
        return lastClickAt;
    }

    public void setStage(Stage newStage) {
        if (newStage == null || this.stage == newStage) {
            return;
        }

        this.stage = newStage;
        this.stageStartedAt = System.currentTimeMillis();
    }

    public void markTargetClicked() {
        this.targetClicked = true;
        this.lastClickAt = System.currentTimeMillis();
        setStage(Stage.CONFIRM_TRANSFER);
    }

    public void finish() {
        this.finished = true;
    }
}
