package com.example.android.dancecar.Moves;

import java.util.ArrayList;

public class CreatedDanceMove {
    private ArrayList<IndividualMove> individualMoves;
    private String newDanceName;

    public CreatedDanceMove(ArrayList<IndividualMove> individualMoves, String newDanceName) {
        this.individualMoves = individualMoves;
        this.newDanceName = newDanceName;
    }

    public ArrayList<IndividualMove> getIndividualMoves() {
        return individualMoves;
    }

    public void setIndividualMoves(ArrayList<IndividualMove> individualMoves) {
        this.individualMoves = individualMoves;
    }

    public String getName() {
        return newDanceName;
    }

    @Override
    public String toString() {
        return "CreatedDanceMove{" +
                "individualMoves=" + individualMoves +
                ", newDanceName='" + newDanceName + '\'' +
                '}';
    }
}