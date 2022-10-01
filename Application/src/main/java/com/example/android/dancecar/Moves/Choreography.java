package com.example.android.dancecar.Moves;

import java.util.ArrayList;
import java.util.Random;

public class Choreography {
    private ArrayList<DanceMove> selectedDances;
    private int chorMoveID;
    private String chorName;
    private Random random = new Random();

    public Choreography(ArrayList<DanceMove> selectedDances, String chorName) {
        this.selectedDances = selectedDances;
        this.chorMoveID = random.nextInt();
        this.chorName = chorName;
    }

    public ArrayList<DanceMove> getSelectedDances() {
        return selectedDances;
    }

    public int getChorMoveID() {
        return chorMoveID;
    }

    public String getChorName() {
        return chorName;
    }

}
