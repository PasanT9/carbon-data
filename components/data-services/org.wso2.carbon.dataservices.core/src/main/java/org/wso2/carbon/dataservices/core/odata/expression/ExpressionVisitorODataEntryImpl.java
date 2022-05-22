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
package org.wso2.carbon.dataservices.core.odata.expression;

import org.apache.axis2.databinding.utils.ConverterUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.*;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceLambdaAll;
import org.apache.olingo.server.api.uri.UriResourceLambdaAny;
import org.apache.olingo.server.api.uri.UriResourceProperty;
import org.apache.olingo.server.api.uri.queryoption.expression.*;
import org.wso2.carbon.dataservices.common.DBConstants;
import org.wso2.carbon.dataservices.core.odata.*;
import org.wso2.carbon.dataservices.core.odata.expression.operand.TypedOperand;
import org.wso2.carbon.dataservices.core.odata.expression.operand.UntypedOperand;
import org.wso2.carbon.dataservices.core.odata.expression.operand.VisitorOperand;
import org.wso2.carbon.dataservices.core.odata.expression.operation.BinaryOperator;
import org.wso2.carbon.dataservices.core.odata.expression.operation.MethodCallOperator;
import org.wso2.carbon.dataservices.core.odata.expression.operation.UnaryOperator;

import java.text.ParseException;
import java.util.*;

public class ExpressionVisitorODataEntryImpl implements ExpressionVisitor<VisitorOperand> {

    final private ODataEntry entity;
    final Collection<DataColumn> tableMetaData;

    public ExpressionVisitorODataEntryImpl(final ODataEntry entity, final EdmBindingTarget bindingTarget, Collection<DataColumn> tableMetaData) {
        this.entity = entity;
        this.tableMetaData = tableMetaData;
    }

    @Override
    public VisitorOperand visitBinaryOperator(final BinaryOperatorKind operator, final VisitorOperand left,
                                              final VisitorOperand right)
            throws ExpressionVisitException, ODataApplicationException {
        final BinaryOperator binaryOperator = new BinaryOperator(left, right);
        switch (operator) {
            case AND:
                return binaryOperator.andOperator();
            case OR:
                return binaryOperator.orOperator();
            case EQ:
                return binaryOperator.equalsOperator();
            case NE:
                return binaryOperator.notEqualsOperator();
            case GE:
                return binaryOperator.greaterEqualsOperator();
            case GT:
                return binaryOperator.greaterThanOperator();
            case LE:
                return binaryOperator.lessEqualsOperator();
            case LT:
                return binaryOperator.lessThanOperator();
            case ADD:
                /* fall through */
            case SUB:
				/* fall through */
            case MUL:
				/* fall through */
            case DIV:
				/* fall through */
            case MOD:
                return binaryOperator.arithmeticOperator(operator);
            default:
                return throwNotImplemented();
        }
    }

    @Override
    public VisitorOperand visitUnaryOperator(final UnaryOperatorKind operator, final VisitorOperand operand)
            throws ExpressionVisitException, ODataApplicationException {
        final UnaryOperator unaryOperator = new UnaryOperator(operand);
        switch (operator) {
            case MINUS:
                return unaryOperator.minusOperation();
            case NOT:
                return unaryOperator.notOperation();
            default:
                // Can't happen.
                return throwNotImplemented();
        }
    }

    @Override
    public VisitorOperand visitMethodCall(final MethodKind methodCall, final List<VisitorOperand> parameters)
            throws ExpressionVisitException, ODataApplicationException {
        final MethodCallOperator methodCallOperation = new MethodCallOperator(parameters);

        switch (methodCall) {
            case ENDSWITH:
                return methodCallOperation.endsWith();
            case INDEXOF:
                return methodCallOperation.indexOf();
            case STARTSWITH:
                return methodCallOperation.startsWith();
            case TOLOWER:
                return methodCallOperation.toLower();
            case TOUPPER:
                return methodCallOperation.toUpper();
            case TRIM:
                return methodCallOperation.trim();
            case SUBSTRING:
                return methodCallOperation.substring();
            case CONTAINS:
                return methodCallOperation.contains();
            case CONCAT:
                return methodCallOperation.concat();
            case LENGTH:
                return methodCallOperation.length();
            case YEAR:
                return methodCallOperation.year();
            case MONTH:
                return methodCallOperation.month();
            case DAY:
                return methodCallOperation.day();
            case HOUR:
                return methodCallOperation.hour();
            case MINUTE:
                return methodCallOperation.minute();
            case SECOND:
                return methodCallOperation.second();
            case FRACTIONALSECONDS:
                return methodCallOperation.fractionalSeconds();
            case ROUND:
                return methodCallOperation.round();
            case FLOOR:
                return methodCallOperation.floor();
            case CEILING:
                return methodCallOperation.ceiling();
            default:
                return throwNotImplemented();
        }
    }

