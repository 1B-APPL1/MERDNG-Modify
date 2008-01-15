/**
 * Mogwai ERDesigner. Copyright (C) 2002 The Mogwai Project.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package de.erdesignerng.dialect;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import de.erdesignerng.ERDesignerBundle;
import de.erdesignerng.exception.ReverseEngineeringException;
import de.erdesignerng.model.Attribute;
import de.erdesignerng.model.CascadeType;
import de.erdesignerng.model.DefaultValue;
import de.erdesignerng.model.Domain;
import de.erdesignerng.model.Index;
import de.erdesignerng.model.IndexType;
import de.erdesignerng.model.Model;
import de.erdesignerng.model.ModelItem;
import de.erdesignerng.model.Relation;
import de.erdesignerng.model.Table;

/**
 * @author $Author: mirkosertic $
 * @version $Date: 2008-01-15 19:22:42 $
 */
public abstract class JDBCReverseEngineeringStrategy {

    protected Dialect dialect;

    protected JDBCReverseEngineeringStrategy(Dialect aDialect) {
        dialect = aDialect;
    }

    protected Domain createDomainFor(Model aModel, String aColumnName, String aTypeName, String aSize,
            String aDecimalDigits, ReverseEngineeringOptions aOptions) {

        StringBuffer theTypeDefinition = new StringBuffer(aTypeName);
        if (((aSize != null)) && (aTypeName.indexOf(")") < 0)) {
            theTypeDefinition.append("(");
            theTypeDefinition.append(aSize);
            if (aDecimalDigits != null) {
                theTypeDefinition.append(",");
                theTypeDefinition.append(aDecimalDigits);
            }
            theTypeDefinition.append(")");
        }

        String theDataType = theTypeDefinition.toString();
        Domain theDomain = aModel.getDomains().findByDataType(theDataType);
        if (theDomain != null) {

            if (theDomain.getName().equals(aColumnName)) {
                return theDomain;
            }

            for (int i = 0; i < 10000; i++) {
                String theName = aColumnName;
                if (i > 0) {
                    theName = theName + "_" + i;
                }

                theDomain = aModel.getDomains().findByName(theName);
                if (theDomain != null) {
                    if (theDomain.getName().equals(aColumnName)) {
                        return theDomain;
                    }
                }

                if (!aModel.getDomains().elementExists(theName, dialect.isCaseSensitive())) {

                    theDomain = new Domain();
                    theDomain.setName(theName);
                    theDomain.setDatatype(theDataType);

                    aModel.getDomains().add(theDomain);

                    return theDomain;
                }
            }

        } else {
            theDomain = new Domain();
            theDomain.setName(aColumnName);
            theDomain.setDatatype(theDataType);

            aModel.getDomains().add(theDomain);
        }

        return theDomain;
    }

