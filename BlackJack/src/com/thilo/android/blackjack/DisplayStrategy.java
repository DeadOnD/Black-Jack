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

import android.app.Activity;

import android.content.SharedPreferences;

import android.os.Bundle;

import android.preference.PreferenceManager;

import android.webkit.WebView;

/**
 * Activity to display the optimal strategy to the user, which is done
 * by building a HTML page displayed then in a WebView.
 */
public class DisplayStrategy extends Activity
{

  /** Shared preferences used.  */
  private SharedPreferences pref;

  /** The strategy displayed.  */
  private Strategy optimal;

  /**
   * Create the activity.
   * @param savedInstanceState Saved state.
   */
  @Override
  public void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);
    PreferenceManager.setDefaultValues (this, R.xml.preferences, false);
    pref = PreferenceManager.getDefaultSharedPreferences (this);
    setContentView (R.layout.strategy_display);

    final boolean h17 = pref.getBoolean ("h17", false);
    optimal = new Strategy ();
    optimal.fill (getResources ().getXml (R.xml.strategy_stand17), false);
    if (h17)
      optimal.fill (getResources ().getXml (R.xml.strategy_h17), true);

    WebView v = (WebView) findViewById (R.id.strategy_display);
    v.loadData (buildHTML (), "text/html", null);
  }

  /**
   * Actually build the HTML page.
   * @return HTML data to display.
   */
  private String buildHTML ()
  {
    StringBuffer data = new StringBuffer ();
    data.append ("<!DOCTYPE html>");
    data.append ("<html><head>");

    data.append ("<style type='text/css'>");
    data.append ("table { border-collapse: collapse; }");
    data.append ("td, th { border: thin solid black; text-align: center; }");
    data.append (".hit { background-color: #0f0; }");
    data.append (".stand { background-color: red; }");
    data.append (".split { background-color: yellow; }");
    data.append (".double { background-color: cyan; }");
    data.append ("</style>");

    data.append ("</head><body>");

    data.append ("<p>");
    if (pref.getBoolean ("h17", false))
      data.append (getString (R.string.html_h17));
    else
      data.append (getString (R.string.html_noH17));
    data.append ("</p>");

    data.append ("<table>");
    data.append ("<thead>");
    data.append ("<tr>");
    data.append ("<th rowspan='2'>");
    data.append (getString (R.string.html_player_hand));
    data.append ("</th>");
    data.append ("<th colspan='10'>");
    data.append (getString (R.string.html_dealer_card));
    data.append ("</th>");
    data.append ("</tr>");
    data.append ("<tr>");
    putDealerHeader (data);
    data.append ("</tr>");
    data.append ("</thead>");

    buildMatrix (data, R.string.html_hard, Strategy.Matrix.HARD, 5, 20);
    buildMatrix (data, R.string.html_soft, Strategy.Matrix.SOFT, 13, 20);
    buildMatrix (data, R.string.html_pairs, Strategy.Matrix.PAIR, 2, 11);

    data.append ("</table>");
    data.append ("</body></html");

    return data.toString ();
  }

  /**
   * Build HTML for one of the three matrices.
   * @param data Put data here.
   * @param titleId String-ID for title to add.
   * @param mType Matrix type to show.
   * @param from From this player index.
   * @param to To this player index.
   */
  private void buildMatrix (StringBuffer data, int titleId,
                            Strategy.Matrix mType, int from, int to)
  {
    data.append ("<tbody><tr>");
    data.append ("<th colspan='11'>");
    data.append (getString (titleId));
    data.append ("</th></tr>");

    if (mType != Strategy.Matrix.HARD)
      {
        data.append ("<tr><td></td>");
        putDealerHeader (data);
        data.append ("</tr>");
      }

    final Strategy.MatrixEntry[][] m = optimal.getMatrix (mType);
    Strategy.MatrixEntry[] currentRow = null;
    String currentEnd = null, currentStart = null;
    for (int i = to; i >= from; --i)
      {
        boolean mismatch = false;
        if (currentRow == null)
          mismatch = true;
        else
          for (int j = 2; j <= 11; ++j)
            if (currentRow[j]  != m[i][j])
              {
                mismatch = true;
                break;
              }

        if (mismatch)
          {
            if (currentRow != null)
              buildRow (data, currentStart, currentEnd, currentRow);

            currentRow = m[i];
            currentEnd = null;
          }

        switch (mType)
          {
            case HARD:
              currentStart = String.format ("%d", i);
              break;

            case SOFT:
              currentStart = String.format ("A,%d", i - 11);
              break;

            case PAIR:
              String val;
              if (i == 11)
                val = "A";
              else
                val = String.format ("%d", i);
              currentStart = String.format ("%s,%s", val, val);
              break;

            default:
              assert (false);
          }

        if (currentEnd == null)
          currentEnd = currentStart;
      }
    buildRow (data, currentStart, currentEnd, currentRow);

    data.append ("</th></tr>");
    data.append ("</tbody>");
  }

  /**
   * Build HTML for one table row.
   * @param data Put data here.
   * @param start Start label.
   * @param end End label.
   * @param row Data of this row.
   */
  private void buildRow (StringBuffer data, String start, String end,
                         Strategy.MatrixEntry[] row)
  {
    String player;
    if (!start.equals (end))
      player = String.format ("%s-%s", start, end);
    else
      player = start;
    
    data.append (String.format ("<tr><th>%s</th>", player));
    for (int i = 2; i <= 11; ++i)
      {
        String clazz = null, content = null;
        switch (row[i])
          {
            case HIT:
              clazz = "hit";
              content = "H";
              break;

            case STAND:
              clazz = "stand";
              content = "S";
              break;

            case SPLIT:
              clazz = "split";
              content = "SP";
              break;

            case DOUBLE_HIT:
              clazz = "double";
              content = "Dh";
              break;

            case DOUBLE_STAND:
              clazz = "double";
              content = "Ds";
              break;

            default:
              assert (false);
          }
        data.append (String.format ("<td class='%s'>%s</td>", clazz, content));
      }
    data.append ("</tr>");
  }

  /**
   * Put HTML code for dealer header.
   * @param data Where to put the data.
   */
  private void putDealerHeader (StringBuffer data)
  {
    for (int i = 2; i <= 10; ++i)
      data.append (String.format ("<th>%d</th>", i));
    data.append ("<th>A</th>");
  }

}
