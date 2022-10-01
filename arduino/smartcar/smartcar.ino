#include <Smartcar.h>
#include <MQTT.h>
#include <WiFi.h>


#ifndef __SMCE__
WiFiClient net;
#endif
MQTTClient mqtt;
//File myFile;

/*
The skeleton for this code is derived from [https://platisd.github.io/smartcar_shield/manual_control_8ino-example.html]
*/
int fSpeed = 60;
int bSpeed = -60;
int lDegrees = -75; // degrees to turn left
int rDegrees = 75;  // degrees to turn right
boolean backward = false;
boolean forward = false;
boolean left = false;
boolean right = false;
boolean braking = false;
boolean brakePress = false;
boolean anglePress = false;
boolean speedPress = false;
long duration;


const int TRIGGER_PIN           = 6; // D6
const int ECHO_PIN              = 7; // D7
const unsigned int MAX_DISTANCE = 100;

ArduinoRuntime arduinoRuntime;
BrushedMotor leftMotor(arduinoRuntime, smartcarlib::pins::v2::leftMotorPins);
BrushedMotor rightMotor(arduinoRuntime, smartcarlib::pins::v2::rightMotorPins);
DifferentialControl control(leftMotor, rightMotor);

SR04 front(arduinoRuntime, TRIGGER_PIN, ECHO_PIN, MAX_DISTANCE);

GY50 gyroscope(arduinoRuntime, 37);

const auto pulsesPerMeter = 600;

DirectionlessOdometer leftOdometer{
    arduinoRuntime,
    smartcarlib::pins::v2::leftOdometerPin,
    []() { leftOdometer.update(); },
    pulsesPerMeter};
DirectionlessOdometer rightOdometer{
    arduinoRuntime,
    smartcarlib::pins::v2::rightOdometerPin,
    []() { rightOdometer.update(); },
    pulsesPerMeter};

SmartCar car(arduinoRuntime, control, gyroscope, leftOdometer, rightOdometer);

void setup() {
    Serial.begin(9600);
    #ifndef __SMCE__
        mqtt.begin(net);
    #else
        mqtt.begin(WiFi);
    #endif
    if (mqtt.connect("arduino", "public", "public")){
        mqtt.subscribe("smartcar/#", 1);

        mqtt.onMessage([](String topic, String message){
            Serial.println(topic);
            if (topic == "smartcar/forward"){
                if (fSpeed == 0) {
                    fSpeed = 60;
                }
                right = false;
                left = false;
                forward = true;
                backward = false;
                car.setSpeed(fSpeed);
                car.setAngle(0);
            } else if (topic == "smartcar/backward"){
                left = false;
                right = false;
                backward = true;
                forward = false;
                car.setSpeed(bSpeed);
                car.setAngle(0);
            } else if (topic == "smartcar/left"){
                car.setAngle(lDegrees);
                left = true;
                right = false;
            } else if (topic == "smartcar/right"){
                left = false;
                right = true;
                car.setAngle(rDegrees);
            } else if (topic == "smartcar/stop"){
                braking = true;
                forward = false;
                backward = false;
                carBraking(0.5);
            } else if (topic == "smartcar/speedOne"){
                if(brakePress == true){
                    carBraking(0.05);
                    speedPress = false;
                    anglePress = false;
                } else if(speedPress == true){
                    carSpeed(10);
                    brakePress = false;
                    anglePress = false;
                } else if(anglePress == true){
                    steeringAngle(5);
                    brakePress = false;
                    speedPress = false;
                }
            } else if (topic == "smartcar/speedTwo"){
                if(brakePress == true){
                    carBraking(0.15);
                    speedPress = false;
                    anglePress = false;
                } else if(speedPress == true){
                    carSpeed(40);
                    brakePress = false;
                    anglePress = false;
                } else if(anglePress == true){
                    steeringAngle(15);
                    brakePress = false;
                    speedPress = false;
                }
            } else if (topic == "smartcar/speedThree"){
                if(brakePress == true){
                    carBraking(0.3);
                    speedPress = false;
                    anglePress = false;
                } else if(speedPress == true){
                    carSpeed(60);
                    brakePress = false;
                    anglePress = false;
                } else if(anglePress == true){
                    steeringAngle(30);
                    brakePress = false;
                    speedPress = false;
                }
            } else if (topic == "smartcar/speedFour"){
                if(brakePress == true){
                    carBraking(0.5);
                    speedPress = false;
                    anglePress = false;
                } else if(speedPress == true){
                    carSpeed(90);
                    brakePress = false;
                    anglePress = false;
                } else if(anglePress == true){
                    steeringAngle(30);
                    brakePress = false;
                    speedPress = false;
                }
            } else if(topic == "smartcar/speedPress"){
                speedPress = true;
                brakePress = false;
                anglePress = false;
                braking = false;
            } else if(topic == "smartcar/brakePress"){
                speedPress = false;
                brakePress = true;
                anglePress = false;
                braking = true;
                backward = false;
                forward = false;
                left = false;
                right = false;
            } else if(topic == "smartcar/anglePress"){
                speedPress = false;
                brakePress = false;
                anglePress = true;
                braking = false;
            } else if(topic == "smartcar/makeCarDance/MoonWalk"){
                moonWalk(50);
            } else if(topic == "smartcar/makeCarDance/SideKick"){
                sideKick(50);
            } else if(topic == "smartcar/makeCarDance/ShowOff"){
                showOff(50);
            } else if (topic == "smartcar/makeCarDance/ChaChaCha"){
                cha(50);
            } else if (topic == "smartcar/duration"){
                duration = atol(message.c_str());
            } else if (topic == "smartcar/direction"){
                replayDance(message);
            } else if (topic == "smartcar/stopDance") {
                car.setSpeed(0);
            } else {
                Serial.println(topic + " " + message);
            }
        });
    }
}

