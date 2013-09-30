package nl.surfsara.visualization.colt.datastructures;

/* Copyright 2013 SURFSara
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class ByteBufferView {
    public ByteBufferView(byte[] buffer, int n) {
        this.buffer = buffer;
        offset = 0;
    }

    public void initialize(byte[] buffer, int n) {
        this.buffer = buffer;
        this.n = 0;
        offset = 0;
    }

    public void initialize(int n) {
        this.n = 0;
        offset = 0;
    }

    public byte getByte() {
        byte value = buffer[offset];
        offset++;
        return value;
    }

    public short getShort() {
        short value = (short) (buffer[offset] | (buffer[offset + 1] << 8));
        offset += 2;
        return value;
    }

    public int getUnsignedShort() {
        int value = buffer[offset] | (buffer[offset + 1] << 8);
        offset += 2;
        return value;
    }

    public int getInt() {
        int value = (buffer[offset] & 0xff) | (buffer[offset + 1] & 0xff) << 8 | (buffer[offset + 2] & 0xff) << 16
                | (buffer[offset + 3] & 0xff) << 24;
        offset += 4;
        return value;
    }

    public long getLong() {
        long value = buffer[offset] & 0xff | (long) (buffer[offset + 1] & 0xff) << 8
                | (long) (buffer[offset + 2] & 0xff) << 16 | (long) (buffer[offset + 3] & 0xff) << 24
                | (long) (buffer[offset + 4] & 0xff) << 32 | (long) (buffer[offset + 5] & 0xff) << 40
                | (long) (buffer[offset + 6] & 0xff) << 48 | (long) (buffer[offset + 7] & 0xff) << 56;
        offset += 8;
        return value;
    }

    public float getFloat() {
        return Float.intBitsToFloat(getInt());
    }

    public double getDouble() {
        return Double.longBitsToDouble(getLong());
    }

    private byte[] buffer;
    private int offset;
    private int n;
};
