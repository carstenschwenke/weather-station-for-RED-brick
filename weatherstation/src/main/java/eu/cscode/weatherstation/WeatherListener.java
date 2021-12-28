package eu.cscode.weatherstation;

import com.tinkerforge.BrickletAmbientLight;
import com.tinkerforge.BrickletAmbientLightV2;
import com.tinkerforge.BrickletAmbientLightV3;
import com.tinkerforge.BrickletBarometer;
import com.tinkerforge.BrickletBarometerV2;
import com.tinkerforge.BrickletHumidity;
import com.tinkerforge.BrickletHumidityV2;
import com.tinkerforge.BrickletLCD20x4;
import com.tinkerforge.IPConnection;
import com.tinkerforge.NotConnectedException;
import com.tinkerforge.TinkerforgeException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WeatherListener implements IPConnection.EnumerateListener,
        IPConnection.ConnectedListener,
        BrickletAmbientLight.IlluminanceListener,
        BrickletAmbientLightV2.IlluminanceListener,
        BrickletAmbientLightV3.IlluminanceListener,
        BrickletHumidity.HumidityListener,
        BrickletHumidityV2.HumidityListener,
        BrickletBarometer.AirPressureListener,
        BrickletBarometerV2.AirPressureListener {
    private IPConnection ipcon = null;
    private BrickletLCD20x4 brickletLCD = null;
    private BrickletAmbientLight brickletAmbientLight = null;
    private BrickletAmbientLightV2 brickletAmbientLightV2 = null;
    private BrickletAmbientLightV3 brickletAmbientLightV3 = null;
    private BrickletHumidity brickletHumidity = null;
    private BrickletHumidityV2 brickletHumidityV2 = null;
    private BrickletBarometer brickletBarometer = null;
    private BrickletBarometerV2 brickletBarometerV2 = null;

    public WeatherListener(IPConnection ipcon) {
        this.ipcon = ipcon;
    }

    public void illuminance(int illuminance) {
        if (brickletLCD != null) {
            String text = String.format("Lichtstromd. %6.2f lx", illuminance / 10.0);

            try {
                brickletLCD.writeLine((short) 0, (short) 0, text);
            } catch (TinkerforgeException e) {
            }

            log.debug("Write to line 0: " + text);
        }
    }

    public void illuminance(long illuminance) {
        if (brickletLCD != null) {
            String text = String.format("Lichtstromd. %8.2f lx", illuminance / 100.0);

            try {
                brickletLCD.writeLine((short) 0, (short) 0, text);
            } catch (TinkerforgeException e) {
            }

            log.debug("Write to line 0: " + text);
        }
    }

    public void humidity(int humidity) {
        if (brickletLCD != null) {
            float factor = 10.0f;

            if (brickletHumidityV2 != null) {
                factor = 100.0f; // FIXME: assuming that only one Humiditiy Bricklet (2.0) is connected
            }

            String text = String.format("Luftfeuchte   %6.2f %%", humidity / factor);

            try {
                brickletLCD.writeLine((short) 1, (short) 0, text);
            } catch (TinkerforgeException e) {
            }

            log.debug("Write to line 1: " + text);
        }
    }

    public void airPressure(int airPressure) {
        if (brickletLCD != null) {
            String text = String.format("Luftdruck %7.2f mb", airPressure / 1000.0);
            try {
                brickletLCD.writeLine((short) 2, (short) 0, text);
            } catch (TinkerforgeException e) {
            }

            log.debug("Write to line 2: " + text);

            int temperature;
            try {
                if (brickletBarometerV2 != null) {
                    temperature = brickletBarometerV2.getTemperature();
                } else {
                    temperature = brickletBarometer.getChipTemperature();
                }
            } catch (TinkerforgeException e) {
                log.error("Could not get temperature: " + e);
                return;
            }

            // 0xDF == ° on LCD 20x4 charset
            text = String.format("Temperatur %5.2f %cC", temperature / 100.0, 0xDF);
            try {
                brickletLCD.writeLine((short) 3, (short) 0, text);
            } catch (TinkerforgeException e) {
            }

            log.debug("Write to line 3: " + text.replace((char) 0xDF, '°'));
        }
    }

    public void enumerate(String uid, String connectedUid, char position,
                          short[] hardwareVersion, short[] firmwareVersion,
                          int deviceIdentifier, short enumerationType) {
        if (enumerationType == IPConnection.ENUMERATION_TYPE_CONNECTED ||
                enumerationType == IPConnection.ENUMERATION_TYPE_AVAILABLE) {
            if (deviceIdentifier == BrickletLCD20x4.DEVICE_IDENTIFIER) {
                try {
                    brickletLCD = new BrickletLCD20x4(uid, ipcon);
                    brickletLCD.clearDisplay();
                    brickletLCD.backlightOn();
                    log.info("LCD 20x4 initialized");
                } catch (TinkerforgeException e) {
                    brickletLCD = null;
                    log.error("LCD 20x4 init failed: " + e);
                }
            } else if (deviceIdentifier == BrickletAmbientLight.DEVICE_IDENTIFIER) {
                try {
                    brickletAmbientLight = new BrickletAmbientLight(uid, ipcon);
                    brickletAmbientLight.setIlluminanceCallbackPeriod(1000);
                    brickletAmbientLight.addIlluminanceListener(this);
                    log.info("Ambient Light initialized");
                } catch (TinkerforgeException e) {
                    brickletAmbientLight = null;
                    log.error("Ambient Light init failed: " + e);
                }
            } else if (deviceIdentifier == BrickletAmbientLightV2.DEVICE_IDENTIFIER) {
                try {
                    brickletAmbientLightV2 = new BrickletAmbientLightV2(uid, ipcon);
                    brickletAmbientLightV2.setConfiguration(BrickletAmbientLightV2.ILLUMINANCE_RANGE_64000LUX,
                            BrickletAmbientLightV2.INTEGRATION_TIME_200MS);
                    brickletAmbientLightV2.setIlluminanceCallbackPeriod(1000);
                    brickletAmbientLightV2.addIlluminanceListener(this);
                    log.info("Ambient Light 2.0 initialized");
                } catch (TinkerforgeException e) {
                    brickletAmbientLightV2 = null;
                    log.error("Ambient Light 2.0 init failed: " + e);
                }
            } else if (deviceIdentifier == BrickletAmbientLightV3.DEVICE_IDENTIFIER) {
                try {
                    brickletAmbientLightV3 = new BrickletAmbientLightV3(uid, ipcon);
                    brickletAmbientLightV3.setConfiguration(BrickletAmbientLightV3.ILLUMINANCE_RANGE_64000LUX,
                            BrickletAmbientLightV3.INTEGRATION_TIME_200MS);
                    brickletAmbientLightV3.setIlluminanceCallbackConfiguration(1000, false, 'x', 0, 0);
                    brickletAmbientLightV3.addIlluminanceListener(this);
                    log.info("Ambient Light 3.0 initialized");
                } catch (TinkerforgeException e) {
                    brickletAmbientLightV3 = null;
                    log.error("Ambient Light 3.0 init failed: " + e);
                }
            } else if (deviceIdentifier == BrickletHumidity.DEVICE_IDENTIFIER) {
                try {
                    brickletHumidity = new BrickletHumidity(uid, ipcon);
                    brickletHumidity.setHumidityCallbackPeriod(1000);
                    brickletHumidity.addHumidityListener(this);
                    log.info("Humidity initialized");
                } catch (TinkerforgeException e) {
                    brickletHumidity = null;
                    log.error("Humidity init failed: " + e);
                }
            } else if (deviceIdentifier == BrickletHumidityV2.DEVICE_IDENTIFIER) {
                try {
                    brickletHumidityV2 = new BrickletHumidityV2(uid, ipcon);
                    brickletHumidityV2.setHumidityCallbackConfiguration(1000, true, 'x', 0, 0);
                    brickletHumidityV2.addHumidityListener(this);
                    log.info("Humidity 2.0 initialized");
                } catch (TinkerforgeException e) {
                    brickletHumidityV2 = null;
                    log.error("Humidity 2.0 init failed: " + e);
                }
            } else if (deviceIdentifier == BrickletBarometer.DEVICE_IDENTIFIER) {
                try {
                    brickletBarometer = new BrickletBarometer(uid, ipcon);
                    brickletBarometer.setAirPressureCallbackPeriod(1000);
                    brickletBarometer.addAirPressureListener(this);
                    log.info("Barometer initialized");
                } catch (TinkerforgeException e) {
                    brickletBarometer = null;
                    log.error("Barometer init failed: " + e);
                }
            } else if (deviceIdentifier == BrickletBarometerV2.DEVICE_IDENTIFIER) {
                try {
                    brickletBarometerV2 = new BrickletBarometerV2(uid, ipcon);
                    brickletBarometerV2.setAirPressureCallbackConfiguration(1000, false, 'x', 0, 0);
                    brickletBarometerV2.addAirPressureListener(this);
                    log.info("Barometer 2.0 initialized");
                } catch (TinkerforgeException e) {
                    brickletBarometerV2 = null;
                    log.error("Barometer 2.0 init failed: " + e);
                }
            }
        }
    }

    public void connected(short connectedReason) {
        if (connectedReason == IPConnection.CONNECT_REASON_AUTO_RECONNECT) {
            log.info("Auto Reconnect");

            while (true) {
                try {
                    ipcon.enumerate();
                    break;
                } catch (NotConnectedException e) {
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ei) {
                }
            }
        }
    }
}
