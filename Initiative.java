import java.util.ArrayList;
import java.util.Scanner;
import java.util.Random;
import java.util.Arrays;
import java.io.*;

public class Initiative implements Serializable{
    private static Scanner scan = null;
    private ArrayList<Player> players = new ArrayList<Player>();
    private File defaultPlayers = null;
    
    public Initiative() {
        do {
            this.defaultPlayers = getPlayerList(false);
        } while (!readList(defaultPlayers));
        listPlayers(0, false);
    }
    
    class PlayerFormatException extends NumberFormatException {
        
        public PlayerFormatException(String message) {
            super(message);
        }
    }
    
    class Player implements Serializable {
        String name;
        int bonus;
        int initiative;
        int roll;
        
        public Player(String name, int bonus) throws PlayerFormatException {
            this.name = name;
            this.bonus = bonus;
            if (name.equals(" ") || name.equals("") ) { 
                throw new PlayerFormatException(String.format("Name cannot be \"%s\".", name));
            }
        }
    }
    
    public static String checkResponse(String tag) {
        String response = scan.nextLine();
        while (!response.equals("Y") && !response.equals("N")) {
            if (response.equals("end")) System.exit(0);
            if (response.equals("y")) {
                response = "Y";
                break;
            } else if (response.equals("n")) {
                response = "N";
                break;
            }
            System.out.printf("Misunderstood: %s? Y/N\n", tag);
            response = scan.nextLine();
        }
        return response;
    }
    
