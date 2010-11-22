import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatTools {
    public static final int lineLength = 54;
    protected static final Logger log = Logger.getLogger("Minecraft");
    
    /*public static ArrayList<String> list(String[] args) {
        return list(Arrays.asList(args));
    }*/
    
    /*public static ArrayList<String> list(List<String> args) {
        if (args.size() > 0) {
            String line = "";
            for (int i=0; i<args.size()-1;i++)
                line += args.get(i) + ", ";
            line += args.get(args.size()-1);
            
            return color(line);
        }
        
        return new ArrayList<String>();
    }*/
    
    public static ArrayList<String> list(Object[] args) {
        return list(Arrays.asList(args));
    }
    
    public static ArrayList<String> list(List<Object> args) {
        if (args.size() > 0) {
            String line = "";
            for (int i=0; i<args.size()-1;i++)
                line += args.get(i) + ", ";
            line += args.get(args.size()-1).toString();
            
            return color(line);
        }
        
        return new ArrayList<String>();
    }
    
    public static ArrayList<String> wordWrap(String[] tokens) {
        ArrayList<String> out = new ArrayList<String>();
        out.add("");
        
        for (String s : tokens) {
            if (stripColour(out.get(out.size()-1)).length() + stripColour(s).length() + 1 > lineLength) {
                out.add("");
            }
            out.set(out.size()-1, out.get(out.size()-1) + s + " ");
        }
        
        return out;
    }
    
    public static ArrayList<String> color(String line) {
        ArrayList<String> out = wordWrap(line.split(" "));
        
        String c = "f";
        for (int i = 0; i < out.size(); i++) {
            if (!out.get(i).startsWith("§"))
                out.set(i, "§" + c + out.get(i));
            
            for (int index = 0; index < lineLength; index++) {
                try {
                    if (out.get(i).substring(index, index+1).equalsIgnoreCase("\u00A7")) {
                        c = out.get(i).substring(index+1, index+2);
                    }
                } catch(Exception e) {}
            }
        }
        
        return out;
    }
    
    public static String stripColour(String s) {
        String out = "";
        for (int i=0; i<s.length()-1; i++) {
            String c = s.substring(i, i+1);
            if (c.equals("§")) {
                i += 1;
            } else {
                out += c;
            }
        }
        return out;
    }
    
    public static String formatTitle(String title) {
        return Colors.Gold + "_____[ " + Colors.Yellow + title + Colors.Gold + " ]_____";
        
    }
    
    public static void main(String[] args) {
        String[] players = {"dude", "bowie", "blarg", "sonbitch", "songoku", "pacman", "link", "stacker", "hacker", "newb" };
        for (String line : ChatTools.list(players))
            System.out.println(line);
        
        String testLine = "Loren Ipsum blarg voila tssssssh, boom wakka wakka §apacman on a boat bitch. From the boat union. Beata lingiushtically §1nootchie lolk erness.";
        for (String line : ChatTools.color(testLine))
            System.out.println(line);
    }
}