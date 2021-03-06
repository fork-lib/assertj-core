/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright 2012-2016 the original author or authors.
 */
package org.assertj.core.presentation;

import static java.lang.reflect.Array.getLength;
import static org.assertj.core.util.Arrays.isArray;
import static org.assertj.core.util.Arrays.isArrayTypePrimitive;
import static org.assertj.core.util.Arrays.isObjectArray;
import static org.assertj.core.util.Preconditions.checkArgument;
import static org.assertj.core.util.Strings.concat;
import static org.assertj.core.util.Strings.quote;

import java.io.File;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import org.assertj.core.data.MapEntry;
import org.assertj.core.groups.Tuple;
import org.assertj.core.util.Arrays;
import org.assertj.core.util.Compatibility;
import org.assertj.core.util.DateUtil;

/**
 * Standard java object representation.
 * 
 * @author Mariusz Smykula
 */
public class StandardRepresentation implements Representation {

  // can be shared this at StandardRepresentation has no state
  public static final StandardRepresentation STANDARD_REPRESENTATION = new StandardRepresentation();

  private static final String NULL = "null";

  private static final String TUPPLE_START = "(";
  private static final String TUPPLE_END = ")";

  private static final String DEFAULT_START = "[";
  private static final String DEFAULT_END = "]";

  // 4 spaces indentation : 2 space indentation after new line + '<' + '['
  static final String INDENTATION_AFTER_NEWLINE = "    ";
  // used when formatting iterable to a single line
  static final String INDENTATION_FOR_SINGLE_LINE = " ";

  public static final String ELEMENT_SEPARATOR = ",";
  public static final String ELEMENT_SEPARATOR_WITH_NEWLINE = ELEMENT_SEPARATOR + Compatibility.System.lineSeparator();

  private static int maxLengthForSingleLineDescription = 80;

  private static final Map<Class<?>, Function<Object, String>> customFormatterByType = new HashMap<>();

  public static void setMaxLengthForSingleLineDescription(int value) {
    checkArgument(value <= 0, "maxLengthForSingleLineDescription must be > 0 but was %s", value);
    maxLengthForSingleLineDescription = value;
  }
  
  public static int getMaxLengthForSingleLineDescription() {
    return maxLengthForSingleLineDescription;
  }

  /**
   * Registers new formatter for the given type. All instances of the given type will be formatted with the provided formatter.  
   */
  public static void registerFormatterForType(Class<?> type, Function<Object, String> formatter) {
    customFormatterByType.put(type, formatter);
  }

  /**
   * Clear all formatters registered per type with {@link #registerFormatterForType(Class, Function)}.
   */
  public static void removeAllRegisteredFormatters() {
    customFormatterByType.clear();
  }

  /**
   * Returns standard the {@code toString} representation of the given object. It may or not the object's own
   * implementation of {@code toString}.
   * 
   * @param object the given object.
   * @return the {@code toString} representation of the given object.
   */
  @Override
  public String toStringOf(Object object) {
    if (object == null) return null;
    if (hasCustomFormatterFor(object)) return customFormat(object);
    if (object instanceof Calendar) return toStringOf((Calendar) object);
    if (object instanceof Class<?>) return toStringOf((Class<?>) object);
    if (object instanceof Date) return toStringOf((Date) object);
    if (object instanceof Number) return toStringOf((Number) object);
    if (object instanceof File) return toStringOf((File) object);
    if (object instanceof String) return toStringOf((String) object);
    if (object instanceof Character) return toStringOf((Character) object);
    if (object instanceof Comparator) return toStringOf((Comparator<?>) object);
    if (object instanceof SimpleDateFormat) return toStringOf((SimpleDateFormat) object);
    if (object instanceof PredicateDescription) return toStringOf((PredicateDescription) object);
    if (object instanceof CompletableFuture) return toStringOf((CompletableFuture<?>) object);
    if (isArray(object)) return formatArray(object);
    if (object instanceof Collection<?>) return smartFormat((Collection<?>) object);
    if (object instanceof Map<?, ?>) return toStringOf((Map<?, ?>) object);
    if (object instanceof Tuple) return toStringOf((Tuple) object);
    if (object instanceof MapEntry) return toStringOf((MapEntry<?, ?>) object);
    return object.toString();
  }

  protected String customFormat(Object object) {
    if (object == null) return null;
    return customFormatterByType.get(object.getClass()).apply(object);
  }

  protected boolean hasCustomFormatterFor(Object object) {
    if (object == null) return false;
    return customFormatterByType.containsKey(object.getClass());
  }

