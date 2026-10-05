#include <NimBLEDevice.h>
#include <math.h>

// AirNode BLE protocol v0 for ESP32-C3.
// Requires the NimBLE-Arduino library (2.x) and an ESP32 Arduino core.

static constexpr uint8_t LED_PIN = 8;
static constexpr char DEVICE_NAME[] = "AirNode-C3";
static constexpr char SERVICE_UUID[] = "a1b20001-6c4d-4e8f-9a3b-5d7e1f2c0b90";
static constexpr char READING_UUID[] = "a1b20002-6c4d-4e8f-9a3b-5d7e1f2c0b90";
static constexpr char LED_UUID[] = "a1b20003-6c4d-4e8f-9a3b-5d7e1f2c0b90";

static NimBLECharacteristic* readingCharacteristic = nullptr;
static NimBLECharacteristic* ledCharacteristic = nullptr;
static bool ledOn = false;
static bool readingNotificationsEnabled = false;
static uint16_t sequenceNumber = 0;
static uint32_t lastSampleAt = 0;
static uint32_t sampleIndex = 0;

static void setLed(bool on) {
    ledOn = on;
    // The board's blue LED is active-low.
    digitalWrite(LED_PIN, on ? LOW : HIGH);
    const uint8_t storedValue = on ? 1 : 0;
    ledCharacteristic->setValue(&storedValue, sizeof(storedValue));
}

static void writeReading(int16_t value) {
    uint8_t bytes[4];
    bytes[0] = static_cast<uint8_t>(sequenceNumber & 0xFF);
    bytes[1] = static_cast<uint8_t>((sequenceNumber >> 8) & 0xFF);
    const uint16_t encodedValue = static_cast<uint16_t>(value);
    bytes[2] = static_cast<uint8_t>(encodedValue & 0xFF);
    bytes[3] = static_cast<uint8_t>((encodedValue >> 8) & 0xFF);
    readingCharacteristic->setValue(bytes, sizeof(bytes));
}

class ServerCallbacks final : public NimBLEServerCallbacks {
    void onConnect(NimBLEServer*, NimBLEConnInfo&) override {
        Serial.println("BLE client connected");
    }

    void onDisconnect(NimBLEServer*, NimBLEConnInfo&, int) override {
        Serial.println("BLE client disconnected");
        NimBLEDevice::startAdvertising();
    }
};

class LedCallbacks final : public NimBLECharacteristicCallbacks {
    void onWrite(NimBLECharacteristic* characteristic, NimBLEConnInfo&) override {
        const std::string value = characteristic->getValue();
        if (value.size() == 1 && (value[0] == 0 || value[0] == 1)) {
            setLed(value[0] == 1);
            Serial.printf("LED write: %s\n", ledOn ? "on" : "off");
            return;
        }

        // Reject malformed values and restore the characteristic's real state.
        setLed(ledOn);
        Serial.printf("Rejected LED write (%u byte(s)); state restored to %s\n",
                static_cast<unsigned>(value.size()), ledOn ? "on" : "off");
    }
};

class ReadingCallbacks final : public NimBLECharacteristicCallbacks {
    void onSubscribe(NimBLECharacteristic*, NimBLEConnInfo&, uint16_t subValue) override {
        // Bit 0 indicates notifications; bit 1 indicates indications.
        readingNotificationsEnabled = (subValue & 0x01) != 0;
        Serial.printf("Reading notifications %s\n",
                readingNotificationsEnabled ? "enabled" : "disabled");
    }
};

void setup() {
    Serial.begin(115200);
    pinMode(LED_PIN, OUTPUT);
    digitalWrite(LED_PIN, HIGH);  // Off (active-low)

    NimBLEDevice::init(DEVICE_NAME);
    NimBLEServer* server = NimBLEDevice::createServer();
    server->setCallbacks(new ServerCallbacks());

    NimBLEService* service = server->createService(SERVICE_UUID);
    readingCharacteristic = service->createCharacteristic(
            READING_UUID, NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::NOTIFY);
    readingCharacteristic->setCallbacks(new ReadingCallbacks());
    ledCharacteristic = service->createCharacteristic(
            LED_UUID, NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::WRITE);
    ledCharacteristic->setCallbacks(new LedCallbacks());

    // Initialize the readable values before the service becomes visible.
    writeReading(2000);  // Initial fake reading is 20.00.
    const uint8_t initialLed = 0;
    ledCharacteristic->setValue(&initialLed, sizeof(initialLed));

    service->start();
    NimBLEAdvertising* advertising = NimBLEDevice::getAdvertising();
    advertising->addServiceUUID(SERVICE_UUID);
    // Keep the 128-bit service UUID in the advertisement and the device name
    // in the scan response; both do not reliably fit in one legacy BLE packet.
    NimBLEAdvertisementData scanResponseData;
    scanResponseData.setName(DEVICE_NAME);
    advertising->setScanResponseData(scanResponseData);
    advertising->enableScanResponse(true);
    const bool advertisingStarted = advertising->start();

    lastSampleAt = millis();
    Serial.printf("AirNode-C3 ready; advertising %s\n",
            advertisingStarted ? "started" : "FAILED");
}

void loop() {
    const uint32_t now = millis();
    if (static_cast<uint32_t>(now - lastSampleAt) < 1000) {
        delay(5);
        return;
    }
    lastSampleAt = now;

    // Fake temperature: 15.00 to 25.00, with a 60-second period.
    const double phase = (2.0 * PI * static_cast<double>(sampleIndex % 60)) / 60.0;
    const int16_t hundredths = static_cast<int16_t>(lround(2000.0 + 500.0 * sin(phase)));
    ++sampleIndex;
    ++sequenceNumber;  // uint16_t wraps naturally after 65535.
    writeReading(hundredths);

    if (readingNotificationsEnabled) {
        readingCharacteristic->notify();
        Serial.printf("Notify: seq=%u value=%d.%02d\n",
                sequenceNumber,
                hundredths / 100,
                abs(hundredths % 100));
    }
}
