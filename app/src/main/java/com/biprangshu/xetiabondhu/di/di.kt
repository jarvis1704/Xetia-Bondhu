package com.biprangshu.xetiabondhu.di

import android.content.Context
import com.biprangshu.xetiabondhu.authentication.GoogleAuthClient
import com.biprangshu.xetiabondhu.repository.FirebaseRepository
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule{


    @Provides
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideSignInClient(@ApplicationContext context: Context): SignInClient{
        return Identity.getSignInClient(context)
    }

    @Provides
    @Singleton
    fun provideGoogleAuthClient(
        @ApplicationContext context: Context,
        oneTapClient: SignInClient
    ): GoogleAuthClient {
        return GoogleAuthClient(context = context, oneTapClient = oneTapClient)
    }

    @Provides
    fun provideFirebaseFirestore(): FirebaseFirestore{
        return FirebaseFirestore.getInstance()
    }

    @Provides
    fun providesFirebaseStorage(): FirebaseStorage{
        return FirebaseStorage.getInstance()
    }

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideFirebaseRepository(
        auth: FirebaseAuth,
        db: FirebaseFirestore,
        firebaseStorage: FirebaseStorage,
        context: Context
    ): FirebaseRepository{
        return FirebaseRepository(auth = auth, db = db, firebaseStorage = firebaseStorage, context = context)
    }
}