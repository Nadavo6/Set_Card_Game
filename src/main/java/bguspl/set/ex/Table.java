package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)
    public BlockingQueue<int[]> possibleSetsQueue;
    
    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        possibleSetsQueue = new LinkedBlockingDeque<int[]>();
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public  void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        cardToSlot[card] = slot;
        slotToCard[slot] = card;
        env.ui.placeCard(card, slot);
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}
        int card = slotToCard[slot];
        cardToSlot[card] = null;
        slotToCard[slot] = null;
        env.ui.removeTokens(slot);
        env.ui.removeCard(slot);
        
    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public synchronized void placeToken(Player player, int slot) {               //SYNCRONISED
        if(slotToCard[slot]!=null)
        {
            player.currentTokens[player.tokenCount] = slot;
            player.tokenCount++;
            env.ui.placeToken(player.id, slot);
            if(player.tokenCount == 3)
            {
                int[] possibleSetWithPlayer = new int[3];
                int[] SetWithPlayer = new int[5];
                possibleSetWithPlayer[0] = slotToCard[player.currentTokens[0]];
                possibleSetWithPlayer[1] = slotToCard[player.currentTokens[1]];
                possibleSetWithPlayer[2] = slotToCard[player.currentTokens[2]];
                SetWithPlayer[0] = player.currentTokens[0];
                SetWithPlayer[1] = player.currentTokens[1];
                SetWithPlayer[2] = player.currentTokens[2];
                SetWithPlayer[3] = player.id;
                if(env.util.testSet(possibleSetWithPlayer))
                {
                    SetWithPlayer[4] = 1;
                    
                }
                else
                {
                    SetWithPlayer[4] = 0;
                }
                possibleSetsQueue.add(SetWithPlayer);
            }
        }
        
    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public synchronized boolean removeToken(Player player, int slot) {               //SYNCRONISED
        if (player.tokenCount == 0){
            return false;
    }
        int j=0;
        for (int i=0; i<player.tokenCount; i++){
            if(player.currentTokens[i]==slot){
                j = i;
            }
        }
        for (int i=j+1; i<player.tokenCount; i++){
            player.currentTokens[i-1] = player.currentTokens[i];
        }
        player.tokenCount--;
        env.ui.removeToken(player.id, slot);
        return true;
    }
}
