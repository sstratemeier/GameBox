package Sokoban.Model;

import java.io.*;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Stack;

/**
 * Main logic class
 *@Author Tobias Fetzer 198318, Simon Stratemeier 199067
 *@Version: 1.0
 *@Date: 21/05/18
 */
public class Sokoban extends Observable implements Serializable, Cloneable {

    public Square[][][] gameBoard;  //Array of Game Elements. Third Dimension for Players and Crates on Fields
    public Player player;
    private boolean isDone = false; //checks if the game is finished
    int arrayHeight = 0;
    int arrayLength = 0;
    String[] inputFromFileArray;
    int goalCount = 0;
    GameStateBackup gameStateBackup=new GameStateBackup();
    private File file;
    private int level;
    private Stack<GameStateBackup> backlog=new Stack<>();


    /**
     * Construktor, Creates the gameBoard based on a text file
     *
     * @param file  text File that contains the Game field
     * @param level The level of the version, which should be loaded from the file
     */
    public Sokoban(File file, int level) {
        ArrayList<String> inputFromFileArray = new ArrayList<>();
        this.file = file;
        this.level = level;

        BufferedReader br;
        String line;

        try {
            boolean correctLevel = false;

            br = new BufferedReader(new FileReader(file));
            while ((line = br.readLine()) != null) {
                //Only saves the correct Level
                if (line.equals("Level " + level)) {
                    correctLevel = true;
                }
                if (correctLevel) {
                    if (line.equals("Level " + (level + 1))) {
                        correctLevel = false;
                    }

                }
                if(correctLevel&&!line.isEmpty()) {
                    inputFromFileArray.add(line);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        /**
         * removes the first two lines (Level, name)
         */
        inputFromFileArray.remove(0);
        inputFromFileArray.remove(0);

        arrayHeight = inputFromFileArray.size();

        /**
         * finds the longest string, to get max lenght
         */
        for (String s : inputFromFileArray) {
            if (s.length() >= arrayLength) {
                arrayLength = s.length();
            }
        }
        this.inputFromFileArray = inputFromFileArray.toArray(new String[0]);
        buildGameBoard();

    }

    /**
     * Builds the gameboard, goes through the String[] and builds the gameboard based on that
     */
    private void buildGameBoard() {
        goalCount = 0;
        gameBoard = new Square[arrayLength][arrayHeight][2];
        for (int y = 0; y < arrayHeight; y++) {
            char[] temp = inputFromFileArray[y].toCharArray();

            for (int x = 0; x < temp.length; x++) {
                switch (temp[x]) {
                    case '#': {
                        gameBoard[x][y][0] = new Wall();
                        break;
                    }
                    case '$': {
                        gameBoard[x][y][0] = new Floor(FloorElement.EMPTY);
                        gameBoard[x][y][1] = new Crate(x, y);

                        break;
                    }
                    case '.': {
                        gameBoard[x][y][0] = new Floor(FloorElement.GOAL);
                        goalCount++;
                        break;
                    }
                    /**
                     * Crate on Goal
                     */
                    case '*': {
                        gameBoard[x][y][0] = new Floor(FloorElement.GOAL);
                        gameBoard[x][y][1] = new Crate(x, y);
                        goalCount++;
                        break;
                    }
                    /**
                     * Player on Goal
                     */
                    case '+': {

                        gameBoard[x][y][0] = new Floor(FloorElement.GOAL);
                        break;
                    }
                    case '@': {
                        gameBoard[x][y][0] = new Floor(FloorElement.EMPTY);
                        player = new Player(x, y);
                        gameBoard[x][y][1] = player;
                        break;
                    }
                    case ' ': {
                        gameBoard[x][y][0] = new Floor(FloorElement.EMPTY);
                        break;
                    }

                }
            }
        }
    }

    /**
     * Returns Both Layers of a Game Square
     *
     * @param x x Position
     * @param y y Position
     * @return Array of two Sqaures, the Field square and a potential Crate/Player
     */
    public Square[] getSquare(int x, int y) {
        Square[] sqr = {gameBoard[x][y][0], gameBoard[x][y][1]};
        return sqr;
    }

    /**
     * moves a Movable Element (player of square) in a given direction
     *
     * @param direction Direction in which the Element is to be moved
     * @param element   The Element to be moved
     * @return if the move was sucessful or not (not moving against wall or moving multiple crates/crates against wall)
     */
    public boolean moveElement(Direction direction, MovableElement element) {
        System.out.println("Player Postion: " + player.position);
        /**
         * Creates Backup of the positions of Players and crates before an update, for undo option
         */
        gameStateBackup=new GameStateBackup();
        gameStateBackup.movableObjectsBackup = new Square[arrayLength][arrayHeight];
        gameStateBackup.positionBackup = new Position[arrayLength][arrayHeight];  //Created new everytime to delete old one
        for (int x = 0; x < arrayLength; x++) {
            for (int y = 0; y < arrayHeight; y++) {
                gameStateBackup.movableObjectsBackup[x][y] = gameBoard[x][y][1];
                if (gameBoard[x][y][1] != null) {
                    MovableElement temp = (MovableElement) gameBoard[x][y][1];
                    gameStateBackup.positionBackup[x][y] = new Position(temp.position);
                }
            }
        }
        if (element == null) return false;
        Position position = Position.movePosition(direction, element.position);
        System.out.println(element.position);
        if (!(gameBoard[position.xPos][position.yPos][0] instanceof Wall)) {  //Not trying to move into wall
            MovableElement move = (MovableElement) gameBoard[position.xPos][position.yPos][1];
            if (!((gameBoard[position.xPos][position.yPos][1] instanceof Crate) && element instanceof Crate)) { //Player moving a crate
                if (move == null || moveElement(direction, move)) {
                    gameBoard[element.position.xPos][element.position.yPos][1] = null;
                    element.position = position;
                    gameBoard[element.position.xPos][element.position.yPos][1] = element;
                    setChanged();
                    notifyObservers();
                    return true;
                }
                System.out.println("not wall");

            }

        }
        System.out.println("Wall");
        return false;
    }

    public boolean moveElement(Direction direction) {
        if(moveElement(direction, player)){
            backlog.add(gameStateBackup);
            return true;
        }
        return false;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
        setChanged();
        notifyObservers();
    }

    /**
     * loads last gamestate from backup
     */
    public void undo() {
        if(!backlog.isEmpty()) {
            GameStateBackup backup = backlog.pop();
            for (int x = 0; x < arrayLength; x++) {
                for (int y = 0; y < arrayHeight; y++) {
                    gameBoard[x][y][1] = backup.movableObjectsBackup[x][y];
                    if (backup.movableObjectsBackup[x][y] != null) {
                        MovableElement temp = (MovableElement) gameBoard[x][y][1];
                        temp.position = backup.positionBackup[x][y];
                    }
                }
            }
            setChanged();
            notifyObservers();

        }
    }

    /**
     * Rebuilds board, for exaple when stuck and want to start again
     */
    public void rebuildBoard() {
        buildGameBoard();
        setChanged();
        notifyObservers();

    }

    public int getGoalCount() {
        return goalCount;
    }

    public int getArrayHeight() {
        return arrayHeight;
    }

    public int getArrayLength() {
        return arrayLength;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        super.clone();
        return new Sokoban(file, level);
    }
}



