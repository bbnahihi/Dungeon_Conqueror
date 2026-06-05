package game.system;

import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

public class Sound {

    public int volumeScale = 3; 
    URL soundURL[] = new URL[30];
    
    // Five clips per sound let effects overlap.
    Clip[][] clipArray = new Clip[30][5]; 
    int currentSoundId;

    public Sound() {
        soundURL[0] = getClass().getResource("/res/music/level1_forest.wav");
        soundURL[1] = getClass().getResource("/res/gun_shoot.wav");   
        soundURL[2] = getClass().getResource("/res/sword_swing.wav"); 
        soundURL[3] = getClass().getResource("/res/gun_ulti.wav");    
        soundURL[4] = getClass().getResource("/res/sword_ulti.wav");  
        soundURL[5] = getClass().getResource("/res/gameover.wav");
        soundURL[6] = getClass().getResource("/res/music/lobby.wav");
        soundURL[7] = getClass().getResource("/res/win.wav");
        soundURL[8] = getClass().getResource("/res/music/boss.wav");
        soundURL[9] = getClass().getResource("/res/music/level2_ice.wav");
        soundURL[10] = getClass().getResource("/res/music/level3_desert.wav");
        
        for(int i = 0; i < soundURL.length; i++) {
            if(soundURL[i] != null) {
                try {
                    for (int j = 0; j < 5; j++) {
                        AudioInputStream ais = AudioSystem.getAudioInputStream(soundURL[i]);
                        clipArray[i][j] = AudioSystem.getClip();
                        clipArray[i][j].open(ais);
                    }
                } catch (Exception e) {
                    System.out.println("ERROR LOADING SOUND ID: " + i);
                    e.printStackTrace();
                }
            }
        }
    }

    public void setFile(int i) {
        currentSoundId = i;
    }

    public void play() {
        // Use the first free channel.
        for (int j = 0; j < 5; j++) {
            if (clipArray[currentSoundId][j] != null && !clipArray[currentSoundId][j].isRunning()) {
                clipArray[currentSoundId][j].setFramePosition(0);
                clipArray[currentSoundId][j].start();
                return;
            }
        }
        
        // If all channels are busy, reuse channel 0.
        if (clipArray[currentSoundId][0] != null) {
            clipArray[currentSoundId][0].setFramePosition(0);
            clipArray[currentSoundId][0].start();
        }
    }

    public void loop() {
        if(clipArray[currentSoundId][0] != null) {
            clipArray[currentSoundId][0].loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public void stop() {
        // Stop every channel for this sound.
        for (int j = 0; j < 5; j++) {
            if (clipArray[currentSoundId][j] != null) {
                clipArray[currentSoundId][j].stop();
            }
        }
    }

    public void setVolume(float volume) {
        // Keep all channels at the same volume.
        for (int j = 0; j < 5; j++) {
            if(clipArray[currentSoundId][j] != null) {
                try {
                    FloatControl gainControl = (FloatControl) clipArray[currentSoundId][j].getControl(FloatControl.Type.MASTER_GAIN);
                    gainControl.setValue(volume);
                } catch (IllegalArgumentException e) {
                }
            }
        }
    }

    public void pause() {
        if(clipArray[currentSoundId][0] != null) {
            clipArray[currentSoundId][0].stop(); 
        }
    }

    public void resume() {
        if(clipArray[currentSoundId][0] != null) {
            clipArray[currentSoundId][0].start();
        }
    }
}
