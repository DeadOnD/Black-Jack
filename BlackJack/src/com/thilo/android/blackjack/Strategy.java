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

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * A playing strategy, given as matrix with optimal decisions based
 * on player total and dealer card.
 */
public class Strategy
{

  /** Namespace for strategy XML files.  */
  private static final String NS
    = "http://www.domob.eu/projects/bjtrainer/strategy/";

  /**
   * Possible decisions that can be entries in the matrix.
   * @see Decision
   */
  public enum MatrixEntry
  {
    NAN,
    STAND,
    HIT,
    SPLIT,
    DOUBLE_HIT,
    DOUBLE_STAND
  }

  /**
   * Possible types of matrices.
   */
  public enum Matrix
  {
    HARD,
    SOFT,
    PAIR
  }

  /**
   * Possible decisions as returned when queried.  The difference to
   * MatrixEntry is that here we already resolve double vs hit/stand.
   * @see MatrixEntry
   */
  public enum Decision
  {
    STAND,
    HIT,
    SPLIT,
    DOUBLE
  }

  /* For simplicity, we store entries without translating the index, thus
     keeping some entries empty.  First index always represents the player's
     cards, and the second index the dealer's card.

  /** Decisions on hard totals.  */
  MatrixEntry[][] hard;

  /** Decisions on soft totals.  */
  MatrixEntry[][] soft;

  /** Decisions on pairs.  Player index is single card, not total.  */
  MatrixEntry[][] pair;

  /**
   * Construct an empty matrix.
   */
  public Strategy ()
  {
    hard = new MatrixEntry[22][12];
    soft = new MatrixEntry[22][12];
    pair = new MatrixEntry[12][12];

    nanMatrix (hard);
    nanMatrix (soft);
    nanMatrix (pair);
  }

  /**
   * Given a player and dealer hand, decide what to do.
   * @param g The current game.
   * @return Playing decision according to the strategy.
   * @throws RuntimeException If the strategy has no entry.
   */
  public Decision decide (Game g)
  {
    final Hand player = g.getPlayerHand ();
    final Hand dealer = g.getDealerHand ();
    final byte dealerTotal = dealer.getTotal ();

    MatrixEntry entry;
    if (player.isPair ())
      {
        final byte pairValue = player.getPairValue ();
        entry = pair[pairValue][dealerTotal];
      }
    else
      {
        final byte playerTotal = player.getTotal ();
        if (player.isSoft ())
          entry = soft[playerTotal][dealerTotal];
        else
          entry = hard[playerTotal][dealerTotal];
      }

    switch (entry)
      {
        case NAN:
          throw new RuntimeException ("No matching strategy entry found!");

        case HIT:
          return Decision.HIT;

        case STAND:
          return Decision.STAND;

        case SPLIT:
          return Decision.SPLIT;

        case DOUBLE_HIT:
          if (player.canDouble ())
            return Decision.DOUBLE;
          return Decision.HIT;

        case DOUBLE_STAND:
          if (player.canDouble ())
            return Decision.DOUBLE;
          return Decision.STAND;

        default:
          assert (false);
      }

    /* Silence compiler.  */
    return Decision.STAND;
  }

