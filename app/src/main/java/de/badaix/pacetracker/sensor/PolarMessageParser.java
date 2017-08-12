/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package de.badaix.pacetracker.sensor;

import de.badaix.pacetracker.util.Helper;
import de.badaix.pacetracker.util.Hint;

/**
 * An implementation of a Sensor MessageParser for Polar Wearlink Bluetooth HRM.
 * <p>
 * Polar Bluetooth Wearlink packet example; Hdr Len Chk Seq Status HeartRate
 * RRInterval_16-bits FE 08 F7 06 F1 48 03 64 where; Hdr always = 254 (0xFE),
 * Chk = 255 - Len Seq range 0 to 15 Status = Upper nibble may be battery
 * voltage bit 0 is Beat Detection flag.
 * <p>
 * Additional packet examples; FE 08 F7 06 F1 48 03 64 FE 0A F5 06 F1 48 03 64
 * 03 70
 *
 * @author John R. Gerthoffer
 */

//01-07 07:56:06.809: D/PolarMessageParser(30207): fe:08:f7:05:f1:38:04:9c:
//												   fe:08:f7:06:f1:37:04:9a
//01-07 07:56:06.809: D/PolarMessageParser(30207): HeartRate: 56
//01-07 07:56:06.809: D/PolarMessageParser(30207): fe:08:f7:07:f1:38:04:35:fe:08:f7:08:f1:37:04:53
//01-07 07:56:06.809: D/PolarMessageParser(30207): HeartRate: 56
//01-07 07:56:13.195: D/PolarMessageParser(30207): fe:08:f7:09:f1:38:03:cd:fe:0a:f5:0a:f1:38:04:5b
//01-07 07:56:13.195: D/PolarMessageParser(30207): HeartRate: 56
//01-07 07:56:19.591: D/PolarMessageParser(30207): fe:08:f7:0b:f1:39:03:ac:fe:08:f7:0c:f1:38:04:5e
//01-07 07:56:19.591: D/PolarMessageParser(30207): HeartRate: 57
//01-07 07:56:22.154: D/PolarMessageParser(30207): fe:08:f7:0f:f1:39:03:f8:fe:08:f7:00:f1:38:04:5d
//01-07 07:56:22.154: D/PolarMessageParser(30207): HeartRate: 57
//01-07 07:56:29.841: D/PolarMessageParser(30207): fe:08:f7:01:f1:39:04:01:fe:08:f7:02:f1:39:04:31
//01-07 07:56:29.841: D/PolarMessageParser(30207): HeartRate: 57
//01-07 07:56:29.841: D/PolarMessageParser(30207): fe:08:f7:05:f1:3b:03:a0:fe:0a:f5:06:f1:3b:04:48
//01-07 07:56:29.841: D/PolarMessageParser(30207): HeartRate: 59

public class PolarMessageParser implements MessageParser {

    private byte[] word = new byte[17];
    private int offset = 0;

    public byte[] processChar(byte character) {
        word[offset++] = character;
        if (!packetValid()) {
            Hint.log(this, "Packet not valid");
            offset = 0;
            return null;
        }

        if (isWordComplete()) {
            byte[] result = new byte[offset];
            System.arraycopy(word, 0, result, 0, offset - 1);
            offset = 0;
            return result;
        }
        return null;
    }


    /**
     * Applies Polar packet validation rules to buffer. Polar packets are
     * checked for following; offset 0 = header byte, 254 (0xFE). offset 1 =
     * packet length byte, 8, 10, 12, 14. offset 2 = check byte, 255 - packet
     * length. offset 3 = sequence byte, range from 0 to 15.
     *
     * @param buffer an array of bytes to parse
     * @param i      buffer offset to beginning of packet.
     * @return whether buffer has a valid packet at offset i
     */
    private boolean packetValid() {
        boolean headerValid = true;
        boolean checkbyteValid = true;
        boolean sequenceValid = true;
        boolean sizeValid = true;
        boolean offsetValid = (offset <= 16);

        if (offset == 1)
            headerValid = (word[0] & 0xFF) == 0xFE;
        if (offset == 2)
            sizeValid = (word[1] & 0xFF) <= 16;
        if (offset == 3)
            checkbyteValid = (word[2] & 0xFF) == (0xFF - (word[1] & 0xFF));
        if (offset == 4)
            sequenceValid = (word[3] & 0xFF) < 16;

        return headerValid && sizeValid && checkbyteValid && sequenceValid && offsetValid;
    }

    public boolean isWordComplete() {
        if (offset < 4)
            return false;

        return (offset - (word[1] & 0xFF) == 0);
    }


    @Override
    public SensorData parseBuffer(byte[] buffer) {
        Hint.log(this, Helper.bytesToHex(buffer, buffer.length));

        int length = buffer[1] & 0xFF;
        int status = buffer[4] & 0xFF;
        int heartRate = buffer[5] & 0xFF;
        int RRI1 = -1;
        int RRI2 = -1;
        int beat = (status >> 4) & 1;
        int battery = (status >> 5) & 3;

        if (length >= 8) {
            RRI1 = 256 * (buffer[6] & 0xFF) + (buffer[7] & 0xFF);
            if (length >= 10) {
                RRI2 = 256 * (buffer[6] & 0xFF) + (buffer[7] & 0xFF);
            }
        }

        Hint.log(this, "HR: " + heartRate + ", beat: " + beat + ", battery: " + battery + ", RRI1: " + RRI1 + ", RRI2: " + RRI2);
        return new SensorData().setHeartRate(heartRate).setBatteryLevel(battery, 3);
        // Minimum length Polar packets is 8, so stop search 8 bytes before
        // buffer ends.
/*		for (int i = 0; i < word.length - 8; i++) {
            heartrateValid = packetValid(buffer, i);
			if (heartrateValid) {
				heartRate = buffer[i + 5] & 0xFF;
				break;
			}
		}

		// If our buffer is corrupted, use decaying last good value.
		if (!heartrateValid) {
			heartRate = (int) (lastHeartRate * 0.8);
			if (heartRate < 50)
				heartRate = 0;
		}

		lastHeartRate = heartRate; // Remember good value for next time.
		Hint.log(this, "HeartRate: " + heartRate);
*/
    }

}
