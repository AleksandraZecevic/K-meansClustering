import guiThings.GUI;
import util.Logger;

public class Main {
    public static void main(String[] args) {
        Logger.log("Main");
        //new MapFrame();
        GUI gui = new GUI();
        gui.setVisible(true);

        // note for professor
        // I added internet checking because map cannot load without it and code would break
        // I tried to get the outlines of europe using polygons, but as you can see I failed, so better luck next time I guess
        // so , at the end I used Domen's suggestion of putting random dot in Germany
        // hope everything works okay : )
    }
}