    @Override
    public VisitorOperand visitLambdaExpression(final String lambdaFunction, final String lambdaVariable,
                                                final Expression expression)
            throws ExpressionVisitException, ODataApplicationException {
        return throwNotImplemented();
    }

    @Override
    public VisitorOperand visitLiteral(Literal literal) throws ExpressionVisitException, ODataApplicationException {
        return new UntypedOperand(literal.getText());
    }

    @Override
    public VisitorOperand visitMember(Member member) throws ExpressionVisitException, ODataApplicationException {
        final List<UriResource> uriResourceParts = member.getResourcePath().getUriResourceParts();
        int size = uriResourceParts.size();
        if (uriResourceParts.get(0) instanceof UriResourceProperty) {
            EdmProperty currentEdmProperty = ((UriResourceProperty) uriResourceParts.get(0)).getProperty();
            Property currentProperty = null;
            for (DataColumn column : this.tableMetaData) {
                String columnName = column.getColumnName();
                try {
                    Property temp = createPrimitive(column.getColumnType(), columnName, this.entity.getValue(columnName));
                    if(temp.getName().equals(currentEdmProperty.getName())) {
                        currentProperty = temp;
                        break;
                    }
                }
                catch (ParseException | ODataServiceFault e){
                    e.printStackTrace();
                }
            }
            return new TypedOperand(currentProperty.getValue(), currentEdmProperty.getType(), currentEdmProperty);
        } else if (uriResourceParts.get(size - 1) instanceof UriResourceLambdaAll) {
            return throwNotImplemented();
        } else if (uriResourceParts.get(size - 1) instanceof UriResourceLambdaAny) {
            return throwNotImplemented();
        } else {
            return throwNotImplemented();
        }
    }

