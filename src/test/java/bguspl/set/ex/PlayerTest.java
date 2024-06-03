package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerTest {

    Player player;
    @Mock
    Util util;
    @Mock
    private UserInterface ui;
    @Mock
    private Table table;
    @Mock
    private Dealer dealer;
    @Mock
    private Logger logger;

    void assertInvariants() {
        assertTrue(player.id >= 0);
        assertTrue(player.getScore() >= 0);
    }

    @BeforeEach
    void setUp() {
        // purposely do not find the configuration files (use defaults here).
        Env env = new Env(logger, new Config(logger, (String) null), ui, util);
        player = new Player(env, dealer, table, 0, false);
        assertInvariants();
    }

    @AfterEach
    void tearDown() {
        assertInvariants();
    }

    
    @Test
    //this test verifies that if you give a player a penalty it will be frozen
    void checkPenaltyFreeze() {
        boolean first = player.isFrozen;
        assertTrue(!first,"Player started as frozen");
        player.penalty();
        assertTrue(player.isFrozen,"Player didn't freeze in penalty");
        }

    
    @Test
    //this test verifies that the a key was added to the queue if the conditions are met
    void keyPressedCheck() {
        dealer.tableIsFull = true;
        int numberPressedSize = player.numberPressed.size();
        player.keyPressed(0);
        assertEquals(numberPressedSize+1,player.numberPressed.size(),"key was not added to queue");
    }
}