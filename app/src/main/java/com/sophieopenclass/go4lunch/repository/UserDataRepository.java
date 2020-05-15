package com.sophieopenclass.go4lunch.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.sophieopenclass.go4lunch.models.User;
import com.sophieopenclass.go4lunch.models.UserPlaceId;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.firebase.ui.auth.AuthUI.TAG;
import static com.sophieopenclass.go4lunch.injection.Injection.USER_COLLECTION_NAME;
import static com.sophieopenclass.go4lunch.utils.Constants.PLACE_ID;

public class UserDataRepository {
    private CollectionReference userCollectionRef;

    public UserDataRepository(CollectionReference userCollectionRef) {
        this.userCollectionRef = userCollectionRef;
    }

    public CollectionReference getCollectionReference() {
            return userCollectionRef;
    }

    public MutableLiveData<User> createUser(User user) {
        MutableLiveData<User> userToCreate = new MutableLiveData<>();
        userCollectionRef.document(user.getUid()).get().addOnCompleteListener(uidTask -> {
            if (uidTask.isSuccessful()) {
                if (uidTask.getResult() != null)
                    userCollectionRef.document(user.getUid()).set(user).addOnCompleteListener(userCreationTask -> {
                        if (userCreationTask.isSuccessful())
                            userToCreate.setValue(user);
                        else if (userCreationTask.getException() != null)
                            Log.e(TAG, " createUser: " + userCreationTask.getException().getMessage());
                    });
                else
                    userToCreate.setValue(user);
            } else if (uidTask.getException() != null)
                Log.e(TAG, " createUser: " + uidTask.getException().getMessage());
        });
        return userToCreate;
    }

    public MutableLiveData<User> getUser(String uid) {
        MutableLiveData<User> userData = new MutableLiveData<>();
        userCollectionRef.document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful())
                if (task.getResult() != null)
                    userData.postValue(task.getResult().toObject(User.class));
                else if (task.getException() != null)
                    Log.e(TAG, "getUser" + (task.getException().getMessage()));
        });
        return userData;
    }

    public LiveData<List<User>> getUsersByPlaceIdDate(String placeId, Date date) {
        MutableLiveData<List<User>> users = new MutableLiveData<>();
        userCollectionRef.whereEqualTo("datesAndPlacesIds." +date.toString(), placeId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful())
                if (task.getResult() != null)
                    users.postValue(task.getResult().toObjects(User.class));
                else if (task.getException() != null)
                    Log.e(TAG, "getUsersByPlaceId: " + (task.getException().getMessage()));
        });
        return users;
    }

    public MutableLiveData<String> getPlaceIdByDate(String userId, Date date) {
        MutableLiveData<String> placeId = new MutableLiveData<>();
        userCollectionRef.document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful())
                if (task.getResult() != null)
                    placeId.postValue((String)task.getResult().get("datesAndPlacesIds."+date.toString()));
                else if (task.getException() != null)
                    Log.e(TAG, "getPlaceId: " + (task.getException().getMessage()));
        });
        return placeId;
    }

    public MutableLiveData<String> updateUsername(String username, String uid) {
        MutableLiveData<String> newUsername = new MutableLiveData<>();
        userCollectionRef.document(uid).update("username", username).addOnCompleteListener(updateUsername -> {
            if (updateUsername.isSuccessful())
                newUsername.setValue(username);
            else if (updateUsername.getException() != null)
                Log.e(TAG, "updatePlaceId: " + (updateUsername.getException().getMessage()));
        });
        return newUsername;
    }

    // TODO: is this good practice ?
    public void deleteUser(String uid) {
        userCollectionRef.document(uid).delete().addOnCompleteListener(deleteUser -> {
            if (deleteUser.isSuccessful())
                Log.i(TAG, "deleteUser: " + (deleteUser.isSuccessful()));
            else if (deleteUser.getException() != null)
                Log.e(TAG, "deleteUser: " + (deleteUser.getException().getMessage()));
        });
    }

    // TODO: is this good practice ?
    public void deletePlaceId(String uid, Date date) {
        userCollectionRef.document(uid).update("datesAndPlacesIds",
                FieldValue.arrayRemove(date.toString())).addOnCompleteListener(deleteUser -> {
            if (deleteUser.isSuccessful())
                Log.i(TAG, "deleteUser: " + (deleteUser.isSuccessful()));

            else if (deleteUser.getException() != null)
                Log.e(TAG, "deleteUser: " + (deleteUser.getException().getMessage()));
        });
    }


    public MutableLiveData<String> updateUserPlaceId(String uid, String placeId, Date date) {
        MutableLiveData<String> newPlaceId = new MutableLiveData<>();
        Map<String, Object> updates = new HashMap<>();
        updates.put(("datesAndPlacesIds." +date.toString()), placeId);
        userCollectionRef.document(uid).get().addOnCompleteListener(uidTask -> {
            if (uidTask.isSuccessful()) {
                if (uidTask.getResult() != null)
                    userCollectionRef.document(uid).update(updates).addOnCompleteListener(addPlaceIdTask -> {
                        if (addPlaceIdTask.isSuccessful())
                            newPlaceId.setValue(placeId);
                        else if (addPlaceIdTask.getException() != null)
                            Log.e(TAG, " addPlaceId: " + addPlaceIdTask.getException().getMessage());
                    });
                else
                    newPlaceId.setValue(placeId);
            } else if (uidTask.getException() != null)
                Log.e(TAG, " addPlaceId: " + uidTask.getException().getMessage());
        });
        return newPlaceId;
    }
}
