package com.synclabos.diningphilosophers;

import java.util.List;

public record DiningState(
    List<PhilosopherState> philosopherStates,
    List<String> forkOwners,
    List<Integer> mealsByPhilosopher
) {
}
