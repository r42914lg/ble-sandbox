const int LED_PIN = 8;

void ledOn()  { digitalWrite(LED_PIN, LOW);  }  // active-low
void ledOff() { digitalWrite(LED_PIN, HIGH); }

void setup() {
  Serial.begin(115200);
  pinMode(LED_PIN, OUTPUT);
}

void loop() {
  ledOn();
  Serial.println("LED on");
  delay(1000);
  ledOff();
  Serial.println("LED off");
  delay(1000);
}