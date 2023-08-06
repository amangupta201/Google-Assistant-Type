package com.example.eldervoice;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import android.speech.tts.Voice;
import androidx.appcompat.app.AlertDialog;



public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final int SPEECH_REQUEST_CODE = 100;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private TextView tvResult;
    private Locale recognitionLanguage = Locale.getDefault();
    private Locale selectedLanguage = Locale.getDefault();
    private void showError(String errorMessage) {
        // Display an error message to the user (you can use a dialog, toast, or a dedicated error TextView)
        // For example:
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }


    // ActivityResultLauncher for speech recognition
    private final ActivityResultLauncher<Intent> speechRecognitionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null) {
                                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                                if (results != null && !results.isEmpty()) {
                                    String spokenText = results.get(0);
                                    handleUserCommand(spokenText);
                                }
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Speech Recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, this);
        tvResult = findViewById(R.id.tvResult);
    }
    public void onSpeakClick(View view) {
        startSpeechRecognition();
    }
    public void onSelectLanguageClick(View view) {
        // Show the language selection dialog
        showLanguageSelectionDialog();
    }
    private void showLanguageSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Recognition Language")
                .setItems(getSupportedLanguageNames(), (dialog, which) -> {
                    recognitionLanguage = getSupportedLanguages().get(which);
                    startSpeechRecognition();
                })
                .show();
    }
    private List<Locale> getSupportedLanguages() {
        List<Locale> languages = new ArrayList<>();
        for (Locale locale : Locale.getAvailableLocales()) {
            int res = textToSpeech.isLanguageAvailable(locale);
            if (res == TextToSpeech.LANG_COUNTRY_AVAILABLE || res == TextToSpeech.LANG_AVAILABLE) {
                languages.add(locale);
            }
        }
        return languages;
    }
    private String[] getSupportedLanguageNames() {
        List<Locale> languages = getSupportedLanguages();
        String[] languageNames = new String[languages.size()];
        for (int i = 0; i < languages.size(); i++) {
            languageNames[i] = languages.get(i).getDisplayName();
        }
        return languageNames;
    }


    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, recognitionLanguage);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something...");
        speechRecognitionLauncher.launch(intent); // Launch speech recognition using ActivityResultLauncher
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (results != null && !results.isEmpty()) {
                    String spokenText = results.get(0);
                    handleUserCommand(spokenText);
                } else {
                    showError("No speech recognized. Please try again.");
                }
            } else {
                showError("Speech recognition failed. Please try again.");
            }
        }
    }

    private void handleUserCommand(String command) {
        // For demonstration purposes, let's just echo back the user's command using text-to-speech.
        tvResult.setText(command);
        speak(command);
    }

    private void showVoiceSelectionDialog(Set<Voice> voices) {
        // Convert the Set of Voice objects to a List of voice names
        List<String> voiceNames = new ArrayList<>();
        for (Voice voice : voices) {
            voiceNames.add(voice.getName());
        }

        // Show a dialog with a list of voice names
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Voice")
                .setItems(voiceNames.toArray(new String[0]), (dialog, which) -> {
                    // When the user selects a voice from the list, apply the selected voice
                    // to the TextToSpeech engine using setVoice()
                    Voice selectedVoice = voices.toArray(new Voice[0])[which];
                    textToSpeech.setVoice(selectedVoice);

                    // Update the displayed text with the user's spoken command
                    String selectedVoiceName = selectedVoice.getName();
                    String displayText = getString(R.string.selected_voice_text, selectedVoiceName);
                    tvResult.setText(displayText);

                    // Set the selected language to match the selected voice's language
                    selectedLanguage = new Locale(selectedVoice.getLocale().getLanguage(),
                            selectedVoice.getLocale().getCountry());

                })
                .show();
    }


    @Override

    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(recognitionLanguage);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language not supported");
            } else {
                // Text-to-speech is initialized successfully.

                // Check available voices and let the user choose their preferred voice (optional)
                // Uncomment the following lines to enable voice selection
                Set<Voice> voices = textToSpeech.getVoices();
                if (voices != null && !voices.isEmpty()) {
                    // Let the user choose a voice here, and then use setVoice() to apply the selected voice.
                    // You can also set a specific voice programmatically using textToSpeech.setVoice(voice).
                    showVoiceSelectionDialog(voices);
                }

                // You can start speech recognition here or after a button click, etc.
                startSpeechRecognition();
            }
        } else {
            Log.e("TTS", "Initialization failed");
        }
    }

    private void speak(String text) {
        // Set the language of the Text-to-Speech engine to the recognitionLanguage
        int result = textToSpeech.setLanguage(recognitionLanguage);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("TTS", "Language not supported");
        } else {
            // Successfully set the language, now speak the text in the recognitionLanguage
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // ... (other methods)








    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
