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
import java.util.Properties;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DealerTest {
    private Dealer dealer;
    private Table table;
    private Integer[] slotToCard;
    private Integer[] cardToSlot;
//    private Player[] players;

    @Mock
    Util util;
    @Mock
    private UserInterface ui;
    @Mock
    private Logger logger;


    @BeforeEach
    void setUp() {
        // purposely do not find the configuration files (use defaults here).
        Properties properties = new Properties();
        properties.put("Rows", "2");
        properties.put("Columns", "2");
        properties.put("FeatureSize", "3");
        properties.put("FeatureCount", "4");
        properties.put("TableDelaySeconds", "0");
        properties.put("PlayerKeys1", "81,87,69,82");
        properties.put("PlayerKeys2", "85,73,79,80");
        TableTest.MockLogger logger = new TableTest.MockLogger();
        Config config = new Config(logger, properties);
        slotToCard = new Integer[config.tableSize];
        cardToSlot = new Integer[config.deckSize];

        Env env = new Env(logger, new Config(logger, (String) null), ui, util);
        table = new Table(env, slotToCard, cardToSlot);        
        Player player = new Player(env, dealer, table, 0, false);
        Player[] players = {player};
        dealer = new Dealer(env, table, players);
    }
    
    @Test
    //Checks the Reshuffle time makes sense
    void checkTimeSanity() {
        assertTrue(dealer.getReshuffleTime()>=dealer.getCurrentTime(),"Reshuffle Time is wrong!");
    }

      
    @Test
    //Checks the Reshuffle time makes sense
    void CountPlayerThreads() {
        assertTrue((dealer.playerThreads.length)==(dealer.players.length),"Not all players have a thread!");
    }
}