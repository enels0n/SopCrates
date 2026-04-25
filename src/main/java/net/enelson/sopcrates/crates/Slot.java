package net.enelson.sopcrates.crates;

public interface Slot {
    void setPrize(OpenPrize prize);
    OpenPrize getPrize();
    void remove();
}