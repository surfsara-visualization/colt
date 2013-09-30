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

public class TuioTouchHistory {
    public TuioTouchHistory(double t, int id, float x, float y, float vx, float vy) {
        timestamp = t;
        this.id = id;
        last_x = x;
        last_y = y;
        last_xvel = vx;
        last_yvel = vy;
    }

    public int id;
    public double timestamp;
    public float last_x, last_y;
    public float last_xvel, last_yvel;
};
