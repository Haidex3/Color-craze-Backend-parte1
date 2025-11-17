package com.colorcraze.configs;

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
                throw new IllegalStateException("No se encontró el archivo firebase-service-account.json");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);

            System.out.println("🔥 Firebase Admin inicializado correctamente!");

        } catch (Exception e) {
            throw new RuntimeException("Error inicializando Firebase", e);
        }
    }
}
