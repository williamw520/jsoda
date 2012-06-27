/******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0.  If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for 
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is: Jsoda
 * The Initial Developer of the Original Code is: William Wong (williamw520@gmail.com)
 * Portions created by William Wong are Copyright (C) 2012 William Wong, All Rights Reserved.
 *
 ******************************************************************************/

package wwutil.sys;

import java.util.*;



/**
 * A simple class to add functional operations to make iteration easier.
 *
 * Import the interfaces and methods by:
 *  import wwutil.sys.FnUtil.*;
 */
public class FnUtil {


    /** Callback function to work on a value and return a value of same type */
    public static interface ApplyFn<V> {
        public V apply(V value);
    }

    /** Callback function to work on two parameters */
    public static interface ApplyFn2<V1, V2> {
        public void apply(V1 value1, V2 value2);
    }

    /** Callback function to work on a value of type I and return a value of type R */
    public static interface TransformFn<R, I> {
        public R apply(I value);
    }

    /** Callback function to check a value satisfying the predicate */
    public static interface PredicateFn<V> {
        public boolean apply(V value);
    }

    /** Callback function to work on a value of type I and an accumulator of type R and return a value of type R */
    public static interface FoldFn<R, I> {
        public R apply(R accumulator, I value);
    }

    public static interface FoldMapFn<R, K, V> {
        public R apply(R accumulator, K key, V value);
    }
    

    /** Iterate over the values of a map and transform the values in place. */
    public static <V>  Map<?, V> apply(Map<?, V> map, ApplyFn<V> fn) {
        for (Map.Entry<?, V> entry : map.entrySet())
            entry.setValue( fn.apply( entry.getValue() ) );
        return map;
    }

    /** Iterate over the values of a map and transform the values to a new map. */
    public static <K, V>  Map<K, V> map(Map<K, V> map, ApplyFn<V> fn) {
        Map<K, V>   newMap = new HashMap<K, V>();
        for (Map.Entry<K, V> entry : map.entrySet())
            newMap.put(entry.getKey(), fn.apply( entry.getValue() ) );
        return newMap;
    }

    /** Iterate over the values of a map and transform the values to a new map of different type. */
    public static <K, V, R>  Map<K, R> map(Map<K, V> map, TransformFn<R, V> fn) {
        Map<K, R>   newMap = new HashMap<K, R>();
        for (Map.Entry<K, V> entry : map.entrySet())
            newMap.put(entry.getKey(), fn.apply( entry.getValue() ) );
        return newMap;
    }

    /** Loop over a map's keys and values. */
    public static <K, V>  void loop(Map<K, V> map, ApplyFn2<K, V> fn) {
        for (Map.Entry<K, V> entry : map.entrySet())
            fn.apply(entry.getKey(), entry.getValue());
    }

    /** Fold the values of a map into an accumulator, e.g. sum. */
    public static <R, V>  R fold(R accumulator, Map<?, V> map, FoldFn<R, V> fn) {
        for (Map.Entry<?, V> entry : map.entrySet())
            accumulator = fn.apply(accumulator, entry.getValue());
        return accumulator;
    }

    /** Fold the entries of a map into an accumulator, e.g. sum. */
    public static <R, K, V>  R fold(R accumulator, Map<K, V> map, FoldMapFn<R, K, V> fn) {
        for (Map.Entry<K, V> entry : map.entrySet())
            accumulator = fn.apply(accumulator, entry.getKey(), entry.getValue());
        return accumulator;
    }

