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
import android.app.Dialog;

import android.content.SharedPreferences;
import android.content.Intent;

import android.os.Bundle;

import android.preference.PreferenceManager;

import android.text.method.LinkMovementMethod;

import android.util.Log;

import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.ArrayList;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;


/**
 * The activity class for explicitly training the optimal strategy.
 */
public class StrategyTrainer extends Activity implements View.OnClickListener
{

  /** Log tag.  */
  private static final String TAG = "BJTrainer/StrategyTrainer";

  /** ID for help dialog.  */
  private static final int DIALOG_HELP = 0;
  /** ID for about dialog.  */
  private static final int DIALOG_ABOUT = 1;

  /** Display for player's hand.  */
  private HandDisplay playerDisplay;
  /** Display for dealer's hand.  */
  private HandDisplay dealerDisplay;

  /** Total view containing the whole layout.  */
  private View wholeLayout;
  /** Text view for showing status message.  */
  private TextView message;
  /** Text view for displaying total amount won.  */
  private TextView totalView;

  /** Hit button.  */
  private Button btnHit;
  /** Stand button.  */
  private Button btnStand;
  /** Double button.  */
  private Button btnDouble;
  /** Split button.  */
  private Button btnSplit;

  /** SharedPreferences handler.  */
  private SharedPreferences pref;

  /** Card supply used.  */
  private CardSupply deck;

  /** Systematic trainer instance used.  */
  private SystematicTrainer trainer;

  /** Stack of queued games from splits.  */
  private ArrayList<Game> gameStack;
  /** Current game in the UI.  */
  private Game currentGame;

  /** Optimal strategy.  */
  private Strategy optimal;
  /** Whether the calculated strategy is h17.  */
  private boolean h17Strategy;

  /** Running total gains.  */
  private float total;

  /** Keep track about whether the user answered wrong for current try.  */
  private boolean wrongAnswer;

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
    setContentView (R.layout.main);
    
    AdView mAdView = (AdView) findViewById(R.id.adView);
    AdRequest adRequest = new AdRequest.Builder().build();
    mAdView.loadAd(adRequest);

    CardImages img = new CardImages (getResources ());
    deck = new RandomSupply ();

    SurfaceView v = (SurfaceView) findViewById (R.id.player_cards);
    playerDisplay = new HandDisplay (this, img, v);

    v = (SurfaceView) findViewById (R.id.dealer_cards);
    dealerDisplay = new HandDisplay (this, img, v);

    wholeLayout = findViewById (R.id.whole_layout);
    message = (TextView) findViewById (R.id.game_message);
    totalView = (TextView) findViewById (R.id.total_display);

    btnHit = (Button) findViewById (R.id.hit_button);
    btnStand = (Button) findViewById (R.id.stand_button);
    btnDouble = (Button) findViewById (R.id.double_button);
    btnSplit = (Button) findViewById (R.id.split_button);

    wholeLayout.setOnClickListener (this);
    btnHit.setOnClickListener (this);
    btnStand.setOnClickListener (this);
    btnDouble.setOnClickListener (this);
    btnSplit.setOnClickListener (this);

