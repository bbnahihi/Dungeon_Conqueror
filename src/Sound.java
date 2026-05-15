import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

public class Sound {

    public int volumeScale = 3; 
    URL soundURL[] = new URL[30];
    
    // ==========================================
    // NÂNG CẤP ĐA ÂM (POLYPHONY): MẢNG 2 CHIỀU
    // Tạo 5 kênh (channels) cho mỗi ID âm thanh để chúng có thể phát chồng lên nhau!
    // ==========================================
    Clip[][] clipArray = new Clip[30][5]; 
    int currentSoundId;

    public Sound() {
        // Đường dẫn file .wav của bạn
        soundURL[0] = getClass().getResource("/res/music.wav");       
        soundURL[1] = getClass().getResource("/res/gun_shoot.wav");   
        soundURL[2] = getClass().getResource("/res/sword_swing.wav"); 
        soundURL[3] = getClass().getResource("/res/gun_ulti.wav");    
        soundURL[4] = getClass().getResource("/res/sword_ulti.wav");  
        soundURL[5] = getClass().getResource("/res/gameover.wav");
        soundURL[6] = getClass().getResource("/res/menu.wav");
        soundURL[7] = getClass().getResource("/res/win.wav");
        soundURL[8] = getClass().getResource("/res/boss_music.wav");
        
        // Vòng lặp nạp RAM siêu tốc
        for(int i = 0; i < soundURL.length; i++) {
            if(soundURL[i] != null) {
                try {
                    // Tạo ra 5 bản sao độc lập cho mỗi file âm thanh
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
        // Thuật toán quét kênh: Tìm kênh nào ĐANG RẢNH thì nhét âm thanh vào đó
        for (int j = 0; j < 5; j++) {
            if (clipArray[currentSoundId][j] != null && !clipArray[currentSoundId][j].isRunning()) {
                clipArray[currentSoundId][j].setFramePosition(0);
                clipArray[currentSoundId][j].start();
                return; // Tìm được kênh rảnh và phát xong thì thoát hàm
            }
        }
        
        // Nếu bắn quá nhanh (Cả 5 kênh đều đang bận kêu), thì ép kênh số 0 phát đè lại
        if (clipArray[currentSoundId][0] != null) {
            clipArray[currentSoundId][0].setFramePosition(0);
            clipArray[currentSoundId][0].start();
        }
    }

    public void loop() {
        // Nhạc nền (Loop) thì chỉ cần chạy ở kênh 0 là đủ
        if(clipArray[currentSoundId][0] != null) {
            clipArray[currentSoundId][0].loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public void stop() {
        // Muốn tắt tiếng thì phải tắt cả 5 kênh
        for (int j = 0; j < 5; j++) {
            if (clipArray[currentSoundId][j] != null) {
                clipArray[currentSoundId][j].stop();
            }
        }
    }

    public void setVolume(float volume) {
        // Tương tự, chỉnh âm lượng là phải áp dụng đồng bộ cho cả 5 kênh
        for (int j = 0; j < 5; j++) {
            if(clipArray[currentSoundId][j] != null) {
                try {
                    FloatControl gainControl = (FloatControl) clipArray[currentSoundId][j].getControl(FloatControl.Type.MASTER_GAIN);
                    gainControl.setValue(volume);
                } catch (IllegalArgumentException e) {
                    // Chống Crash game nếu file .wav bị lỗi định dạng
                }
            }
        }
    }
    // ==========================================
    // HÀM TẠM DỪNG VÀ PHÁT TIẾP (DÀNH CHO NHẠC NỀN)
    // ==========================================
    public void pause() {
        // Hàm stop() của thư viện Clip thực chất là tạm ngắt âm thanh tại chỗ
        if(clipArray[currentSoundId][0] != null) {
            clipArray[currentSoundId][0].stop(); 
        }
    }

    public void resume() {
        // start() sẽ cho phép Clip chạy tiếp từ frame nó vừa bị ngắt
        if(clipArray[currentSoundId][0] != null) {
            clipArray[currentSoundId][0].start();
        }
    }
}
