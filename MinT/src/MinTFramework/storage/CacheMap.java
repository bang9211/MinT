/*
 * Copyright (C) 2015 Software&System Lab. Kangwon National University.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package MinTFramework.storage;

import MinTFramework.ExternalDevice.DeviceType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * to be a CacheMap for MinT, use this interface
 * @author soobin
 */
public interface CacheMap<T> {
    public void put(String name, T data);
    public T get(String name);
    public ArrayList<T> getResourcebyDeviceType(DeviceType type);
    public List<DeviceType> getAllDeviceType();
    public ArrayList<String> getAllResourceName();
    public HashMap<String,T> getAllResourceHashMap();
    public List<T> getAllResources();
}
