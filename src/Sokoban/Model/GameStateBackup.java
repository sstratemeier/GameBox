package Sokoban.Model;

import java.io.Serializable;

/**
 * Used to save Movable objects and their position
 *
 * @Author Tobias Fetzer 198318, Simon Stratemeier 199067
 * @Version: 1.0
 * @Date: 21/05/18
 */
public class GameStateBackup implements Serializable {
    public Square[][] movableObjectsBackup;     //position of movable objects
    public Position[][] positionBackup;         //backup of positon objects

}
