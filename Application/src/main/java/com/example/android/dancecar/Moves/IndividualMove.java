package com.example.android.dancecar.Moves;

public class IndividualMove {
    private String carInstruction;
    private int duration;

    public IndividualMove(String carInstruction, int duration) {
        this.carInstruction = carInstruction;
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "IndividualMove{" +
                "carInstruction='" + carInstruction + '\'' +
                ", duration=" + duration +
                '}';
    }

    public String getCarInstruction() {
        return carInstruction;
    }
    
    public int getDuration() {
        return duration;
    }
}