  protected String toStringOf(Number number) {
    if (number instanceof Float) return toStringOf((Float) number);
    if (number instanceof Long) return toStringOf((Long) number);
    // fallback to default formatting
    return number.toString();
  }

  protected String toStringOf(Comparator<?> comparator) {
    if (!comparator.toString().contains("@")) return quote(comparator.toString());
    String comparatorSimpleClassName = comparator.getClass().getSimpleName();
    if (comparatorSimpleClassName.length() == 0) return quote("anonymous comparator class");
    // if toString has not been redefined, let's use comparator simple class name.
    if (comparator.toString().contains(comparatorSimpleClassName + "@")) return quote(comparatorSimpleClassName);
    return quote(comparator.toString());
  }

  protected String toStringOf(Calendar c) {
    return DateUtil.formatAsDatetime(c);
  }

  protected String toStringOf(Class<?> c) {
    return c.getCanonicalName();
  }

  protected String toStringOf(String s) {
    return concat("\"", s, "\"");
  }

  protected String toStringOf(Character c) {
    return concat("'", c, "'");
  }

  protected String toStringOf(PredicateDescription p) {
    // don't enclose default description with ''
    return p.isDefault() ? String.format("%s", p.description) : String.format("'%s'", p.description);
  }

  protected String toStringOf(Date d) {
    return DateUtil.formatAsDatetimeWithMs(d);
  }

  protected String toStringOf(Float f) {
    return String.format("%sf", f);
  }

  protected String toStringOf(Long l) {
    return String.format("%sL", l);
  }

  protected String toStringOf(File f) {
    return f.getAbsolutePath();
  }

  protected String toStringOf(SimpleDateFormat dateFormat) {
    return dateFormat.toPattern();
  }

  protected String toStringOf(CompletableFuture<?> future) {
    String className = future.getClass().getSimpleName();
    if (!future.isDone()) return concat(className, "[Incomplete]");
    try {
      return concat(className, "[Completed: ", toStringOf(future.join()), "]");
    } catch (CompletionException e) {
      return concat(className, "[Failed: ", toStringOf(e.getCause()), "]");
    } catch (CancellationException e) {
      return concat(className, "[Cancelled]");
    }
  }

  protected String toStringOf(Tuple tuple) {
    return singleLineFormat(tuple.toList(), TUPPLE_START, TUPPLE_END);
  }

  protected String toStringOf(MapEntry<?, ?> mapEntry) {
    return String.format("MapEntry[key=%s, value=%s]", toStringOf(mapEntry.key), toStringOf(mapEntry.value));
  }

  protected String toStringOf(Map<?, ?> map) {
    if (map == null) return null;
    Map<?, ?> sortedMap = toSortedMapIfPossible(map);
    Iterator<?> entriesIterator = sortedMap.entrySet().iterator();
    if (!entriesIterator.hasNext()) return "{}";
    StringBuilder builder = new StringBuilder("{");
    for (;;) {
      Entry<?, ?> entry = (Entry<?, ?>) entriesIterator.next();
      builder.append(format(map, entry.getKey())).append('=').append(format(map, entry.getValue()));
      if (!entriesIterator.hasNext()) return builder.append("}").toString();
      builder.append(", ");
    }
  }

  private static Map<?, ?> toSortedMapIfPossible(Map<?, ?> map) {
    try {
      return new TreeMap<>(map);
    } catch (ClassCastException | NullPointerException e) {
      return map;
    }
  }