    public static File getPlayerList(boolean importing) {
        if (!importing) System.out.printf("Choose a default player list: ");
        else System.out.printf("Choose a player list to import: (type cancel to cancel)");
        String fileName = scan.nextLine();
        if (fileName.equals("end")) System.exit(0);
        if (importing && fileName.equals("cancel")) return null;
        File f = new File(fileName + ".txt");
        if (!f.exists()) {
            System.out.printf("File does not exist. ");
            String response = "";
            if (!importing) {
                System.out.println("Create it? Y/N");
                response = checkResponse("create it");
            }
            if (response.equals("Y") && !importing) {
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    System.out.println("File not created.");
                    return null;
                }
            } else if (response.equals("N") || importing) {
                System.out.println("Try again? Y/N");
                String answer = checkResponse("try again");
                if (answer.equals("Y")) return getPlayerList(importing);
                else if (answer.equals("N")) return null;
            }
        }
        return f;
    }
    
    public void listPlayers(int start, boolean beingRolled) {
        if (players.size() == 0) {
            System.out.println("List is empty.");
            return;
        }
        System.out.println("List:");
        ArrayList<Player> ordered = new ArrayList<Player>();
        if (beingRolled) {
            ordered.add(players.get(0));
            for (int i = 1; i < players.size(); i++) {
                Player play = players.get(i);
                boolean added = false;
                for (int j = 0; j < ordered.size(); j++) {
                    Player check = ordered.get(j);
                    if (play.initiative > check.initiative) {
                        ordered.add(j, play);
                        added = true;
                        break;
                    } else if (play.initiative == check.initiative) {
                        if (play.bonus < check.bonus) {
                            ordered.add(j+1, play);
                        } else {
                            ordered.add(j, play);
                        }
                        added = true;
                        break;
                    } else {
                        continue;
                    }
                }
                if (!added) ordered.add(play);
            }
        }
        for (int i = start; i < players.size(); i++) {
            Player play = beingRolled ? ordered.get(i) : players.get(i);
            System.out.printf("%s %d\n", play.name, beingRolled ? play.initiative : play.bonus);
        }
    }    
    
    public ArrayList<Player> checkPlayers(ArrayList<Player> input) {
        for (int i = 0; i < input.size(); i++) {
            Player checked = input.get(i);
            int index = checkPlayers(checked);
            if (index >= 0) {
                players.remove(index);
                break;
            } else if (index == -1) {
                input.remove(i);
                break;
            }
        }
        return input;
    }
    
    public int checkPlayers(Player checked) {
        for (int i = 0; i < players.size(); i++) {
            Player play = players.get(i);
            if (play.name.equals(checked.name)) {
                System.out.printf("Player %s is already in the list, input bonus: %d list bonus: %d\n", 
                                  checked.name, checked.bonus, play.bonus);
                System.out.printf("Would you like to replace the current \"%s\"? Y/N\n", play.name);
                String response = checkResponse(String.format("replace the current \"%s\"", play.name));
                if (response.equals("Y")) {
                    return i;
                } else if (response.equals("N")) {
                    return -1;
                }
            }
        }
        return -2;
    }
    
    public boolean readList(File f) {
        ArrayList<Player> temp = new ArrayList<Player>();
        try {
            ObjectInputStream objIn = new ObjectInputStream(new FileInputStream(f));
            temp = (ArrayList<Player>) objIn.readObject();
            objIn.close();
        } catch (InvalidClassException e) {
            System.out.println("Incompatible File.");
            return false;
        } catch (NullPointerException e) {
            System.out.println("File does not exist.");
            return false;
        } catch (FileNotFoundException e) {
            System.out.printf("File %s not Found. Please try again.\n", f.getPath());
            return false;
        } catch (EOFException e) {
            temp = new ArrayList<Player>();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        players.addAll(checkPlayers(temp));
        return true;
    }
    
    public void roll() {
        if (players.isEmpty()) {
            System.out.println("No Players in the list.");
            return;
        }
        Random r = new Random();
        for (int i = 0; i < players.size(); i++) {
            Player play = players.get(i);
            play.roll = r.nextInt(20) + 1;
            play.initiative = play.roll + play.bonus;
        }
        listPlayers(0, true);
    }
    
    public void add() {
        System.out.printf("Enter name of player followed by the init bonus: (type cancel to cancel)\nAdding:");
        String toAdd = scan.nextLine();
        if (toAdd.equals("end")) System.exit(0);
        else if (toAdd.equals("cancel")) return;
        String name = "";
        int bonus = 0;
        boolean wasRight = true;
        Player adding = null;
        try {
            int index = toAdd.indexOf(" ");
            if (index < 1) throw new PlayerFormatException("Info must be in format \"'name' 'bonus'\"");
            name = toAdd.substring(0, index);
            bonus = Integer.parseInt(toAdd.substring(index+1));
            adding = new Player(name, bonus);
        } catch (NumberFormatException e) {
            System.out.println(e.getMessage());
            wasRight = false;
        }
        if (adding != null && checkPlayers(adding) >= -1) {
            wasRight = false;
        }
        if (wasRight) {
            players.add(new Player(name, bonus));
            System.out.printf("Added %s %d\n", name, bonus);
        }
        System.out.println("Add other PCs? Y/N");
        String response = checkResponse("add other PCs");
        if (response.equals("Y")) {
            add();
        } else if (response.equals("N")){
            return;
        }
    }
    
    public void remove() {
        if (players.isEmpty()) {
            System.out.println("No players to remove.");
            return;
        }
        System.out.println("Choose name of players to remove: (type cancel to cancel)");
        listPlayers(0, false);
        System.out.printf("Removing: ");
        String toRemove = scan.nextLine();
        if (toRemove.equals("end")) System.exit(0);
        else if (toRemove.equals("cancel")) return;
        boolean removedCheck = false;
        for (int i = 0; i < players.size(); i++) {
            Player play = players.get(i);
            if (toRemove.equalsIgnoreCase(play.name)) {
                System.out.printf("Removed %s %d\n", play.name, play.bonus);
                players.remove(i);
                removedCheck = true;
                break;
            }
        }
        if (!removedCheck) System.out.println("No Player with that name found.");
        System.out.println("Remove other PCs? Y/N");
        String response = checkResponse("remove other PCs");
        if (response.equals("Y")) {
            remove();
        } else if (response.equals("N")) {
            return;
        }
    }
    
    public boolean save(File f) {
        System.out.println("Are you sure you want to save the following player list:");
        listPlayers(0, false);
        System.out.printf("to the file \"%s\"? Y/N\n", f.getPath());
        String response = checkResponse("save player list");
        if (response.equals("N")) {
            System.out.println("Save to a different file? Y/N");
            String answer = checkResponse("save to a different file");
            if (answer.equals("Y")) {
                f = getPlayerList(false);
            } else if (answer.equals("N")) {
                System.out.println("Save to current file? Y/N");
                String s = checkResponse("save to current file");
                if (s.equals("N")) return false;
            }
        }
        try {
            ObjectOutputStream objOut = new ObjectOutputStream(new FileOutputStream(f));
            objOut.writeObject(players);
            objOut.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    public static void main(String[] args) {
        scan = new Scanner(System.in);
        Initiative init = new Initiative();
        while (true) {
            System.out.println("\nEnter a command:");
            String line = scan.nextLine();
            switch (line) {
                case "add":
                    init.add();
                    break;
                case "remove":
                    init.remove();
                    break;
                case "roll":
                    init.roll();
                    break;
                case "clear":
                    init.players.clear();
                    System.out.println("List cleared.");
                    break;
                case "change":
                    init.defaultPlayers = init.getPlayerList(false);
                    break;
                case "save":
                    System.out.println(init.save(init.defaultPlayers) ? "Saved" : "Not Saved");
                    break;
                case "default":
                    init.players.clear();
                    init.readList(init.defaultPlayers);
                case "list":
                    init.listPlayers(0, false);
                    break;
                case "import":
                    File toImport = init.getPlayerList(true);
                    if (toImport != null) {
                        init.readList(toImport);
                        init.listPlayers(0, false);
                    }
                    break;
                case "end":
                    init.players.clear();
                    scan.close();
                    return;
                case "help":
                    System.out.println("Commands:\n" +
                                  "\"add\"\nto add a player\n" + 
                                  "\"remove\"\nto remove a player\n" + 
                                  "\"roll\"\nto roll initiative\n" +  
                                  "\"clear\"\nto clear the list\n" +
                                  "\"change\"\nto change the default file\n" +
                                  "\"save\"\nto save the current list to a default\n" +
                                  "\"default\"\nto restore the default players\n" +
                                  "\"list\"\nto see the players currently in the list\n" +
                                  "\"import\"\nto add another player list onto the current\n" +
                                  "\"end\"\nto close the initiative roller");
                    break;
                default:
                    System.out.println("Unknown Command. Type \"help\" to view commands.");
                    break;
            }    
        }
    }
}
