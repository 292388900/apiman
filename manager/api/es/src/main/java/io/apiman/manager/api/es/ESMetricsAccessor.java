/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apiman.manager.api.es;

import io.apiman.manager.api.beans.metrics.UsageDataPoint;
import io.apiman.manager.api.beans.metrics.UsageHistogramBean;
import io.apiman.manager.api.beans.metrics.UsageHistogramIntervalType;
import io.apiman.manager.api.beans.metrics.UsagePerAppBean;
import io.apiman.manager.api.beans.metrics.UsagePerPlanBean;
import io.apiman.manager.api.core.IMetricsAccessor;
import io.apiman.manager.api.core.logging.ApimanLogger;
import io.apiman.manager.api.core.logging.IApimanLogger;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.DateHistogramAggregation;
import io.searchbox.core.search.aggregation.DateHistogramAggregation.DateHistogram;
import io.searchbox.core.search.aggregation.MetricAggregation;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang.time.DateFormatUtils;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * An elasticsearch implementation of the {@link IMetricsAccessor} interface.  This
 * implementation knows how to get metrics/analytics information out of an
 * elasticsearch store.
 *
 * @author eric.wittmann@redhat.com
 */
@ApplicationScoped @Alternative
public class ESMetricsAccessor implements IMetricsAccessor {

    private static final String INDEX_NAME = "apiman_metrics"; //$NON-NLS-1$

    @Inject @ApimanLogger(ESMetricsAccessor.class)
    IApimanLogger log;

    @Inject @Named("metrics")
    private JestClient esClient;

    /**
     * Constructor.
     */
    public ESMetricsAccessor() {
    }

    /**
     * @see io.apiman.manager.api.core.IMetricsAccessor#getUsage(java.lang.String, java.lang.String, java.lang.String, io.apiman.manager.api.beans.metrics.UsageHistogramIntervalType, org.joda.time.DateTime, org.joda.time.DateTime)
     */
    @SuppressWarnings("nls")
    @Override
    public UsageHistogramBean getUsage(String organizationId, String serviceId, String version,
            UsageHistogramIntervalType interval, DateTime from, DateTime to) {
        UsageHistogramBean rval = new UsageHistogramBean();
        Map<String, UsageDataPoint> index = generateHistogramSkeleton(rval, from, to, interval);

        try {
            String query =
                    "{" +
                    "  \"query\": {" +
                    "    \"filtered\" : {" +
                    "      \"query\" : {" +
                    "        \"range\" : {" +
                    "          \"requestStart\" : {" +
                    "            \"gte\": \"${from}\"," +
                    "            \"lte\": \"${to}\"" +
                    "          }" +
                    "        }" +
                    "      }," +
                    "      \"filter\": {" +
                    "        \"and\" : [" +
                    "          { \"term\" : { \"serviceOrgId\" : \"${serviceOrgId}\" } }," +
                    "          { \"term\" : { \"serviceId\" : \"${serviceId}\" } }," +
                    "          { \"term\" : { \"serviceVersion\" : \"${serviceVersion}\" } }" +
                    "        ]" +
                    "      }" +
                    "    }" +
                    "  }," +
                    "  \"size\": 0, " +
                    "  \"aggs\" : {" +
                    "      \"usage\" : {" +
                    "          \"date_histogram\" : {" +
                    "              \"field\" : \"requestStart\"," +
                    "              \"interval\" : \"${interval}\"" +
                    "          }" +
                    "      }" +
                    "  }" +
                    "}";
            Map<String, String> params = new HashMap<>();
            params.put("from", formatDate(from));
            params.put("to", formatDate(to));
            params.put("serviceOrgId", organizationId.replace('"', '_'));
            params.put("serviceId", serviceId.replace('"', '_'));
            params.put("serviceVersion", version.replace('"', '_'));
            params.put("interval", interval.name());
            StrSubstitutor ss = new StrSubstitutor(params);
            query = ss.replace(query);

            Search search = new Search.Builder(query).addIndex(INDEX_NAME).addType("request").build();
            SearchResult response = getEsClient().execute(search);
            MetricAggregation aggregations = response.getAggregations();
            DateHistogramAggregation aggregation = aggregations.getDateHistogramAggregation("usage");

            List<DateHistogram> buckets = aggregation.getBuckets();
            for (DateHistogram entry : buckets) {
                String keyAsString = entry.getTimeAsString();
                if (index.containsKey(keyAsString)) {
                    index.get(keyAsString).setCount(entry.getCount());
                }
            }
        } catch (IOException e) {
            log.error(e);
        }

        return rval;
    }

