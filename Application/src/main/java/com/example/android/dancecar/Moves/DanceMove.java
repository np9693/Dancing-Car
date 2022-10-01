package com.example.android.dancecar.Moves;

import androidx.annotation.NonNull;

import java.util.Random;

public class DanceMove {
    private String danceName;
    private int id;
    private Random random = new Random();
    private boolean isCreated;

    public DanceMove(String danceName) {
        this.danceName = danceName;
        this.id = random.nextInt();
        this.isCreated = isCreated();
    }

    @NonNull
    @Override
    public String toString() {
        return danceName;
    }

    public String getDanceName() {
        return danceName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isCreated() { return isCreated; }

    public void setCreated(boolean created) { isCreated = created; }
}
