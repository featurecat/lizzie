package wagner.stephanie.lizzie.gui;


public class Config {
    public boolean showMoveNumber = false;
    
    public Config() {
        
    }

    public void toggleShowMoveNumber() {
        this.showMoveNumber = !this.showMoveNumber;
    }
}