    /**
     * Reverse engineer an existing table.
     * 
     * @param aModel
     * @param aCatalogName
     * @param aSchemaName
     * @param aTableName
     * @param aConnection
     * @throws SQLException
     * @throws ReverseEngineeringException
     */
    protected void reverseEngineerTable(Model aModel, ReverseEngineeringOptions aOptions,
            ReverseEngineeringNotifier aNotifier, SchemaEntry aEntry, String aTableName, Connection aConnection)
            throws SQLException, ReverseEngineeringException {

        aNotifier.notifyMessage(ERDesignerBundle.ENGINEERINGTABLE, aTableName);

        String theSchemaName = null;
        String theCatalogName = null;
        if (aEntry != null) {
            theSchemaName = aEntry.getSchemaName();
            theCatalogName = aEntry.getCatalogName();
        }

        DatabaseMetaData theMetaData = aConnection.getMetaData();

        ResultSet theTablesResultSet = theMetaData.getTables(theCatalogName, theSchemaName, aTableName,
                new String[] { "TABLE" });
        while (theTablesResultSet.next()) {

            String theTableRemarks = theTablesResultSet.getString("REMARKS");

            Table theTable = new Table();
            theTable.setName(dialect.getCastType().cast(aTableName));

            if ((theTableRemarks != null) && (!"".equals(theTableRemarks))) {
                theTable.getProperties().setProperty(ModelItem.PROPERTY_REMARKS, theTableRemarks);
            }

            // Reverse engineer attributes
            ResultSet theColumnsResultSet = theMetaData.getColumns(theCatalogName, theSchemaName, aTableName, null);
            while (theColumnsResultSet.next()) {

                String theColumnName = theColumnsResultSet.getString("COLUMN_NAME");
                String theTypeName = theColumnsResultSet.getString("TYPE_NAME");
                String theSize = theColumnsResultSet.getString("COLUMN_SIZE");
                String theDecimalDigits = theColumnsResultSet.getString("DECIMAL_DIGITS");
                String theNullable = theColumnsResultSet.getString("NULLABLE");
                String theDefaultValue = theColumnsResultSet.getString("COLUMN_DEF");
                String theColumnRemarks = theColumnsResultSet.getString("REMARKS");

                Attribute theAttribute = new Attribute();
                theAttribute.setName(dialect.getCastType().cast(theColumnName));
                if ((theColumnRemarks != null) && (!"".equals(theColumnRemarks))) {
                    theAttribute.getProperties().setProperty(ModelItem.PROPERTY_REMARKS, theColumnRemarks);
                }

                Domain theDomain = createDomainFor(aModel, theColumnName, theTypeName, theSize, theDecimalDigits,
                        aOptions);

                DefaultValue theDefault = createDefaultValueFor(aModel, theColumnName, theDefaultValue);

                theAttribute.setDefinition(theDomain, "1".equals(theNullable), theDefault);

                try {
                    theTable.addAttribute(aModel, theAttribute);
                } catch (Exception e) {
                    throw new ReverseEngineeringException(e.getMessage());
                }
            }
            theColumnsResultSet.close();

            // Reverse engineer primary keys
            reverseEngineerPrimaryKey(aModel, aTableName, theSchemaName, theCatalogName, theMetaData, theTable);

            // Reverse engineer indexes
            reverseEngineerIndexes(aModel, aTableName, theSchemaName, theCatalogName, theMetaData, theTable);

            // We are done here
            try {
                aModel.addTable(theTable);
            } catch (Exception e) {
                throw new ReverseEngineeringException(e.getMessage());
            }

        }
        theTablesResultSet.close();
    }

    protected void reverseEngineerPrimaryKey(Model aModel, String aTableName, String aSchemaName, String aCatalogName,
            DatabaseMetaData aMetaData, Table aTable) throws SQLException, ReverseEngineeringException {

        ResultSet thePrimaryKeyResultSet = aMetaData.getPrimaryKeys(aCatalogName, aSchemaName, aTableName);
        Index thePrimaryKeyIndex = null;
        while (thePrimaryKeyResultSet.next()) {

            String thePKName = thePrimaryKeyResultSet.getString("PK_NAME");
            String theColumnName = thePrimaryKeyResultSet.getString("COLUMN_NAME");

            if (thePrimaryKeyIndex == null) {
                thePrimaryKeyIndex = new Index();
                thePrimaryKeyIndex.setIndexType(IndexType.PRIMARYKEY);
                thePrimaryKeyIndex.setName(thePKName);

                try {
                    aTable.addIndex(aModel, thePrimaryKeyIndex);
                } catch (Exception e) {
                    throw new ReverseEngineeringException(e.getMessage());
                }
            }

            Attribute theIndexAttribute = aTable.getAttributes().findByName(dialect.getCastType().cast(theColumnName));
            if (theIndexAttribute == null) {
                throw new ReverseEngineeringException("Cannot find attribute " + theColumnName + " in table "
                        + aTable.getName());
            }

            theIndexAttribute.setPrimaryKey(true);

        }
        thePrimaryKeyResultSet.close();
    }

    protected void reverseEngineerIndexes(Model aModel, String aTableName, String aSchemaName, String aCatalogName,
            DatabaseMetaData aMetaData, Table aTable) throws SQLException, ReverseEngineeringException {

        ResultSet theIndexResults = aMetaData.getIndexInfo(aCatalogName, aSchemaName, aTableName, false, true);
        Index theIndex = null;
        while (theIndexResults.next()) {

            String theIndexName = theIndexResults.getString("INDEX_NAME");
            if ((theIndexName != null)
                    && ((theIndex == null) || (!theIndex.getName().equals(theIndexName)))) {

                if (aTable.getIndexes().findByName(theIndexName) == null) {
                    theIndex = new Index();
                    theIndex.setName(theIndexName);
                    
                    boolean isNonUnique = theIndexResults.getBoolean("NON_UNIQUE");
                    if (isNonUnique) {
                        theIndex.setIndexType(IndexType.UNIQUE);
                    } else {
                        theIndex.setIndexType(IndexType.NONUNIQUE);
                    }

                    try {
                        aTable.addIndex(aModel, theIndex);
                    } catch (Exception e) {
                        throw new ReverseEngineeringException("Cannot add index " + theIndexName + " in table "
                                + aTable.getName() + " : " + e.getMessage());
                    }
                } else {
                    theIndex = null;
                }
            }

            if (theIndex != null) {
                String theColumnName = theIndexResults.getString("COLUMN_NAME");
                Attribute theIndexAttribute = aTable.getAttributes().findByName(
                        dialect.getCastType().cast(theColumnName));
                if (theIndexAttribute == null) {
                    throw new ReverseEngineeringException("Cannot find attribute " + theColumnName + " in table "
                            + aTable.getName());
                }
                theIndex.getAttributes().add(theIndexAttribute);
            }

        }
        theIndexResults.close();
    }

