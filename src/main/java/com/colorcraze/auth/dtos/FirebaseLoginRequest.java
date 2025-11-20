package com.colorcraze.auth.dtos;

import lombok.NoArgsConstructor;

/**
 * Data Transfer Object used for Firebase authentication requests.
 * This object carries the Firebase ID token sent by the client during login
 * or guest authentication. The token is later validated by the authentication service.
 */
@NoArgsConstructor
public class FirebaseLoginRequest {

    private String idToken;

    /**
     * Creates a new {@link FirebaseLoginRequest} with the given ID token.
     *
     * @param idToken the Firebase ID token issued by Firebase Authentication
     */
    public FirebaseLoginRequest(String idToken) {
        this.idToken = idToken;
    }

    /**
     * Returns the Firebase ID token included in the request.
     *
     * @return the Firebase ID token
     */
    public String getIdToken() {
        return idToken;
    }

    /**
     * Sets the Firebase ID token for this request.
     *
     * @param idToken the Firebase ID token to set
     */
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
