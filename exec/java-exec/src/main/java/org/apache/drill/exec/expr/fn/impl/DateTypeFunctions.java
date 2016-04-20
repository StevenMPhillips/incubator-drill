/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.drill.exec.expr.fn.impl;

import io.netty.buffer.ArrowBuf;

import javax.inject.Inject;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.arrow.vector.holders.BigIntHolder;
import org.apache.arrow.vector.holders.DateHolder;
import org.apache.arrow.vector.holders.IntervalDayHolder;
import org.apache.arrow.vector.holders.IntervalHolder;
import org.apache.arrow.vector.holders.IntervalYearHolder;
import org.apache.arrow.vector.holders.TimeHolder;
import org.apache.arrow.vector.holders.TimeStampHolder;
import org.apache.arrow.vector.holders.VarCharHolder;
import org.apache.drill.exec.ops.ContextInformation;

public class DateTypeFunctions {

    /**
     * Function to check if a varchar value can be cast to a date.
     *
     * At the time of writing this function, several other databases were checked
     * for behavior compatibility. There was not a consensus between oracle and
     * Sql server about the expected behavior of this function, and Postgres
     * lacks it completely.
     *
     * Sql Server appears to have both a DATEFORMAT and language locale setting
     * that can change the values accepted by this function. Oracle appears to
     * support several formats, some of which are not mentioned in the Sql
     * Server docs. With the lack of standardization, we decided to implement
     * this function so that it would only consider date strings that would be
     * accepted by the cast function as valid.
     */
    @SuppressWarnings("unused")
    @FunctionTemplate(name = "isdate", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls=NullHandling.INTERNAL,
        costCategory = FunctionTemplate.FunctionCostCategory.COMPLEX)
    public static class IsDate implements DrillSimpleFunc {

      @Param NullableVarCharHolder in;
      @Output BitHolder out;

      public void setup() { }

      public void eval() {
        // for a null input return false
        if (in.isSet == 0) {
          out.value = 0;
        } else {
          out.value = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.isReadableAsDate(in.buffer, in.start, in.end) ? 1 : 0;
        }
      }
    }

    // Same as above, just for required input
    @SuppressWarnings("unused")
    @FunctionTemplate(name = "isdate", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls=NullHandling.INTERNAL,
        costCategory = FunctionTemplate.FunctionCostCategory.COMPLEX)
    public static class IsDateRequiredInput implements DrillSimpleFunc {

      @Param VarCharHolder in;
      @Output BitHolder out;

      public void setup() { }

      public void eval() {
        // for a null input return false
        out.value = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.isReadableAsDate(in.buffer, in.start, in.end) ? 1 : 0;
      }
    }

