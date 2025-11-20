package com.colorcraze.configs;

import com.colorcraze.configs.exceptions.FirebaseInitializationException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

import java.io.InputStream;

@Configuration
public class FirebaseConfig {

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