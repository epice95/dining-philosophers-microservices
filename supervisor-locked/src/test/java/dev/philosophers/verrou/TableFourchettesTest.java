package dev.philosophers.verrou;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Comportement de base, sans concurrence. */
class TableFourchettesTest {

    private final TableFourchettes table = new TableFourchettes(5);

    @Test
    void fourchettesLibresAccordees() {
        assertThat(table.prendre(1)).isTrue();
    }

    @Test
    void voisinRefuseSiFourchetteOccupee() {
        table.prendre(1);

        assertThat(table.prendre(0)).isFalse(); // F1 occupée
        assertThat(table.prendre(2)).isFalse(); // F2 occupée
    }

    @Test
    void nonVoisinsMangentEnMemeTemps() {
        assertThat(table.prendre(0)).isTrue();
        assertThat(table.prendre(2)).isTrue();
    }

    @Test
    void rendreLibereLesFourchettes() {
        table.prendre(1);
        table.rendre(1);

        assertThat(table.prendre(0)).isTrue();
    }
}