    protected DefaultValue createDefaultValueFor(Model aModel, String aColumnName, String aDefaultValue) {
        return null;
    }

    /**
     * Reverse engineer relations.
     * 
     * @param aModel
     * @param aSchemaName
     * @param aConnection
     * @throws SQLException
     * @throws ReverseEngineeringException
     */
    protected void reverseEngineerRelations(Model aModel, ReverseEngineeringOptions aOptions,
            ReverseEngineeringNotifier aNotifier, SchemaEntry aEntry, Connection aConnection) throws SQLException,
            ReverseEngineeringException {

        DatabaseMetaData theMetaData = aConnection.getMetaData();

        String theSchemaName = null;
        String theCatalogName = null;
        if (aEntry != null) {
            theSchemaName = aEntry.getSchemaName();
            theCatalogName = aEntry.getCatalogName();
        }

        for (Table theTable : aModel.getTables()) {

            aNotifier.notifyMessage(ERDesignerBundle.ENGINEERINGRELATION, theTable.getName());

            // Foreign keys
            Relation theRelation = null;
            ResultSet theForeignKeys = theMetaData.getImportedKeys(theCatalogName, theSchemaName, theTable.getName());
            while (theForeignKeys.next()) {
                String theFKName = theForeignKeys.getString("FK_NAME");
                if ((theRelation == null) || (!theFKName.equals(theRelation.getName()))) {

                    String thePKTableName = theForeignKeys.getString("PKTABLE_NAME");
                    String theUpdateRule = theForeignKeys.getString("UPDATE_RULE");
                    String theDeleteRule = theForeignKeys.getString("DELETE_RULE");

                    Table theExportingTable = aModel.getTables().findByName(dialect.getCastType().cast(thePKTableName));
                    if (theExportingTable == null) {
                        throw new ReverseEngineeringException("Cannot find table " + thePKTableName + " in model");
                    }

                    theRelation = new Relation();
                    theRelation.setName(dialect.getCastType().cast(theFKName));
                    theRelation.setExportingTable(theExportingTable);
                    theRelation.setImportingTable(theTable);

                    if (theUpdateRule != null) {
                        int theType = Integer.parseInt(theUpdateRule.toString());
                        switch (theType) {
                        case DatabaseMetaData.importedKeyNoAction: {
                            theRelation.setOnUpdate(CascadeType.NOTHING);
                            break;
                        }
                        case DatabaseMetaData.importedKeySetNull: {
                            theRelation.setOnUpdate(CascadeType.SET_NULL);
                            break;
                        }
                        case DatabaseMetaData.importedKeyCascade: {
                            theRelation.setOnUpdate(CascadeType.CASCADE);
                            break;
                        }
                        default: {
                            theRelation.setOnUpdate(CascadeType.CASCADE);
                        }
                        }
                    } else {
                        theRelation.setOnUpdate(CascadeType.NOTHING);
                    }

                    if (theDeleteRule != null) {
                        int theType = Integer.parseInt(theDeleteRule.toString());
                        switch (theType) {
                        case DatabaseMetaData.importedKeyNoAction: {
                            theRelation.setOnDelete(CascadeType.NOTHING);
                            break;
                        }
                        case DatabaseMetaData.importedKeySetNull: {
                            theRelation.setOnDelete(CascadeType.SET_NULL);
                            break;
                        }
                        case DatabaseMetaData.importedKeyCascade: {
                            theRelation.setOnDelete(CascadeType.CASCADE);
                            break;
                        }
                        default: {
                            theRelation.setOnDelete(CascadeType.CASCADE);
                        }
                        }
                    } else {
                        theRelation.setOnDelete(CascadeType.NOTHING);
                    }

                    try {
                        aModel.addRelation(theRelation);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new ReverseEngineeringException(e.getMessage());
                    }
                }

                String thePKColumnName = theForeignKeys.getString("PKCOLUMN_NAME");
                String theFKColumnName = theForeignKeys.getString("FKCOLUMN_NAME");

                Attribute theExportingAttribute = theRelation.getExportingTable().getAttributes().findByName(
                        dialect.getCastType().cast(thePKColumnName));
                if (theExportingAttribute == null) {
                    throw new ReverseEngineeringException("Cannot find column " + thePKColumnName + " in table "
                            + theRelation.getExportingTable().getName());
                }

                Attribute theImportingAttribute = theRelation.getImportingTable().getAttributes().findByName(
                        dialect.getCastType().cast(theFKColumnName));
                if (theImportingAttribute == null) {
                    throw new ReverseEngineeringException("Cannot find column " + theFKColumnName + " in table "
                            + theRelation.getImportingTable().getName());
                }

                theRelation.getMapping().put(theExportingAttribute, theImportingAttribute);
            }
            theForeignKeys.close();
        }
    }

