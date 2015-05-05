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

import java.io.Serializable;

/**
 * Non-UI (general) stuff for managing a single BlackJack game.  This keeps
 * track of player decisions and manages the dealer's actions as well as
 * finding the game outcome.  It is serializable,
 * which is however only used to store the state in Android.  A non-Android
 * way is used for that so that this class can be used for non-Android
 * standalone Java game simulation.
 */
public class Game implements Serializable
{

  /** Serial version id.  */
  private static final long serialVersionUID = 0l;

  /**
   * Possible game endings.
   */
  public enum Ending
  {
    PLAYER_BLACKJACK,
    PLAYER_BUSTED,
    PLAYER_WON, /* Just higher score.  */
    PUSH,
    DEALER_BLACKJACK,
    DEALER_BUSTED,
    DEALER_WON /* Just higher score.  */
  }

  /** Setting of dealer hits soft 17 to use in this game.  */
  public final boolean hitSoft17;

  /** The player's hand.  */
  private Hand player;

  /** The dealer's hand.  */
  private Hand dealer;

  /** Card supply for drawing.  */
  private transient CardSupply deck;

  /** Whether we have already calculated current state.  */
  private boolean calculated;

  /** Did the player double?  */
  private boolean doubled;
  /** Is this a split hand?  */
  private boolean split;

  /** Is the game running, i.e., waiting for player's choice?  */
  private boolean running;
  /** If the game is finished, result.  */
  private Ending result;
  /** Payout factor in case of end.  */
  private float payout;

  /**
   * Construct it with initial hands for player and dealer as well as supply.
   * @param p Player hand.
   * @param d Dealer hand.
   * @param s Card supply to use.
   * @param h17 Dealer hits soft 17?
   */
  public Game (Hand p, Hand d, CardSupply s, boolean h17)
  {
    player = p;
    dealer = d;
    deck = s;
    hitSoft17 = h17;

    running = true;
    doubled = false;
    split = false;
    calculate ();
  }

  /**
   * Perform player hit.
   * @throws RuntimeException If the game is already finished.
   */
  public void doHit ()
  {
    if (!running)
      throw new RuntimeException ("Game is already finished!");

    player.add (deck.getNextCard ());
    calculate ();
  }

  /**
   * Perform player stand.
   * @throws RuntimeException If the game is already finished.
   */
  public void doStand ()
  {
    if (!running)
      throw new RuntimeException ("Game is already finished!");

    /* Now play the dealer.  */
    while (dealer.getTotal () < 17
           || (hitSoft17 && dealer.getTotal () == 17 && dealer.isSoft ()))
      dealer.add (deck.getNextCard ());

    running = false;
    calculate ();
  }

  /**
   * Perform player double.
   * @throws RuntimeException If the game is already finished.
   * @throws RuntimeException If the player can not double.
   */
  public void doDouble ()
  {
    if (!player.canDouble ())
      throw new RuntimeException ("Player can not double!");
    assert (!doubled);
    doubled = true;

    doHit ();
    doStand ();
  }

  /**
   * Perform a split.  The dealer's hand is either copied or simply
   * referenced from both.  In the first case, the player can play both via
   * the UI, and in the second, the dealer's draws will be the same for the
   * split hands, as they should be in reality.
   * @param copyDealer Copy the dealer's hand rather than referencing it.
   * @return The newly generated second game.
   * @throws RuntimeException If the game is already finished.
   * @throws RuntimeException If the player can not split.
   */
  public Game doSplit (boolean copyDealer)
  {
    if (!running)
      throw new RuntimeException ("Game is already finished!");
    
    Hand newPlayer = player.split ();
    Hand newDealer;
    if (copyDealer)
      newDealer = new Hand (dealer);
    else
      newDealer = dealer;

    Game res = new Game (newPlayer, newDealer, deck, hitSoft17);
    res.split = true;
    split = true;

    doHit ();
    res.doHit ();

    return res;
  }

  /**
   * Get whether the game is running or not.
   * @return True iff running.
   */
  public boolean isRunning ()
  {
    return running;
  }

  /**
   * Return game ending.
   * @return Game ending.
   * @throws RuntimeException If the game is still running.
   */
  public Ending getResult ()
  {
    if (running)
      throw new RuntimeException ("Game is still running!");
    return result;
  }

  /**
   * Return game payout.
   * @return Game payout.
   * @throws RuntimeException If the game is still running.
   */
  public float getPayout ()
  {
    if (running)
      throw new RuntimeException ("Game is still running!");
    return payout;
  }

  /**
   * Get player hand.
   * @return Player hand.
   */
  public Hand getPlayerHand ()
  {
    return player;
  }

  /**
   * Get dealer hand.
   * @return Dealer hand.
   */
  public Hand getDealerHand ()
  {
    return dealer;
  }

  /**
   * Query whether the player can double at the moment.
   * @return True iff yes.
   * @throws RuntimeException If the game is already over.
   */
  public boolean canDouble ()
  {
    if (!running)
      throw new RuntimeException ("Game is already finished!");

    return player.canDouble ();
  }

  /**
   * Query whether the player can split at the moment.
   * @return True iff yes.
   * @throws RuntimeException If the game is already over.
   */
  public boolean canSplit ()
  {
    if (!running)
      throw new RuntimeException ("Game is already finished!");

    return player.isPair ();
  }

  /**
   * Query whether this game is "initial", which means that the player
   * has not yet done a move.
   * @return True iff the game is initial.
   */
  public boolean isInitial ()
  {
    return !split && player.getCards ().size () == 2;
  }

  /**
   * Set the card supply.  This is used to set it after deserializing, since
   * the card deck is declared transient.
   * @param d The new deck to use.
   */
  public void setCardSupply (CardSupply d)
  {
    deck = d;
  }

  /**
   * Update internal stats.
   */
  private void calculate ()
  {
    final boolean playerBJ = (!split && player.isBlackJack ());
    final boolean dealerBJ = dealer.isBlackJack ();

    if (playerBJ && dealerBJ)
      {
        running = false;
        result = Ending.PUSH;
        payout = 0.0f;
      }
    else if (player.getTotal () > 21)
      {
        running = false;
        result = Ending.PLAYER_BUSTED;
        payout = -1.0f;
      }
    else if (playerBJ)
      {
        running = false;
        result = Ending.PLAYER_BLACKJACK;
        payout = 1.5f;
      }
    else if (dealer.getTotal () > 21)
      {
        running = false;
        result = Ending.DEALER_BUSTED;
        payout = 1.0f;
      }
    else if (dealerBJ)
      {
        running = false;
        result = Ending.DEALER_BLACKJACK;
        payout = -1.0f;
      }
    else if (player.getTotal () == dealer.getTotal ())
      {
        /* running not known!  */
        result = Ending.PUSH;
        payout = 0.0f;
      }
    else if (player.getTotal () > dealer.getTotal ())
      {
        /* running not known!  */
        result = Ending.PLAYER_WON;
        payout = 1.0f;
      }
    else
      {
        assert (player.getTotal () < dealer.getTotal ());
        /* running not known!  */
        result = Ending.DEALER_WON;
        payout = -1.0f;
      }

    if (doubled)
      payout *= 2.0f;
  }

}