    optimal = null;
    trainer = null;
    total = 0.0f;
    gameStack = new ArrayList<Game> ();
    startNewGame ();
  }

  /**
   * Save current instance state.
   * @param outState Bundle where to save it to.
   */
  @Override
  public void onSaveInstanceState (Bundle outState)
  {
    super.onSaveInstanceState (outState);
    outState.putFloat ("total", total);

    outState.putInt ("numGames", gameStack.size ());
    outState.putBoolean ("hasTrainer", trainer != null);
    try
      {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream ();
        ObjectOutputStream out = new ObjectOutputStream (byteOut);

        out.writeObject (currentGame);
        for (Game g : gameStack)
          out.writeObject (g);

        if (trainer != null)
          out.writeObject (trainer);

        out.close ();
        byteOut.close ();

        outState.putByteArray ("gameStack", byteOut.toByteArray ());
        Log.i (TAG, "Saved state successfully.");
      }
    catch (IOException exc)
      {
        exc.printStackTrace ();
        Log.w (TAG, "Saving state failed with IOException, not saving.");
      }
  }

  /**
   * Restore saved instance state.
   * @param savedInstanceState Saved state to restore.
   */
  @Override
  public void onRestoreInstanceState (Bundle savedInstanceState)
  {
    super.onRestoreInstanceState (savedInstanceState);
    total = savedInstanceState.getFloat ("total");

    final int numGames = savedInstanceState.getInt ("numGames");
    final byte[] data = savedInstanceState.getByteArray ("gameStack");

    /* Force re-intialisation of the strategy.  */
    optimal = null;
    trainer = null;

    boolean notRestored;
    if (data == null)
      notRestored = true;
    else
      try
        {
          notRestored = false;

          ByteArrayInputStream byteIn = new ByteArrayInputStream (data);
          ObjectInputStream in = new ObjectInputStream (byteIn);

          currentGame = (Game) in.readObject ();
          currentGame.setCardSupply (deck);

          gameStack = new ArrayList<Game> ();
          for (int i = 0; i < numGames; ++i)
            {
              Game g = (Game) in.readObject ();
              g.setCardSupply (deck);
              gameStack.add (g);
            }

          if (savedInstanceState.getBoolean ("hasTrainer"))
            trainer = (SystematicTrainer) in.readObject ();

          in.close ();
          byteIn.close ();

          Log.i (TAG, "Loaded state successfully.");
        }
      catch (Exception exc)
        {
          notRestored = true;
          exc.printStackTrace ();
          Log.w (TAG, "Loading state failed with Exception, not loading.");
        }

    /* If restoring failed, initialise empty.  */
    if (notRestored)
      {
        total = 0.0f;
        trainer = null;
        gameStack = new ArrayList<Game> ();
        startNewGame ();
      }
    else
      updateAll ();
  }

  /**
   * Handle UI clicks.
   * @param v The view clicked on.
   */
  public void onClick (View v)
  {
    if (!currentGame.isRunning ())
      {
        startNewGame ();
        return;
      }

    final Strategy.Decision dec = optimal.decide (currentGame);

    assert (currentGame.isRunning ());
    if (v == btnHit)
      {
        if (dec != Strategy.Decision.HIT)
          warnStrategy (dec);
        else
          currentGame.doHit ();
      }
    if (v == btnStand)
      {
        if (dec != Strategy.Decision.STAND)
          warnStrategy (dec);
        else
          currentGame.doStand ();
      }
    if (v == btnDouble)
      {
        if (!currentGame.canDouble ())
          {
            Toast t = Toast.makeText (this, getString (R.string.cantDouble),
                                      Toast.LENGTH_SHORT);
            t.setGravity (Gravity.CENTER, 0, 0);
            t.show ();
          }
        else if (dec != Strategy.Decision.DOUBLE)
          warnStrategy (dec);
        else
          currentGame.doDouble ();
      }
    if (v == btnSplit)
      {
        if (!currentGame.canSplit ())
          {
            Toast t = Toast.makeText (this, getString (R.string.cantSplit),
                                      Toast.LENGTH_SHORT);
            t.setGravity (Gravity.CENTER, 0, 0);
            t.show ();
          }
        else if (dec != Strategy.Decision.SPLIT)
          warnStrategy (dec);
        else
          {
            Game split = currentGame.doSplit (true);
            gameStack.add (split);
          }
      }
    update ();
  }

  /**
   * Create the menu.
   * @param menu The menu to create.
   * @return True for success.
   */
  @Override
  public boolean onCreateOptionsMenu (Menu menu)
  {
    MenuInflater inflater = getMenuInflater ();
    inflater.inflate (R.menu.main, menu);
    return true;
  }

  /**
   * Handle menu "click".
   * @param itm The menu item selected.
   * @return True if the event was handled.
   */
  @Override
  public boolean onOptionsItemSelected (MenuItem itm)
  {
    switch (itm.getItemId ())
      {
        case R.id.show_strategy:
          startActivity (new Intent (this, DisplayStrategy.class));
          return true;

        case R.id.preferences:
          startActivity (new Intent (this, Preferences.class));
          return true;

        case R.id.reset_trainer:
          deleteFile ("trainer");
          Log.d (TAG, "Deleted trainer data on local storage.");
          trainer = null;
          return true;

        case R.id.about:
          showDialog (DIALOG_ABOUT);
          return true;

        case R.id.help:
          showDialog (DIALOG_HELP);
          return true;

        default:
          return super.onOptionsItemSelected (itm);
      }
  }

  /**
   * Create a dialog.
   * @param id ID of dialog to create.
   */
  @Override
  public Dialog onCreateDialog (int id)
  {
    Dialog dlg = new Dialog (this);

    switch (id)
      {
        case DIALOG_HELP:
          dlg.setContentView (R.layout.help);

          TextView tv = (TextView) dlg.findViewById (R.id.help_link);
          tv.setMovementMethod (LinkMovementMethod.getInstance ());

          dlg.setTitle (R.string.help_title);
          break;

        case DIALOG_ABOUT:
          dlg.setContentView (R.layout.about);

          tv = (TextView) dlg.findViewById (R.id.about_version);
          final String aboutVersion = getString (R.string.about_version);
          final String appName = getString (R.string.app_name);
          final String appVersion = getString (R.string.app_version);
          tv.setText (String.format (aboutVersion, appName, appVersion));

          tv = (TextView) dlg.findViewById (R.id.about_link1);
          tv.setMovementMethod (LinkMovementMethod.getInstance ());
          tv = (TextView) dlg.findViewById (R.id.about_link2);
          tv.setMovementMethod (LinkMovementMethod.getInstance ());

          dlg.setTitle (R.string.about_title);
          break;

        default:
          assert (false);
      }

    return dlg;
  }

  /**
   * Start a fresh game.
   */
  private void startNewGame ()
  {
    final boolean h17 = pref.getBoolean ("h17", false);
    if (!gameStack.isEmpty ())
      currentGame = gameStack.remove (gameStack.size () - 1);
    else
      {
        if (pref.getBoolean ("train", false))
          {
            if (trainer == null)
              {
                restoreTrainer ();
                wrongAnswer = false;
              }

            if (wrongAnswer)
              trainer.repeat ();
            wrongAnswer = false;

            /* Do this before getNext(), so that the current index is
               not "lost" if quit before answering it!  */
            saveTrainer ();

            final Game next = trainer.getNext (deck, h17);
            if (next == null)
              {
                final String msg = getString (R.string.finished_learning);
                deleteFile ("trainer");
                Toast t = Toast.makeText (this, msg, Toast.LENGTH_SHORT);
                t.setGravity (Gravity.CENTER, 0, 0);
                t.show ();
                return;
              }
            else
              currentGame = next;
          }
        else
          {
            Hand player = new Hand ();
            player.add (deck.getNextCard ());
            player.add (deck.getNextCard ());

            Hand dealer = new Hand ();
            dealer.add (deck.getNextCard ());

            currentGame = new Game (player, dealer, deck, h17);
          }
      }

    assert (currentGame != null);
    updateAll ();
  }

  /**
   * Update everything for a new game, including the displays but also
   * the strategy.
   */
  private void updateAll ()
  {
    update ();
    if (optimal == null || (currentGame.hitSoft17 != h17Strategy))
      {
        optimal = new Strategy ();
        optimal.fill (getResources ().getXml (R.xml.strategy_stand17), false);
        if (currentGame.hitSoft17)
          optimal.fill (getResources ().getXml (R.xml.strategy_h17), true);

        h17Strategy = currentGame.hitSoft17;
      }
  }

  /**
   * Update displays.
   */
  private void update ()
  {
    playerDisplay.setHand (currentGame.getPlayerHand ());
    dealerDisplay.setHand (currentGame.getDealerHand ());

    String msg = "";
    if (currentGame.isRunning ())
      msg = getString (R.string.player_choice);
    else
      {
        final byte playerTotal = currentGame.getPlayerHand ().getTotal ();
        final byte dealerTotal = currentGame.getDealerHand ().getTotal ();
        switch (currentGame.getResult ())
          {
            case PLAYER_BLACKJACK:
              msg = getString (R.string.player_blackjack);
              break;
            case PLAYER_BUSTED:
              msg = getString (R.string.player_busted);
              break;
            case PLAYER_WON:
              msg = String.format (getString (R.string.player_won),
                                   playerTotal, dealerTotal);
              break;
            case DEALER_BLACKJACK:
              msg = getString (R.string.dealer_blackjack);
              break;
            case DEALER_BUSTED:
              msg = getString (R.string.dealer_busted);
              break;
            case DEALER_WON:
              msg = String.format (getString (R.string.dealer_won),
                                   playerTotal, dealerTotal);
              break;
            case PUSH:
              assert (playerTotal == dealerTotal);
              msg = String.format (getString (R.string.push), playerTotal);
              break;
          }

        total += currentGame.getPayout ();
      }
    message.setText (msg);

    String extraMsg = "";
    if (!pref.getBoolean ("train", false))
      extraMsg = String.format (getString (R.string.total_template), total);
    else if (trainer != null)
      extraMsg = String.format (getString (R.string.remaining_template),
                                trainer.getRemainingCount ());
    totalView.setText (extraMsg);
  }

  /**
   * Warn the user about suboptimal strategy decision made.
   * @param d Optimal one.
   */
  private void warnStrategy (Strategy.Decision d)
  {
    if (currentGame.isInitial ())
      wrongAnswer = true;

    String msg = "";
    switch (d)
      {
        case HIT:
          msg = getString (R.string.btnHit);
          break;
        case STAND:
          msg = getString (R.string.btnStand);
          break;
        case DOUBLE:
          msg = getString (R.string.btnDouble);
          break;
        case SPLIT:
          msg = getString (R.string.btnSplit);
          break;
        default:
          assert (false);
      }
    msg = String.format (getString (R.string.suboptimal_decision), msg);

    Toast t = Toast.makeText (this, msg, Toast.LENGTH_SHORT);
    t.setGravity (Gravity.CENTER, 0, 0);
    t.show ();
  }

  /**
   * Save the strategy trainer to persistent internal storage.
   */
  private void saveTrainer ()
  {
    try
      {
        FileOutputStream fileOut = openFileOutput ("trainer", MODE_PRIVATE);
        BufferedOutputStream bufferedOut = new BufferedOutputStream (fileOut);
        ObjectOutputStream out = new ObjectOutputStream (bufferedOut);

        out.writeObject (trainer);

        out.close ();
        bufferedOut.close ();
        fileOut.close ();

        Log.d (TAG, "Saved trainer state.");
      }
    catch (IOException exc)
      {
        exc.printStackTrace ();
        Log.e (TAG, "Saving trainer to persistent storage failed!");
      }
  }

  /**
   * Restore the trainer from local storage, if there is one.  Construct
   * a new one if there's none.
   */
  private void restoreTrainer ()
  {
    trainer = null;
    try
      {
        FileInputStream fileIn = openFileInput ("trainer");
        BufferedInputStream bufferedIn = new BufferedInputStream (fileIn);
        ObjectInputStream in = new ObjectInputStream (bufferedIn);

        trainer = (SystematicTrainer) in.readObject ();
        Log.d (TAG, "Restored trainer state.");

        in.close ();
        bufferedIn.close ();
        fileIn.close ();
      }
    catch (FileNotFoundException exc)
      {
        /* Do nothing, this is not really a serious error and just means
           that no trainer was saved yet.  */
      }
    catch (Exception exc)
      {
        exc.printStackTrace ();
        Log.e (TAG, "Reading trainer from persistent storage failed!");
      }

    if (trainer == null)
      trainer = new SystematicTrainer ();
  }

}
