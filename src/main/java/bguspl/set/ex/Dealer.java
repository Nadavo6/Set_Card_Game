package bguspl.set.ex;

import bguspl.set.Env;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.*;
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
    public final Player[] players;
    public int[] timesUpdated;
    public boolean[] isFrozen;
    public Thread[] playerThreads;
    public boolean tableIsFull = false;
    private Thread dealerThread;
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
    private long reshuffleTime;
    private Long currentTime;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        timesUpdated = new int[players.length];
        isFrozen = new boolean[players.length];
        reshuffleTime = System.currentTimeMillis()+env.config.turnTimeoutMillis;
        currentTime = System.currentTimeMillis();
        playerThreads = new Thread[players.length];
        dealerThread = Thread.currentThread();
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
        for (int i=0; i<playerThreads.length; i++)
        {
            playerThreads[i] = new Thread(players[i]);
            playerThreads[i].start();
        }
        while (!shouldFinish()) {
            Collections.shuffle(deck);
            placeCardsOnTable();
            updateTimerDisplay(true, currentTime);
            timerLoop();
            removeAllCardsFromTable();
            if (!shouldFinish()) {
            updateTimerDisplay(true,currentTime);}
        }      
        announceWinners();
        env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
        
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        while (!terminate && System.currentTimeMillis() <= reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false, System.currentTimeMillis());
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        terminate = true;
        for (int i=players.length-1; i>=0; i--)
        {
            players[i].terminate();
        }
        try{
            dealerThread.join();
        }
        catch(InterruptedException e){}
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    public boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }
    
    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
             
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    public void placeCardsOnTable() {
        for(int i =0;i<=11;i++)                 //for every empty slot we put a card there
        {
            if (!deck.isEmpty() && table.slotToCard[i] == null) {
                Integer card = deck.remove(0);
                try {
                    Thread.sleep(env.config.tableDelayMillis);
                } catch (InterruptedException ignored) {
                }
                table.placeCard(card, i);
            }
        }
        tableIsFull = true;
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private synchronized void sleepUntilWokenOrTimeout() {
        List<Integer> currentCards = new ArrayList<Integer>();
        for(Integer card:table.slotToCard)
        {
            if(card!=null)
                currentCards.add(card);
        }
        if(tableIsFull && env.util.findSets(currentCards, 1).size() != 0){
        try { 
            int[] possibleSetAndPlayer = table.possibleSetsQueue.poll(1,TimeUnit.SECONDS);
            if(possibleSetAndPlayer!=null){
                Player player = players[possibleSetAndPlayer[3]];            
                if(possibleSetAndPlayer[4] == 1)
                {
                    player.isLegal = true;
                    player.isFrozen = true;
                    synchronized(table){
                    player.point();
                
                    for (Player player2 : players){
                        for(int i=0; i<player2.tokenCount; i++){
                            if (player2.currentTokens[i] == possibleSetAndPlayer[0] | 
                                player2.currentTokens[i] == possibleSetAndPlayer[1] |
                                player2.currentTokens[i] == possibleSetAndPlayer[2]){
                                table.removeToken(player2, i);
                            }
                        }
                    }
                    for (int i=0; i<possibleSetAndPlayer.length-2;i++){
                        try {
                            Thread.sleep(env.config.tableDelayMillis);
                        } catch (InterruptedException ignored) {}
                        tableIsFull=false;
                        
                        table.removeCard(possibleSetAndPlayer[i]);
                        
                    }
                    if(deck.size() == 0){tableIsFull = true;}
                }
                    updateTimerDisplay(true, currentTime);   
                }
                else{
                    player.penalty();
                }
            }
        } catch (InterruptedException ignored) {}
    }   
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset, Long currentTime) {
        if(!reset)
        {
            for(int i=0; i<players.length; i++){
                if(isFrozen[i]&& !players[i].isLegal){ 
                    timesUpdated[i]++;
                    env.ui.setFreeze(i,(env.config.penaltyFreezeMillis-timesUpdated[i]*1000));
                    if(timesUpdated[i] >= env.config.penaltyFreezeMillis/1000){
                        timesUpdated[i] = 0;
                    }
                }
                else if(isFrozen[i]&& players[i].isLegal)
                {
                    timesUpdated[i]++;
                    env.ui.setFreeze(i,(env.config.pointFreezeMillis-timesUpdated[i]*1000));
                    if(timesUpdated[i] >= env.config.pointFreezeMillis/1000){
                        timesUpdated[i] = 0;
                    }
                }
                
            }
            if((reshuffleTime-currentTime) <= env.config.turnTimeoutWarningMillis)
                env.ui.setCountdown((Math.round((reshuffleTime-currentTime)/1000))*1000, true);
            else
            {
                env.ui.setCountdown(reshuffleTime-currentTime, false);
            }
            
        }
        else
        {
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
            env.ui.setCountdown(env.config.turnTimeoutMillis, false);
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        for(int i =0;i<=11;i++)//removing all the cards on the table
        {
            if (table.slotToCard[i] != null) {
                deck.add(table.slotToCard[i]);
            }
            try {
                Thread.sleep(env.config.tableDelayMillis);
            } catch (InterruptedException ignored) {}
            tableIsFull = false;
            if (table.slotToCard[i] != null) {
            table.removeCard(i);
            }
        }
        for (Player player : players){
            player.tokenCount = 0;
        }
        Collections.shuffle(deck);//rearrange the cards so it will be different
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        for (int i = 1; i < players.length; i++)
        {
            Player player = players[i];
            int j = i-1;
            while (j >= 0 && players[j].getScore() > player.getScore())
            {
                players[j + 1] = players[j];
                j = j - 1;
            }
            players[j + 1] = player;
        }
        List<Integer> bestPlayers = new ArrayList<Integer>();
        int bestScore = players[players.length-1].getScore();
        int index = players.length-1;
        while(index>=0 && players[index].getScore() == bestScore )
        {
            bestPlayers.add(players[index].id);
            index--;
        }
        int[] winners = new int[bestPlayers.size()];
        for (int i = 0; i < winners.length; i++)
        {
            winners[i] = bestPlayers.get(i);
        }
        env.ui.announceWinner(winners);
    }

    public long getReshuffleTime() {
        return reshuffleTime;
    }

    public long getCurrentTime() {
        return currentTime;
    }

}