    private Property createPrimitive(final DataColumn.ODataDataType columnType, final String name,
                                     final String paramValue) throws ODataServiceFault, ParseException {
        String propertyType;
        Object value;
        switch (columnType) {
            case INT32:
                propertyType = EdmPrimitiveTypeKind.Int32.getFullQualifiedName().getFullQualifiedNameAsString();
                value = paramValue == null ? null : ConverterUtil.convertToInt(paramValue);
                break;
            case INT16:
                propertyType = EdmPrimitiveTypeKind.Int16.getFullQualifiedName().getFullQualifiedNameAsString();
                value = paramValue == null ? null : ConverterUtil.convertToShort(paramValue);
                break;
            case DOUBLE:
                propertyType = EdmPrimitiveTypeKind.Double.getFullQualifiedName().getFullQualifiedNameAsString();
                value = paramValue == null ? null : ConverterUtil.convertToDouble(paramValue);
                break;
            case STRING:
                propertyType = EdmPrimitiveTypeKind.String.getFullQualifiedName().getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case BOOLEAN:
                propertyType = EdmPrimitiveTypeKind.Boolean.getFullQualifiedName().getFullQualifiedNameAsString();
                value = paramValue == null ? null : ConverterUtil.convertToBoolean(paramValue);
                break;
            case BINARY:
                propertyType = EdmPrimitiveTypeKind.Binary.getFullQualifiedName().getFullQualifiedNameAsString();
                value = paramValue == null ? null : getBytesFromBase64String(paramValue);
                break;
            case BYTE:
                propertyType = EdmPrimitiveTypeKind.Byte.getFullQualifiedName().getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case SBYTE:
                propertyType = EdmPrimitiveTypeKind.SByte.getFullQualifiedName().getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case DATE:
                propertyType = EdmPrimitiveTypeKind.Date.getFullQualifiedName().getFullQualifiedNameAsString();
                value = ConverterUtil.convertToDate(paramValue);
                break;
            case DURATION:
                propertyType = EdmPrimitiveTypeKind.Duration.getFullQualifiedName().getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case DECIMAL:
                propertyType = EdmPrimitiveTypeKind.Decimal.getFullQualifiedName().getFullQualifiedNameAsString();
                value = paramValue == null ? null : ConverterUtil.convertToBigDecimal(paramValue);
                break;
            case SINGLE:
                propertyType = EdmPrimitiveTypeKind.Single.getFullQualifiedName().getFullQualifiedNameAsString();
                value = paramValue == null ? null : ConverterUtil.convertToFloat(paramValue);
                break;
            case TIMEOFDAY:
                propertyType = EdmPrimitiveTypeKind.TimeOfDay.getFullQualifiedName().getFullQualifiedNameAsString();
                value = paramValue == null ? null : ConverterUtil.convertToTime(paramValue).getAsCalendar();
                break;
            case INT64:
                propertyType = EdmPrimitiveTypeKind.Int64.getFullQualifiedName().getFullQualifiedNameAsString();
                value = paramValue == null ? null : ConverterUtil.convertToLong(paramValue);
                break;
            case DATE_TIMEOFFSET:
                propertyType = EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()
                        .getFullQualifiedNameAsString();
                value = ConverterUtil.convertToDateTime(paramValue);
                break;
            case GUID:
                propertyType = EdmPrimitiveTypeKind.Guid.getFullQualifiedName().getFullQualifiedNameAsString();
                value = UUID.fromString(paramValue);
                break;
            case STREAM:
                propertyType = EdmPrimitiveTypeKind.Stream.getFullQualifiedName().getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case GEOGRAPHY:
                propertyType = EdmPrimitiveTypeKind.Geography.getFullQualifiedName().getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case GEOGRAPHY_POINT:
                propertyType = EdmPrimitiveTypeKind.GeographyPoint.getFullQualifiedName()
                        .getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case GEOGRAPHY_LINE_STRING:
                propertyType = EdmPrimitiveTypeKind.GeographyLineString.getFullQualifiedName()
                        .getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case GEOGRAPHY_POLYGON:
                propertyType = EdmPrimitiveTypeKind.GeographyPolygon.getFullQualifiedName()
                        .getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case GEOGRAPHY_MULTIPOINT:
                propertyType = EdmPrimitiveTypeKind.GeographyMultiPoint.getFullQualifiedName()
                        .getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case GEOGRAPHY_MULTILINE_STRING:
                propertyType = EdmPrimitiveTypeKind.GeographyMultiLineString.getFullQualifiedName()
                        .getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case GEOGRAPHY_MULTIPOLYGON:
                propertyType = EdmPrimitiveTypeKind.GeographyMultiPolygon.getFullQualifiedName()
                        .getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case GEOGRAPHY_COLLECTION:
                propertyType = EdmPrimitiveTypeKind.GeographyCollection.getFullQualifiedName()
                        .getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case GEOMETRY:
                propertyType = EdmPrimitiveTypeKind.Geometry.getFullQualifiedName().getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case GEOMETRY_POINT:
                propertyType = EdmPrimitiveTypeKind.GeometryPoint.getFullQualifiedName().getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case GEOMETRY_LINE_STRING:
                propertyType = EdmPrimitiveTypeKind.GeometryLineString.getFullQualifiedName()
                        .getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case GEOMETRY_POLYGON:
                propertyType = EdmPrimitiveTypeKind.GeometryPolygon.getFullQualifiedName()
                        .getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case GEOMETRY_MULTIPOINT:
                propertyType = EdmPrimitiveTypeKind.GeometryMultiPoint.getFullQualifiedName()
                        .getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case GEOMETRY_MULTILINE_STRING:
                propertyType = EdmPrimitiveTypeKind.GeographyMultiLineString.getFullQualifiedName()
                        .getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case GEOMETRY_MULTIPOLYGON:
                propertyType = EdmPrimitiveTypeKind.GeometryMultiPolygon.getFullQualifiedName()
                        .getFullQualifiedNameAsString();
                value = paramValue;
                break;
            case GEOMETRY_COLLECTION:
                propertyType = EdmPrimitiveTypeKind.GeometryCollection.getFullQualifiedName()
                        .getFullQualifiedNameAsString();
                value = paramValue;
                break;
            default:
                propertyType = EdmPrimitiveTypeKind.String.getFullQualifiedName().getFullQualifiedNameAsString();
                value = paramValue;
                break;
        }
        return new Property(propertyType, name, ValueType.PRIMITIVE, value);
    }

    private byte[] getBytesFromBase64String(String base64Str) throws ODataServiceFault {
        try {
            return Base64.decodeBase64(base64Str.getBytes(DBConstants.DEFAULT_CHAR_SET_TYPE));
        } catch (Exception e) {
            throw new ODataServiceFault(e.getMessage());
        }
    }

    @Override
    public VisitorOperand visitAlias(final String aliasName)
            throws ExpressionVisitException, ODataApplicationException {
        return throwNotImplemented();
    }

    @Override
    public VisitorOperand visitTypeLiteral(final EdmType type)
            throws ExpressionVisitException, ODataApplicationException {
        return throwNotImplemented();
    }

    @Override
    public VisitorOperand visitLambdaReference(final String variableName)
            throws ExpressionVisitException, ODataApplicationException {
        return throwNotImplemented();
    }

    @Override
    public VisitorOperand visitEnum(final EdmEnumType type, final List<String> enumValues)
            throws ExpressionVisitException, ODataApplicationException {
        return throwNotImplemented();
    }

    private VisitorOperand throwNotImplemented() throws ODataApplicationException {
        throw new ODataApplicationException("Not implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
                                            Locale.ROOT);
    }
}
