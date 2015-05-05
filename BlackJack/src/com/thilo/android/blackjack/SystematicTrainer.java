/*
    BlackJack Trainer.  BJ strategy tutor.
    Copyright (C) 2012  Daniel Kraft <d@domob.eu>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.thilo.android.blackjack;

import android.util.Log;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Handle systematic training of all strategy cells, repeating wrong ones
 * until handled correctly.
 */
public class SystematicTrainer implements Serializable
{
  /** Log tag.  */
  private static final String TAG = "BJTrainer/SystematicTrainer";

  /**
   * Index into one of the cells, including hard/soft/pair and the
   * player as well as dealer values.
   */
  private static class Index implements Serializable
  {

    /** Serial version id.  */
    private static final long serialVersionUID = 0l;

    /** This index' matrix type.  */
    public final Strategy.Matrix matrix;

    /** This index' player total, as indexing in the strategy.  */
    public final byte player;

    /** Dealer face card value.  */
    public final byte dealer;

    /**
     * Construct it with given parameters.
     * @param m The matrix.
     * @param p Player value.
     * @param d Dealer value.
     */
    public Index (Strategy.Matrix m, byte p, byte d)
    {
      matrix = m;
      player = p;
      dealer = d;
    }

    /**
     * Construct a game fitting this index.
     * @param deck Card supply for this game.
     * @param h17 H17 flag to use for the game.
     * @return A game whose configuration is in this matrix entry.
     */
    public Game constructGame (CardSupply deck, boolean h17)
    {
      Log.d (TAG, String.format ("Constructing game for %s...", toString ()));

      final Hand dealerHand = new Hand ();
      dealerHand.add (constructCard (dealer));

      final Hand playerHand = new Hand ();
      switch (matrix)
        {
          case HARD:
            switch (player)
              {
                case 21:
                  playerHand.add (constructCard (10));
                  playerHand.add (constructCard (11));
                  break;

                case 20:
                  /* It's not possible to actually have hard 20, this
                     is always going to be a pair.  */
                  playerHand.add (constructCard (10));
                  playerHand.add (constructCard (10));
                  break;

                default:
                  int v1, v2;
                  do
                    {
                      v1 = RandomSupply.rng.nextInt (9) + 2;
                      v2 = RandomSupply.rng.nextInt (9) + 2;
                    }
                  while (v1 == v2 || v1 + v2 != player);
                  playerHand.add (constructCard (v1));
                  playerHand.add (constructCard (v2));
                  break;
              }
            break;

          case SOFT:
            playerHand.add (constructCard (player - 11));
            playerHand.add (constructCard (11));
            break;

          case PAIR:
            playerHand.add (constructCard (player));
            playerHand.add (constructCard (player));
            break;

          default:
            assert (false);
        }

      return new Game (playerHand, dealerHand, deck, h17);
    }

    /**
     * Construct a card with the given value.
     * @param v Value the card should have.
     * @return A random card with this value.
     */
    private Card constructCard (int v)
    {
      byte type;
      switch (v)
        {
          case 11:
            type = Card.ACE;
            break;

          case 10:
            final int cardInt = RandomSupply.rng.nextInt (4);
            switch (cardInt)
              {
                case 0:
                  type = 10;
                  break;
                case 1:
                  type = Card.JACK;
                  break;
                case 2:
                  type = Card.QUEEN;
                  break;
                case 3:
                  type = Card.KING;
                  break;
                default:
                  assert (false);
                  /* Silence the compiler.  */
                  type = -1;
              }
            break;

          default:
            type = (byte) v;
            break;
        }

      return new Card (RandomSupply.getRandomSuit (), type);
    }

    /**
     * Convert to string for logging.
     * @return String representation.
     */
    @Override
    public String toString ()
    {
      return String.format ("%s(%d, %d)", matrix.toString (), player, dealer);
    }

  }

  /** Serial version id.  */
  private static final long serialVersionUID = 0l;

  /**
   * Queue for entries to learn.  A deque would be ideal for this task, but
   * unfortunately ArrayDeque is not available in the SDK version targetted.
   * Thus use array list and take the (probably anyway unimportant)
   * performance hit from removing at the beginning anyway.
   */
  private ArrayList<Index> queue;

  /** Remember the current index for the sake of repeating it.  */
  private Index current;

  /**
   * Construct it, which starts with all possible entries shuffled into
   * a random ordering.
   */
  public SystematicTrainer ()
  {
    queue = new ArrayList<Index> ();
    fillIn (Strategy.Matrix.HARD, 5, 21);
    fillIn (Strategy.Matrix.SOFT, 13, 21);
    fillIn (Strategy.Matrix.PAIR, 2, 11);
    Collections.shuffle (queue);

    current = null;
  }

  /**
   * Get number of remaining entries to learn.
   * @return Number of remaining entries.
   */
  public int getRemainingCount ()
  {
    return queue.size ();
  }

  /**
   * Access the next game, returns null if the queue is already empty.
   * @param deck The card supply to use for the constructed game.
   * @param h17 The game's h17 value.
   * @return The next game to play or null.
   */
  public Game getNext (CardSupply deck, boolean h17)
  {
    if (queue.isEmpty ())
      return null;

    Log.i (TAG, String.format ("Training next entry, %d remaining.",
                               getRemainingCount ()));

    current = queue.remove (0);
    return current.constructGame (deck, h17);
  }

  /**
   * Assume the last game should be repeated.  This duplicates the queue's
   * head into a random position, since getNext() follows and pops the original
   * version off the queue.
   * @throws RuntimeException If no game is there to repeat.
   */
  public void repeat ()
  {
    if (current == null)
      throw new RuntimeException ("No game yet there to repeat!");
    Log.i (TAG, String.format ("Repeating last %s.", current.toString ()));

    int pos;
    if (queue.isEmpty ())
      pos = 0;
    else
      pos = RandomSupply.rng.nextInt (queue.size ()) + 1;
    queue.add (pos, current);
  }

  /**
   * Fill in the indices for a matrix type and given range of player
   * totals into the queue.
   * @param m Matrix type.
   * @param from From this player index.
   * @param to To this player index.
   */
  private void fillIn (Strategy.Matrix m, int from, int to)
  {
    for (int p = from; p <= to; ++p)
      for (int d = 2; d <= 11; ++d)
        queue.add (new Index (m, (byte) p, (byte) d));
  }

}
