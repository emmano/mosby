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

package com.hannesdorfmann.mosby3.mvp;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * An Activity that uses a {@link MvpPresenter} to implement a Model-View-Presenter
 * architecture.
 *
 * @author Hannes Dorfmann
 * @since 1.0.0
 */
public abstract class MvpActivity<V extends MvpView, P extends MvpPresenter<V>>
        extends AppCompatActivity implements MvpView,
        com.hannesdorfmann.mosby3.mvp.delegate.MvpDelegateCallback<V, P> {

    protected P presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        P retainedPresenter = getRetainedPresenter();
        if (retainedPresenter != null) {
            presenter = retainedPresenter;
        }
        if (presenter == null) {
            presenter = createPresenter();
        }
        presenter.attachView((V) this);
    }

    @Nullable
    @Override
    public P getLastNonConfigurationInstance() {
        return presenter;
    }

    private P getRetainedPresenter() {
        return (P) super.getLastCustomNonConfigurationInstance();
    }

    @Override
    public P onRetainCustomNonConfigurationInstance() {
        return presenter;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.detachView();
    }

    /**
     * Instantiate a presenter instance
     *
     * @return The {@link MvpPresenter} for this view
     */
    @NonNull
    public abstract P createPresenter();

    @Override
    public V getMvpView() {
        return (V) this;
    }

    @Override
    public P getPresenter() {
        return presenter;
    }
}