void loop() {
    if(mqtt.connected()){
        mqtt.loop();
    }

    handleInput();
    if (handleObstacle()){
        car.setSpeed(-90);
        delay(300);
        car.setSpeed(0);
    }
    handleOutput();
}

void handleOutput(){
    float currentSpeed1 = car.getSpeed();
    delay(500);
    float currentSpeed2 = car.getSpeed();
    if (currentSpeed1 != currentSpeed2){
        sendSpeed();
    }
}

/*
When standing still you need to choose a direction and then enter a speed mode 1-4.
Invalid input will still get passed to setCarSpeed which will cause the car to stop.
*/

void handleInput(){
  if (Serial.available()){
      String inputCommand = Serial.readStringUntil('\n');
      char oneCharCommand = inputCommand.charAt(0);
      String value;
      switch (oneCharCommand) {
          case 'l': // Set steering angle to negative number(lDegrees/left).
          car.setAngle(lDegrees);
          left = true;
          right = false;
          break;
        case 'r': // Set steering angle to positive number(rDegrees/right).
          car.setAngle(rDegrees);
          left = false;
          right = true;
          break;
        case 'f': // go ahead
          car.setSpeed(fSpeed);
          car.setAngle(0);
          right = false;
          left = false;
          forward = true;
          backward = false;
          break;
        case 'b': // go back
          car.setSpeed(bSpeed);
          car.setAngle(0);
          left = false;
          right = false;
          backward = true;
          forward = false;
          break;
        case 'g': //break
          braking = true;
          forward = false;
          backward = false;
          carBraking(0.05);
          break;
        case 'h': // break
          braking = true;
          forward = false;
          backward = false;
          carBraking(0.15);
          break;
        case 'j': // break
          braking = true;
          forward = false;
          backward = false;
          carBraking(0.3);
          break;
        case 'k': // break
          braking = true;
          forward = false;
          backward = false;
          carBraking(0.5);
          break;
        case '5':
          steeringAngle(5); //Steering angle mode (User must enter left or right before entering angle).
          break;
        case '6':
          steeringAngle(15);
          break;
        case '7':
          steeringAngle(30);
          break;
        case '8':
          steeringAngle(60);
          break;
        case '1':
          carSpeed(10); //Car speed mode (User must enter forward or backward before entering car speed).
          break;
        case '2':
          carSpeed(40);
          break;
        case '3':
          carSpeed(60);
          break;
        case '4':
          carSpeed(90);
          break;
        default: // if you receive something that you don't know, just stop
          car.setSpeed(0);
          car.setAngle(0);
        }

      if(inputCommand.startsWith("r"))
      {
          int delimiterIndex = inputCommand.indexOf(",");
          if(delimiterIndex != -1)
          {
            int degreesToRotate = inputCommand.substring(1, delimiterIndex).toInt();
            int cSpeed = inputCommand.substring(delimiterIndex + 1).toInt();
            rotateOnSpot(degreesToRotate, cSpeed);
          }
          else
          {
            int degreesToRotate = inputCommand.substring(1).toInt();
            rotateOnSpot(degreesToRotate, 80);
          }
      }
      else if (inputCommand.startsWith("m")){
        int danceSpeed = inputCommand.substring(1).toInt();
        moonWalk(danceSpeed);
      }
      else if(inputCommand.startsWith("sh")){
        int danceSpeed=inputCommand.substring(2).toInt();
        showOff(danceSpeed);
      }
      else if(inputCommand.startsWith("ch")){
        int danceSpeed=inputCommand.substring(2).toInt();
        cha(danceSpeed);
      }
      else if (inputCommand.startsWith("si")){
        int danceSpeed=inputCommand.substring(2).toInt();
        sideKick(danceSpeed);
      }
    }

 }

void replayDance(String message) {
    defaultDirections();
    if (message.equals("forward")) {
        forward = true;
        backward = false;
        car.setAngle(0);
        car.setSpeed(fSpeed);
        delay(duration);
    } else if (message.equals("backward")) {
        backward = true;
        forward = false;
        car.setAngle(0);
        car.setSpeed(bSpeed);
        delay(duration);
    } else if (message.equals("left")) {
        left = true;
        right = false;
        car.setAngle(lDegrees);
        delay(duration);
    } else if (message.equals("right")) {
        right = true;
        left = false;
        car.setAngle(rDegrees);
        delay(duration);
    }
}

