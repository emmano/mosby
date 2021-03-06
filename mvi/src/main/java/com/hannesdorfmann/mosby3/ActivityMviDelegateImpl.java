/*
 * Copyright 2015 Hannes Dorfmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hannesdorfmann.mosby3;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.hannesdorfmann.mosby3.mvi.MviPresenter;
import com.hannesdorfmann.mosby3.mvp.MvpPresenter;
import com.hannesdorfmann.mosby3.mvp.MvpView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.UUID;

/**
 * The concrete implementation of {@link ActivityMviDelegate}.
 * This delegate creates the Presenter and attaches the View to the presenter in {@link
 * Activity#onStart()}. The view is detached from presenter in {@link
 * Activity#onStop()}
 *
 * @param <V> The type of {@link MvpView}
 * @param <P> The type of {@link MvpPresenter}
 * @author Hannes Dorfmann
 * @see ActivityMviDelegate
 * @since 3.0
 */
public class ActivityMviDelegateImpl<V extends MvpView, P extends MviPresenter<V, ?>>
    implements ActivityMviDelegate {


  @SuppressFBWarnings(value = "MS_SHOULD_BE_FINAL", justification = "Could be enabled for debugging purpose")
  public static boolean DEBUG = false;
  private static final String DEBUG_TAG = "ActivityMviDelegateImpl";
  private static final String KEY_MOSBY_VIEW_ID = "com.hannesdorfmann.mosby3.activity.mvi.id";
  private String mosbyViewId = null;

  private MviDelegateCallback<V, P> delegateCallback;
  private Activity activity;
  private boolean keepPresenterInstance;
  private P presenter;

  /**
   * Creates a new Delegate with an presenter that survives screen orientation changes
   *
   * @param activity The activity
   * @param delegateCallback The delegate callback
   */
  public ActivityMviDelegateImpl(@NonNull Activity activity,
      @NonNull MviDelegateCallback<V, P> delegateCallback) {
    this(activity, delegateCallback, true);
  }

  /**
   * Creates a new delegate
   *
   * @param activity The activity
   * @param delegateCallback The delegate callback
   * @param keepPresenterInstance true, if the presenter instance should be kept through screen
   * orientation changes, false if not (a new presenter instance will be created every time you
   * rotate your device)
   */
  public ActivityMviDelegateImpl(@NonNull Activity activity,
      @NonNull MviDelegateCallback<V, P> delegateCallback, boolean keepPresenterInstance) {

    if (activity == null) {
      throw new NullPointerException("Activity is null");
    }

    if (delegateCallback == null) {
      throw new NullPointerException("MvpDelegateCallback is null!");
    }
    this.delegateCallback = delegateCallback;
    this.activity = activity;
    this.keepPresenterInstance = keepPresenterInstance;
  }

  @Override public void onCreate(@Nullable Bundle bundle) {
    if (keepPresenterInstance && bundle != null) {
      mosbyViewId = bundle.getString(KEY_MOSBY_VIEW_ID);
    }

    if (DEBUG) {
      Log.d(DEBUG_TAG,
          "MosbyView ID = " + mosbyViewId + " for MvpView: " + delegateCallback.getMvpView());
    }
  }

  @Override public void onStart() {
    boolean viewStateWillBeRestored = false;

    if (mosbyViewId == null) {
      // No presenter available,
      // Activity is starting for the first time (or keepPresenterInstance == false)
      presenter = createViewIdAndCreatePresenter();
      if (DEBUG) {
        Log.d(DEBUG_TAG, "new Presenter instance created: "
            + presenter
            + " for "
            + delegateCallback.getMvpView());
      }
    } else {
      presenter = PresenterManager.getPresenter(activity, mosbyViewId);
      if (presenter == null) {
        // Process death,
        // hence no presenter with the given viewState id stored, although we have a viewState id
        presenter = createViewIdAndCreatePresenter();
        if (DEBUG) {
          Log.d(DEBUG_TAG,
              "No Presenter instance found in cache, although MosbyView ID present. This was caused by process death, therefore new Presenter instance created: "
                  + presenter);
        }
      } else {
        viewStateWillBeRestored = true;
        if (DEBUG) {
          Log.d(DEBUG_TAG, "Presenter instance reused from internal cache: " + presenter);
        }
      }
    }

    // presenter is ready, so attach viewState
    V view = delegateCallback.getMvpView();
    if (view == null) {
      throw new NullPointerException(
          "MvpView returned from getMvpView() is null. Returned by " + activity);
    }

    if (viewStateWillBeRestored) {
      delegateCallback.setRestoringViewState(true);
    }

    presenter.attachView(view);

    if (viewStateWillBeRestored) {
      delegateCallback.setRestoringViewState(false);
    }

    if (DEBUG) {
      Log.d(DEBUG_TAG,
          "MvpView attached to Presenter. MvpView: " + view + "   Presenter: " + presenter);
    }
  }

  /**
   * Generates the unique (mosby internal) view id and calls {@link
   * MviDelegateCallback#createPresenter()}
   * to create a new presenter instance
   *
   * @return The new created presenter instance
   */
  private P createViewIdAndCreatePresenter() {

    P presenter = delegateCallback.createPresenter();
    if (presenter == null) {
      throw new NullPointerException(
          "Presenter returned from createPresenter() is null. Activity is " + activity);
    }
    if (keepPresenterInstance) {
      mosbyViewId = UUID.randomUUID().toString();
      PresenterManager.putPresenter(activity, mosbyViewId, presenter);
    }
    return presenter;
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    if (keepPresenterInstance && outState != null) {
      outState.putString(KEY_MOSBY_VIEW_ID, mosbyViewId);
      if (DEBUG) {
        Log.d(DEBUG_TAG, "Saving MosbyViewId into Bundle. ViewId: " + mosbyViewId);
      }
    }
  }

  /**
   * Determines whether or not a Presenter Instance should be kept
   *
   * @param keepPresenterInstance true, if the delegate has enabled keep
   */
  static boolean retainPresenterInstance(boolean keepPresenterInstance, Activity activity) {
    return keepPresenterInstance && (activity.isChangingConfigurations()
        || !activity.isFinishing());
  }

  @Override public void onStop() {
    boolean retainPresenterInstance = retainPresenterInstance(keepPresenterInstance, activity);
    presenter.detachView(retainPresenterInstance);

    if (DEBUG) {
      Log.d(DEBUG_TAG, "detached MvpView from Presenter. MvpView "
          + delegateCallback.getMvpView()
          + "   Presenter: "
          + presenter);
    }

    if (!retainPresenterInstance){
      if (mosbyViewId != null) { // mosbyViewId == null if keepPresenterInstance == false
        PresenterManager.remove(activity, mosbyViewId);
      }
      Log.d(DEBUG_TAG, "Destroying Presenter permanently " + presenter);
    }
  }

  @Override public void onDestroy() {
    presenter = null;
    activity = null;
    delegateCallback = null;
  }

  @Override public void onPostCreate(Bundle savedInstanceState) {
  }

  @Override public void onPause() {
  }

  @Override public void onResume() {
  }

  @Override public void onRestart() {
  }

  @Override public void onContentChanged() {
  }

  @Override public Object onRetainCustomNonConfigurationInstance() {
    return null;
  }
}
