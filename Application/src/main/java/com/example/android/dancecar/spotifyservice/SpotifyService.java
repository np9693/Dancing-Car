package com.example.android.dancecar.spotifyservice;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.miscellaneous.AudioAnalysis;
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import com.wrapper.spotify.requests.data.tracks.GetAudioAnalysisForTrackRequest;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@RequiresApi(api = Build.VERSION_CODES.N)
public class SpotifyService {
    private static final String clientId = "764ef5ad07284dd499fcb8bb5604bc26";
    private static final String clientSecret = "b03def4d359c495aa566984f3941396d";

    private static final SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .build();
    private static final ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials()
            .build();

    public SpotifyService()
    {
        clientCredentials_Async();
    }

    public static void clientCredentials_Async() {
        try {
            final CompletableFuture<ClientCredentials> clientCredentialsFuture = clientCredentialsRequest.executeAsync();

            // Example Only. Never block in production code.
            final ClientCredentials clientCredentials = clientCredentialsFuture.join();

            // Set access token for further "spotifyApi" object usage
            spotifyApi.setAccessToken(clientCredentials.getAccessToken());

            //System.out.println("Expires in: " + clientCredentials.getExpiresIn());
        }
        catch (CancellationException e) {
            //System.out.println("Async operation cancelled.");
        }
        catch (Exception e) {
            //System.out.println("Async operation cancelled.");
        }
    }

    public static AudioAnalysis getAudioAnalysisForTrack_Async(String id) {
        try {
            final GetAudioAnalysisForTrackRequest getAudioAnalysisForTrackRequest = spotifyApi
                    .getAudioAnalysisForTrack(id)
                    .build();
            final CompletableFuture<AudioAnalysis> audioAnalysisFuture = getAudioAnalysisForTrackRequest.executeAsync();


            final AudioAnalysis audioAnalysis = audioAnalysisFuture.join();

            //System.out.println("Track duration: " + audioAnalysis.getTrack().getDuration());
            return audioAnalysis;
        } catch (CompletionException e) {
            //System.out.println("Error: " + e.getCause().getMessage());
        } catch (CancellationException e) {
            //System.out.println("Async operation cancelled.");
        }
        return null;
    }



}
