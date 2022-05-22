/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.dataservices.core.odata;

import com.datastax.driver.core.*;
import org.apache.axis2.databinding.utils.ConverterUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.OrderByItem;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.wso2.carbon.dataservices.common.DBConstants;
import org.wso2.carbon.dataservices.core.DBUtils;
import org.wso2.carbon.dataservices.core.DataServiceFault;
import org.wso2.carbon.dataservices.core.odata.DataColumn.ODataDataType;
import org.wso2.carbon.dataservices.core.odata.expression.ExpressionVisitorImpl;
import org.wso2.carbon.dataservices.core.odata.expression.ExpressionVisitorODataEntryImpl;
import org.wso2.carbon.dataservices.core.odata.expression.operand.TypedOperand;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.*;

/**
 * This class implements cassandra datasource related operations for ODataDataHandler.
 *
 * @see ODataDataHandler
 */
public class CassandraDataHandler implements ODataDataHandler {

    /**
     * Table metadata.
     */
    private Map<String, Map<String, DataColumn>> tableMetaData;

    /**
     * Primary Keys of the Tables (Map<Table Name, List>).
     */
    private Map<String, List<String>> primaryKeys;

    /**
     * Config ID.
     */
    private final String configID;

    /**
     * List of Tables in the Database.
     */
    private List<String> tableList;

    /**
     * Cassandra session.
     */
    private final Session session;

    /**
     * Cassandra keyspace.
     */
    private final String keyspace;

    private final int EntityCount = 5000;

    private ResultSet streamResultSet;

    private ArrayList<ODataEntry> entryList;

    private ThreadLocal<Boolean> transactionAvailable = new ThreadLocal<Boolean>() {
        protected synchronized Boolean initialValue() {
            return false;
        }
    };

    private static final int RECORD_INSERT_STATEMENTS_CACHE_SIZE = 10000;

    private Map<String, PreparedStatement> preparedStatementMap =
            Collections.synchronizedMap(new LinkedHashMap<String, PreparedStatement>() {
                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(final Map.Entry<String, PreparedStatement> eldest) {
                    return super.size() > RECORD_INSERT_STATEMENTS_CACHE_SIZE;
                }
            });

    public CassandraDataHandler(String configID, Session session, String keyspace) {
        this.configID = configID;
        this.session = session;
        this.keyspace = keyspace;
        this.tableList = generateTableList();
        this.primaryKeys = generatePrimaryKeyList();
        this.tableMetaData = generateMetaData();
    }

    @Override
    public List<ODataEntry> readTable(String tableName) throws ODataServiceFault {
        Statement statement = new SimpleStatement("Select * from " + this.keyspace + "." + tableName);
        ResultSet resultSet = this.session.execute(statement);
        Iterator<Row> iterator = resultSet.iterator();
        List<ODataEntry> entryList = new ArrayList<>();
        ColumnDefinitions columnDefinitions = resultSet.getColumnDefinitions();
        while (iterator.hasNext()) {
            ODataEntry dataEntry = createDataEntryFromRow(tableName, iterator.next(), columnDefinitions);
            entryList.add(dataEntry);
        }
        return entryList;
    }

    public List<ODataEntry> streamTable(String tableName) throws ODataServiceFault {

        if(this.streamResultSet == null) {
            Statement statement = new SimpleStatement("Select * from " + this.keyspace + "." + tableName);
            statement.setFetchSize(this.EntityCount);
            this.streamResultSet = session.execute(statement);
            this.streamResultSet.fetchMoreResults();
        }

        List<ODataEntry> entryList = new ArrayList<>();

        if(!this.streamResultSet.isFullyFetched()){
            ColumnDefinitions columnDefinitions = this.streamResultSet.getColumnDefinitions();
            Iterator<Row> iterator = this.streamResultSet.iterator();
            int count = 0;
            while (iterator.hasNext()) {
                ODataEntry dataEntry = createDataEntryFromRow(tableName, iterator.next(), columnDefinitions);
                entryList.add(dataEntry);
                count++;
                if(count >= this.EntityCount) {
                    break;
                }
            }
        }
        return entryList;
    }

