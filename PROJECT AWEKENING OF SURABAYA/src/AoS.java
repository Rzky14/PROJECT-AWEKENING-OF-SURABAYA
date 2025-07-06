
import menu.BaseFrame;
import menu.MenuComponent;

public class AoS {

    public static void main(String[] args) {
        BaseFrame mainFrame = new BaseFrame("Awakening of Surabaya");
        mainFrame.setContent(new MenuComponent(mainFrame));
        mainFrame.setVisible(true);
    }
}
