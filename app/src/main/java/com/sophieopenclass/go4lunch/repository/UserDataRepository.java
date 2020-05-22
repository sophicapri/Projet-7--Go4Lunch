package com.sophieopenclass.go4lunch.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.sophieopenclass.go4lunch.models.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.firebase.ui.auth.AuthUI.TAG;
import static com.sophieopenclass.go4lunch.utils.Constants.CHOSEN_RESTAURANT_NAME_FIELD;
import static com.sophieopenclass.go4lunch.utils.Constants.DATES_AND_PLACE_IDS_FIELD;
import static com.sophieopenclass.go4lunch.utils.Constants.FAVORITE_RESTAURANTS_FIELD;

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

    public MutableLiveData<List<User>> getListUsers(){
        MutableLiveData<List<User>> users = new MutableLiveData<>();
        userCollectionRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful())
                if (task.getResult() != null)
                    users.postValue(task.getResult().toObjects(User.class));
                else if (task.getException() != null)
                    Log.e(TAG, "getListUsers: " + (task.getException().getMessage()));
        });
        return users;
    }


    public MutableLiveData<List<User>> getUsersByPlaceIdAndDate(String placeId, String date) {
        MutableLiveData<List<User>> users = new MutableLiveData<>();
        userCollectionRef.whereEqualTo(DATES_AND_PLACE_IDS_FIELD + date, placeId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful())
                if (task.getResult() != null)
                    users.postValue(task.getResult().toObjects(User.class));
                else if (task.getException() != null)
                    Log.e(TAG, "getUsersByPlaceId: " + (task.getException().getMessage()));
        });
        return users;
    }

    public MutableLiveData<String> getPlaceIdByDate(String userId, String date) {
        MutableLiveData<String> placeId = new MutableLiveData<>();
        userCollectionRef.document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful())
                if (task.getResult() != null)
                    placeId.postValue((String)task.getResult().get(DATES_AND_PLACE_IDS_FIELD +date));
                else if (task.getException() != null)
                    Log.e(TAG, "getPlaceId: " + (task.getException().getMessage()));
        });
        return placeId;
    }

    public void addRestaurantToFavorites(String placeId, String userId){
        userCollectionRef.document(userId).update(FAVORITE_RESTAURANTS_FIELD, FieldValue.arrayUnion(placeId))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful())
                        Log.i(TAG, "addToFavorites: " + (task.isSuccessful()));
                });
    }

    public MutableLiveData<Integer> getNumberOfLikesByPlaceId(String placeId) {
        MutableLiveData<Integer> numberOfLikes = new MutableLiveData<>();
        userCollectionRef.whereArrayContains(FAVORITE_RESTAURANTS_FIELD, placeId).get().addOnCompleteListener(task -> {
           if (task.isSuccessful())
               if (task.getResult() != null)
               numberOfLikes.setValue(task.getResult().getDocuments().size());
               else if (task.getException() != null)
                   Log.e(TAG, "getLikes: " + (task.getException().getMessage()));

        });
        return numberOfLikes;
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

    public void deleteUser(String uid) {
        userCollectionRef.document(uid).delete().addOnCompleteListener(deleteUser -> {
            if (deleteUser.isSuccessful())
                Log.i(TAG, "deleteUser: " + (deleteUser.isSuccessful()));
            else if (deleteUser.getException() != null)
                Log.e(TAG, "deleteUser: " + (deleteUser.getException().getMessage()));
        });
    }

    public void deletePlaceId(String uid, String date) {
        userCollectionRef.document(uid).update(DATES_AND_PLACE_IDS_FIELD + date,
                FieldValue.delete()).addOnCompleteListener(deleteUser -> {
            if (deleteUser.isSuccessful())
                Log.i(TAG, "deleteUser: " + (deleteUser.isSuccessful()));

            else if (deleteUser.getException() != null)
                Log.e(TAG, "deleteUser: " + (deleteUser.getException().getMessage()));
        });
    }

    public MutableLiveData<String> updateUserPlaceId(String uid, String placeId, String date) {
        MutableLiveData<String> newPlaceId = new MutableLiveData<>();
        Map<String, Object> updates = new HashMap<>();
        updates.put((DATES_AND_PLACE_IDS_FIELD + date), placeId);
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

    public void updateRestaurantName(String uid, String restaurantName) {
        MutableLiveData<String> newRestaurantName = new MutableLiveData<>();
        userCollectionRef.document(uid).update(CHOSEN_RESTAURANT_NAME_FIELD, restaurantName).addOnCompleteListener(updateRestaurant -> {
            if(updateRestaurant.isSuccessful())
                newRestaurantName.setValue(restaurantName);
            else if (updateRestaurant.getException() != null){
                Log.e(TAG, "updateRestaurant: " + (updateRestaurant.getException().getMessage()));
            }
        });
    }


}