  /**
   * Fill by parsing an XML file.
   * @param p The parser to use.
   * @param overwrite Assume already filled in matrix and only change
   *                  the given entries by overwriting them.
   * @throws RuntimeException On error with the parsing.
   */
  public void fill (XmlPullParser p, boolean overwrite)
  {
    try
      {
        p.setFeature (XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        if (p.next () != XmlPullParser.START_DOCUMENT)
          throw new RuntimeException ("Expected document start event!");
        p.nextTag ();
        if (!checkTag (p, "strategy"))
          throw new RuntimeException ("Expected strategy as root element!");

        p.nextTag ();
        if (!checkTag (p, "hard"))
          throw new RuntimeException ("Expected hard tag!");
        parseMatrix (p, hard, overwrite);

        p.nextTag ();
        if (!checkTag (p, "soft"))
          throw new RuntimeException ("Expected soft tag!");
        parseMatrix (p, soft, overwrite);

        p.nextTag ();
        if (!checkTag (p, "pairs"))
          throw new RuntimeException ("Expected pair tag!");
        parseMatrix (p, pair, overwrite);

        if (p.next () != XmlPullParser.END_TAG)
          throw new RuntimeException ("Additional matrix-tags found!");
        if (p.next () != XmlPullParser.END_DOCUMENT)
          throw new RuntimeException ("Expected end of document!");
      }
    catch (XmlPullParserException exc)
      {
        exc.printStackTrace ();
        throw new RuntimeException ("Parsing failed: " + exc.getMessage ());
      }
    catch (IOException exc)
      {
        exc.printStackTrace ();
        throw new RuntimeException ("Reading XML failed: " + exc.getMessage ());
      }

    if (!filledIn ())
      throw new RuntimeException ("Matrix not fully filled in by XML!");
  }

  /**
   * Return the matrices.  This is used for the strategy display.
   * @param m The matrix queried for.
   * @return This matrix.
   */
  public MatrixEntry[][] getMatrix (Matrix m)
  {
    switch (m)
      {
        case HARD:
          return hard;
        case SOFT:
          return soft;
        case PAIR:
          return pair;
        default:
          assert (false);
      }

    /* Silence compiler.  */
    return null;
  }

  /**
   * Check that every entry that should be filled in is actually
   * already filled in.
   * @return True iff this is the case.
   */
  private boolean filledIn ()
  {
    if (!filledIn (hard, 5, 21))
      return false;
    if (!filledIn (soft, 13, 21))
      return false;
    if (!filledIn (pair, 2, 11))
      return false;

    return true;
  }

  /**
   * Helper routine to fill a matrix with NAN values.
   * @param m Matrix to fill.
   */
  private static void nanMatrix (MatrixEntry[][] m)
  {
    for (int i = 0; i < m.length; ++i)
      for (int j = 0; j < m[i].length; ++j)
        m[i][j] = MatrixEntry.NAN;
  }

  /**
   * Helper routine checking whether a matrix is filled in properly.
   * @param m The matrix to check.
   * @param from Check from this player index.
   * @param to Check to this player index.
   * @return True iff the selected part is all different from NAN.
   */
  private static boolean filledIn (MatrixEntry[][] m, int from, int to)
  {
    for (int i = from; i <= to; ++i)
      for (int j = 2; j <= 11; ++j)
        if (m[i][j] == MatrixEntry.NAN)
          {
          android.util.Log.d ("", i + " " + j);
          return false;
          }

    return true;
  }

  /**
   * Helper routine to check for found element, handling the namespace.
   * @param p The parser to use.
   * @param el The element we want.
   * @return True iff the current element is the one.
   * @throws RuntimeException If namespace does not match.
   */
  private static boolean checkTag (XmlPullParser p, String el)
  {
    if (!p.getNamespace ().equals (NS))
      throw new RuntimeException ("Wrong namespace in strategy XML!");
    return p.getName ().equals (el);
  }

  /**
   * Parse parts of one matrix.
   * @param p The parser to use.
   * @param m The matrix to fill in.
   * @param overwrite Assume matrix already filled in and overwrite it.
   * @throws RuntimeException If parsing fails.
   * @throws XmlPullParserException If the parser throws;
   * @throws IOException If reading the XML fails.
   */
  private static void parseMatrix (XmlPullParser p, MatrixEntry[][] m,
                                   boolean overwrite)
    throws XmlPullParserException, IOException
  {
    while (true)
      {
        final int type = p.nextTag ();
        if (type == XmlPullParser.END_TAG)
          return;
        assert (type == XmlPullParser.START_TAG);
        if (!checkTag (p, "group"))
          throw new RuntimeException ("Expected group tag!");

        final String player = p.getAttributeValue (null, "player");
        final String dealer = p.getAttributeValue (null, "dealer");
        if (player == null || dealer == null)
          throw new RuntimeException ("player or dealer attribute missing!");

        final int[] playerBds = parseBounds (player);
        final int[] dealerBds = parseBounds (dealer);

        final String action = p.nextText ();
        MatrixEntry value;
        if (action.equals ("H"))
          value = MatrixEntry.HIT;
        else if (action.equals ("S"))
          value = MatrixEntry.STAND;
        else if (action.equals ("SP"))
          value = MatrixEntry.SPLIT;
        else if (action.equals ("Dh"))
          value = MatrixEntry.DOUBLE_HIT;
        else if (action.equals ("Ds"))
          value = MatrixEntry.DOUBLE_STAND;
        else
          throw new RuntimeException ("Invalid action: " + action);

        for (int i = playerBds[0]; i <= playerBds[1]; ++i)
          for (int j = dealerBds[0]; j <= dealerBds[1]; ++j)
            {
              if (!overwrite && m[i][j] != MatrixEntry.NAN)
                throw new RuntimeException ("Cell already filled in!");
              else if (overwrite && m[i][j] == MatrixEntry.NAN)
                throw new RuntimeException ("Overwriting still empty cell!");
              m[i][j] = value;
            }

        /* nextText advances to END_TAG already!  */
        if (p.getEventType () != XmlPullParser.END_TAG)
          throw new RuntimeException ("Expected group end tag!");
      }
  }

  /**
   * Parse bounds in the form given in the XML file.  Can be either a single
   * number, or of the form A-B.  Returned is a 2 element array with lower
   * and upper bound.
   * @param str String representation.
   * @return Bounds as 2 element array [lower, upper].
   * @throws RuntimeException If format is wrong.
   */
  private static int[] parseBounds (String str)
  {
    StringBuffer before = new StringBuffer ();
    StringBuffer after = new StringBuffer ();
    boolean seenDash = false;

    for (int i = 0; i < str.length (); ++i)
      {
        final char c = str.charAt (i);
        if (c == '-')
          {
            if (seenDash)
              throw new RuntimeException ("Found two dashes in bounds!");
            seenDash = true;
          }
        else if (c >= '0' && c <= '9')
          {
            if (seenDash)
              after.append (c);
            else
              before.append (c);
          }
        else
          throw new RuntimeException ("Invalid bounds string: " + str);
      }

    if (before.length () == 0)
      throw new RuntimeException ("No from index given in bounds!");
    if (seenDash && after.length () == 0)
      throw new RuntimeException ("Dash but no to index given in bounds!");

    int[] res = new int[2];
    res[0] = Integer.parseInt (before.toString ());
    if (seenDash)
      res[1] = Integer.parseInt (after.toString ());
    else
      res[1] = res[0];

    return res;
  }

}
