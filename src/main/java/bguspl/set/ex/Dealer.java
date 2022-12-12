package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;


    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
       /* if(env.config.turnTimeoutMillis > 0){
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        }
        else if(env.config.turnTimeoutMillis == 0){
            //timer go up (since last action)
        }
        else {
            //no timer
        }*/
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
        while (!shouldFinish()) {
            placeCardsOnTable();
            timerLoop();
            updateTimerDisplay(true, env.config.turnTimeoutMillis);
            removeAllCardsFromTable();
        }
        announceWinners();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        long timeFromStartMillis = System.currentTimeMillis();
        reshuffleTime = timeFromStartMillis + env.config.turnTimeoutMillis;
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout(reshuffleTime);
            updateTimerDisplay(false, reshuffleTime - System.currentTimeMillis());
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
        terminate = true;
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        // TODO implement
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout(long reshuffleTimeMillis) {
        // TODO implement
        synchronized (this){
            try {
                wait(reshuffleTimeMillis - System.currentTimeMillis());
            }
            catch(InterruptedException ex){}
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset, long timeLeftMillis) {
        // TODO implement
        // what is da diff reset == true/false?
        env.ui.setCountdown(timeLeftMillis, timeLeftMillis < env.config.turnTimeoutWarningMillis);
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
//        int max = Arrays.stream(players).max(Comparator.comparingInt(p -> p.getScore())).get().getScore();
        int max = Arrays.stream(players)
                        .mapToInt(Player::getScore)
                        .max()
                        .orElse(-1);
        int[] maxPlayers = Arrays.stream(players)
                                .filter(p -> p.getScore()==max)
                                .mapToInt(p -> p.id)
                                .toArray();
        env.ui.announceWinner(maxPlayers);
    }
}
