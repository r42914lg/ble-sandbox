# AirNode BLE protocol (v0, fake data)

Device: ESP32-C3, advertised name `AirNode-C3`.
All multi-byte values are **little-endian**. No bonding or encryption in this version.

## Service

| Name | UUID |
|---|---|
| AirNode service | `a1b20001-6c4d-4e8f-9a3b-5d7e1f2c0b90` |

## Characteristics

| Name | UUID | Properties | Size |
|---|---|---|---|
| Reading | `a1b20002-6c4d-4e8f-9a3b-5d7e1f2c0b90` | Read, Notify | 4 bytes |
| LED | `a1b20003-6c4d-4e8f-9a3b-5d7e1f2c0b90` | Read, Write | 1 byte |

### Reading (notify once per second)

| Offset | Type | Field | Notes |
|---|---|---|---|
| 0 | uint16 | `seq` | Increments per sample, wraps at 65535 |
| 2 | int16 | `value` | Units of 0.01. Fake temperature: sine wave 15.00 to 25.00, 60 s period |

Example: bytes `05 00 D0 07` = seq 5, value 2000 = 20.00.

The value is also readable at any time (last computed sample), but notifications are sent only while a client is connected and subscribed.

### LED (write)

| Byte | Meaning |
|---|---|
| `00` | LED off |
| `01` | LED on |

- Any other value, or a write that is not exactly 1 byte, is rejected and the stored value is restored to the real LED state.
- Hardware note: the blue LED is on GPIO 8 and is **active-low** (pin LOW = LED on). The protocol hides this: `01` always means "on".
- Reading the characteristic returns the current state.

## Test with nRF Connect

1. Scan and find `AirNode-C3`, then Connect.
2. Open the unknown service `a1b20001-...`.
3. Reading characteristic: tap the notify (triple-arrow) icon. You should see 4 new bytes every second, with the first two incrementing.
4. LED characteristic: tap write, choose the byte array format, send `01` (LED on) then `00` (LED off).
5. Serial Monitor (115200) should log each notification and LED write.

## Change log

- v0: fake data and LED control.
