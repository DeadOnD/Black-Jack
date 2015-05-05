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

import android.content.res.Resources;

import android.graphics.drawable.Drawable;

import android.util.Log;

/**
 * This class handles access to the card image ressources.  It is intended
 * so that these can be changed flexibly, from access via multiple files to
 * a single large image which is cropped and the like.
 */
public class CardImages
{

  /** Log tag for this class.  */
  private static final String TAG = "BJTrainer/CardImages";

  /** The stored ressources object.  */
  private Resources res;

  /** Store a dummy ressource here for size requests.  */
  private Drawable dummy;

  /**
   * Construct it.
   * @param r Ressources to use.
   */
  public CardImages (Resources r)
  {
    res = r;
    dummy = getCard (new Card (Card.Suit.CLUBS, Card.JACK));

    Log.d (TAG, String.format ("Card images:"));
    Log.d (TAG, String.format ("  width:  %d", getWidth ()));
    Log.d (TAG, String.format ("  height: %d", getHeight ()));
    Log.d (TAG, String.format ("  shift:  %d", getMinShift ()));
  }

  /**
   * Get width of images returned.
   * @return Card width in pixels.
   */
  public int getWidth ()
  {
    return dummy.getIntrinsicWidth ();
  }

  /**
   * Get height of images returned.
   * @return Card height in pixels.
   */
  public int getHeight ()
  {
    return dummy.getIntrinsicHeight ();
  }

  /**
   * Get minimum amount (also in pixels) we have to shift a card to the right
   * in order to have the lower one visible.
   * @return Minimum shift amount in pixels.
   */
  public int getMinShift ()
  {
    return getWidth () * 12 / 72;
  }

  /**
   * Get the drawable for a specified card.
   * @param c The card we want to draw.
   * @return Drawable for it.
   */
  public Drawable getCard (Card c)
  {
    int suit = 0;
    switch (c.suit)
      {
        case CLUBS:
          suit = 1;
          break;
        case SPADES:
          suit = 2;
          break;
        case HEARTS:
          suit = 3;
          break;
        case DIAMONDS:
          suit = 4;
          break;
        default:
          assert (false);
      }

    int type;
    switch (c.type)
      {
        case Card.ACE:
          type = 0;
          break;
        case Card.KING:
          type = 1;
          break;
        case Card.QUEEN:
          type = 2;
          break;
        case Card.JACK:
          type = 3;
          break;
        default:
          type = 14 - c.type;
          break;
      }

    final int ind = suit + 4 * type;
    return res.getDrawable (IDs[ind]);
  }

  /** Auto-generated array mapping integers to IDs of corresponding cards.  */
  private static final int[] IDs = new int[]
    {
      -1
, R.drawable.card_1
, R.drawable.card_2
, R.drawable.card_3
, R.drawable.card_4
, R.drawable.card_5
, R.drawable.card_6
, R.drawable.card_7
, R.drawable.card_8
, R.drawable.card_9
, R.drawable.card_10
, R.drawable.card_11
, R.drawable.card_12
, R.drawable.card_13
, R.drawable.card_14
, R.drawable.card_15
, R.drawable.card_16
, R.drawable.card_17
, R.drawable.card_18
, R.drawable.card_19
, R.drawable.card_20
, R.drawable.card_21
, R.drawable.card_22
, R.drawable.card_23
, R.drawable.card_24
, R.drawable.card_25
, R.drawable.card_26
, R.drawable.card_27
, R.drawable.card_28
, R.drawable.card_29
, R.drawable.card_30
, R.drawable.card_31
, R.drawable.card_32
, R.drawable.card_33
, R.drawable.card_34
, R.drawable.card_35
, R.drawable.card_36
, R.drawable.card_37
, R.drawable.card_38
, R.drawable.card_39
, R.drawable.card_40
, R.drawable.card_41
, R.drawable.card_42
, R.drawable.card_43
, R.drawable.card_44
, R.drawable.card_45
, R.drawable.card_46
, R.drawable.card_47
, R.drawable.card_48
, R.drawable.card_49
, R.drawable.card_50
, R.drawable.card_51
, R.drawable.card_52
, R.drawable.card_53
, R.drawable.card_54
    };

}
