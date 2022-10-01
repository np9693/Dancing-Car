package com.example.android.dancecar.Connections;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.example.android.dancecar.Moves.IndividualMove;

import java.util.ArrayList;

import com.example.android.dancecar.Moves.CreatedDanceMove;
import com.example.android.dancecar.Moves.DanceMove;

//Source code : https://www.geeksforgeeks.org/how-to-create-and-add-data-to-sqlite-database-in-android/
//Source code : https://github.com/DIT112-V20/group-04/blob/master/app/src/main/java/se/healthrover/conectivity/SqlHelper.java

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "dancemoves.sqlite";
    private static final String DATABASE_TABLE_1 = "dancemoves_table";
    private static final String DATABASE_COL_ID = "id";
    private static final String DATABASE_COL_DANCE_NAME = "dance_name";
    private static final String DATABASE_TABLE_2 = "individualMove_table";
    private static final String DATABASE_COL_INDIVIDUAL_ID = "individual_id";
    private static final String DATABASE_COL_INSTRUCTION = "instruction";
    private static final String DATABASE_COL_DURATION = "individual_duration";
    private static final String DATABASE_COL_ORDER = "instruction_order";
    private static final String DATABASE_COL_NEWID = "dance_id";
    private static final String DATABASE_TABLE_3 = "chorMoves_table";
    private static final String DATABASE_COL_CHORMOVES_ID = "chor_id";
    private static final String DATABASE_COL_CHORMOVESNAME = "chor_name";
    private static final String DATABASE_COL_SET_OF_MOVES = "set_of_moves";
    private SQLiteDatabase db = getReadableDatabase();

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        System.out.println("Hi");
        SQLiteStatement moveData1 = db.compileStatement("CREATE TABLE "+DATABASE_TABLE_1 +" ("
                + DATABASE_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + DATABASE_COL_DANCE_NAME +" VARCHAR );");

        SQLiteStatement individualMoveData2 = db.compileStatement("CREATE TABLE "+DATABASE_TABLE_2 +" ("
                + DATABASE_COL_INDIVIDUAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + DATABASE_COL_NEWID + " INTEGER, "
                + DATABASE_COL_INSTRUCTION +" VARCHAR, "
                + DATABASE_COL_ORDER +" INTEGER NOT NULL, "
                + DATABASE_COL_DURATION +" INTEGER NOT NULL, " +
                "FOREIGN KEY ("+ DATABASE_COL_NEWID +") REFERENCES "+ DATABASE_TABLE_1 + " ("+DATABASE_COL_ID+"));");

        SQLiteStatement chorMovesData = db.compileStatement("CREATE TABLE "+DATABASE_TABLE_3 +" ("
                + DATABASE_COL_CHORMOVES_ID + " INTEGER PRIMARY KEY NOT NULL, "
                + DATABASE_COL_ID + " INTEGER, "
                + DATABASE_COL_SET_OF_MOVES + " VARCHAR NOT NULL, "
                + DATABASE_COL_CHORMOVESNAME +" VARCHAR NOT NULL, " +
                "FOREIGN KEY ("+ DATABASE_COL_ID +") REFERENCES "+ DATABASE_TABLE_1 + " ("+DATABASE_COL_ID+"));");

        moveData1.execute();
        individualMoveData2.execute();
        chorMovesData.execute();
    }

    @Override
    // This method is called to check if the table exists already.
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_1);
        db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_2);
        db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_3);
        onCreate(db);
    }

    // This method is use to add new moves to our sqlite database, related to table 1.
    public void insertMove(String danceName) {

        // on below line we are creating a variable for our sqlite database and calling writable method
        // as we are writing data in our database.
        SQLiteDatabase db = getWritableDatabase();

        // on below line we are creating a variable for content values.
        ContentValues values = new ContentValues();

        // on below line we are passing all values along with its key and value pair.
        values.put(DATABASE_COL_DANCE_NAME, danceName);

        db.insert(DATABASE_TABLE_1, null, values);

        db.close();
    }

    // This method is used to add new individual move to our sqlite database, related to table 2.
    public void insertIndividualMove(String danceMoveName, String carInstruction, int individualDuration, int order) {

        SQLiteDatabase db = getWritableDatabase();
        ContentValues valuesIndividual = new ContentValues();

        int iD = getMoveId(danceMoveName);
        System.out.println(individualDuration+"d");

        valuesIndividual.put(DATABASE_COL_INSTRUCTION, carInstruction);
        valuesIndividual.put(DATABASE_COL_DURATION, individualDuration);
        valuesIndividual.put(DATABASE_COL_ORDER, order);
        valuesIndividual.put(DATABASE_COL_NEWID, iD);

        db.insert(DATABASE_TABLE_2, null, valuesIndividual);

        db.close();
    }

    // This method is used to add new choreography to our sqlite database, related to table 3.
    public void insertChorMove(ArrayList<DanceMove> fullChor, String chor_name) {

        SQLiteDatabase db = getWritableDatabase();
        ContentValues chorValues = new ContentValues();

        chorValues.put(DATABASE_COL_SET_OF_MOVES, String.valueOf(fullChor));
        chorValues.put(DATABASE_COL_CHORMOVESNAME, chor_name);

        db.insert(DATABASE_TABLE_3, null, chorValues);

        db.close();
    }

    public ArrayList<CreatedDanceMove> getCreatedDanceMove() {

        ArrayList<CreatedDanceMove> createdDanceMoves = new ArrayList<>();
        String direction = "";
        long duration = 0;

        Cursor allNames = db.rawQuery("SELECT " + DATABASE_COL_DANCE_NAME + " FROM " + DATABASE_TABLE_1, new String[]{});

        String name = "";

        if (allNames.getCount() == 0) {
            return createdDanceMoves;
        } else {
            while (allNames.moveToNext()) {
                name = allNames.getString(0);
                CreatedDanceMove createdDanceMove = new CreatedDanceMove(new ArrayList<IndividualMove>(), name);
                createdDanceMoves.add(createdDanceMove);
            }

            for (CreatedDanceMove createdDanceMove : createdDanceMoves){
                Cursor allIndividualMoves = db.rawQuery("SELECT " + DATABASE_COL_INSTRUCTION + ", " + DATABASE_COL_DURATION +
                        " FROM " + DATABASE_TABLE_2 + " JOIN " + DATABASE_TABLE_1 + " ON " + DATABASE_COL_NEWID + "=" +
                        DATABASE_COL_ID + " WHERE " + DATABASE_COL_DANCE_NAME + "=?", new String[]{createdDanceMove.getName()});

                if (allIndividualMoves.getCount() == 0) {
                    return createdDanceMoves;
                } else {
                    ArrayList<IndividualMove> individualMoves = new ArrayList<>();
                    while (allIndividualMoves.moveToNext()) {
                        direction = allIndividualMoves.getString(0);
                        duration = allIndividualMoves.getInt(1);
                        System.out.println(duration +"e");
                        IndividualMove individualMove = new IndividualMove(direction, (int) duration);
                        individualMoves.add(individualMove);
                    }
                    createdDanceMove.setIndividualMoves(individualMoves);
                }
            }
            return createdDanceMoves;
        }
    }


    public ArrayList<DanceMove> getDanceMove() {

        ArrayList<DanceMove> danceMoves = new ArrayList<>();

        Cursor allNames = db.rawQuery("SELECT " + DATABASE_COL_DANCE_NAME + " FROM " + DATABASE_TABLE_1, new String[]{});

        String name = "";

        if (allNames.getCount() == 0) {
            return danceMoves;
        } else {
            while (allNames.moveToNext()) {
                name = allNames.getString(0);
                DanceMove danceMove = new DanceMove(name);
                danceMove.setCreated(true);
                danceMoves.add(danceMove);
            }
            return danceMoves;
        }
    }

    public int getMoveId(String name) {

        db = getReadableDatabase();
        Cursor allIds = db.rawQuery("SELECT " + DATABASE_COL_ID + " FROM " + DATABASE_TABLE_1 + " WHERE "
                + DATABASE_COL_DANCE_NAME + "=?", new String[]{name});

        int iD = 0;

        if (allIds.getCount() == 0 || allIds.getCount() > 1) {
            return iD;
        } else {
            while (allIds.moveToNext()) {
                iD = allIds.getInt(0);
            }
            return iD;
        }
    }
}