    /**
     * Generate the histogram buckets based on the time frame requested and the interval.  This will
     * add an entry for each 'slot' or 'bucket' in the histogram, setting the count to 0.
     * @param rval
     * @param from
     * @param to
     * @param interval
     */
    public static Map<String, UsageDataPoint> generateHistogramSkeleton(UsageHistogramBean rval, DateTime from, DateTime to,
            UsageHistogramIntervalType interval) {
        Map<String, UsageDataPoint> index = new HashMap<>();

        Calendar fromCal = from.toGregorianCalendar();
        Calendar toCal = to.toGregorianCalendar();

        switch(interval) {
            case day:
                fromCal.set(Calendar.HOUR_OF_DAY, 0);
                fromCal.set(Calendar.MINUTE, 0);
                fromCal.set(Calendar.SECOND, 0);
                fromCal.set(Calendar.MILLISECOND, 0);
                break;
            case hour:
                fromCal.set(Calendar.MINUTE, 0);
                fromCal.set(Calendar.SECOND, 0);
                fromCal.set(Calendar.MILLISECOND, 0);
                break;
            case minute:
                fromCal.set(Calendar.SECOND, 0);
                fromCal.set(Calendar.MILLISECOND, 0);
                break;
            case month:
                fromCal.set(Calendar.DAY_OF_MONTH, 1);
                fromCal.set(Calendar.HOUR_OF_DAY, 0);
                fromCal.set(Calendar.MINUTE, 0);
                fromCal.set(Calendar.SECOND, 0);
                fromCal.set(Calendar.MILLISECOND, 0);
                break;
            case week:
                fromCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                fromCal.set(Calendar.HOUR_OF_DAY, 0);
                fromCal.set(Calendar.MINUTE, 0);
                fromCal.set(Calendar.SECOND, 0);
                fromCal.set(Calendar.MILLISECOND, 0);
                break;
            default:
                break;
        }

        while (fromCal.before(toCal)) {
            String label = formatDateWithMillis(fromCal);
            UsageDataPoint point = new UsageDataPoint(label, 0L);
            rval.getData().add(point);
            index.put(label, point);
            switch (interval) {
                case day:
                    fromCal.add(Calendar.DAY_OF_YEAR, 1);
                    break;
                case hour:
                    fromCal.add(Calendar.HOUR_OF_DAY, 1);
                    break;
                case minute:
                    fromCal.add(Calendar.MINUTE, 1);
                    break;
                case month:
                    fromCal.add(Calendar.MONTH, 1);
                    break;
                case week:
                    fromCal.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                default:
                    break;
            }
        }

        return index;

    }

    /**
     * @see io.apiman.manager.api.core.IMetricsAccessor#getUsagePerApp(java.lang.String, java.lang.String, java.lang.String, org.joda.time.DateTime, org.joda.time.DateTime)
     */
    @SuppressWarnings("nls")
    @Override
    public UsagePerAppBean getUsagePerApp(String organizationId, String serviceId, String version,
            DateTime from, DateTime to) {
        UsagePerAppBean rval = new UsagePerAppBean();

        try {
            String query =
                    "{" +
                    "  \"query\": {" +
                    "    \"filtered\" : {" +
                    "      \"query\" : {" +
                    "        \"range\" : {" +
                    "          \"requestStart\" : {" +
                    "            \"gte\": \"${from}\"," +
                    "            \"lte\": \"${to}\"" +
                    "          }" +
                    "        }" +
                    "      }," +
                    "      \"filter\": {" +
                    "        \"and\" : [" +
                    "          { \"term\" : { \"serviceOrgId\" : \"${serviceOrgId}\" } }," +
                    "          { \"term\" : { \"serviceId\" : \"${serviceId}\" } }," +
                    "          { \"term\" : { \"serviceVersion\" : \"${serviceVersion}\" } }" +
                    "        ]" +
                    "      }" +
                    "    }" +
                    "  }," +
                    "  \"size\": 0, " +
                    "  \"aggs\" : {" +
                    "      \"usage_by_app\" : {" +
                    "        \"terms\" : {" +
                    "          \"field\" : \"applicationId\"" +
                    "        }" +
                    "      }" +
                    "  }" +
                    "}";
            Map<String, String> params = new HashMap<>();
            params.put("from", formatDate(from));
            params.put("to", formatDate(to));
            params.put("serviceOrgId", organizationId.replace('"', '_'));
            params.put("serviceId", serviceId.replace('"', '_'));
            params.put("serviceVersion", version.replace('"', '_'));
            StrSubstitutor ss = new StrSubstitutor(params);
            query = ss.replace(query);

            Search search = new Search.Builder(query).addIndex(INDEX_NAME).addType("request").build();
            SearchResult response = getEsClient().execute(search);
            MetricAggregation aggregations = response.getAggregations();
            ApimanTermsAggregation termsAggregation = aggregations.getAggregation("usage_by_app", ApimanTermsAggregation.class); //$NON-NLS-1$
            List<ApimanTermsAggregation.Entry> buckets = termsAggregation.getBuckets();
            for (ApimanTermsAggregation.Entry entry : buckets) {
                rval.getData().put(entry.getKey(), entry.getCount());
            }
        } catch (IOException e) {
            log.error(e);
        }

        return rval;
    }

