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

import java.util.ArrayList;
import java.util.List;

/**
 * Non-UI stuff about a full hand (a set of cards).  It is serializable,
 * which is however only used to store the state in Android.  A non-Android
 * way is used for that so that this class can be used for non-Android
 * standalone Java game simulation.
 */
public class Hand implements Serializable
{

  /** Serial version id.  */
  private static final long serialVersionUID = 0l;

  /** Cards in this hand.  */
  private List<Card> cards;

  /** Current total value.  */
  private byte total;

  /** Whether the total is soft.  */
  private boolean soft;

  /** Whether we hold a blackjack.  */
  private boolean blackJack;

  /** Whether this is a pair that can be split.  */
  private boolean pair;

  /**
   * Construct it.  Will be empty for now.
   */
  public Hand ()
  {
    cards = new ArrayList<Card> ();
    calculate ();
  }

  /**
   * Copy constructor.
   * @param h The other hand, which is copied.
   */
  public Hand (Hand h)
  {
    cards = new ArrayList<Card> ();
    for (Card c : h.cards)
      cards.add (new Card (c.suit, c.type));
    calculate ();
  }

  /**
   * Clear it, by removing all cards.
   */
  public void reset ()
  {
    cards.clear ();
    calculate ();
  }

  /**
   * Add a card.
   * @param c The card to add.
   */
  public void add (Card c)
  {
    cards.add (c);
    calculate ();
  }

  /**
   * Split the hand.  This is only possible for pairs and removes one of
   * the cards, returning it as a new hand.
   * @return The second hand generated.
   * @throws RuntimeException If this is not a pair.
   */
  public Hand split ()
  {
    if (!isPair ())
      throw new RuntimeException ("Hand is not a pair!");

    Hand res = new Hand ();
    res.add (cards.get (1));
    /* res is calculated above already.  */

    cards.remove (1);
    calculate ();

    return res;
  }

  /**
   * Access the cards.
   * @return List of all cards.
   */
  public List<Card> getCards ()
  {
    return cards;
  }

  /**
   * Perform calculation of data values.
   */
  private void calculate ()
  {
    byte aces = 0;
    total = 0;
    for (Card c : cards)
      {
        total += c.getValue ();
        if (c.isAce ())
          ++aces;
      }
    while (aces > 0 && total > 21)
      {
        --aces;
        total -= 10;
      }
    soft = (aces > 0);
    
    blackJack = false;
    pair = false;
    if (cards.size () == 2)
      {
        final Card a = cards.get (0);
        final Card b = cards.get (1);

        if (a.getValue () == b.getValue ())
          pair = true;
        if (Card.isBlackJack (a, b))
          blackJack = true;
      }
  }

  /**
   * Query for total value.
   * @return Total.
   */
  public byte getTotal ()
  {
    return total;
  }

  /**
   * Query whether we have a soft total.
   * @return True iff the total is soft.
   */
  public boolean isSoft ()
  {
    return soft;
  }

  /**
   * Query whether we have a blackjack.
   * @return True iff we have a blackjack.
   */
  public boolean isBlackJack ()
  {
    return blackJack;
  }

  /**
   * Query whether we have a split-able pair.
   * @return True iff this is a pair.
   */
  public boolean isPair ()
  {
    return pair;
  }

  /**
   * If this is a pair, get the pair card value.
   * @return The pair card value.
   * @throws RuntimeException If this is not a pair.
   */
  public byte getPairValue ()
  {
    if (!isPair ())
      throw new RuntimeException ("This is not a pair!");

    assert (cards.size () == 2);
    assert (cards.get (0).getValue () == cards.get (1).getValue ());

    return cards.get (0).getValue ();
  }

  /**
   * Query whether the hand is busted.
   * @return True iff it is busted.
   */
  public boolean isBusted ()
  {
    return total > 21;
  }

  /**
   * Query whether we can double.  This basically is simply two cards.
   * @return True iff one can double on this hand.
   */
  public boolean canDouble ()
  {
    return cards.size () == 2;
  }

}
