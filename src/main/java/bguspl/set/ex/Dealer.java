package bguspl.set.ex;

import bguspl.set.Env;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private ConcurrentLinkedQueue<Integer> playerWithSet;

    private Thread[] playersThreads;

    private long[] playersFreezeTimes;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        playerWithSet = new ConcurrentLinkedQueue<>();
        reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        playersFreezeTimes = new long[env.config.players];
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
        playersThreads = new Thread[env.config.players];
        for(int i=0; i<playersThreads.length;i++){
            playersThreads[i] = new Thread(players[i], "player "+i+" Thread ");
            playersThreads[i].start();
        }
        while (!shouldFinish()) {
            placeCardsOnTable();
            updateTimerDisplay(true);
            timerLoop();
            updateTimerDisplay(true);
            removeAllCardsFromTable();
            shuffleDeck();
        }
        announceWinners();
        for(Player player: players){
            player.terminate();
        }
        for(Thread t: playersThreads){
            try {
//                t.interrupt();
                t.join();
            } catch (InterruptedException e) {}
        }
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            resumePlay();
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            updatePlayersFreezeTime();
            stopPlay();
            checkSet();
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
    private void checkSet() {
        // TODO implement
        if(!playerWithSet.isEmpty()) {
            Integer playerId = playerWithSet.remove();
            List<Integer> set = table.getSetAsSlots(playerId);
            int[] setAsArray = table.getSetAsCards(playerId);
            if(set.size() == 3 && setAsArray.length ==3){
                //if the set is correct
                if(env.util.testSet(setAsArray)){
                    updateTimerDisplay(true);
                    for(Integer slot: set){
                        table.removeCard(slot);
                        /*for(Player player: players){
                            table.removeToken(player.id, slot);
                        }*/
                        table.removeTokensFromSlot(slot);
                    }
                    //freeze and increment score
                    players[playerId].point();
                    freezePlayer(playerId, env.config.pointFreezeMillis);
                }
                //false set
                else{
                    //penalty time
                    players[playerId].penalty();
                    freezePlayer(playerId, env.config.penaltyFreezeMillis);
                }
            }
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        List<Integer> emptySlots = table.getEmptySlots();
        while(!deck.isEmpty() && !emptySlots.isEmpty()){
                table.placeCard(deck.remove(0), emptySlots.remove(0));
        }
    }

    private void stopPlay(){
        for(Player player: players){
            player.blockPlay();
        }
    }

    private void resumePlay(){
        for(Player player: players){
            player.resumePlay();
        }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        synchronized (this){
            try {
                if(reshuffleTime - System.currentTimeMillis() >= env.config.turnTimeoutWarningMillis + 1000)
                    wait(50);
            }
            catch(InterruptedException ex){}
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
        if (reset)
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        long timeLeft = reshuffleTime - System.currentTimeMillis();
        env.ui.setCountdown(timeLeft, timeLeft <= env.config.turnTimeoutWarningMillis);
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement
        Integer[] cards = table.removeAllCardsAndReturn();
        deck.addAll(Arrays.asList(cards));
        table.clearAllTokens();
    }

    private void shuffleDeck(){
        Collections.shuffle(deck);
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
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

    public void enterPlayerWithSet(int playerId){
        playerWithSet.add(playerId);
    }

    private void updatePlayersFreezeTime(){
        int index = 0;
        for(long freezeTime: playersFreezeTimes){
            long freezeTimeLeft = freezeTime - System.currentTimeMillis();
            if(players[index].isFreeze() && freezeTimeLeft <= 0)
                removeFreezePlayer(index);
            env.ui.setFreeze(index, freezeTimeLeft);
            index++;
        }
    }

    private void freezePlayer(int playerId, long millisFreeze){
        players[playerId].freezePlay();
        playersFreezeTimes[playerId] = System.currentTimeMillis() + millisFreeze;
    }

    private void removeFreezePlayer(int playerId){
        players[playerId].unfreezePlay();
    }

}
