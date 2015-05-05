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
 * An "abstract" card.  This simply contains the card's type and some
 * basic routines, and has nothing to do with the UI.  It is serializable,
 * which is however only used to store the state in Android.  A non-Android
 * way is used for that so that this class can be used for non-Android
 * standalone Java game simulation.
 */
public class Card implements Serializable
{

  /** Serial version id.  */
  private static final long serialVersionUID = 0l;

  /**
   * Enum for the card suits.
   */
  public static enum Suit
  {
    DIAMONDS,
    HEARTS,
    SPADES,
    CLUBS
  }

  /* In constrast to the suits, the types within a suit are not stored
     as enum but rather as integer.  That way, 2-10 can be represented
     directly.  */
  /** Type for ace.  */
  public static final byte ACE = 1;
  public static final byte JACK = 11;
  public static final byte QUEEN = 12;
  public static final byte KING = 13;

  /** Suit of this card.  */
  public final Suit suit;
  /** Type of this card.  */
  public final byte type;

  /**
   * Construct it.
   * @param s The suit.
   * @param t The type.
   */
  public Card (Suit s, byte t)
  {
    suit = s;
    type = t;
  }

  /**
   * Return the black jack value of the card.  Aces are counted as 11
   * here, and "soft" values are handled differently.
   * @return BlackJack value of this card, aces always as 11.
   */
  public byte getValue ()
  {
    switch (type)
      {
        case ACE:
          return 11;

        case JACK:
        case QUEEN:
        case KING:
          return 10;

        default:
          assert (type >= 2 && type <= 10);
          return type;
      }
  }

  /**
   * Return if this card is an ace.  Helper routine for the handling of
   * soft values.
   * @return True iff this is an ace.
   */
  public boolean isAce ()
  {
    return type == ACE;
  }

  /**
   * Check if two cards form a black jack.
   * @param a First card.
   * @param b Second card.
   * @return True iff a and b give black jack.
   */
  public static boolean isBlackJack (Card a, Card b)
  {
    return a.getValue () + b.getValue () == 21;
  }

}