    @FunctionTemplate(name = "intervaltype", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
    public static class IntervalType implements DrillSimpleFunc {

        @Param  BigIntHolder inputYears;
        @Param  BigIntHolder inputMonths;
        @Param  BigIntHolder inputDays;
        @Param  BigIntHolder inputHours;
        @Param  BigIntHolder inputMinutes;
        @Param  BigIntHolder inputSeconds;
        @Param  BigIntHolder inputMilliSeconds;
        @Output IntervalHolder out;

        public void setup() {
        }

        public void eval() {

            out.months       =  (int) ((inputYears.value * org.apache.arrow.vector.util.DateUtility.yearsToMonths) +
                                       (inputMonths.value));
            out.days         =  (int) inputDays.value;
            out.milliseconds =  (int) ((inputHours.value * org.apache.arrow.vector.util.DateUtility.hoursToMillis) +
                                       (inputMinutes.value * org.apache.arrow.vector.util.DateUtility.minutesToMillis) +
                                       (inputSeconds.value * org.apache.arrow.vector.util.DateUtility.secondsToMillis) +
                                       (inputMilliSeconds.value));
        }
    }

    @FunctionTemplate(name = "interval_year", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
    public static class IntervalYearType implements DrillSimpleFunc {

        @Param  BigIntHolder inputYears;
        @Param  BigIntHolder inputMonths;
        @Output IntervalYearHolder out;

        public void setup() {
        }

        public void eval() {

            out.value       = (int) ((inputYears.value * org.apache.arrow.vector.util.DateUtility.yearsToMonths) +
                                      (inputMonths.value));
        }
    }

    @FunctionTemplate(name = "interval_day", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
    public static class IntervalDayType implements DrillSimpleFunc {

        @Param  BigIntHolder inputDays;
        @Param  BigIntHolder inputHours;
        @Param  BigIntHolder inputMinutes;
        @Param  BigIntHolder inputSeconds;
        @Param  BigIntHolder inputMillis;
        @Output IntervalDayHolder out;

        public void setup() {
        }

        public void eval() {

            out.days  = (int) inputDays.value;
            out.milliseconds =  (int) ((inputHours.value * org.apache.arrow.vector.util.DateUtility.hoursToMillis) +
                                       (inputMinutes.value * org.apache.arrow.vector.util.DateUtility.minutesToMillis) +
                                       (inputSeconds.value * org.apache.arrow.vector.util.DateUtility.secondsToMillis) +
                                 (inputMillis.value));
        }
    }

    @FunctionTemplate(name = "datetype", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
    public static class DateType implements DrillSimpleFunc {

        @Param  BigIntHolder inputYears;
        @Param  BigIntHolder inputMonths;
        @Param  BigIntHolder inputDays;
        @Output DateHolder   out;

        public void setup() {
        }

        public void eval() {
            out.value = ((new org.joda.time.MutableDateTime((int) inputYears.value,
                                                            (int) inputMonths.value,
                                                            (int)inputDays.value,
                                                            0,
                                                            0,
                                                            0,
                                                            0,
                                                            org.joda.time.DateTimeZone.UTC))).getMillis();
        }
    }

    @FunctionTemplate(name = "timestamptype", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
    public static class TimeStampType implements DrillSimpleFunc {

        @Param  BigIntHolder inputYears;
        @Param  BigIntHolder inputMonths;
        @Param  BigIntHolder inputDays;
        @Param  BigIntHolder inputHours;
        @Param  BigIntHolder inputMinutes;
        @Param  BigIntHolder inputSeconds;
        @Param  BigIntHolder inputMilliSeconds;
        @Output TimeStampHolder out;

        public void setup() {
        }

        public void eval() {
            out.value = ((new org.joda.time.MutableDateTime((int)inputYears.value,
                                                            (int)inputMonths.value,
                                                            (int)inputDays.value,
                                                            (int)inputHours.value,
                                                            (int)inputMinutes.value,
                                                            (int)inputSeconds.value,
                                                            (int)inputMilliSeconds.value,
                                                            org.joda.time.DateTimeZone.UTC))).getMillis();
        }
    }

    @FunctionTemplate(name = "timetype", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
    public static class TimeType implements DrillSimpleFunc {

        @Param  BigIntHolder inputHours;
        @Param  BigIntHolder inputMinutes;
        @Param  BigIntHolder inputSeconds;
        @Param  BigIntHolder inputMilliSeconds;
        @Output TimeHolder   out;

        public void setup() {
        }

        public void eval() {
            out.value = (int) ((inputHours.value * org.apache.arrow.vector.util.DateUtility.hoursToMillis) +
                               (inputMinutes.value * org.apache.arrow.vector.util.DateUtility.minutesToMillis) +
                               (inputSeconds.value * org.apache.arrow.vector.util.DateUtility.secondsToMillis) +
                                inputMilliSeconds.value);
        }
    }

    @FunctionTemplate(name = "current_date", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
    public static class CurrentDate implements DrillSimpleFunc {
        @Workspace long queryStartDate;
        @Output DateHolder out;
        @Inject ContextInformation contextInfo;

        public void setup() {

            int timeZoneIndex = contextInfo.getRootFragmentTimeZone();
            org.joda.time.DateTimeZone timeZone = org.joda.time.DateTimeZone.forID(org.apache.arrow.vector.util.DateUtility.getTimeZone(timeZoneIndex));
            org.joda.time.DateTime now = new org.joda.time.DateTime(contextInfo.getQueryStartTime(), timeZone);
            queryStartDate = (new org.joda.time.DateMidnight(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), timeZone)).
                withZoneRetainFields(org.joda.time.DateTimeZone.UTC).getMillis();
        }

        public void eval() {
            out.value = queryStartDate;
        }

    }

    @FunctionTemplate(name = "timeofday", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL, isRandom = true)
    public static class TimeOfDay implements DrillSimpleFunc {
        @Inject ArrowBuf buffer;
        @Output VarCharHolder out;

        public void setup() {
        }

        public void eval() {
            org.joda.time.DateTime temp = new org.joda.time.DateTime();
            String str = org.apache.arrow.vector.util.DateUtility.formatTimeStampTZ.print(temp);
            out.buffer = buffer;
            out.start = 0;
            out.end = Math.min(100,  str.length()); // truncate if target type has length smaller than that of input's string
            out.buffer.setBytes(0, str.substring(0,out.end).getBytes());
        }
    }

    @FunctionTemplate(names = {"localtimestamp", "current_timestamp", "now", "statement_timestamp", "transaction_timestamp"}, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
    public static class LocalTimeStamp implements DrillSimpleFunc {
        @Workspace long queryStartDate;
        @Output TimeStampHolder out;
        @Inject ContextInformation contextInfo;

        public void setup() {

            org.joda.time.DateTime now = (new org.joda.time.DateTime(contextInfo.getQueryStartTime())).withZoneRetainFields(org.joda.time.DateTimeZone.UTC);
            queryStartDate = now.getMillis();
        }

        public void eval() {
            out.value = queryStartDate;
        }
    }

    @FunctionTemplate(names = {"current_time", "localtime"}, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
    public static class CurrentTime implements DrillSimpleFunc {
        @Workspace int queryStartTime;
        @Output TimeHolder out;
        @Inject ContextInformation contextInfo;

        public void setup() {

            int timeZoneIndex = contextInfo.getRootFragmentTimeZone();
            org.joda.time.DateTimeZone timeZone = org.joda.time.DateTimeZone.forID(org.apache.arrow.vector.util.DateUtility.getTimeZone(timeZoneIndex));
            org.joda.time.DateTime now = new org.joda.time.DateTime(contextInfo.getQueryStartTime(), timeZone);
            queryStartTime= (int) ((now.getHourOfDay() * org.apache.arrow.vector.util.DateUtility.hoursToMillis) +
                                   (now.getMinuteOfHour() * org.apache.arrow.vector.util.DateUtility.minutesToMillis) +
                                   (now.getSecondOfMinute() * org.apache.arrow.vector.util.DateUtility.secondsToMillis) +
                                   (now.getMillisOfSecond()));
        }

        public void eval() {
            out.value = queryStartTime;
        }
    }

    @SuppressWarnings("unused")
    @FunctionTemplate(names = {"date_add", "add"}, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls=NullHandling.NULL_IF_NULL)
    public static class DateTimeAddFunction implements DrillSimpleFunc {
    @Param DateHolder left;
    @Param TimeHolder right;
    @Output TimeStampHolder out;

        public void setup() {
        }

        public void eval() {
            out.value = left.value + right.value;
        }
    }

    @SuppressWarnings("unused")
    @FunctionTemplate(names = {"date_add", "add"}, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls=NullHandling.NULL_IF_NULL)
    public static class TimeDateAddFunction implements DrillSimpleFunc {
        @Param TimeHolder right;
        @Param DateHolder left;
        @Output TimeStampHolder out;

        public void setup() {
        }

        public void eval() {
            out.value = left.value + right.value;
        }
    }

    /* Dummy function template to allow Optiq to validate this function call.
     * At DrillOptiq time we rewrite all date_part() functions to extract functions,
     * since they are essentially the same
     */
    @SuppressWarnings("unused")
    @FunctionTemplate(names = "date_part", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls=NullHandling.NULL_IF_NULL)
    public static class DatePartFunction implements DrillSimpleFunc {
        @Param VarCharHolder left;
        @Param DateHolder right;
        @Output BigIntHolder out;

        public void setup() {
        }

        public void eval() {
            if (1 == 1) {
                throw new UnsupportedOperationException("date_part function should be rewritten as extract() functions");
            }
        }
    }

    @SuppressWarnings("unused")
    @FunctionTemplate(name = "age", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
    public static class AgeTimeStampFunction implements DrillSimpleFunc {
        @Param TimeStampHolder left;
        @Param TimeStampHolder right;
        @Output IntervalHolder out;

        public void setup() {
        }

        public void eval() {
            long diff = left.value - right.value;
            long days = diff / org.apache.arrow.vector.util.DateUtility.daysToStandardMillis;
            out.months = (int) (days / org.apache.arrow.vector.util.DateUtility.monthToStandardDays);
            out.days = (int) (days % org.apache.arrow.vector.util.DateUtility.monthToStandardDays);
            out.milliseconds = (int) (diff % org.apache.arrow.vector.util.DateUtility.daysToStandardMillis);
        }
    }

    @SuppressWarnings("unused")
    @FunctionTemplate(name = "age", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
    public static class AgeTimeStamp2Function implements DrillSimpleFunc {
        @Param TimeStampHolder right;
        @Workspace long queryStartDate;
        @Output IntervalHolder out;
        @Inject ContextInformation contextInfo;

        public void setup() {
            int timeZoneIndex = contextInfo.getRootFragmentTimeZone();
            org.joda.time.DateTimeZone timeZone = org.joda.time.DateTimeZone.forID(org.apache.arrow.vector.util.DateUtility.getTimeZone(timeZoneIndex));
            org.joda.time.DateTime now = new org.joda.time.DateTime(contextInfo.getQueryStartTime(), timeZone);
            queryStartDate = (new org.joda.time.DateMidnight(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), timeZone)).getMillis();
        }

        public void eval() {
            long diff = queryStartDate - right.value;
            long days = diff / org.apache.arrow.vector.util.DateUtility.daysToStandardMillis;
            out.months = (int) (days / org.apache.arrow.vector.util.DateUtility.monthToStandardDays);
            out.days = (int) (days % org.apache.arrow.vector.util.DateUtility.monthToStandardDays);
            out.milliseconds = (int) (diff % org.apache.arrow.vector.util.DateUtility.daysToStandardMillis);
        }
    }

    @SuppressWarnings("unused")
    @FunctionTemplate(name = "age", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
    public static class AgeDateFunction implements DrillSimpleFunc {
        @Param DateHolder left;
        @Param DateHolder right;
        @Output IntervalHolder out;

        public void setup() {
        }

        public void eval() {
          long diff = left.value - right.value;
          long days = diff / org.apache.arrow.vector.util.DateUtility.daysToStandardMillis;
          out.months = (int) (days / org.apache.arrow.vector.util.DateUtility.monthToStandardDays);
          out.days = (int) (days % org.apache.arrow.vector.util.DateUtility.monthToStandardDays);
          out.milliseconds = (int) (diff % org.apache.arrow.vector.util.DateUtility.daysToStandardMillis);
        }
    }

    @SuppressWarnings("unused")
    @FunctionTemplate(name = "age", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
    public static class AgeDate2Function implements DrillSimpleFunc {
        @Param DateHolder right;
        @Workspace long queryStartDate;
        @Output IntervalHolder out;
        @Inject ContextInformation contextInfo;

        public void setup() {
            int timeZoneIndex = contextInfo.getRootFragmentTimeZone();
            org.joda.time.DateTimeZone timeZone = org.joda.time.DateTimeZone.forID(org.apache.arrow.vector.util.DateUtility.getTimeZone(timeZoneIndex));
            org.joda.time.DateTime now = new org.joda.time.DateTime(contextInfo.getQueryStartTime(), timeZone);
            queryStartDate = (new org.joda.time.DateMidnight(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), timeZone)).getMillis();
        }

        public void eval() {
            long diff = queryStartDate - right.value;
            long days = diff / org.apache.arrow.vector.util.DateUtility.daysToStandardMillis;
            out.months = (int) (days / org.apache.arrow.vector.util.DateUtility.monthToStandardDays);
            out.days = (int) (days % org.apache.arrow.vector.util.DateUtility.monthToStandardDays);
            out.milliseconds = (int) (diff % org.apache.arrow.vector.util.DateUtility.daysToStandardMillis);
        }
    }

    @FunctionTemplate(name = "castTIME", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
    public static class CastTimeStampToTime implements DrillSimpleFunc {
        @Param TimeStampHolder in;
        @Output TimeHolder out;

        @Override
        public void setup() {
        }

        @Override
        public void eval() {
            out.value = (int) (in.value % org.apache.arrow.vector.util.DateUtility.daysToStandardMillis);
        }
    }

    @FunctionTemplate(name = "unix_timestamp", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
    public static class UnixTimeStamp implements DrillSimpleFunc {
      @Output BigIntHolder out;
      @Workspace long queryStartDate;
      @Inject ContextInformation contextInfo;

      @Override
      public void setup() {
         queryStartDate = contextInfo.getQueryStartTime();
      }

      @Override
      public void eval() {
        out.value = queryStartDate / 1000;
      }
    }

    @FunctionTemplate(name = "unix_timestamp", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
    public static class UnixTimeStampForDate implements DrillSimpleFunc {
      @Param VarCharHolder inputDateValue;
      @Output BigIntHolder out;
      @Workspace org.joda.time.DateTime date;
      @Workspace org.joda.time.format.DateTimeFormatter formatter;

      @Override
      public void setup() {
         formatter = org.joda.time.format.DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
      }

      @Override
      public void eval() {
        String inputDate = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(inputDateValue.start, inputDateValue.end, inputDateValue.buffer);
        date = (org.joda.time.DateTime) formatter.parseDateTime(inputDate);
        out.value = date.getMillis() / 1000;
      }
    }

    @FunctionTemplate(name = "unix_timestamp", scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
    public static class UnixTimeStampForDateWithPattern implements DrillSimpleFunc {
      @Param VarCharHolder inputDateValue;
      @Param VarCharHolder inputPattern;
      @Output BigIntHolder out;
      @Workspace org.joda.time.DateTime date;
      @Workspace org.joda.time.format.DateTimeFormatter formatter;

      @Override
      public void setup() {
         String pattern = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(inputPattern.start, inputPattern.end, inputPattern.buffer);
         formatter = org.joda.time.format.DateTimeFormat.forPattern(pattern);
      }

      @Override
      public void eval() {
          String inputDate = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(inputDateValue.start, inputDateValue.end, inputDateValue.buffer);
          date = (org.joda.time.DateTime) formatter.parseDateTime(inputDate);
          out.value = date.getMillis() / 1000;
      }
    }
}
