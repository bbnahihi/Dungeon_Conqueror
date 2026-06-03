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
        if (currentMoment == StoryMoment.INTRO) return "The Rift Opens";
        if (currentMoment == StoryMoment.PRE_BOSS) return "The Deepest Gate";
        if (currentMoment == StoryMoment.ENDING) return "After the Abyss";
        return "";
    }

    public String[] getLines() {
        if (currentMoment == StoryMoment.INTRO) {
            return new String[] {
                    "The world was shattered when dimensional rifts opened across the land.",
                    "Dark creatures poured out, destroying cities, villages, and most of human civilization.",
                    "Blessed by the gods, a lone hero enters the greatest rift to hold back the darkness and buy mankind enough time to rebuild."
            };
        }

        if (currentMoment == StoryMoment.PRE_BOSS) {
            return new String[] {
                    "At the deepest gate of the rift, the hero finally meets the master of the abyss.",
                    "But behind the dark power stands a familiar face - the childhood friend he lost many years ago.",
                    "Her sorrow has become hatred, and the abyss has turned that hatred into power.",
                    "",
                    "Hero: \"It does not have to end like this.\"",
                    "Dark Conqueror: \"You left. The world left me. Now I will return that pain to all of it.\"",
                    "Hero: \"Then I will stop you... even if it costs me everything.\""
            };
        }

        if (currentMoment == StoryMoment.ENDING) {
            return new String[] {
                    "The Dark Conqueror has fallen.",
                    "The rift begins to close, and humanity gains the time it needs to rebuild.",
                    "But deep within the ruins, the memory of the abyss still remains."
            };
        }

        return new String[0];
    }
}