    /** Iterate over the values of a map and filter out the values to a new map. */
    public static <K, V>  Map<K, V> filter(Map<K, V> map, PredicateFn<V> fn) {
        Map<K, V>   newMap = new HashMap<K, V>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (!fn.apply(entry.getValue()))
                newMap.put(entry.getKey(), entry.getValue());
        }
        return newMap;
    }


    /** Iterate over the values of a list and transform the values to a new list. */
    public static <V>  List<V> map(List<V> list, ApplyFn<V> fn) {
        List<V> newList = new ArrayList<V>();
        for (V entry : list)
            newList.add(fn.apply( entry ));
        return newList;
    }

    /** Iterate over the values of a list and transform the values to a new list of different type. */
    public static <R, V>  List<R> map(List<V> list, TransformFn<R, V> fn) {
        List<R> newList = new ArrayList<R>();
        for (V entry : list)
            newList.add(fn.apply( entry ));
        return newList;
    }

    /** Iterate over the values of a list and transform the values in place. */
    public static <V>  List<V> apply(List<V> list, ApplyFn<V> fn) {
        for (int i = 0; i < list.size(); i++)
            list.set(i, fn.apply(list.get(i)));
        return list;
    }

    /** Fold a list of values into an accumulator, e.g. sum. */
    public static <R, V>  R fold(R accumulator, List<V> list, FoldFn<R, V> fn) {
        for (V entry : list)
            accumulator = fn.apply(accumulator, entry);
        return accumulator;
    }

    public static <R, V>  R fold(R accumulator, V[] array, FoldFn<R, V> fn) {
        return fold(accumulator, Arrays.asList(array), fn);
    }

    /** Iterate over the values of a list and filter the values to a new list. */
    public static <V>  List<V> filter(List<V> list, PredicateFn<V> fn) {
        List<V> newList = new ArrayList<V>();
        for (V entry : list) {
            if (!fn.apply(entry))
                newList.add(entry);
        }
        return newList;
    }


    /** Turns a list of arguments (or array) into a map.  Arguments are listed as key1,value1, key2,value2, ... */
    @SuppressWarnings("unchecked")
    public static <K,V> Map<K,V> asMap(K key1, V value1, Object... keyValPair) {
        Map<K,V> map = new HashMap<K,V>();

        if (keyValPair.length % 2 != 0){
            throw new IllegalArgumentException("Keys and values must be pairs.");
        }
        map.put(key1, value1);
        for (int i = 0; i < keyValPair.length; i += 2) {
            map.put((K)keyValPair[i], (V)keyValPair[i+1]);
        }
        return map;
    }

    

    // Unit test and samples
    /*
    public static void main(String[] args) {


        Map<String,String>  map1 = FnUtil.asMap("k1", "v1", "k2", "v2");
        Map<String,String>  map2 = FnUtil.filter(map1, new PredicateFn<String>() {
                public boolean apply(String value) {
                    return value.equals("v2");
                }
            });
        System.out.println( ReflectUtil.mapToStr(map1) );
        System.out.println( ReflectUtil.mapToStr(map2) );

        Map<String,String>  map3 = FnUtil.map(map1, new ApplyFn<String>() {
                public String apply(String value) {
                    return "new mapped " + value;
                }
            });
        System.out.println( ReflectUtil.mapToStr(map3) );

        FnUtil.apply(map1, new ApplyFn<String>() {
                public String apply(String value) {
                    return "new applied " + value;
                }
            });
        System.out.println( ReflectUtil.mapToStr(map1) );

        Map<String,String>  map4 = FnUtil.map(FnUtil.asMap("k1", 100, "k2", 200, "k3", 300),
                                              new TransformFn<String, Integer>() {
                                                  public String apply(Integer value) {
                                                      return "Transformed " + value;
                                                  }
                                              });
        System.out.println( ReflectUtil.mapToStr(map4) );
        System.out.println( FnUtil.fold(0, map4, new FoldFn<Integer, String>() {
                    public Integer apply(Integer sum, String value) {
                        return sum + Integer.valueOf(value.split(" ")[1]);
                    }
                }) );
        System.out.println( FnUtil.fold(new StringBuilder(), map4, new FoldMapFn<StringBuilder, String, String>() {
                    public StringBuilder apply(StringBuilder sb, String key, String value) {
                        if (sb.length() > 0)
                            sb.append(", ");
                        sb.append(key).append(": ").append(value);
                        return sb;
                    }
                }).toString() );

        List<Integer>   list1 = FnUtil.map(Arrays.asList(100, 200, 300, 400), new ApplyFn<Integer>() {
                public Integer apply(Integer value) {
                    return value + 10;
                }
            });
        System.out.println( list1 );
        int sum1 = FnUtil.fold(0, list1, new FoldFn<Integer, Integer>() {
                public Integer apply(Integer sum, Integer value) {
                    return sum + value;
                }
            });
        System.out.println( sum1 );

        List<Float>   list2 = FnUtil.map(Arrays.asList(100, 200, 300, 400), new TransformFn<Float, Integer>() {
                public Float apply(Integer value) {
                    return value + 0.1f;
                }
            });
        System.out.println( list2 );

        FnUtil.apply(list1, new ApplyFn<Integer>() {
                public Integer apply(Integer value) {
                    return value + 50;
                }
            });
        System.out.println( list1 );

        String concat1 = FnUtil.fold(new StringBuilder(), list1, new FoldFn<StringBuilder, Integer>() {
                public StringBuilder apply(StringBuilder sb, Integer value) {
                    return sb.append(value).append(" ");
                }
            }).toString();
        System.out.println( concat1 );

        List<Float>   list3 = FnUtil.filter(list2, new PredicateFn<Float>() {
                public boolean apply(Float value) {
                    return value > 200;
                }
            });
        System.out.println( list3 );

    }
    */

}

