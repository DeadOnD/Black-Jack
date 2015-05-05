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

import java.util.Random;

/**
 * Card supply that simply produces fully random cards.
 */
public class RandomSupply implements CardSupply
{

  /** RNG used.  Also available to other consumers.  */
  public static Random rng = new Random ();

  /**
   * Construct it.
   */
  public RandomSupply ()
  {
    // Nothing to do.
  }

  /**
   * Draw a card.
   * @return A new card.
   */
  public Card getNextCard ()
  {
    final long typeInt = rng.nextInt (13) + 1;
    assert (typeInt >= Card.ACE && typeInt <= Card.KING);

    return new Card (getRandomSuit (), (byte) typeInt);
  }

  /**
   * Construct a random suit.  This is used also for constructing systematic
   * training situations and thus provided publicly.
   * @return A random suit value.
   */
  public static Card.Suit getRandomSuit ()
  {
    final int suitInt = rng.nextInt (4);
    Card.Suit suit = Card.Suit.HEARTS;
    switch (suitInt)
      {
        case 0:
          suit = Card.Suit.DIAMONDS;
          break;
        case 1:
          suit = Card.Suit.HEARTS;
          break;
        case 2:
          suit = Card.Suit.SPADES;
          break;
        case 3:
          suit = Card.Suit.CLUBS;
          break;
        default:
          assert (false);
      }

    return suit;
  }

}