    public List<ODataEntry> streamTableWithKeys(String tableName, ODataEntry keys) throws ODataServiceFault {
        throw new ODataServiceFault("Cassandra datasources doesn't support navigation.");
    }

    public void initStreaming() {
        this.streamResultSet = null;
        this.entryList = null;
    }

    public List<ODataEntry> StreamTableWithOrder(final String tableName, final OrderByOption orderByOption, EdmEntitySet edmEntitySet) throws ODataServiceFault {

        if(this.entryList == null) {
            this.entryList = new ArrayList<>();

            Statement statement = new SimpleStatement("Select * from " + this.keyspace + "." + tableName);
            ResultSet resultSet = session.execute(statement);

            ColumnDefinitions columnDefinitions = resultSet.getColumnDefinitions();
            Iterator<Row> iterator = resultSet.iterator();
            while (iterator.hasNext()) {
                ODataEntry dataEntry = createDataEntryFromRow(tableName, iterator.next(), columnDefinitions);
                this.entryList.add(dataEntry);
            }

           // final EdmEntitySet edmBindingTarget = edmEntitySet;
           // final order
            final EdmBindingTarget edmBindingTarget = edmEntitySet;
            Collections.sort(this.entryList, new Comparator<ODataEntry>() {
                @Override
                @SuppressWarnings({ "unchecked", "rawtypes" })
                public int compare(final ODataEntry e1, final ODataEntry e2) {
                    // Evaluate the first order option for both entity
                    // If and only if the result of the previous order option is equals to 0
                    // evaluate the next order option until all options are evaluated or they are not equals
                    int result = 0;
                    for (int i = 0; i < orderByOption.getOrders().size() && result == 0; i++) {
                        try {
                            final OrderByItem item = orderByOption.getOrders().get(i);
                            final TypedOperand op1 = item.getExpression()
                                    .accept(new ExpressionVisitorODataEntryImpl(e1, edmBindingTarget, getTableMetadata().get(tableName).values()))
                                    .asTypedOperand();
                            final TypedOperand op2 = item.getExpression()
                                    .accept(new ExpressionVisitorODataEntryImpl(e2, edmBindingTarget, getTableMetadata().get(tableName).values()))
                                    .asTypedOperand();
                            if (op1.isNull() || op2.isNull()) {
                                if (op1.isNull() && op2.isNull()) {
                                    result = 0; // null is equals to null
                                } else {
                                    result = op1.isNull() ? -1 : 1;
                                }
                            } else {
                                Object o1 = op1.getValue();
                                Object o2 = op2.getValue();

                                if (o1.getClass() == o2.getClass() && o1 instanceof Comparable) {
                                    result = ((Comparable) o1).compareTo(o2);
                                } else {
                                    result = 0;
                                }
                            }
                            result = item.isDescending() ? result * -1 : result;
                        } catch (ExpressionVisitException | ODataApplicationException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return result;
                }
            });
        }

        List<ODataEntry> resultSet = new ArrayList<>();

        Iterator<ODataEntry> iterator = this.entryList.iterator();
        int count = 0;
        while (iterator.hasNext()) {
            resultSet.add(iterator.next());
            iterator.remove();
            count++;
            if(count >= this.EntityCount) {
                break;
            }
        }
        

        return resultSet;
    }

    @Override
    public List<ODataEntry> readTableWithKeys(String tableName, ODataEntry keys) throws ODataServiceFault {
        List<ColumnMetadata> cassandraTableMetaData = this.session.getCluster().getMetadata().getKeyspace(this.keyspace)
                                                                  .getTable(tableName).getColumns();
        List<String> pKeys = this.primaryKeys.get(tableName);
        String query = createReadSqlWithKeys(tableName, keys);
        List<Object> values = new ArrayList<>();
        for (String column : this.tableMetaData.get(tableName).keySet()) {
            if (keys.getNames().contains(column) && pKeys.contains(column)) {
                bindParams(column, keys.getValue(column), values, cassandraTableMetaData);
            }
        }
        PreparedStatement statement = this.preparedStatementMap.get(query);
        if (statement == null) {
            statement = this.session.prepare(query);
            this.preparedStatementMap.put(query, statement);
        }
        ResultSet resultSet = this.session.execute(statement.bind(values.toArray()));
        List<ODataEntry> entryList = new ArrayList<>();
        Iterator<Row> iterator = resultSet.iterator();
        ColumnDefinitions definitions = resultSet.getColumnDefinitions();
        while (iterator.hasNext()) {
            ODataEntry dataEntry = createDataEntryFromRow(tableName, iterator.next(), definitions);
            entryList.add(dataEntry);
        }
        return entryList;
    }

    @Override
    public ODataEntry insertEntityToTable(String tableName, ODataEntry entity) throws ODataServiceFault {
        List<ColumnMetadata> cassandraTableMetaData = this.session.getCluster().getMetadata().getKeyspace(this.keyspace)
                                                                  .getTable(tableName).getColumns();
        for (String pkey : this.primaryKeys.get(tableName)) {
            if (this.tableMetaData.get(tableName).get(pkey).getColumnType().equals(ODataDataType.GUID) &&
                entity.getValue(pkey) == null) {
                UUID uuid = UUID.randomUUID();
                entity.addValue(pkey, uuid.toString());
            }
        }
        String query = createInsertCQL(tableName, entity);
        List<Object> values = new ArrayList<>();
        for (DataColumn column : this.tableMetaData.get(tableName).values()) {
            String columnName = column.getColumnName();
            if (entity.getNames().contains(columnName) && entity.getValue(columnName) != null) {
                bindParams(columnName, entity.getValue(columnName), values, cassandraTableMetaData);
            }
        }
        PreparedStatement statement = this.preparedStatementMap.get(query);
        if (statement == null) {
            statement = this.session.prepare(query);
            this.preparedStatementMap.put(query, statement);
        }
        this.session.execute(statement.bind(values.toArray()));
        entity.addValue(ODataConstants.E_TAG, ODataUtils.generateETag(this.configID, tableName, entity));
        return entity;
    }

    @Override
    public boolean deleteEntityInTable(String tableName, ODataEntry entity) throws ODataServiceFault {
        if (transactionAvailable.get()) {
            return deleteEntityInTableTransactional(tableName, entity);
        } else {
            return deleteEntityTableNonTransactional(tableName, entity);
        }
    }

    private boolean deleteEntityTableNonTransactional(String tableName, ODataEntry entity) throws ODataServiceFault {
        List<ColumnMetadata> cassandraTableMetaData = this.session.getCluster().getMetadata().getKeyspace(this.keyspace)
                                                                  .getTable(tableName).getColumns();
        List<String> pKeys = this.primaryKeys.get(tableName);
        String query = createDeleteCQL(tableName);
        List<Object> values = new ArrayList<>();
        for (String column : pKeys) {
            if (entity.getNames().contains(column)) {
                bindParams(column, entity.getValue(column), values, cassandraTableMetaData);
            }
        }
        PreparedStatement statement = this.preparedStatementMap.get(query);
        if (statement == null) {
            statement = this.session.prepare(query);
            this.preparedStatementMap.put(query, statement);
        }
        ResultSet result = this.session.execute(statement.bind(values.toArray()));
        return result.wasApplied();
    }

    private boolean deleteEntityInTableTransactional(String tableName, ODataEntry entity) throws ODataServiceFault {
        List<ColumnMetadata> cassandraTableMetaData = this.session.getCluster().getMetadata().getKeyspace(this.keyspace)
                                                                  .getTable(tableName).getColumns();
        List<String> pKeys = this.primaryKeys.get(tableName);
        String query = createDeleteTransactionalCQL(tableName, entity);
        List<Object> values = new ArrayList<>();
        for (String column : entity.getNames()) {
            if (pKeys.contains(column)) {
                bindParams(column, entity.getValue(column), values, cassandraTableMetaData);
            }
        }
        for (String column : entity.getNames()) {
            if (!pKeys.contains(column)) {
                bindParams(column, entity.getValue(column), values, cassandraTableMetaData);
            }
        }
        PreparedStatement statement = this.preparedStatementMap.get(query);
        if (statement == null) {
            statement = this.session.prepare(query);
            this.preparedStatementMap.put(query, statement);
        }
        ResultSet result = this.session.execute(statement.bind(values.toArray()));
        return result.wasApplied();
    }

    @Override
    public boolean updateEntityInTable(String tableName, ODataEntry newProperties) throws ODataServiceFault {
        List<ColumnMetadata> cassandraTableMetaData = this.session.getCluster().getMetadata().getKeyspace(this.keyspace)
                                                                  .getTable(tableName).getColumns();
        List<String> pKeys = this.primaryKeys.get(tableName);
        String query = createUpdateEntityCQL(tableName, newProperties);
        List<Object> values = new ArrayList<>();
        for (String column : newProperties.getNames()) {
            if (this.tableMetaData.get(tableName).keySet().contains(column) && !pKeys.contains(column)) {
                bindParams(column, newProperties.getValue(column), values, cassandraTableMetaData);
            }
        }
        for (String column : newProperties.getNames()) {
            if (pKeys.contains(column)) {
                bindParams(column, newProperties.getValue(column), values, cassandraTableMetaData);
            }
        }
        PreparedStatement statement = this.preparedStatementMap.get(query);
        if (statement == null) {
            statement = this.session.prepare(query);
            this.preparedStatementMap.put(query, statement);
        }
        ResultSet result = this.session.execute(statement.bind(values.toArray()));
        return result.wasApplied();
    }

    public boolean updateEntityInTableTransactional(String tableName, ODataEntry oldProperties,
                                                    ODataEntry newProperties) throws ODataServiceFault {
        List<ColumnMetadata> cassandraTableMetaData =
                this.session.getCluster().getMetadata().getKeyspace(this.keyspace).getTable(tableName).getColumns();
        List<String> pKeys = this.primaryKeys.get(tableName);
        String query = createUpdateEntityTransactionalCQL(tableName, oldProperties, newProperties);
        List<Object> values = new ArrayList<>();
        for (String column : newProperties.getNames()) {
            if (this.tableMetaData.get(tableName).keySet().contains(column) && !pKeys.contains(column)) {
                bindParams(column, newProperties.getValue(column), values, cassandraTableMetaData);
            }
        }
        for (String column : oldProperties.getNames()) {
            if (pKeys.contains(column)) {
                bindParams(column, oldProperties.getValue(column), values, cassandraTableMetaData);
            }
        }
        for (String column : oldProperties.getNames()) {
            if (!pKeys.contains(column)) {
                bindParams(column, oldProperties.getValue(column), values, cassandraTableMetaData);
            }
        }
        PreparedStatement statement = this.preparedStatementMap.get(query);
        if (statement == null) {
            statement = this.session.prepare(query);
            this.preparedStatementMap.put(query, statement);
        }
        ResultSet result = this.session.execute(statement.bind(values.toArray()));
        return result.wasApplied();
    }

    @Override
    public Map<String, Map<String, DataColumn>> getTableMetadata() {
        return this.tableMetaData;
    }

    @Override
    public List<String> getTableList() {
        return this.tableList;
    }

    @Override
    public Map<String, List<String>> getPrimaryKeys() {
        return this.primaryKeys;
    }

    @Override
    public Map<String, NavigationTable> getNavigationProperties() {
        return null;
    }

    @Override
    public void openTransaction() throws ODataServiceFault {
        this.transactionAvailable.set(true);
        // doesn't support
    }

    @Override
    public void commitTransaction() {
        this.transactionAvailable.set(false);
        //doesn't support
    }

    @Override
    public void rollbackTransaction() throws ODataServiceFault {
        this.transactionAvailable.set(false);
        //doesn't support
    }

    @Override
    public void updateReference(String rootTableName, ODataEntry rootTableKeys, String navigationTable,
                                ODataEntry navigationTableKeys) throws ODataServiceFault {
        throw new ODataServiceFault("Cassandra datasources doesn't support references.");
    }

    @Override
    public void deleteReference(String rootTableName, ODataEntry rootTableKeys, String navigationTable,
                                ODataEntry navigationTableKeys) throws ODataServiceFault {
        throw new ODataServiceFault("Cassandra datasources doesn't support references.");
    }

    @Override
    public int getRowCount(String tableName) throws ODataServiceFault {
        Statement statement = new SimpleStatement("Select count(*) as rowcount from " + this.keyspace + "." + tableName);
        ResultSet resultSet = this.session.execute(statement);
        int count = (int) resultSet.one().getLong("rowcount");

        return count;
    }

    @Override
    public int getRowCountWithKeys(String tableName, ODataEntry keys) throws ODataServiceFault {
        throw new ODataServiceFault("Cassandra datasources doesn't support navigation.");
    }

    /**
     * This method wraps result set data in to DataEntry and creates a list of DataEntry.
     *
     * @param tableName         Table Name
     * @param row               Row
     * @param columnDefinitions Column Definition
     * @return DataEntry
     * @throws ODataServiceFault
     */
    private ODataEntry createDataEntryFromRow(String tableName, Row row, ColumnDefinitions columnDefinitions)
            throws ODataServiceFault {
        String paramValue;
        ODataEntry entry = new ODataEntry();
        //Creating a unique string to represent the
        try {
            for (int i = 0; i < columnDefinitions.size(); i++) {
                String columnName = columnDefinitions.getName(i);
                DataType columnType = columnDefinitions.getType(i);

                switch (columnType.getName()) {
                    case ASCII:
                        paramValue = row.getString(i);
                        break;
                    case BIGINT:
                        paramValue = row.isNull(i) ? null : ConverterUtil.convertToString(row.getLong(i));
                        break;
                    case BLOB:
                        paramValue = this.base64EncodeByteBuffer(row.getBytes(i));
                        break;
                    case BOOLEAN:
                        paramValue = row.isNull(i) ? null : ConverterUtil.convertToString(row.getBool(i));
                        break;
                    case COUNTER:
                        paramValue = row.isNull(i) ? null : ConverterUtil.convertToString(row.getLong(i));
                        break;
                    case DECIMAL:
                        paramValue = row.isNull(i) ? null : ConverterUtil.convertToString(row.getDecimal(i));
                        break;
                    case DOUBLE:
                        paramValue = row.isNull(i) ? null : ConverterUtil.convertToString(row.getDouble(i));
                        break;
                    case FLOAT:
                        paramValue = row.isNull(i) ? null : ConverterUtil.convertToString(row.getFloat(i));
                        break;
                    case INET:
                        paramValue = row.getInet(i).toString();
                        break;
                    case INT:
                        paramValue = row.isNull(i) ? null : ConverterUtil.convertToString(row.getInt(i));
                        break;
                    case TEXT:
                        paramValue = row.getString(i);
                        break;
                    case TIMESTAMP:
                        paramValue = row.isNull(i) ? null : ConverterUtil.convertToString(row.getDate(i));
                        break;
                    case UUID:
                        paramValue = row.isNull(i) ? null : ConverterUtil.convertToString(row.getUUID(i));
                        break;
                    case VARCHAR:
                        paramValue = row.getString(i);
                        break;
                    case VARINT:
                        paramValue = row.isNull(i) ? null : ConverterUtil.convertToString(row.getVarint(i));
                        break;
                    case TIMEUUID:
                        paramValue = row.isNull(i) ? null : ConverterUtil.convertToString(row.getUUID(i));
                        break;
                    case LIST:
                        paramValue = row.isNull(i) ? null : Arrays.toString(row.getList(i, Object.class).toArray());
                        break;
                    case SET:
                        paramValue = row.isNull(i) ? null : row.getSet(i, Object.class).toString();
                        break;
                    case MAP:
                        paramValue = row.isNull(i) ? null : row.getMap(i, Object.class, Object.class).toString();
                        break;
                    case UDT:
                        paramValue = row.isNull(i) ? null : row.getUDTValue(i).toString();
                        break;
                    case TUPLE:
                        paramValue = row.isNull(i) ? null : row.getTupleValue(i).toString();
                        break;
                    case DATE:
                        paramValue = row.isNull(i) ? null : row.getDate(i).toString();
                        break;
                    case CUSTOM:
                        paramValue = row.isNull(i) ? null : this.base64EncodeByteBuffer(row.getBytes(i));
                        break;
                    default:
                        paramValue = row.getString(i);
                        break;
                }
                entry.addValue(columnName, paramValue);
            }
        } catch (DataServiceFault e) {
            throw new ODataServiceFault(e, "Error occurred when creating OData entry. :" + e.getMessage());
        }
        //Set E-Tag to the entity
        entry.addValue("ETag", ODataUtils.generateETag(this.configID, tableName, entry));
        return entry;
    }

    private List<String> generateTableList() {
        List<String> tableList = new ArrayList<>();
        for (TableMetadata tableMetadata : this.session.getCluster().getMetadata().getKeyspace(this.keyspace)
                                                       .getTables()) {
            tableList.add(tableMetadata.getName());
        }
        return tableList;
    }

    private Map<String, List<String>> generatePrimaryKeyList() {
        Map<String, List<String>> primaryKeyMap = new HashMap<>();
        for (String tableName : this.tableList) {
            List<String> primaryKey = new ArrayList<>();
            for (ColumnMetadata columnMetadata : this.session.getCluster().getMetadata().getKeyspace(this.keyspace)
                                                             .getTable(tableName).getPrimaryKey()) {
                primaryKey.add(columnMetadata.getName());
            }
            primaryKeyMap.put(tableName, primaryKey);
        }
        return primaryKeyMap;
    }

    private Map<String, Map<String, DataColumn>> generateMetaData() {
        Map<String, Map<String, DataColumn>> metadata = new HashMap<>();
        for (String tableName : this.tableList) {
            Map<String, DataColumn> dataColumnMap = new HashMap<>();
            for (ColumnMetadata columnMetadata : this.session.getCluster().getMetadata().getKeyspace(this.keyspace)
                                                             .getTable(tableName).getColumns()) {
                DataColumn dataColumn;
                if (this.primaryKeys.get(tableName).contains(columnMetadata.getName())) {
                    dataColumn = new DataColumn(columnMetadata.getName(),
                                                getDataType(columnMetadata.getType().getName()), false);
                } else {
                    dataColumn = new DataColumn(columnMetadata.getName(),
                                                getDataType(columnMetadata.getType().getName()), true);
                }
                dataColumnMap.put(dataColumn.getColumnName(), dataColumn);
            }
            metadata.put(tableName, dataColumnMap);
        }
        return metadata;
    }

    private void bindParams(String columnName, String value, List<Object> values, List<ColumnMetadata> metaData)
            throws ODataServiceFault {
        DataType.Name dataType = null;
        for (ColumnMetadata columnMetadata : metaData) {
            if (columnMetadata.getName().equals(columnName)) {
                dataType = columnMetadata.getType().getName();
                break;
            }
        }
        if (dataType == null) {
            throw new ODataServiceFault("Error occurred when binding data. DataType was missing for " +
                                        columnName + " column.");
        }
        try {
            switch (dataType) {
                case ASCII:
                /* fall through */
                case TEXT:
				/* fall through */
                case VARCHAR:
				/* fall through */
                case TIMEUUID:
                    values.add(value);
                    break;
                case UUID:
                    values.add(value == null ? null : UUID.fromString(value));
                    break;
                case BIGINT:
                    values.add(value == null ? null : Long.parseLong(value));
                    break;
                case VARINT:
				/* fall through */
                case COUNTER:
                    values.add(value == null ? null : value);
                    break;
                case BLOB:
                    values.add(value == null ? null : this.base64DecodeByteBuffer(value));
                    break;
                case BOOLEAN:
                    values.add(value == null ? null : Boolean.parseBoolean(value));
                    break;
                case DECIMAL:
                    values.add(value == null ? null : new BigDecimal(value));
                    break;
                case DOUBLE:
                    values.add(value == null ? null : Double.parseDouble(value));
                    break;
                case FLOAT:
                    values.add(value == null ? null : Float.parseFloat(value));
                    break;
                case INT:
                    values.add(value == null ? null : Integer.parseInt(value));
                    break;
                case TIMESTAMP:
                    values.add(value == null ? null : DBUtils.getTimestamp(value));
                    break;
                case TIME:
                    values.add(value == null ? null : DBUtils.getTime(value));
                    break;
                case DATE:
                    values.add(value == null ? null : DBUtils.getDate(value));
                    break;
                default:
                    values.add(value);
                    break;
            }
        } catch (Exception e) {
            throw new ODataServiceFault(e, "Error occurred when binding data. :" + e.getMessage());
        }
    }

    private ODataDataType getDataType(DataType.Name dataTypeName) {
        ODataDataType dataType;
        switch (dataTypeName) {
            case ASCII:
				/* fall through */
            case TEXT:
				/* fall through */
            case VARCHAR:
				/* fall through */
            case TIMEUUID:
                dataType = ODataDataType.STRING;
                break;
            case UUID:
                dataType = ODataDataType.GUID;
                break;
            case BIGINT:
				/* fall through */
            case VARINT:
				/* fall through */
            case COUNTER:
                dataType = ODataDataType.INT64;
                break;
            case BLOB:
                dataType = ODataDataType.BINARY;
                break;
            case BOOLEAN:
                dataType = ODataDataType.BOOLEAN;
                break;
            case DECIMAL:
				/* fall through */
            case FLOAT:
                dataType = ODataDataType.DECIMAL;
                break;
            case DOUBLE:
                dataType = ODataDataType.DOUBLE;
                break;
            case INT:
                dataType = ODataDataType.INT32;
                break;
            case TIMESTAMP:
                dataType = ODataDataType.DATE_TIMEOFFSET;
                break;
            case TIME:
                dataType = ODataDataType.TIMEOFDAY;
                break;
            case DATE:
                dataType = ODataDataType.DATE;
                break;
            default:
                dataType = ODataDataType.STRING;
                break;
        }
        return dataType;
    }

    /**
     * This method creates a CQL query to update data.
     *
     * @param tableName     Name of the table
     * @param newProperties update entry
     * @return sql Query
     */
    private String createUpdateEntityCQL(String tableName, ODataEntry newProperties) {
        List<String> pKeys = this.primaryKeys.get(tableName);
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(tableName).append(" SET ");
        boolean propertyMatch = false;
        for (String column : newProperties.getNames()) {
            if (!pKeys.contains(column)) {
                if (propertyMatch) {
                    sql.append(",");
                }
                sql.append(column).append(" = ").append(" ? ");
                propertyMatch = true;
            }
        }
        sql.append(" WHERE ");
        // Handling keys
        propertyMatch = false;
        for (String key : pKeys) {
            if (propertyMatch) {
                sql.append(" AND ");
            }
            sql.append(key).append(" = ").append(" ? ");
            propertyMatch = true;
        }
        return sql.toString();
    }

    /**
     * This method creates a CQL query to update data.
     *
     * @param tableName     Name of the table
     * @param oldProperties old Properties
     * @param newProperties update entry
     * @return sql Query
     */
    private String createUpdateEntityTransactionalCQL(String tableName, ODataEntry oldProperties,
                                                      ODataEntry newProperties) {
        List<String> pKeys = this.primaryKeys.get(tableName);
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(tableName).append(" SET ");
        boolean propertyMatch = false;
        for (String column : newProperties.getNames()) {
            if (!pKeys.contains(column)) {
                if (propertyMatch) {
                    sql.append(",");
                }
                sql.append(column).append(" = ").append(" ? ");
                propertyMatch = true;
            }
        }
        sql.append(" WHERE ");
        // Handling keys
        propertyMatch = false;
        for (String key : pKeys) {
            if (propertyMatch) {
                sql.append(" AND ");
            }
            sql.append(key).append(" = ").append(" ? ");
            propertyMatch = true;
        }
        sql.append(" IF ");
        propertyMatch = false;
        for (String column : oldProperties.getNames()) {
            if (!pKeys.contains(column)) {
                if (propertyMatch) {
                    sql.append(" AND ");
                }
                sql.append(column).append(" = ").append(" ? ");
                propertyMatch = true;
            }
        }
        return sql.toString();
    }

    /**
     * This method creates a CQL query to insert data in table.
     *
     * @param tableName Name of the table
     * @return sqlQuery
     */
    private String createInsertCQL(String tableName, ODataEntry entry) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableName).append(" (");
        boolean propertyMatch = false;
        for (DataColumn column : this.tableMetaData.get(tableName).values()) {
            if (entry.getValue(column.getColumnName()) != null) {
                if (propertyMatch) {
                    sql.append(",");
                }
                sql.append(column.getColumnName());
                propertyMatch = true;
            }
        }
        sql.append(" ) VALUES ( ");
        propertyMatch = false;
        for (DataColumn column : this.tableMetaData.get(tableName).values()) {
            if (entry.getValue(column.getColumnName()) != null) {
                if (propertyMatch) {
                    sql.append(",");
                }
                sql.append(" ? ");
                propertyMatch = true;
            }
        }
        sql.append(" ) ");
        return sql.toString();
    }

