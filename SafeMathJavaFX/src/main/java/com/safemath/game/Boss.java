package com.safemath.game;

public class Boss {
    public String name;
    public int health;
    public int maxHealth;

    public Boss(String name, int health) {
        this.name = name;
        this.health = health;
        this.maxHealth = health; // guarda a vida máxima
    }

    public void takeDamage(int dmg) {
        this.health -= dmg;
        if (this.health < 0) this.health = 0;
    }

    public boolean isAlive() {
        return this.health > 0;
    }
}