void defaultDirections() {
    fSpeed = 60;
    bSpeed = -60;
    lDegrees = -75;
    rDegrees =75;
}

void carBraking(double brakeMode){
    while(fSpeed >= 0){
        fSpeed = fSpeed - fSpeed * brakeMode;
        car.setSpeed(fSpeed);
        delay(150);
        handleInput();
        if (forward == true || backward == true || fSpeed == 0){
            braking = false;
            break;
        } else if(left == true){
            car.setAngle(lDegrees);
        } else if (right == true){
            car.setAngle(rDegrees);
        }
    }
}

void steeringAngle(int angle){
    if(right == true){
        rDegrees = angle;
        car.setAngle(rDegrees);
    } else if (left == true){
        lDegrees = angle * -1;
        car.setAngle(lDegrees);
    } else {
        return;
    }
}

void carSpeed(int carSpeed) {
    if (forward == true){
        fSpeed = carSpeed;
        bSpeed = carSpeed * -1;
        car.setSpeed(fSpeed);
    } else if(backward == true){
        bSpeed = carSpeed * -1;
        fSpeed = carSpeed;
        car.setSpeed(bSpeed);
    } else {
        car.setSpeed(0);
    }
}

boolean handleObstacle(){
    int allowedDistance = 100;
    int distance = front.getDistance();
    if (distance != 0 && distance < allowedDistance){
        return true;
    } else {
        return false;
    }
}

void moonWalk(int speed){
    car.setSpeed(speed);
    delay(1000);
    car.setSpeed(-speed);
    delay(1000);
    car.setSpeed(speed);
    delay(1000);
    car.setSpeed(0);
}

void showOff(int speed) {
    car.setSpeed(speed);
    delay(1000);
    car.setSpeed(0);
    delay(500);
    rotateOnSpot(360, speed);
    rotateOnSpot(140,speed);
    car.setSpeed(speed);
    delay(2000);
    car.setSpeed(0);
}

void sideKick(int speed) {
    car.setSpeed(speed);
    delay(1000);
    car.setSpeed(0);
    rotateOnSpot(90,speed);
    car.setSpeed(speed);
    delay(1000);
    car.setSpeed(0);
    rotateOnSpot(180,speed);
    car.setSpeed(speed);
    delay(1000);
    rotateOnSpot(90,speed);
    car.setSpeed(speed);
    delay(1000);
    car.setSpeed(0);
    rotateOnSpot(180,speed);
    car.setSpeed(speed);
    delay(2000);
    car.setSpeed(0);
}

void cha(int speed) {
    rotateOnSpot(-90,90);
    car.setSpeed(speed);
    delay(1000);
    car.setSpeed(0);
    rotateOnSpot(180,90);
    car.setSpeed(speed);
    delay(1000);
    car.setSpeed(0);
    car.setSpeed(speed);
    delay(1000);
    car.setSpeed(0);
    rotateOnSpot(180,90);
    car.setSpeed(speed);
    delay(1000);
    car.setSpeed(0);
    rotateOnSpot(120,90);
    car.setSpeed(speed);
    delay(1000);
    car.setSpeed(0);
    rotateOnSpot(180,90);
    car.setSpeed(speed);
    delay(1000);
    car.setSpeed(0);
    car.setSpeed(speed);
    delay(1000);
    car.setSpeed(0);
    rotateOnSpot(180,90);
    car.setSpeed(speed);
    delay(1000);
    car.setSpeed(0);
}

/**
   Rotate the car on spot at the specified degrees with the certain speed
   @param degrees   The degrees to rotate on spot. Positive values for clockwise
                    negative for counter-clockwise.
   @param speed     The speed to rotate
*/

void rotateOnSpot(int targetDegrees, int speed) {
    speed = smartcarlib::utils::getAbsolute(speed);
    int degreesRotatedSoFar = 0;
    car.update();
    int previousHeading = car.getHeading();
    if(targetDegrees>0) {
        car.overrideMotorSpeed(-speed, speed);
    } else {
        car.overrideMotorSpeed(speed, -speed);
    }

    while(degreesRotatedSoFar < abs(targetDegrees)) {
        car.update();
        int currentHeading = car.getHeading();
        int delta = fmin(abs(currentHeading - previousHeading), abs(currentHeading - previousHeading + 360));
        degreesRotatedSoFar += delta;
        previousHeading = currentHeading;
    }
    car.setSpeed(0); // we have reached the target, so stop the car
}

void sendSpeed(){
   #ifndef __SMCE__
       mqtt.publish("smartcar/odometerSpeed", String(car.getSpeed()), false, 0);
   #else
       mqtt.publish("smartcar/odometerSpeed", String(car.getSpeed()));
   #endif
}
