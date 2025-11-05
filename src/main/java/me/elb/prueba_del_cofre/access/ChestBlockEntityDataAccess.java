package me.elb.prueba_del_cofre.access;

public interface ChestBlockEntityDataAccess {
    void chestLoot$setUsed(boolean used);
    boolean chestLoot$isUsed();
}