package dev.gyro.spin;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpinSkillsTest {
    @Test void mentorIsRequiredEvenInCreative() {
        assertFalse(SpinSkills.canLearn(0, 100, false));
        assertFalse(SpinSkills.canLearn(0, 100, true));
    }
    @Test void everyPurchaseRequiresFullCost() {
        for (int rank=1;rank<5;rank++) {
            int cost=SpinSkills.cost(rank+1);
            assertFalse(SpinSkills.canLearn(rank,cost-1,false));
            assertTrue(SpinSkills.canLearn(rank,cost,false));
        }
    }
    @Test void creativeBypassesOnlyExperienceCost() {
        for(int rank=1;rank<5;rank++) assertTrue(SpinSkills.canLearn(rank,0,true));
        assertFalse(SpinSkills.canLearn(5,100,true));
    }
    @Test void ranksCannotOverflowOrStartBelowZero() {
        assertFalse(SpinSkills.canLearn(-1,100,false));
        assertFalse(SpinSkills.canLearn(5,100,false));
        assertFalse(SpinSkills.canLearn(6,100,true));
        assertThrows(IllegalArgumentException.class,()->SpinSkills.cost(6));
        assertThrows(IllegalArgumentException.class,()->SpinSkills.cost(0));
    }
    @Test void fullSurvivalProgressionCosts28Levels() {
        int levels=28;
        for(int rank=1;rank<5;rank++) {
            assertTrue(SpinSkills.canLearn(rank,levels,false));
            levels-=SpinSkills.cost(rank+1);
        }
        assertEquals(0,levels);
    }
}
