package bguspl.set.ex;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class DealerTest {

    Player player1;
    Player player2;
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

    private Env env;

    @BeforeEach
    void setUp() {
        // purposely do not find the configuration files (use defaults here).
        TableTest.MockLogger logger = new TableTest.MockLogger();
        ui = new TableTest.MockUserInterface();
        env = new Env(logger, new Config(logger, (String) null), ui, util);
        table = new Table(env);
        player1 = new Player(env, dealer, table, 0, false);
        player2 = new Player(env, dealer, table, 1, false);
        dealer = new Dealer(env, table, new Player[]{player1,player2});
    }

    @Test
    void freezeTest(){
        dealer.freezePlayer(0, 3000);
        Assertions.assertEquals(true, player1.isFreeze());
        dealer.removeFreezePlayer(0);
        Assertions.assertEquals(false, player1.isFreeze());
    }

    @Test
    void placeCardsTest(){
        List<Integer> deck= dealer.getDeck();
        Assertions.assertEquals(env.config.deckSize, deck.size());
        dealer.placeCardsOnTable();
        Assertions.assertEquals(env.config.deckSize - env.config.tableSize, deck.size());

    }

}