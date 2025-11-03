package com.safemath.game;

public class Player {
    private int health;
    private int maxHealth;

    public Player(int maxHealth) {
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }

    public void takeDamage(int dmg) {
        health -= dmg;
        if (health < 0) health = 0;
    }

    public boolean isAlive() { return health > 0; }

    public void healFull() {
        this.health = maxHealth;
    }
}
