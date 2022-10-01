package com.example.android.dancecar.Activities;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.android.dancecar.Connections.DBHelper;
import com.example.android.dancecar.Connections.MqttClient;
import com.example.android.dancecar.R;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.ArrayList;

import com.example.android.dancecar.Moves.CreatedDanceMove;
import com.example.android.dancecar.Moves.DanceMove;
import com.example.android.dancecar.Moves.IndividualMove;
import jServe.Core.StopWatch;

public class RecordDanceMoveActivity extends AppCompatActivity {
    private static final String TAG = "SmartcarMqttController";
    private static final String LOCALHOST = "10.0.2.2";
    private static final String MQTT_SERVER = "tcp://" + LOCALHOST + ":1883";
    private MqttClient mMqttClient;
    private boolean isConnected = false;
    private String direction = "";
    private String lastDirection = "";
    private String currentSpeed;
    private String inputText;
    private CountDownTimer countDownTimer;
    private long timeLeft = 15000;
    private boolean isRecording = false;
    private String timerText = "";
    private long duration;
    private ImageButton forward;
    private ImageButton backward;
    private ImageButton left;
    private ImageButton right;
    private Button speedometer;
    private Button save;
    private TextView recordingTimer;
    private TextView saveMessage;
    private ToggleButton startStop;
    private ArrayList<IndividualMove> individualMoves = new ArrayList<>();
    private DancingActivity dance = new DancingActivity();
    private StopWatch stopWatch = new StopWatch();
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_dance_move);
        mMqttClient = new MqttClient(getApplicationContext(), MQTT_SERVER, TAG);
        connectToMqttBroker();
        initialiseButtons();

        // creating a new dbHelper class and passing our context to it.
        dbHelper = new DBHelper(this);

        Button saveDance = findViewById(R.id.saveDance);
        saveDance.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                AlertDialog.Builder myDialog = new AlertDialog.Builder(RecordDanceMoveActivity.this);
                myDialog.setTitle("Name");
                final EditText name = new EditText(RecordDanceMoveActivity.this);
                name.setInputType(InputType.TYPE_CLASS_TEXT);
                myDialog.setView(name);
                myDialog.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (individualMoves.size() >= 2 && !isRecording) {
                            if (!dance.getCreatedDanceMoves().isEmpty()){
                                for (CreatedDanceMove createdDanceMove : dance.getCreatedDanceMoves()){
                                    inputText = name.getText().toString();
                                    if (inputText.equals(createdDanceMove.getName())){
                                        String message = "A dance move with this name already exists.";
                                        saveMessage.setText(message);
                                    } else {
                                        createDanceMove(inputText);
                                    }
                                }
                            } else {
                                createDanceMove(name.getText().toString());
                            }
                        } else {
                            String error = "No move created, please press \"Start\" and give the car at least 2 instructions.";
                            saveMessage.setText(error);
                        }
                    }
                });

                myDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                myDialog.show();
            }
        });

        //Source for the code below: https://developer.android.com/guide/topics/ui/controls/togglebutton
        startStop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    // Start recording
                    startStop.setBackgroundColor(Color.parseColor("#FFC34A4E"));
                    save.setVisibility(View.GONE);
                    saveMessage.setText("");
                    startStopTimer();
                } else {
                    // Stop recording
                    startStop.setBackgroundColor(Color.parseColor("#8BC34A"));
                    startStopTimer();
                    ToggleButton button = findViewById(R.id.startstopButton);
                    button.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, DancingActivity.class);
        startActivity(intent);
    }

    public void createDanceMove(String name){
        CreatedDanceMove danceMove = new CreatedDanceMove(individualMoves, name);
        createNewDance(name);
        dance.setCreatedDanceMoves(danceMove);
        int moveDuration;
        String carInstruction;
        int order = 0;
        dbHelper.insertMove(name);
        for (IndividualMove individualMove : individualMoves){
            moveDuration = individualMove.getDuration();
            carInstruction = individualMove.getCarInstruction();
            order++;
            int duration = Math.toIntExact(moveDuration);
            dbHelper.insertIndividualMove(name, carInstruction, duration, order);
        }
        String message = "Dance move saved";
        saveMessage.setText(message);
        individualMoves.clear();
    }

    public void createNewDance(String name){
        DanceMove newDance = new DanceMove(name);
        newDance.setCreated(true);
    }


    @Override
    protected void onResume() {
        super.onResume();
        connectToMqttBroker();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMqttClient.disconnect(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.i(TAG, "Disconnected from broker");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            }
        });
    }

    private void connectToMqttBroker() {
        if (!isConnected) {
            mMqttClient.connect(TAG, "", new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    isConnected = true;
                    mMqttClient.subscribe("smartcar/odometerSpeed", 1, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        }
                    });
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    final String failedConnection = "Failed to connect to MQTT broker";
                    Log.e(TAG, failedConnection);
                }
            }, new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    isConnected = false;

                    final String connectionLost = "Connection to MQTT broker lost";
                    Log.w(TAG, connectionLost);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    //Add code for data to app.
                    Log.e(TAG, message.toString());
                    if (topic.equals("smartcar/odometerSpeed")){
                        currentSpeed = message.toString();
                        showSpeed(currentSpeed);
                        if (currentSpeed.equals("0.000000")) {
                            resetSettings();
                        }
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d(TAG, "Message delivered");
                }
            });
        }
    }

    public void initialiseButtons() {
        forward = findViewById(R.id.arrowUp);
        backward = findViewById(R.id.arrowDown);
        left = findViewById(R.id.arrowLeft);
        right = findViewById(R.id.arrowRight);
        speedometer = findViewById(R.id.currentSpeed);
        recordingTimer = findViewById(R.id.recordingTimer);
        startStop = findViewById(R.id.startstopButton);
        saveMessage = findViewById(R.id.saveMessage);
        save = findViewById(R.id.saveDance);
    }

    // Timer code partially derived from https://www.youtube.com/watch?v=zmjfAcnosS0
    public void startStopTimer() {
        if (isRecording) {
            stopTimer();
        } else {
            countDown();
        }
    }

    public void stopTimer () {
        if (!lastDirection.equals("") && individualMoves.size() <= 20) {
            saveIndividualMove();
        }
        save.setVisibility(View.VISIBLE);
        countDownTimer.cancel();
        isRecording = false;
        timeLeft = 15000;
        timerText = "";
        recordingTimer.setText(timerText);
        uncolorButtons();
        direction = "";
        lastDirection = "";
        stopWatch.stop();
        mMqttClient.publish("smartcar/stopDance", "0", 1, null);
    }

    public void countDown() {
        isRecording = true;
        countDownTimer = new CountDownTimer(timeLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;
                updateTimer();
            }

            @Override
            public void onFinish() {
                startStop.setChecked(true);
                stopTimer();
            }
        };
        countDownTimer.start();
    }

    public void updateTimer() {
        int seconds = (int) timeLeft % 15000 / 1000;
        timerText = seconds + " sec";
        recordingTimer.setText(timerText);
    }

    public void saveIndividualMove() {
        stopWatch.stop();
        if (individualMoves.size() <= 20) {
            duration = stopWatch.elapsed() / 1000000;
            int milliseconds = Math.toIntExact(duration);
            IndividualMove individualMove = new IndividualMove(lastDirection, milliseconds);
            individualMoves.add(individualMove);
            stopWatch.start();
            lastDirection = direction;
        } else {
            stopTimer();
        }
    }

    public void driveForward(View view){
        if (isRecording) {
            int message = 0;
            direction = "forward";
            colorArrowButtons(direction);
            mMqttClient.publish("smartcar/forward", Integer.toString(message), 1, null);

            // First time the user presses an arrow button
            if (lastDirection.equals("")) {
                stopWatch.start();
                lastDirection = direction;
                // When the user presses the next arrow button, the individual move will be saved
            } else if (stopWatch.isRunning() && !lastDirection.equals(direction) && !lastDirection.equals("")) {
                saveIndividualMove();
            }
        }
    }

    public void driveBackward(View view){
        if (isRecording) {
            int message = 0;
            direction = "backward";
            colorArrowButtons(direction);
            mMqttClient.publish("smartcar/backward", Integer.toString(message), 1, null);

            // First time the user presses an arrow button
            if (lastDirection.equals("")) {
                stopWatch.start();
                lastDirection = direction;
                // When the user presses the next arrow button, the individual move will be saved
            } else if (stopWatch.isRunning() && !lastDirection.equals(direction) && !lastDirection.equals("")) {
                saveIndividualMove();
            }
        }
    }

    public void driveLeft(View view){
        if (isRecording) {
            int message = 0;
            direction = "left";
            colorArrowButtons(direction);
            mMqttClient.publish("smartcar/left", Integer.toString(message), 1, null);

            // First time the user presses an arrow button
            if (!lastDirection.equals(direction) && lastDirection.equals("")) {
                stopWatch.start();
                lastDirection = direction;
                // When the user presses the next arrow button, the individual move will be saved
            } else if (stopWatch.isRunning() && !lastDirection.equals(direction) && !lastDirection.equals("")) {
                saveIndividualMove();
            }
        }
    }

    public void driveRight(View view){
        if (isRecording) {
            int message = 0;
            direction = "right";
            colorArrowButtons(direction);
            mMqttClient.publish("smartcar/right", Integer.toString(message), 1, null);

            // First time the user presses an arrow button
            if (!lastDirection.equals(direction) && lastDirection.equals("")) {
                stopWatch.start();
                lastDirection = direction;
                // When the user presses the next arrow button, the individual move will be saved
            } else if (stopWatch.isRunning() && !lastDirection.equals(direction) && !lastDirection.equals("")) {
                saveIndividualMove();
            }
        }
    }

    public void driveStop(View view){
        if (isRecording) {
            int message = 0;
            direction = "stop";
            mMqttClient.publish("smartcar/stop", Integer.toString(message), 1, null);
            if (!lastDirection.equals(direction) && lastDirection.equals("")) {
                stopWatch.start();
                lastDirection = direction;
                // When the user presses the next arrow button, the individual move will be saved
            } else if (stopWatch.isRunning() && !lastDirection.equals(direction) && !lastDirection.equals("")) {
                saveIndividualMove();
            }
        }
    }


    /*
    Only one arrow button can show as pressed/color at a time.
    The other buttons will get unpressed when one button is pressed.
    */
    public void colorArrowButtons(String direction){
        uncolorButtons();
        if (direction.equals("forward")) {
            colorImageButton(forward);
        } else if (direction.equals("backward")) {
            colorImageButton(backward);
        } else if (direction.equals("left")) {
            colorImageButton(left);
        } else if (direction.equals("right")) {
            colorImageButton(right);
        }
    }

    public void showSpeed(String message){
        speedometer.setText(message);
    }

    public void goBackToDanceMenu(View view){
        Intent intent = new Intent(this, DancingActivity.class);
        startActivity(intent);
    }

    public void resetSettings(){
        uncolorButtons();
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    public void colorImageButton(ImageButton button){
        button.setColorFilter(Color.parseColor("#8BC34A"));
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    public void uncolorButtons() {
        forward.setColorFilter(Color.TRANSPARENT);
        backward.setColorFilter(Color.TRANSPARENT);
        left.setColorFilter(Color.TRANSPARENT);
        right.setColorFilter(Color.TRANSPARENT);
    }
}