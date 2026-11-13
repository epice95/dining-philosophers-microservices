package dev.philosophers.unlocked;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Comportement séquentiel : sans concurrence, la table donne les bonnes réponses. */
class UnlockedForkTableTest {

    private final UnlockedForkTable table = new UnlockedForkTable(5, RaceWindow.NONE);

    @Test
    void freeForksAreGrantedTogether() {
        assertThat(table.acquire(1)).isTrue();
    }

    @Test
    void neighbourIsRefusedWhileAForkIsBusy() {
        table.acquire(1);

        assertThat(table.acquire(0)).isFalse(); // F1 occupée
        assertThat(table.acquire(2)).isFalse(); // F2 occupée
    }

    @Test
    void nonNeighboursEatAtTheSameTime() {
        assertThat(table.acquire(0)).isTrue();
        assertThat(table.acquire(2)).isTrue();
    }

    @Test
    void releaseFreesTheForksForTheNeighbour() {
        table.acquire(1);
        table.release(1);

        assertThat(table.acquire(0)).isTrue();
    }
}
