package com.safemath.game;

public class Animal {
    public String name;
    public int force;
    public int cooldown = 0;

    public Animal(String name,int force) {
        this.name = name;
        this.force = force;
    }

    public boolean canAttack() {
        return cooldown==0;
    }

    public void decrementCooldown() {
        if(cooldown>0) cooldown--;
        else cooldown = 0;
    }
}
