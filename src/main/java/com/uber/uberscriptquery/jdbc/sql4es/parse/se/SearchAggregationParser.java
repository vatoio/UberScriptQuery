package com.uber.uberscriptquery.jdbc.sql4es.parse.se;

import com.uber.uberscriptquery.jdbc.sql4es.ESResultSet;
import com.uber.uberscriptquery.jdbc.sql4es.model.Column;
import com.uber.uberscriptquery.jdbc.sql4es.model.Column.Operation;
import com.uber.uberscriptquery.jdbc.sql4es.model.Utils;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.InternalNumericMetricsAggregation;
import org.elasticsearch.search.aggregations.metrics.avg.InternalAvg;
import org.elasticsearch.search.aggregations.metrics.cardinality.InternalCardinality;
import org.elasticsearch.search.aggregations.metrics.max.InternalMax;
import org.elasticsearch.search.aggregations.metrics.min.InternalMin;
import org.elasticsearch.search.aggregations.metrics.percentiles.Percentile;
import org.elasticsearch.search.aggregations.metrics.sum.InternalSum;
import org.elasticsearch.search.aggregations.metrics.valuecount.InternalValueCount;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * Parses aggregation part of elasticsearch result.
 *
 * @author cversloot
 */
public class SearchAggregationParser {

    /**
     * Parses an ES aggregation into a set of ResultRows
     *
     * @param agg
     * @return
     * @throws SQLException
     */
    public void parseAggregation(Aggregation agg, ESResultSet rs) throws SQLException {
        if (agg instanceof Terms) {
            dfsAggregations((Terms) agg, rs, rs.getNewRow());
        } else if (agg instanceof InternalFilter) {
            processFilterAgg((InternalFilter) agg, rs);
        } else if (agg instanceof InternalCardinality) {
            processCardinalityAgg((InternalCardinality) agg, rs);
        } else throw new SQLException("Unknown aggregation type " + agg.getClass().getName());
    }

    /**
     * Parse an aggregation result based on one or more aggregated terms
     *
     * @param terms
     * @param rs
     * @param row
     * @throws SQLException
     */
    private void dfsAggregations(Terms terms, ESResultSet rs, List<Object> row) throws SQLException {
        List<Object> currentRow = Utils.clone(row);
        String columnName = terms.getName();
        if (!rs.getHeading().hasLabel(columnName)) throw new SQLException("Unable to identify column for aggregation named " + columnName);
        Column aggCol = rs.getHeading().getColumnByLabel(columnName);
        for (Terms.Bucket bucket : terms.getBuckets()) {
            if (bucket instanceof StringTerms.Bucket) {
                aggCol.setSqlType(Types.VARCHAR);
            } else if (bucket instanceof LongTerms.Bucket) {
                aggCol.setSqlType(Types.TIMESTAMP);
                //ToDO: chack Timestamp
            }
            boolean metricAggs = false;
            List<Aggregation> aggs = bucket.getAggregations().asList();
            if (aggs.size() == 0) {
                currentRow.set(aggCol.getIndex(), bucket.getKey());
                metricAggs = true;
            } else for (Aggregation agg : bucket.getAggregations().asList()) {
                if (agg instanceof Terms) {
                    currentRow.set(aggCol.getIndex(), bucket.getKey());
                    dfsAggregations((Terms) agg, rs, currentRow);
                } else {
                    if (metricAggs == false) {
                        currentRow.set(aggCol.getIndex(), bucket.getKey());
                        metricAggs = true;
                    }
                    String metricName = agg.getName();
                    if (!rs.getHeading().hasLabel(metricName))
                        throw new SQLException("Unable to identify column for aggregation named " + metricName);
                    Column metricCol = rs.getHeading().getColumnByLabel(metricName);
                    // ToDo: check it
                    if (agg instanceof InternalAvg) {
                        currentRow.set(metricCol.getIndex(), ((InternalAvg) agg).getValue());
                    } else if (agg instanceof InternalCardinality) {
                        currentRow.set(metricCol.getIndex(), ((InternalCardinality) agg).getValue());
                    } else if (agg instanceof InternalMax) {
                        currentRow.set(metricCol.getIndex(), ((InternalMax) agg).getValue());
                    } else if (agg instanceof InternalMin) {
                        currentRow.set(metricCol.getIndex(), ((InternalMin) agg).getValue());
                    } else if (agg instanceof Percentile) {
                        currentRow.set(metricCol.getIndex(), ((Percentile) agg).getValue());
                    } else if (agg instanceof InternalSum) {
                        currentRow.set(metricCol.getIndex(), ((InternalSum) agg).getValue());
                    } else if (agg instanceof InternalValueCount) {
                        currentRow.set(metricCol.getIndex(), ((InternalValueCount) agg).getValue());
                    } else if (agg instanceof InternalNumericMetricsAggregation.SingleValue) {
                        currentRow.set(metricCol.getIndex(), ((InternalNumericMetricsAggregation.SingleValue) agg).getValueAsString());
                    } else {
                        // ToDo: I don't know (
                        currentRow.set(metricCol.getIndex(), agg.getName());
                    }
                }
            }
            if (metricAggs) {
                rs.add(currentRow);
                currentRow = Utils.clone(row);
            }
            currentRow = Utils.clone(row);
        }
    }

    /**
     * Parse an aggregation performed without grouping.
     *
     * @param filter
     * @param rs
     * @throws SQLException
     */
    private void processFilterAgg(InternalFilter filter, ESResultSet rs) throws SQLException {
        //String name = global.getName(); // we do not care about the global name for now
        List<Object> row = rs.getNewRow();
        Column count = null;
        for (Column c : rs.getHeading().columns())
            if (c.getOp() == Operation.COUNT) count = c;

        if (count != null) {
            row.set(count.getIndex(), filter.getDocCount());
        }
        for (Aggregation agg : filter.getAggregations()) {
            if (agg instanceof InternalNumericMetricsAggregation.SingleValue) {
                InternalNumericMetricsAggregation.SingleValue numericAgg =
                        (InternalNumericMetricsAggregation.SingleValue) agg;
                String name = numericAgg.getName();
                Column column = rs.getHeading().getColumnByLabel(name);
                if (column == null) {
                    throw new SQLException("Unable to identify column for " + name);
                }
                row.set(column.getIndex(), numericAgg.value());
            } else throw new SQLException("Unable to parse aggregation of type " + agg.getClass());
        }
        rs.add(row);
    }

    private void processCardinalityAgg(InternalCardinality agg, ESResultSet rs) {
        List<Object> row = rs.getNewRow();
        Column column = rs.getHeading().getColumnByLabel(agg.getName());
        row.set(column.getIndex(), agg.value());
        rs.add(row);
    }

}
