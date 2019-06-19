package com.tkb.dagger.ui.auth;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.tkb.dagger.SessionManager;
import com.tkb.dagger.models.User;
import com.tkb.dagger.network.auth.AuthApi;

import javax.inject.Inject;

import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class AuthViewModel extends ViewModel {

    private static final String TAG = "AuthViewModel";
    private final AuthApi authApi;
    private MediatorLiveData<AuthResource<User>>anUser = new MediatorLiveData<>();
    private SessionManager sessionManager;

    @Inject
   public AuthViewModel(AuthApi authApi, SessionManager sessionManager){
        this.authApi = authApi;
        this.sessionManager = sessionManager;
        Log.e(TAG,"ViewModel is working properly");

    }

    public void authenticationWithId(int userId){

        // Following line will let the UI know  , that the request is about to start and progress bar will be shown

        anUser.setValue(AuthResource.loading((User)null));
        final LiveData<AuthResource<User>> source = LiveDataReactiveStreams.fromPublisher(
                authApi.getUser(userId)
                        //Instead of calling onError()
                        .onErrorReturn(new Function<Throwable, User>() {
                            @Override
                            public User apply(Throwable throwable) throws Exception {
                                User user = new User();
                                user.setId(-1);
                                return user;
                            }
                        }).map(new Function<User, AuthResource<User>>() {
                    @Override
                    public AuthResource<User> apply(User user) throws Exception {
                        if (user.getId() == -1){
                            return AuthResource.error("Authentication failed",(User)null);
                        }
                        return AuthResource.authenticated(user);
                    }
                })
                        .subscribeOn(Schedulers.io())
        );

        anUser.addSource(source, new Observer<AuthResource<User>>() {
            @Override
            public void onChanged(AuthResource<User> user) {
                anUser.setValue(user);
                anUser.removeSource(source);
            }
        });
    }
    public LiveData<AuthResource<User>>observeUser (){
        return anUser;
    }
}