    protected String[] getReverseEngineeringTableTypes() {
        return new String[] { "TABLE" };
    }

    /**
     * Reverse engineer the existing tables in a schema.
     * 
     * @param aModel
     * @param aOptions
     * @param aNotifier
     * @param aEntry
     * @param aConnection
     * @throws SQLException
     * @throws ReverseEngineeringException
     */
    protected void reverseEnginnerTables(Model aModel, ReverseEngineeringOptions aOptions,
            ReverseEngineeringNotifier aNotifier, SchemaEntry aEntry, Connection aConnection) throws SQLException,
            ReverseEngineeringException {

        aNotifier.notifyMessage(ERDesignerBundle.ENGINEERINGSCHEMA, aOptions.toString());

        DatabaseMetaData theMetaData = aConnection.getMetaData();

        // Reverse engineer tables
        ResultSet theTablesResultSet = null;
        if (aEntry != null) {
            theTablesResultSet = theMetaData.getTables(aEntry.getCatalogName(), aEntry.getSchemaName(), null,
                    getReverseEngineeringTableTypes());
        } else {
            theTablesResultSet = theMetaData.getTables(null, null, null, getReverseEngineeringTableTypes());
        }
        while (theTablesResultSet.next()) {

            String theTableType = theTablesResultSet.getString("TABLE_TYPE");
            String theTableName = theTablesResultSet.getString("TABLE_NAME");

            // Make sure that tables are not reverse engineered twice!
            if (!aModel.getTables().elementExists(theTableName, dialect.isCaseSensitive())) {
                reverseEngineerTable(aModel, aOptions, aNotifier, aEntry, theTableName, aConnection);
            }
        }
        theTablesResultSet.close();

        // Reverse engineer also relations
        reverseEngineerRelations(aModel, aOptions, aNotifier, aEntry, aConnection);
    }

    public Model createModelFromConnection(Connection aConnection, ReverseEngineeringOptions aOptions,
            ReverseEngineeringNotifier aNotifier) throws SQLException, ReverseEngineeringException {

        Model theNewModel = new Model();
        theNewModel.setDialect(dialect);

        if (dialect.supportsSchemaInformation()) {
            for (SchemaEntry theEntry : aOptions.getSchemaEntries()) {
                reverseEnginnerTables(theNewModel, aOptions, aNotifier, theEntry, aConnection);
            }
        } else {
            reverseEnginnerTables(theNewModel, aOptions, aNotifier, null, aConnection);
        }

        aNotifier.notifyMessage(ERDesignerBundle.ENGINEERINGFINISHED, "");

        return theNewModel;
    }

    public List<SchemaEntry> getSchemaEntries(Connection aConnection) throws SQLException {

        List<SchemaEntry> theList = new ArrayList<SchemaEntry>();

        DatabaseMetaData theMetadata = aConnection.getMetaData();
        ResultSet theResult = theMetadata.getSchemas();

        while (theResult.next()) {
            String theSchemaName = theResult.getString("TABLE_SCHEM");
            String theCatalogName = theResult.getString("TABLE_CATALOG");

            theList.add(new SchemaEntry(theCatalogName, theSchemaName));
        }

        return theList;
    }
}
