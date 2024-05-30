package com.better.player;

import androidx.annotation.Nullable;

public class SpeedItems {

    private String name;
    private float speed;
    private int chosen;

    public SpeedItems(String name, float speed, int chosen) {
        this.name = name;
        this.speed = speed;
        this.chosen = chosen;
    }

    public SpeedItems(String name, float speed) {
        this.name = name;
        this.speed = speed;
        this.chosen = 0;
    }

    public SpeedItems(float speed) {
        this.speed = speed;
    }

    public SpeedItems(int chosen) {
        this.chosen = chosen;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public int getChoose() {
        return chosen;
    }

    public void setChoose(int chosen) {
        this.chosen = chosen;
    }

    public boolean getChosen() {
        return chosen == 1;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SpeedItems speedItems = (SpeedItems) obj;
        return getSpeed() == speedItems.getSpeed();
    }
}
