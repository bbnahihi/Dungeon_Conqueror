package game.system;

public class StoryManager {

    public enum StoryMoment {
        INTRO,
        PRE_BOSS,
        ENDING
    }

    public enum StoryAction {
        START_RUN,
        START_BOSS,
        SHOW_WIN
    }

    private StoryMoment currentMoment;
    private StoryAction currentAction;
    private boolean preBossShown;

    public void begin(StoryMoment moment, StoryAction action) {
        currentMoment = moment;
        currentAction = action;
        if (moment == StoryMoment.PRE_BOSS) {
            preBossShown = true;
        }
    }

    public StoryAction finishCurrentStory() {
        StoryAction action = currentAction;
        currentMoment = null;
        currentAction = null;
        return action;
    }

    public boolean isActive() {
        return currentMoment != null;
    }

    public boolean shouldShowPreBossStory() {
        return preBossShown == false;
    }

    public void resetRunFlags() {
        preBossShown = false;
        currentMoment = null;
        currentAction = null;
    }

    public StoryMoment getCurrentMoment() {
        return currentMoment;
    }

    public String getTitle() {
        if (currentMoment == StoryMoment.INTRO) return "Into the Rift";
        if (currentMoment == StoryMoment.PRE_BOSS) return "The Abyssal Rose";
        if (currentMoment == StoryMoment.ENDING) return "When the Rose Falls";
        return "";
    }

    public String[] getLines() {
        if (currentMoment == StoryMoment.INTRO) {
            return new String[] {
                    "Years ago, the Rift swallowed kingdoms, villages, and the girl the hero once loved.",
                    "Her name was Seraphine.",
                    "The gods sent him into the abyss to save what remained of humanity.",
                    "But his heart carried another wish: to find her, and bring her home.",
                    "Beyond the gate, however, love and duty may no longer lead to the same ending."
            };
        }

        if (currentMoment == StoryMoment.PRE_BOSS) {
            return new String[] {
                    "At the heart of the Rift, the air turns heavy and still.",
                    "Before the shattered throne stands Seraphine, the Abyssal Rose - once the one he loved, now the sovereign of the Void.",
                    "Wrapped in grief, rage, and impossible power, she rules the darkness that nearly swallowed the world.",
                    "",
                    "Seraphine: \"If this world could only offer pain... then let it be consumed by the abyss.\"",
                    "Hero: \"Seraphine... I came to save you.\"",
                    "Seraphine: \"Then face what your absence made of me.\""
            };
        }

        if (currentMoment == StoryMoment.ENDING) {
            return new String[] {
                    "As Seraphine falls, her crown breaks into violet ash, drawn back into the Rift.",
                    "The abyss gives no miracle, no final return - only the silence of someone already lost.",
                    "The hero lowers his weapon as the gate collapses. The world is saved, but Seraphine is gone.",
                    "Victory comes, but it does not feel like triumph."
            };
        }

        return new String[0];
    }
}