    /**
     * This method creates CQL query to read data with keys.
     *
     * @param tableName Name of the table
     * @param keys      Keys
     * @return sql Query
     */
    private String createReadSqlWithKeys(String tableName, ODataEntry keys) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(tableName).append(" WHERE ");
        boolean propertyMatch = false;
        for (DataColumn column : this.tableMetaData.get(tableName).values()) {
            if (keys.getValue(column.getColumnName()) != null) {
                if (propertyMatch) {
                    sql.append(" AND ");
                }
                sql.append(column.getColumnName()).append(" = ").append(" ? ");
                propertyMatch = true;
            }
        }
        return sql.toString();
    }

    /**
     * This method creates CQL query to delete data.
     *
     * @param tableName Name of the table
     * @return sql Query
     */
    private String createDeleteCQL(String tableName) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(tableName).append(" WHERE ");
        List<String> pKeys = this.primaryKeys.get(tableName);
        boolean propertyMatch = false;
        for (String key : pKeys) {
            if (propertyMatch) {
                sql.append(" AND ");
            }
            sql.append(key).append(" = ").append(" ? ");
            propertyMatch = true;
        }
        return sql.toString();
    }

    /**
     * This method creates CQL query to delete data.
     *
     * @param tableName Name of the table
     * @return sql Query
     */
    private String createDeleteTransactionalCQL(String tableName, ODataEntry entry) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(tableName).append(" WHERE ");
        List<String> pKeys = this.primaryKeys.get(tableName);
        boolean propertyMatch = false;
        for (String key : entry.getNames()) {
            if (pKeys.contains(key)) {
                if (propertyMatch) {
                    sql.append(" AND ");
                }
                sql.append(key).append(" = ").append(" ? ");
                propertyMatch = true;
            }
        }
        sql.append(" IF ");
        propertyMatch = false;
        for (String column : entry.getNames()) {
            if (!pKeys.contains(column)) {
                if (propertyMatch) {
                    sql.append(" AND ");
                }
                sql.append(column).append(" = ").append(" ? ");
                propertyMatch = true;
            }
        }
        return sql.toString();
    }

    private String base64EncodeByteBuffer(ByteBuffer byteBuffer) throws ODataServiceFault {
        byte[] data = byteBuffer.array();
        byte[] base64Data = Base64.encodeBase64(data);
        try {
            return new String(base64Data, DBConstants.DEFAULT_CHAR_SET_TYPE);
        } catch (UnsupportedEncodingException e) {
            throw new ODataServiceFault(e, "Error in encoding result binary data: " + e.getMessage());
        }
    }

    private ByteBuffer base64DecodeByteBuffer(String data) throws ODataServiceFault {
        try {
            byte[] buff = Base64.decodeBase64(data.getBytes(DBConstants.DEFAULT_CHAR_SET_TYPE));
            ByteBuffer result = ByteBuffer.allocate(buff.length);
            result.put(buff);
            return result;
        } catch (UnsupportedEncodingException e) {
            throw new ODataServiceFault(e, "Error in decoding input base64 data: " + e.getMessage());
        }
    }

}
