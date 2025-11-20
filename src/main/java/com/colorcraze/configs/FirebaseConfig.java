package com.colorcraze.configs;

import com.colorcraze.configs.exceptions.FirebaseInitializationException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

import java.io.InputStream;

/**
 * Configuration class responsible for initializing Firebase during application startup.
 * Loads service account credentials and initializes the FirebaseApp instance.
 * Throws {@link FirebaseInitializationException} if initialization fails or the credentials file is missing.
 */
@Configuration
public class FirebaseConfig {

    /**
     * Initializes Firebase using the service account JSON file.
     * Called automatically after the Spring context is constructed.
     *
     * @throws FirebaseInitializationException if the service account file is missing or initialization fails
     */
    @PostConstruct
    public void initializeFirebase() {
        try {
            InputStream serviceAccount =
                    getClass().getResourceAsStream("/firebase/firebase-service-account.json");

            if (serviceAccount == null) {
                throw new FirebaseInitializationException(
                        "No se encontró el archivo firebase-service-account.json", null
                );
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);

        } catch (Exception e) {
            throw new FirebaseInitializationException("Error inicializando Firebase", e);
        }
    }
}