  private Object format(Map<?, ?> map, Object o) {
    return o == map ? "(this Map)" : toStringOf(o);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  /**
   * Returns the {@code String} representation of the given array, or {@code null} if the given object is either
   * {@code null} or not an array. This method supports arrays having other arrays as elements.
   *
   * @param representation
   * @param array the object that is expected to be an array.
   * @return the {@code String} representation of the given array.
   */
  protected String formatArray(Object o) {
    if (!isArray(o)) return null;
    return isObjectArray(o) ? smartFormat(this, (Object[]) o) : formatPrimitiveArray(o);
  }

  protected String multiLineFormat(Representation representation, Object[] iterable, Set<Object[]> alreadyFormatted) {
    return format(iterable, StandardRepresentation.ELEMENT_SEPARATOR_WITH_NEWLINE, INDENTATION_AFTER_NEWLINE,
                  alreadyFormatted);
  }

  protected String singleLineFormat(Representation representation, Object[] iterable, String start, String end,
                                    Set<Object[]> alreadyFormatted) {
    return format(iterable, ELEMENT_SEPARATOR, INDENTATION_FOR_SINGLE_LINE, alreadyFormatted);
  }

  protected String smartFormat(Representation representation, Object[] iterable) {
    Set<Object[]> alreadyFormatted = new HashSet<>();
    String singleLineDescription = singleLineFormat(representation, iterable, DEFAULT_START, DEFAULT_END,
                                                    alreadyFormatted);
    return StandardRepresentation.doesDescriptionFitOnSingleLine(singleLineDescription) ? singleLineDescription
        : multiLineFormat(representation, iterable, alreadyFormatted);
  }

  protected String format(Object[] array, String elementSeparator,
                          String indentation, Set<Object[]> alreadyFormatted) {
    if (array == null) return null;
    if (array.length == 0) return DEFAULT_START + DEFAULT_END;
    // iterable has some elements
    StringBuilder desc = new StringBuilder();
    desc.append(DEFAULT_START);
    alreadyFormatted.add(array); // used to avoid infinite recursion when array contains itself
    int i = 0;
    while (true) {
      Object element = array[i];
      // do not indent first element
      if (i != 0) desc.append(indentation);
      // add element representation
      if (!isArray(element)) desc.append(element == null ? NULL : toStringOf(element));
      else if (isArrayTypePrimitive(element)) desc.append(formatPrimitiveArray(element));
      else if (alreadyFormatted.contains(element)) desc.append("(this array)");
      else desc.append(format((Object[]) element, elementSeparator, indentation, alreadyFormatted));
      // manage end description
      if (i == array.length - 1) {
        alreadyFormatted.remove(array);
        return desc.append(DEFAULT_END).toString();
      }
      // there are still elements to describe
      desc.append(elementSeparator);
      i++;
    }
  }

  protected String formatPrimitiveArray(Object o) {
    if (!isArray(o)) return null;
    if (!isArrayTypePrimitive(o)) throw Arrays.notAnArrayOfPrimitives(o);
    int size = getLength(o);
    if (size == 0) return DEFAULT_START + DEFAULT_END;
    StringBuilder buffer = new StringBuilder();
    buffer.append(DEFAULT_START);
    buffer.append(toStringOf(Array.get(o, 0)));
    for (int i = 1; i < size; i++) {
      buffer.append(ELEMENT_SEPARATOR)
            .append(INDENTATION_FOR_SINGLE_LINE)
            .append(toStringOf(Array.get(o, i)));
    }
    buffer.append(DEFAULT_END);
    return buffer.toString();
  }

  public String format(Iterable<?> iterable, String start, String end, String elementSeparator, String indentation) {
    if (iterable == null) return null;
    Iterator<?> iterator = iterable.iterator();
    if (!iterator.hasNext()) return start + end;
    // iterable has some elements
    StringBuilder desc = new StringBuilder(start);
    boolean firstElement = true;
    while (true) {
      Object element = iterator.next();
      // do not indent first element
      if (firstElement) firstElement = false;
      else desc.append(indentation);
      // add element representation
      desc.append(element == iterable ? "(this Collection)" : toStringOf(element));
      // manage end description
      if (!iterator.hasNext()) return desc.append(end).toString();
      // there are still elements to be describe
      desc.append(elementSeparator);
    }
  }

  protected String multiLineFormat(Iterable<?> iterable) {
    return format(iterable, DEFAULT_START, DEFAULT_END, ELEMENT_SEPARATOR_WITH_NEWLINE, INDENTATION_AFTER_NEWLINE);
  }

  protected String singleLineFormat(Iterable<?> iterable, String start, String end) {
    return format(iterable, start, end, ELEMENT_SEPARATOR, INDENTATION_FOR_SINGLE_LINE);
  }

  /**
   * Returns the {@code String} representation of the given {@code Iterable}, or {@code null} if the given
   * {@code Iterable} is {@code null}.
   * <p>
   * The {@code Iterable} will be formatted to a single line if it does not exceed 100 char, otherwise each elements
   * will be formatted on a new line with 4 space indentation.
   * 
   * @param representation
   * @param iterable the {@code Iterable} to format.
   * @return the {@code String} representation of the given {@code Iterable}.
   */
  protected String smartFormat(Iterable<?> iterable) {
    String singleLineDescription = singleLineFormat(iterable, DEFAULT_START, DEFAULT_END);
    return doesDescriptionFitOnSingleLine(singleLineDescription) ? singleLineDescription : multiLineFormat(iterable);
  }

  private static boolean doesDescriptionFitOnSingleLine(String singleLineDescription) {
    return singleLineDescription == null || singleLineDescription.length() < maxLengthForSingleLineDescription;
  }

}