    /**
     * @see io.apiman.manager.api.core.IMetricsAccessor#getUsagePerPlan(java.lang.String, java.lang.String, java.lang.String, org.joda.time.DateTime, org.joda.time.DateTime)
     */
    @SuppressWarnings("nls")
    @Override
    public UsagePerPlanBean getUsagePerPlan(String organizationId, String serviceId, String version,
            DateTime from, DateTime to) {
        UsagePerPlanBean rval = new UsagePerPlanBean();

        try {
            String query =
                    "{" +
                    "  \"query\": {" +
                    "    \"filtered\" : {" +
                    "      \"query\" : {" +
                    "        \"range\" : {" +
                    "          \"requestStart\" : {" +
                    "            \"gte\": \"${from}\"," +
                    "            \"lte\": \"${to}\"" +
                    "          }" +
                    "        }" +
                    "      }," +
                    "      \"filter\": {" +
                    "        \"and\" : [" +
                    "          { \"term\" : { \"serviceOrgId\" : \"${serviceOrgId}\" } }," +
                    "          { \"term\" : { \"serviceId\" : \"${serviceId}\" } }," +
                    "          { \"term\" : { \"serviceVersion\" : \"${serviceVersion}\" } }" +
                    "        ]" +
                    "      }" +
                    "    }" +
                    "  }," +
                    "  \"size\": 0, " +
                    "  \"aggs\" : {" +
                    "      \"usage_by_plan\" : {" +
                    "        \"terms\" : {" +
                    "          \"field\" : \"planId\"" +
                    "        }" +
                    "      }" +
                    "  }" +
                    "}";
            Map<String, String> params = new HashMap<>();
            params.put("from", formatDate(from));
            params.put("to", formatDate(to));
            params.put("serviceOrgId", organizationId.replace('"', '_'));
            params.put("serviceId", serviceId.replace('"', '_'));
            params.put("serviceVersion", version.replace('"', '_'));
            StrSubstitutor ss = new StrSubstitutor(params);
            query = ss.replace(query);

            Search search = new Search.Builder(query).addIndex(INDEX_NAME).addType("request").build();
            SearchResult response = getEsClient().execute(search);
            MetricAggregation aggregations = response.getAggregations();
            ApimanTermsAggregation termsAggregation = aggregations.getAggregation("usage_by_plan", ApimanTermsAggregation.class); //$NON-NLS-1$
            List<ApimanTermsAggregation.Entry> buckets = termsAggregation.getBuckets();
            for (ApimanTermsAggregation.Entry entry : buckets) {
                rval.getData().put(entry.getKey(), entry.getCount());
            }
        } catch (IOException e) {
            log.error(e);
        }

        return rval;
    }

    /**
     * @param date
     */
    protected static String formatDate(DateTime date) {
        return ISODateTimeFormat.dateTimeNoMillis().print(date);
    }

    /**
     * @param date
     */
    protected static String formatDateWithMillis(DateTime date) {
        return ISODateTimeFormat.dateTime().print(date);
    }

    /**
     * @param date
     */
    protected static String formatDate(Calendar date) {
        return DateFormatUtils.formatUTC(date.getTimeInMillis(), "yyyy-MM-dd'T'HH:mm:ss'Z'"); //$NON-NLS-1$
    }

    /**
     * @param date
     */
    protected static String formatDateWithMillis(Calendar date) {
        return DateFormatUtils.formatUTC(date.getTimeInMillis(), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); //$NON-NLS-1$
    }

    /**
     * @return the esClient
     */
    public JestClient getEsClient() {
        return esClient;
    }

    /**
     * @param esClient the esClient to set
     */
    public void setEsClient(JestClient esClient) {
        this.esClient = esClient;
    }

}
