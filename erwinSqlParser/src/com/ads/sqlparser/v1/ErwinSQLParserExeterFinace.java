/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ads.sqlparser.v1;

import com.ads.api.beans.mm.Mapping;
import com.ads.api.beans.mm.MappingSpecificationRow;
import com.ads.edf.overview.updateforsource.v4.UpdateSpecsForSourceAndTarget_91EDF;
import com.sqlToJson.BindingPojo.Column;
import com.sqlToJson.BindingPojo.Dlineage;
import demos.*;
import demos.dlineage.DataFlowAnalyzer;
import demos.dlineage.DataFlowAnalyzerV1;
import demos.visitors.toXml;
import demos.visitors.xmlVisitor;
import gudusoft.gsqlparser.EDbVendor;
import gudusoft.gsqlparser.EExpressionType;
import gudusoft.gsqlparser.EJoinType;
import gudusoft.gsqlparser.TCustomSqlStatement;
import gudusoft.gsqlparser.TGSqlParser;
import gudusoft.gsqlparser.nodes.TExpression;
import gudusoft.gsqlparser.nodes.TJoin;
import gudusoft.gsqlparser.nodes.TJoinItem;
import gudusoft.gsqlparser.nodes.TResultColumn;
import gudusoft.gsqlparser.nodes.TResultColumnList;
import gudusoft.gsqlparser.nodes.TTable;
import gudusoft.gsqlparser.nodes.TTableList;
import gudusoft.gsqlparser.nodes.TWhereClause;
import gudusoft.gsqlparser.stmt.TCreateViewSqlStatement;
import gudusoft.gsqlparser.stmt.TDeleteSqlStatement;
import gudusoft.gsqlparser.stmt.TInsertSqlStatement;
import gudusoft.gsqlparser.stmt.TSelectSqlStatement;
import gudusoft.gsqlparser.stmt.TTruncateStatement;
import gudusoft.gsqlparser.stmt.TUpdateSqlStatement;
import gudusoft.gsqlparser.stmt.mssql.TMssqlCreateProcedure;
import gudusoft.gsqlparser.stmt.teradata.TTeradataLock;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Sy
 *
 * @author shashikanta
 */
public class ErwinSQLParserExeterFinace {

    public static Map<String, String> sourceTargetMap = new LinkedHashMap<>();
    public static Map<String, LinkedHashMap<String, String>> sourcemap = new LinkedHashMap<String, LinkedHashMap<String, String>>();
    public static Map<String, LinkedHashMap<String, String>> target = new LinkedHashMap();
    public static LinkedHashMap<String, String> sorceinformation = new LinkedHashMap();
    public static LinkedHashMap<String, String> targetinformation = new LinkedHashMap();
    public static LinkedHashMap<String, String> sourcetarget = new LinkedHashMap();
    public static LinkedHashMap<String, String> tableAliasMap = new LinkedHashMap();
    public static List<String> filequerycontent = new LinkedList<>();
    public static ArrayList<MappingSpecificationRow> mapspeclist = new ArrayList<>();
    public static String sourceSystemNamegl = "";
    public static String sourceEnvironmentNamegl = "";
    public static String targetSystemNamegl = "";
    public static String targetEnvironmentNamegl = "";
    public static Map<String, List<String>> tableColRel = new LinkedHashMap();
    public static EDbVendor dbVendor = null;
    public static TGSqlParser sqlparser = null;
    public static String updateTargetTableName = "";
    public static String deleteTargetTableName = "";
    public static boolean unionDelete;
    public static String unionTargetTableName = "";
    public static HashMap<String, ArrayList<String>> specTableColumnDetails = new HashMap<String, ArrayList<String>>();
    public static HashMap<String, String> sourceTableColumndeatilsWithBusinessRule = new HashMap<String, String>();
    public static Set<String> joinconditions = new HashSet<>();
    public static Map<String, String> keyvaluepair = new LinkedHashMap<>();
    public static Set<String> whereCondition = new HashSet<>();
    public static Map<String, String> ExpressionTarget = new LinkedHashMap();
    public static String dbName = "";

    public String sqlToDataflow(String sqlfile, String sourceSystemName, String sourceEnvironmentName, String targetSystemName, String targetEnvironmentName, String dbvendor, String egtfilepath, String orphantarget, String mapname) {

        sourceSystemNamegl = sourceSystemName;
        sourceEnvironmentNamegl = sourceEnvironmentName;
        targetSystemNamegl = targetSystemName;
        targetEnvironmentNamegl = targetEnvironmentName;
        String json = "";
        try {
            //remove js
            json = getJsonfromSql(sqlfile, dbvendor, egtfilepath, orphantarget, mapname);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public static String getJsonfromSql(String sqlfile, String dbvendor, String egtfilepath, String orphantarget, String mapname) {
        String json = "";

        try {

            ObjectMapper objectMapper = new ObjectMapper();
            List<String> querylist = null;
            Set<String> keyvaluepair = null;
            String lastSubselectQuery = "";
            //  toXml.getXml(sqlfile, dbvendor);
         //   System.out.println("=== SQL File ======= > " + sqlfile);

            toXml.getXml2(sqlfile);
            dbvendor = toXml.dbName;
           // System.out.println(dbvendor);
            File filessql = new File(sqlfile);

            String sqlfileContent = FileUtils.readFileToString(filessql);

            if (sqlfileContent.contains("~")) {
                sqlfileContent = sqlfileContent.replace("~", "tild");
            }
            if (sqlfileContent.contains("'~'")) {
                sqlfileContent = sqlfileContent.replace("'~'", "tild");
            }

            querylist = new LinkedList<>(xmlVisitor.subselectquery);
            Map<String, String> childParentMap = new LinkedHashMap<>(xmlVisitor.childParentQuery);
            getgroupbyclause(querylist, dbvendor);
            querylist.add(sqlfileContent);
            getquerytype(sqlfileContent, dbvendor, querylist, egtfilepath, orphantarget);
//            makelooplineage(sourceTargetMap);
            if (querylist.size() >= 2) {
                lastSubselectQuery = querylist.get(0);
            }
            if (childParentMap.size() == 0) {
                keyvaluepair = getJoinStatementsfromList(sqlfileContent, dbvendor);
                createkeyvalueMap(keyvaluepair);
            } else {
                keyvaluepair = getJoinStatementsfromList(childParentMap, dbvendor);
                createkeyvalueMap(keyvaluepair);

            }

            getExpression(querylist, tableAliasMap, dbvendor);

            ExpressionTarget.clear();
            childParentMap.clear();
            removeNullfromspec(mapspeclist);
            if (mapspeclist.size() != 0) {
                List<Mapping> mappings = createmapfromrow(mapspeclist, mapname, sqlfileContent, querylist);
                //objectMapper.writeValue(new File(jsonpath + "\\" + filessql.getName() + ".json"), mappings);
                json = objectMapper.writeValueAsString(mappings);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        clearStaticvariable();
        return json;
    }

    public static ArrayList<MappingSpecificationRow> getSpecification(String sqlfile, String dbvendor, String egtfilepath, String orphantarget, String mapname) {
        String json = "";

        try {

            ObjectMapper objectMapper = new ObjectMapper();
            List<String> querylist = null;
            Set<String> keyvaluepair = null;
            String lastSubselectQuery = "";
            //  toXml.getXml(sqlfile, dbvendor);
           // System.out.println("=== SQL File ======= > " + sqlfile);

            toXml.getXml2(sqlfile);
            dbvendor = toXml.dbName;
          //  System.out.println(dbvendor);
            File filessql = new File(sqlfile);

            String sqlfileContent = FileUtils.readFileToString(filessql);
            if (sqlfileContent.contains("~")) {
                sqlfileContent = sqlfileContent.replace("~", "tild");
            }
            if (sqlfileContent.contains("'~'")) {
                sqlfileContent = sqlfileContent.replace("'~'", "tild");
            }

            querylist = new LinkedList<>(xmlVisitor.subselectquery);
            Map<String, String> childParentMap = new LinkedHashMap<>(xmlVisitor.childParentQuery);
            getgroupbyclause(querylist, dbvendor);
            querylist.add(sqlfileContent);
            getquerytype(sqlfileContent, dbvendor, querylist, egtfilepath, orphantarget);
//            makelooplineage(sourceTargetMap);
            if (querylist.size() >= 2) {
                lastSubselectQuery = querylist.get(0);
            }
            if (childParentMap.size() == 0) {
                keyvaluepair = getJoinStatementsfromList(sqlfileContent, dbvendor);
                createkeyvalueMap(keyvaluepair);
            } else {
                keyvaluepair = getJoinStatementsfromList(childParentMap, dbvendor);
                createkeyvalueMap(keyvaluepair);

            }

            getExpression(querylist, tableAliasMap, dbvendor);

            ExpressionTarget.clear();
            childParentMap.clear();
            removeNullfromspec(mapspeclist);

            List<Mapping> mappings = createmapfromrow(mapspeclist, mapname, sqlfileContent, querylist);

            //objectMapper.writeValue(new File(jsonpath + "\\" + filessql.getName() + ".json"), mappings);
            json = objectMapper.writeValueAsString(mappings);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return mapspeclist;
//        return json;
    }

    public void getClear() {
        clearStaticvariable();

    }

    public static void getJavaObjectfromxmls(String xmlfile) {
        try {
            InputStream in = IOUtils.toInputStream(xmlfile);
            JAXBContext jaxbContext
                    = JAXBContext.newInstance("com.sqlToJson.BindingPojo");
            Unmarshaller unmarshaller
                    = jaxbContext.createUnmarshaller();
            Dlineage dj = (Dlineage) unmarshaller.unmarshal(in);
            int i = 0;
            for (Object s : dj.getColumnOrTableOrResultset()) {
                LinkedHashMap<String, String> maps = new LinkedHashMap<>();
                List<String> columnsNameList = new LinkedList();
                if (s instanceof Dlineage.Relation) {
                    String sourcetableName = "";
                    String targetTableName = "";
                    String sourceColumnName = "";
                    String targetColumnName = "";
                    String sourceinfomore = "";
//                    if (((Dlineage.Relation) s).getType().equals("dataflow") || ((Dlineage.Relation) s).getType().equals("dataflow_recordset")) {
                    if (((Dlineage.Relation) s).getType().equals("dataflow")) {
                        // System.out.println("----"+((Relation) s).getSource());
                        for (Dlineage.Relation.Source so : ((Dlineage.Relation) s).getSource()) {
                            if (((Dlineage.Relation) s).getSource().size() > 1) {
                                //System.out.println("Column names :------" + so.getColumn().trim());
                                if (so.getColumn().trim().toLowerCase().contains("case")) {
                                    continue;
                                }
                                if (so.getColumn() == null) {
                                    continue;
                                }
                                String sourceColumn = so.getColumn();
                                if (sourceColumn == null) {
                                    continue;
                                }
                                if ("".equals(sourceinfomore)) {

                                    sourceinfomore = so.getParentName() + "$" + sourceColumn;
                                } else {
                                    sourceinfomore = sourceinfomore + "~" + so.getParentName() + "$" + sourceColumn;
                                }
//                                System.out.println("source-----" + sourceinfomore);
                            } else {
                                sourcetableName = so.getParentName();
                                sourceColumnName = so.getColumn();
//                                System.out.println("sourc--tableName---" + sourcetableName + "source---columnName----" + sourceColumnName);
                            }
                            if (sourcetableName == null || sourcetableName.equals("null") || "".equals(sourcetableName)) {
                                sourcetableName = so.getSourceName();
                                if (sourcetableName == null) {
                                    sourcetableName = so.getParentName();
                                }

                            }
                        }
                        for (Dlineage.Relation.Target target : ((Dlineage.Relation) s).getTarget()) {

                            targetTableName = target.getParentName();

                            targetColumnName = target.getColumn();
                            if (targetColumnName == null) {
                                continue;
                            }
                        }
                        if (sourceColumnName != null && sourceColumnName.contains(":")) {
                            sourceColumnName = sourceColumnName.replace(":", "");

                        }
                        if (sourcetableName.contains("$")) {
                            sourcetableName = sourcetableName.replace("$", "");
                        }
                        if (sourceColumnName.contains("$")) {
                            sourceColumnName = sourceColumnName.replace("$", "");
                        }
                        if (targetTableName.contains("$")) {
                            targetTableName = targetTableName.replace("$", "");
                        }
                        if (targetColumnName.contains("$")) {
                            targetColumnName = targetColumnName.replace("$", "");
                        }

                        String source = sourcetableName + "$" + sourceColumnName;
                        String target = targetTableName + "$" + targetColumnName;

                        if (!"".equals(sourceinfomore)) {
                            maps.put(sourceinfomore, target);
                        } else {
                            maps.put(source, target);
                        }

                        sourceTargetMap.put(sourcetableName, targetTableName);

                        if (sourceinfomore != null && source != null) {
                            getAllmapp(maps);
                        }

                    }

                    i++;
                }
                if (s instanceof Dlineage.Table) {

                    String tableName = ((Dlineage.Table) s).getName();
                    List<Column> columnList = ((Dlineage.Table) s).getColumn();
                    String tablealias = ((Dlineage.Table) s).getAlias();
                    for (Column column : columnList) {
                        columnsNameList.add(column.getName());
                    }
                    tableColRel.put(tableName, columnsNameList);
                    tableAliasMap.put(tablealias, tableName);

                }
                if (s instanceof Dlineage.Resultset) {
                    String rsultsetTableName = ((Dlineage.Resultset) s).getName();
                    List<Column> resultcolumnList = ((Dlineage.Resultset) s).getColumn();
                    List<String> resultcolumnsNameList = new LinkedList();
                    for (Column column : resultcolumnList) {
                        resultcolumnsNameList.add(column.getName());
                    }
                    tableColRel.put(rsultsetTableName, resultcolumnsNameList);
                }
            }
            int tesr = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void getAllmapp(LinkedHashMap<String, String> sourcetarget) {
        try {
            MappingSpecificationRow mapspec = new MappingSpecificationRow();
            for (Map.Entry<String, String> entry : sourcetarget.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key.contains("~")) {
                    String[] arrayforUnion = key.split("~");

                    for (String unionsource : arrayforUnion) {
                        MappingSpecificationRow mapspec1 = new MappingSpecificationRow();
                        mapspec1.setSourceSystemEnvironmentName(sourceEnvironmentNamegl);
                        mapspec1.setTargetSystemEnvironmentName(targetEnvironmentNamegl);
                        mapspec1.setSourceSystemName(sourceSystemNamegl);
                        mapspec1.setTargetSystemName(targetSystemNamegl);
                        mapspec1.setSourceTableName(unionsource.split("\\$")[0]);
//                    if (unionsource.split("\\$")[1].equals("null")) {
//                        mapspec1.setSourceColumnName(" ");
////                        continue;
//                    } else {
//                        mapspec1.setSourceColumnName(unionsource.split("\\$")[1]);
//                    }
                        mapspec1.setSourceColumnName(unionsource.split("\\$")[1]);
                        mapspec1.setSourceColumnIdentityFlag(true);
                        mapspec1.setTargetTableName(value.split("\\$")[0]);
                        //  System.out.println("-----------" + value);
                        if (value.split("\\$").length > 1) {
                            mapspec1.setTargetColumnName(value.split("\\$")[1]);
                        }

                        mapspeclist.add(mapspec1);
                    }
                } else {
                    mapspec.setSourceSystemEnvironmentName(sourceEnvironmentNamegl);
                    mapspec.setTargetSystemEnvironmentName(targetEnvironmentNamegl);
                    mapspec.setSourceSystemName(sourceSystemNamegl);
                    mapspec.setTargetSystemName(targetSystemNamegl);

//                if (key.split("\\$")[1].equals("null")) {
//                    mapspec.setSourceColumnName(" ");
////                    continue;
//                } else {
//                    mapspec.setSourceColumnName(key.split("\\$")[1]);
//                }
                    if (key.split("\\$").length > 1) {
                        mapspec.setSourceColumnName(key.split("\\$")[1]);
                        mapspec.setSourceTableName(key.split("\\$")[0]);
                    }

                    mapspec.setSourceColumnIdentityFlag(true);
                    if (value.split("\\$").length > 1) {
                        mapspec.setTargetTableName(value.split("\\$")[0]);
                        mapspec.setTargetColumnName(value.split("\\$")[1]);
                    }

                    mapspeclist.add(mapspec);

                }

            }

        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    public static void clearStaticvariable() {
        sourcemap.clear();
        target.clear();
        sorceinformation.clear();
        targetinformation.clear();
        sourcetarget.clear();
        filequerycontent.clear();
        mapspeclist.clear();
        sourceSystemNamegl = "";
        xmlVisitor.subselectquery.clear();
        DataFlowAnalyzer.xmlfiles.clear();
        xmlVisitor.childParentQuery.clear();
        tableColRel.clear();
        tableAliasMap.clear();
        updateTargetTableName = "";
        deleteTargetTableName = "";
        unionTargetTableName = "";
        specTableColumnDetails.clear();
        sourceTableColumndeatilsWithBusinessRule.clear();
        joinconditions.clear();
        whereCondition.clear();
        keyvaluepair.clear();
        unionDelete = false;

    }

    public static List<Mapping> createmapfromrow(ArrayList<MappingSpecificationRow> specrowlist, String mapfileName, String query, List<String> subselectquery) {
        List<Mapping> mappinglist = new LinkedList();
        Mapping m = new Mapping();
        List<String> subselectQuery = new LinkedList<>(subselectquery);
        String subSelectQuery = org.apache.commons.lang3.StringUtils.join(subselectQuery, "---------------------\n");
        m.setSourceExtractDescription(subSelectQuery);
        m.setMappingName(mapfileName);
        UpdateSpecsForSourceAndTarget_91EDF updateSpecsForSourceAndTarget_91 = new UpdateSpecsForSourceAndTarget_91EDF();
        ArrayList<MappingSpecificationRow> updatedoverviewrow = updateSpecsForSourceAndTarget_91.updateSpecList(specrowlist);
//        OverviewMapping overViewSpecObj = new OverviewMapping();
//        ArrayList<MappingSpecificationRow> updatedoverviewrow = overViewSpecObj.generateOverViewMappings(specrowlist);

        ArrayList<MappingSpecificationRow> updatedspecs = getspecificationdetails(updatedoverviewrow);
       // System.out.println("size of overview " + updatedspecs.size());
        m.setMappingSpecifications(updatedspecs);
        m.setSourceExtractQuery(query);

        mappinglist.add(m);
        return mappinglist;
    }

    public static List<String> cahngelatindexoflist(List<String> querylist) {
        List<String> appndedlist = new LinkedList<>();
        String Query = querylist.get(querylist.size() - 1);
        StringBuffer sb1 = new StringBuffer();
        int i = 0;
        Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(Query);
        while (m.find()) {
            if (i == 0) {
                sb1.append(m.group(1));
            }
            i++;
        }
        String splittingquery = Query.split("\\)")[1].trim().split(" ")[1];
        if (splittingquery.contains("*")) {
            if (splittingquery.contains(".")) {
                String replacedString = Query.replace(splittingquery, sb1.toString());
                querylist.set(querylist.size() - 1, replacedString);
            } else {
                String replacedString = Query.replaceFirst("\\" + Query.split("\\)")[1].split(" ")[1], sb1.toString());
                querylist.set(querylist.size() - 1, replacedString);
            }

        }
        appndedlist.addAll(querylist);
        return appndedlist;
    }

    public static List<String> getAppendedstatement(List<String> queryList) {
        String querycontent = queryList.get(queryList.size() - 1);
        StringBuffer sb = new StringBuffer();
        String[] filessqlstr = querycontent.split("\n");
        int i = 0;
        for (String sqlf : filessqlstr) {
            if (i == 0) {
                String sqlf2 = sqlf.replace((sqlf.substring(sqlf.lastIndexOf(" "), sqlf.length() - 1)), (sqlf.substring(sqlf.lastIndexOf(" "), sqlf.length() - 1) + "_1"));
                sb.append(sqlf2).append("\n");
            } else {
                sb.append(sqlf).append("\n");
            }
            i++;
        }
        String sqlcontent = queryList.get(queryList.size() - 1);
        if (sqlcontent.startsWith("UPDATE")) {
            queryList.remove(queryList.size() - 1);
        } else {
            queryList.set(queryList.size() - 1, sb.toString());
        }
        return queryList;
    }

    public static Dlineage getDataflowObject(String sqlquery, String dbvendor) {
        Dlineage dataflow = null;
        try {
            String dataflowAnalyzerxml = DataFlowAnalyzerV1.getanalyzeXmlfromString(sqlquery, dbvendor);
            InputStream in = IOUtils.toInputStream(dataflowAnalyzerxml);
            JAXBContext jaxbContext
                    = JAXBContext.newInstance("com.sqlToJson.BindingPojo");
            Unmarshaller unmarshaller
                    = jaxbContext.createUnmarshaller();
            dataflow = (Dlineage) unmarshaller.unmarshal(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dataflow;
    }

    public static void getLineageforUpdate(String lastselectQuery, String UpdateQuery, String dbVendor) {
        Map<String, String> updatelineage = new LinkedHashMap<>();
        Map<String, String> selectLineage = new LinkedHashMap<>();
        String sourceDetails = "";
        String targetforUpdateset = "";
        String upDateset = "UPDATE-SET";
        Dlineage source = getDataflowObject(lastselectQuery, dbVendor);
        for (Object Dlineageobject : source.getColumnOrTableOrResultset()) {

            if (Dlineageobject instanceof Dlineage.Relation) {
                if (((Dlineage.Relation) Dlineageobject).getType().equals("dataflow")) {

                    for (Dlineage.Relation.Target subselectsource : ((Dlineage.Relation) Dlineageobject).getTarget()) {
                        if (subselectsource.getParentName().equals("RESULT_OF_SELECT-QUERY") || subselectsource.getParentName().equals("RESULT_OF_SELECT-QUERY-1") || subselectsource.getParentName().equals("RESULT_OF_SELECT-QUERY-2") || subselectsource.getParentName().equals("RESULT_OF_SELECT-QUERY-3")) {

                            String resultofselectquery = "RESULT_OF_SELECT-QUERY";
                            if (selectLineage.get(subselectsource.getParentName()) == null) {
                                selectLineage.put(subselectsource.getParentName(), subselectsource.getColumn());
                            } else {
                                String value = selectLineage.get(subselectsource.getParentName());
                                selectLineage.put(subselectsource.getParentName(), value + "#" + subselectsource.getColumn());

                            }
                            if ("".equals(sourceDetails)) {
                                sourceDetails = resultofselectquery + "#" + subselectsource.getColumn();
                                targetforUpdateset = upDateset + "#" + subselectsource.getColumn();
                            } else {

                                sourceDetails = sourceDetails + "$" + resultofselectquery + "#" + subselectsource.getColumn();
                                targetforUpdateset = targetforUpdateset + "$" + upDateset + "#" + subselectsource.getColumn();
                            }
                        }

                    }

                }
            }
            int g = 0;
        }
        Dlineage target = getDataflowObject(UpdateQuery, dbVendor);
        String updatesettarget = "";
        String updatetarget = "";
        String targetTableName = "";
        String targetside = "";
        for (Object Dlineageobject : target.getColumnOrTableOrResultset()) {
            if (Dlineageobject instanceof Dlineage.Relation) {
                if (((Dlineage.Relation) Dlineageobject).getType().equals("dataflow")) {
                    for (Dlineage.Relation.Source updatequery : ((Dlineage.Relation) Dlineageobject).getSource()) {
                        //  if (updatequery.getParentName().equals("UPDATE-SET")) {
                        if (updatequery.getParentName().contains("UPDATE-SET")) {
                            for (Dlineage.Relation.Target updatequerytgt : ((Dlineage.Relation) Dlineageobject).getTarget()) {
                                targetTableName = updatequerytgt.getParentName();
                                break;
                            }
                            String updateset = "UPDATE-SET";
                            if ("".equals(updatesettarget)) {
                                updatesettarget = updateset + "#" + updatequery.getColumn();

                            } else {
                                updatesettarget = updatesettarget + "$" + updateset + "#" + updatequery.getColumn();

                            }

                        } else if ("".equals(updatetarget)) {
                            updatetarget = updatequery.getParentName() + "#" + updatequery.getColumn();
                            updateTargetTableName = updatequery.getParentName();

                        } else {
                            updatetarget = updatetarget + "$" + updatequery.getParentName() + "#" + updatequery.getColumn();

                        }
                    }

                    for (Dlineage.Relation.Target updatequerytgt : ((Dlineage.Relation) Dlineageobject).getTarget()) {
                        if (updatequerytgt.getParentName().equals(targetTableName)) {
                            if ("".equals(targetside)) {
                                targetside = updatequerytgt.getParentName() + "#" + updatequerytgt.getColumn();
                            } else {
                                targetside = targetside + "$" + updatequerytgt.getParentName() + "#" + updatequerytgt.getColumn();
                            }
                        }

                    }
                }
            }

        }

        updatelineage.put(sourceDetails, targetforUpdateset);
        updatelineage.put(updatesettarget, targetside);
        addUpdatemapspecrowForUpdate(updatelineage);
    }

    public static void addUpdatemapspecrow(Map<String, String> updateLineageMap) {
        try {

            for (Map.Entry<String, String> entry : updateLineageMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                String[] source = key.split("\\$");
                String[] target = value.split("\\$");
                Set<String> sourcelist = new LinkedHashSet<>(Arrays.asList(source));
                Set<String> targetlist = new LinkedHashSet<>(Arrays.asList(target));
                List<String> uniquetargetlist = new LinkedList<>(targetlist);
                int i = 0;
                for (String src : sourcelist) {
                    MappingSpecificationRow mapspec = new MappingSpecificationRow();
                    mapspec.setSourceSystemEnvironmentName(sourceEnvironmentNamegl);
                    mapspec.setTargetSystemEnvironmentName(targetEnvironmentNamegl);
                    mapspec.setSourceSystemName(sourceSystemNamegl);
                    mapspec.setTargetSystemName(targetSystemNamegl);
                    mapspec.setSourceTableName(src.split("#")[0]);
                    if (src.split("#").length > 1) {

                        mapspec.setSourceColumnName(src.split("#")[1]);
                    }

                    if (targetlist.size() <= i) {
                        mapspec.setTargetTableName(" ");
                        mapspec.setTargetColumnName(" ");
                    } else {
                        mapspec.setTargetTableName(uniquetargetlist.get(i).split("#")[0]);
                        if (uniquetargetlist.get(i).split("#").length > 1) {
                            mapspec.setTargetColumnName(uniquetargetlist.get(i).split("#")[1]);
                        }

                    }
                    i++;
                    mapspeclist.add(mapspec);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void addUpdatemapspecrow(Map<String, String> updateLineageMap, Set<String> insertlist) {
        try {
            List<String> uniquetargetlist = null;
            for (Map.Entry<String, String> entry : updateLineageMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                String[] source = key.split("\\$");
                String[] target = value.split("\\$");

                Set<String> sourcelist = new LinkedHashSet<>(Arrays.asList(source));
                Set<String> targetlist = new LinkedHashSet<>(Arrays.asList(target));

                uniquetargetlist = new LinkedList<>(targetlist);
                if (value.contains("INSERT-SELECT")) {
                    uniquetargetlist = new LinkedList<>(insertlist);
                }
                int i = 0;
                for (String src : sourcelist) {
                    MappingSpecificationRow mapspec = new MappingSpecificationRow();
                    mapspec.setSourceSystemEnvironmentName(sourceEnvironmentNamegl);
                    mapspec.setTargetSystemEnvironmentName(targetEnvironmentNamegl);
                    mapspec.setSourceSystemName(sourceSystemNamegl);
                    mapspec.setTargetSystemName(targetSystemNamegl);
                    mapspec.setSourceTableName(src.split("#")[0]);
                    if (src.split("#").length > 1) {

                        mapspec.setSourceColumnName(src.split("#")[1]);
                    }

                    if (targetlist.size() <= i) {
                        mapspec.setTargetTableName(" ");
                        mapspec.setTargetColumnName(" ");
                    } else {
                        mapspec.setTargetTableName(uniquetargetlist.get(i).split("#")[0]);
                        mapspec.setTargetColumnName(uniquetargetlist.get(i).split("#")[1]);
                    }
                    i++;
                    mapspeclist.add(mapspec);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void addUpdatemapspecrowForUpdate(Map<String, String> updateLineageMap) {
        for (Map.Entry<String, String> entry : updateLineageMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String[] source = key.split("\\$");
            String[] target = value.split("\\$");
            int i = 0;
            for (String src : source) {
                MappingSpecificationRow mapspec = new MappingSpecificationRow();
                mapspec.setSourceSystemEnvironmentName(sourceEnvironmentNamegl);
                mapspec.setTargetSystemEnvironmentName(targetEnvironmentNamegl);
                mapspec.setSourceSystemName(sourceSystemNamegl);
                mapspec.setTargetSystemName(targetSystemNamegl);
                if (src.split("#")[0].contains("RESULT_OF_SELECT-QUERY")) {
                    String tableName = "RESULT_OF_SELECT-QUERY";
                    mapspec.setSourceTableName(tableName);
                } else {
                    mapspec.setSourceTableName(src.split("#")[0]);

                }
                if (src.split("#").length > 1) {
                    mapspec.setSourceColumnName(src.split("#")[1]);

                }
                if (target.length <= i) {
                    mapspec.setTargetTableName(" ");
                    mapspec.setTargetColumnName(" ");
                } else {
                    if (value.split("\\$")[i].split("#")[0].equalsIgnoreCase("OUTPUT-OF-SELECT-QUERY-1")) {
                        String tableName = "OUTPUT-OF-SELECT-QUERY";
                        mapspec.setTargetTableName(tableName);
                    } else {
                        mapspec.setTargetTableName(value.split("\\$")[i].split("#")[0]);

                    }
                    if (value.split("\\$")[i].split("#").length > 1) {
                        mapspec.setTargetColumnName(value.split("\\$")[i].split("#")[1]);

                    }
                }
                i++;
                mapspeclist.add(mapspec);
            }
        }

    }

    public static void getLineageForDelete(String lastselectQuery, String deletequery, String dbvendor) {
        String targetTableName = deletequery.split("\n")[0].trim().substring(deletequery.split("\n")[0].trim().lastIndexOf(" ")).trim();
        deleteTargetTableName = targetTableName;
        Dlineage resultofselectQuery = getDataflowObject(lastselectQuery, dbvendor);
        String resultquery = "";
        String deletestmt = "";
        String trgtstmt = "";
        Map<String, String> deletelineageMap = new LinkedHashMap<>();
        String deletestatement = "DELETE-SELECT";
        for (Object Dlineageobject : resultofselectQuery.getColumnOrTableOrResultset()) {
            if (Dlineageobject instanceof Dlineage.Relation) {
                if (((Dlineage.Relation) Dlineageobject).getType().equals("dataflow")) {
                    for (Dlineage.Relation.Target resultofselectquery : ((Dlineage.Relation) Dlineageobject).getTarget()) {
                        if (resultofselectquery.getParentName().equals("RESULT_OF_SELECT-QUERY")) {
                            if ("".equals(resultquery)) {
                                resultquery = resultofselectquery.getParentName() + "#" + resultofselectquery.getColumn();
                                deletestmt = deletestatement + "#" + resultofselectquery.getColumn();
                                trgtstmt = targetTableName + "#" + resultofselectquery.getColumn();

                            } else {
                                deletestmt = deletestmt + "$" + deletestatement + "#" + resultofselectquery.getColumn();
                                resultquery = resultquery + "$" + resultofselectquery.getParentName() + "#" + resultofselectquery.getColumn();
                                trgtstmt = trgtstmt + "$" + targetTableName + "#" + resultofselectquery.getColumn();
                            }

                        }

                    }
                }
            }

        }
        deletelineageMap.put(resultquery, deletestmt);
        deletelineageMap.put(deletestmt, trgtstmt);
        addUpdatemapspecrow(deletelineageMap);
    }

    public static void getLineageFordeleteunion(String lastselectQuery, String deletequery, String dbvendor) {
        try {
            String targetTableName = deletequery.split("\n")[0].substring(deletequery.split("\n")[0].lastIndexOf(" "));
            deleteTargetTableName = targetTableName;
            Dlineage resultofselectQuery = getDataflowObject(lastselectQuery, dbvendor);
            Dlineage deleteunion = getDataflowObject(deletequery, dbvendor);
            String targetunion = "";
            String resultquery = "";
            String targettableName = "";
            String targetvale = "";
            String sourcetableName = "";
            Map<String, String> deletelineageMap = new LinkedHashMap<>();
            String deletestatement = "DELETE_SELECT";
            String deleteselect = "";
            for (Object Dlineageobject : resultofselectQuery.getColumnOrTableOrResultset()) {
                if (Dlineageobject instanceof Dlineage.Relation) {
                    if (((Dlineage.Relation) Dlineageobject).getType().equals("dataflow")) {
                        for (Dlineage.Relation.Target resultofselectquery : ((Dlineage.Relation) Dlineageobject).getTarget()) {

                            if (resultofselectquery.getParentName().equals("RESULT_OF_SELECT-QUERY") || resultofselectquery.getParentName().equals("RESULT_OF_SELECT-QUERY-1")) {
                                if ("".equals(resultquery)) {
                                    resultquery = resultofselectquery.getParentName() + "#" + resultofselectquery.getColumn();

                                    targetvale = targetTableName + "#" + resultofselectquery.getColumn();
                                    deleteselect = deletestatement + "#" + resultofselectquery.getColumn();
                                } else {
                                    resultquery = resultquery + "$" + resultofselectquery.getParentName() + "#" + resultofselectquery.getColumn();
                                    targetvale = targetvale + "$" + targetTableName + "#" + resultofselectquery.getColumn();
                                    deleteselect = deleteselect + "$" + deletestatement + "#" + resultofselectquery.getColumn();
                                }

                            }

                        }

                    }
                }

            }
            for (Object Dlineageobject : deleteunion.getColumnOrTableOrResultset()) {
                if (Dlineageobject instanceof Dlineage.Relation) {
                    if (((Dlineage.Relation) Dlineageobject).getType().equals("dataflow")) {
                        for (Dlineage.Relation.Target resultofselectquery : ((Dlineage.Relation) Dlineageobject).getTarget()) {
                            if (resultofselectquery.getParentName().equals("RESULT_OF_UNION")) {
                                targettableName = resultofselectquery.getParentName();

                                for (Dlineage.Relation.Source updatequery : ((Dlineage.Relation) Dlineageobject).getSource()) {
                                    if ("".equals(targetunion)) {
                                        targetunion = targettableName + "#" + updatequery.getColumn();
//                                        sourcetableName = updatequery.getParentName() + "#" + updatequery.getColumn();

                                    } else {
                                        sourcetableName = sourcetableName + "$" + updatequery.getParentName() + "#" + updatequery.getColumn();
                                        targetunion = targetunion + "$" + targettableName + "#" + updatequery.getColumn();
                                    }
                                }

                            }

                        }
                    }

                }
            }

            String sourceside = resultquery + "$" + sourcetableName;
            deletelineageMap.put(sourceside, targetunion);
            deletelineageMap.put(targetunion, deleteselect);

            deletelineageMap.put(deleteselect, targetvale);
            addUpdatemapspecrow(deletelineageMap);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void updatespecrow(ArrayList<MappingSpecificationRow> mapspeclist, String tablename) {
        Iterator<MappingSpecificationRow> iter = mapspeclist.iterator();
        while (iter.hasNext()) {
            MappingSpecificationRow row = iter.next();
            if (row.getTargetTableName().equals(tablename)) {
                iter.remove();
            }
        }
    }

    public static void updatespecrowforselectQuery(ArrayList<MappingSpecificationRow> mapspeclist) {
        Iterator<MappingSpecificationRow> iter = mapspeclist.iterator();
        while (iter.hasNext()) {
            MappingSpecificationRow row = iter.next();
            if (row.getTargetTableName().contains("RESULT_OF_SELECT-QUERY")) {
                row.setTargetTableName("RESULT_OF_SELECT-QUERY");
            }
        }
    }

    public static void removeNullfromspec(ArrayList<MappingSpecificationRow> mapspeclist) {
        Iterator<MappingSpecificationRow> iter = mapspeclist.iterator();
        while (iter.hasNext()) {
            MappingSpecificationRow row = iter.next();
            if (row.getTargetColumnName().equals("null") || row.getSourceColumnName().equals("null")) {
                iter.remove();
            }
        }
    }

    public static String getcolumnName(TResultColumnList resultcolumnlist, String tableName) {
        String sourceInfo = "";
        for (int i = 0; i < resultcolumnlist.size(); i++) {
            TResultColumn column = resultcolumnlist.getResultColumn(i);
            TExpression expr = column.getExpr();
            String expression = expr.toString();
            String sourcecolumnName = column.getColumnNameOnly();

        }
        return sourceInfo;
    }

    public static String getcolumnAliasName(TResultColumnList resultcolumnlist, String tableName) {
        String sourceInfo = "";

        for (int i = 0; i < resultcolumnlist.size(); i++) {
            TResultColumn column = resultcolumnlist.getResultColumn(i);
            TExpression expr = column.getExpr();
            String expretion = expr.toString();
            String sourcecolumnName = column.getColumnNameOnly();

            if ("".equals(sourcecolumnName)) {
                sourcecolumnName = column.getColumnAlias();
                if ("".equals(sourcecolumnName)) {
                    int j = 0;
                    Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(expretion);
                    while (m.find()) {
                        if (j == 0) {
                            sourcecolumnName = m.group(1);
                        }
                        j++;
                    }
                }
            }

            if ("".equals(sourceInfo)) {
                sourceInfo = tableName + "#" + expretion + "#" + sourcecolumnName;
            } else {
                sourceInfo = sourceInfo + "$" + tableName + "#" + expretion + "#" + sourcecolumnName;
            }
        }
        return sourceInfo;
    }

    public static List<String> changelastselectquery(List<String> queryaliasnamelist) {
        List<String> appendedList = new LinkedList<>();
        String lastquery = queryaliasnamelist.get(queryaliasnamelist.size() - 1);
        String aliasName = lastquery.substring(lastquery.lastIndexOf(" "));
        String selectquery = "(" + lastquery + ")";

        queryaliasnamelist.set(queryaliasnamelist.size() - 1, selectquery);
        appendedList.addAll(queryaliasnamelist);
        return appendedList;
    }

    private static void getExpression(List<String> querylist, HashMap<String, String> tableAliasMap, String dbVender) {

        dbVendor = EDbVendor.dbvoracle;
        sqlparser = new TGSqlParser(dbVendor);

        for (String query : querylist) {
            sqlparser.sqltext = query.toUpperCase();
            int ret = sqlparser.parse();
            if (ret == 0) {
                TCustomSqlStatement stmnt = sqlparser.sqlstatements.get(0);
                TTableList list = stmnt.getTables();
                for (int i = 0; i < list.size(); i++) {
                    TTable table = list.getTable(i);
                    String tablename = table.getFullName();
                    String tableAliasName = table.getAliasName();
                    if ("".equals(tablename)) {
                        tablename = tableAliasName;
                    }

                    TResultColumnList resultlist = stmnt.getResultColumnList();
                    if (resultlist == null) {
                        continue;
                    }

                    for (int j = 0; j < resultlist.size(); j++) {
                        TResultColumn column = resultlist.getResultColumn(j);
                        TExpression expr = column.getExpr();
                        getSourceTableDetailsfrorExpression(column.getExpr().toString(), tableAliasMap);

                    }
                }
            }

        }

    }

    public static void getSourceTableDetailsfrorExpression(String expression, HashMap<String, String> tableAliasMap) {
        ArrayList<String> columnsinExpression = new ArrayList<String>();
        Set<String> keyset = specTableColumnDetails.keySet();
        //    specTableColumnDetails.get(keyset);
        for (String key : keyset) {
            ArrayList<String> colList = specTableColumnDetails.get(key);
            for (String colName : colList) {
                String als = getKeyFromValue(tableAliasMap, key + "#" + colName, expression);
                if (als == null) {
                    continue;
                }
                String[] expSp = expression.split("\\b" + als + "." + colName + "\\b");
                if (expSp.length > 1) {
                    sourceTableColumndeatilsWithBusinessRule.put(tableAliasMap.get(als) + "#" + colName, expression);
                }

            }

        }

    }

    public static String getKeyFromValue(HashMap<String, String> hm, String value, String expression) {
        for (String al : hm.keySet()) {
            if (hm.get(al) != null) {
                if (hm.get(al).toUpperCase().equals(value.split("#")[0])) {
                    return al;
                }
            }
        }
        return null;
    }

    public static Map<String, String> getsourcebrMap(Map<String, String> childParentMap, String dbvender) {
        getBrFromParentChildMap(childParentMap);

        Map<String, String> sourceExpressionTarget = new LinkedHashMap();
        Map<String, List<String>> parentMap = new LinkedHashMap<>();
        try {
            for (Map.Entry<String, String> entry : childParentMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value.startsWith("(")) {
                    value = value.substring(1, value.length() - 1);

                }
                if (key.trim().startsWith("(") && key.trim().endsWith(")")) {
                    key = key.substring(1, key.length() - 1);
                }
                if (value != null && value.trim().toUpperCase().startsWith("INSERT")) {
                    continue;
                }
                if (value.contains("UNION") && !value.startsWith("INSERT") && !value.startsWith("UPDATE")) {
                    value = value.split("UNION")[0];
                    if (value.startsWith("(")) {
                        value = value.substring(1, value.length());
                        key = value;
                    }
                }

                if (value.toUpperCase().startsWith("DELETE")) {
                    TCustomSqlStatement stmt = getselectstatement(value, dbvender);
                    if (stmt == null) {
                        continue;
                    }
                    value = stmt.toString();
                }
                if (value.toUpperCase().startsWith("UPDATE")) {
                    TCustomSqlStatement stmt = getselectstatement(value, dbvender);
                    value = stmt.toString();
                }
                List<String> TargetqueryList = getsourceMap(value, dbvender);
                List<String> columnString = new LinkedList();
                dbVendor = getEDbVendorType(dbvender);

                sqlparser = new TGSqlParser(dbVendor);
                sqlparser.sqltext = key;
                int ret = sqlparser.parse();
                if (ret == 0) {
                    TCustomSqlStatement stmnt = sqlparser.sqlstatements.get(0);

                    TResultColumnList rsltstatement = stmnt.getResultColumnList();
                    if (stmnt.getFirstPhysicalTable() == null || rsltstatement == null) {

                        continue;
                    }
                    for (int j = 0; j < rsltstatement.size(); j++) {
                        if (TargetqueryList.size() == 0) {
                            continue;
                        }

                        String tableName = stmnt.getFirstPhysicalTable().getName();
                        String tableAliasName = stmnt.getTables().getTable(0).getAliasName();// code for alias name
                        if (!"".equals(tableAliasName)) {
                            tableName = "RESULT_OF_" + tableAliasName.toUpperCase();
                        }
                        TResultColumn column = rsltstatement.getResultColumn(j);
                        String columnName = column.getColumnNameOnly();
                        String ColumnAliasname = column.getColumnAlias();
                        String targettablecolumnName = "";
                        if (TargetqueryList.size() == 1 && TargetqueryList.get(0).contains("*")) {
                            String targettableName = TargetqueryList.get(0).split("\\$")[0];
                            if ("".equals(columnName)) {
                                String colalias = ColumnAliasname;
                                targettablecolumnName = targettableName + "$" + colalias;
                            } else {
                                targettablecolumnName = targettableName + "$" + columnName;
                            }
                        } else if (TargetqueryList.get(0).split("\\$").length == 1) {
                            String targettableName = TargetqueryList.get(0).split("\\$")[0];
                            if ("".equals(columnName)) {
                                String colalias = ColumnAliasname;
                                targettablecolumnName = targettableName + "$" + colalias;
                            }
                        } else if (TargetqueryList.size() == j) {
                            targettablecolumnName = TargetqueryList.get(TargetqueryList.size() - 1);
                        } else {
                            targettablecolumnName = TargetqueryList.get(j);

                        }
                        String Expr = column.getExpr().toString();
                        TExpression expression = column.getExpr();
                        EExpressionType type = expression.getExpressionType();
                        if (type == type.parenthesis_t) {
                            if (Expr.startsWith("(") && Expr.endsWith(")")) {
                                Expr = Expr.substring(1, Expr.length() - 1);
                            }
                        } else if (Expr.contains("case")) {
                            if (!Expr.contains("(")) {
                                String temp = Expr.split("then")[0].split("when")[1];
                                String temp1 = "(" + temp + ")";
                                Expr = Expr.replace(temp, temp1);
                                if (Expr.startsWith("(") && Expr.endsWith(")")) {
                                    Expr = Expr.substring(1, Expr.length() - 1);
                                }
                            }

                        }
                        if ("".equals(columnName)) {
                            columnName = getColumnNameFromBr(Expr);
                        }
                        String sourcetableInfor = tableName + "$" + columnName + "#" + Expr;
                        if (type == type.simple_object_name_t) {
                            continue;
                        } else if (sourceExpressionTarget.get(sourcetableInfor) != null) {
                            sourceExpressionTarget.put(sourcetableInfor, sourceExpressionTarget.get(sourcetableInfor) + "##" + targettablecolumnName);
                        } else {
                            sourceExpressionTarget.put(sourcetableInfor, targettablecolumnName);
                        }

                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sourceExpressionTarget;

    }

    public static List<String> getsourceMap(String valueFromchildParentMap, String dbvendor) {
        Map<String, List<String>> ParentSideMap = new LinkedHashMap<>();
        List<String> targetColumnList = new LinkedList();
        try {
            dbVendor = getEDbVendorType(dbvendor);
            sqlparser = new TGSqlParser(dbVendor);
            sqlparser.sqltext = valueFromchildParentMap;
            int ret = sqlparser.parse();
            if (ret == 0) {
                TCustomSqlStatement stmnt = sqlparser.sqlstatements.get(0);
                if (stmnt.getFirstPhysicalTable() == null || stmnt.getResultColumnList() == null) {

                    return targetColumnList;
                }
                String tableName = stmnt.getFirstPhysicalTable().getName();
                String AliasName = stmnt.getTables().getTable(0).getAliasName();
                if (!"".equals(AliasName) && !"".equals(tableName)) {
                    if (!tableName.equalsIgnoreCase(AliasName)) {
                        if (AliasName.contains(tableName)) {
                            tableName = AliasName;
                        }
                    }
                }
                if (!"".equals(AliasName) && (tableName != null && tableName.trim().equals(""))) {
                    tableName = "RESULT_OF_" + AliasName.toUpperCase();
                }
                TResultColumnList rsltstatement = stmnt.getResultColumnList();

                for (int j = 0; j < rsltstatement.size(); j++) {
                    if (stmnt.getJoins().size() == 0) {
                        continue;
                    }
                    for (int join_cond = 0; join_cond < stmnt.getJoins().size(); join_cond++) {
                        TJoin joincond = stmnt.getJoins().getJoin(join_cond);
                        for (int j_cond = 0; j_cond < joincond.getJoinItems().size(); j_cond++) {
                            TJoinItem joinitem = joincond.getJoinItems().getJoinItem(j_cond);
                            joinconditions.add(joinitem.toString());
                            EJoinType jointype = joinitem.getJoinType();
//                            System.out.println("jointypes----" + jointype);
                        }

                    }
                    if (stmnt.getWhereClause() != null) {
                        String wherecondition = stmnt.getWhereClause().toString();

                        whereCondition.add(wherecondition);
                    }

                    TResultColumn column = rsltstatement.getResultColumn(j);
                    String columnName = column.getColumnNameOnly();
                    String columnAliasName = column.getColumnAlias();

                    // stmnt.getResultColumnList().getResultColumn(j).getAliasClause().getAliasName()
//               String srcColumnalias = stmnt.getResultColumnList().getResultColumn(j).getColumnAlias();
                    if (!"".equals(columnAliasName)) {
                        columnName = columnAliasName;
                    }

                    String Expr = column.getExpr().toString();
                    targetColumnList.add(tableName + "$" + columnName);

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return targetColumnList;
    }

    public static String getColumnNameFromBr(String businesRule) {
        Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(businesRule);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (m.find()) {
            if (i == 0) {
                if (businesRule.contains("case") || businesRule.contains("isnull")) {
                    if (businesRule.contains("isnull")) {
                        String columnName = m.group(1).split("\\.")[1].split(",")[0];
                        sb.append(columnName);
                    } else if (m.group(1).contains("=")) {
                        if (m.group(1).contains("case")) {
                            String columnName = m.group(1).split("=")[0].split("\\.")[1];
                            sb.append(columnName);
                        }
                        if (m.group(1).contains("_")) {
                            String columnName = m.group(1).split("=")[0];
                            sb.append(columnName);
                        } else {
                            String columnName = m.group(1).split("=")[0].split("\\.")[1];
                            sb.append(columnName);
                        }

                    } else if (m.group(1).contains(",")) {
                        String columnName = m.group(1).split(",")[0].split("\\.")[1];
                        sb.append(columnName);
                    } else if (m.group(1).contains("cast")) {

                        String columnName = m.group(1).split("\\(")[1].substring(0, m.group(1).split("\\(")[1].indexOf(" "));
                        sb.append(columnName);
                    } else {
                        String columnName = m.group(1).split("\\.")[1].substring(0, m.group(1).split("\\.")[1].indexOf(" "));
                        sb.append(columnName);
                    }

                } else {
                    String columnName = m.group(1);
                    sb.append(columnName);
                }

            }
            i++;
        }
        return sb.toString();
    }

    public static TCustomSqlStatement getselectstatement(String statement, String dbvendor) {
        dbVendor = getEDbVendorType(dbvendor);
        TCustomSqlStatement stmt2 = null;
        sqlparser = new TGSqlParser(dbVendor);
        sqlparser.sqltext = statement;
        int ret = sqlparser.parse();
        if (ret == 0) {
            for (int i = 0; i < sqlparser.sqlstatements.size(); i++) {
                TCustomSqlStatement statement2 = sqlparser.sqlstatements.get(i);
                if ((statement2 instanceof TDeleteSqlStatement)) {
                    for (int j = 0; j < statement2.getStatements().size(); j++) {
                        stmt2 = (TSelectSqlStatement) statement2.getStatements().get(j);
                    }

                }
                if ((statement2 instanceof TUpdateSqlStatement)) {
                    for (int j = 0; j < statement2.getStatements().size(); j++) {
                        stmt2 = (TSelectSqlStatement) statement2.getStatements().get(j);
                    }

                }
                if ((statement2 instanceof TDeleteSqlStatement)) {
                    for (int j = 0; j < statement2.getStatements().size(); j++) {
                        stmt2 = (TSelectSqlStatement) statement2.getStatements().get(j);
                    }

                }
                if ((statement2 instanceof TInsertSqlStatement)) {
                    for (int j = 0; j < statement2.getStatements().size(); j++) {
                        stmt2 = (TSelectSqlStatement) statement2.getStatements().get(j);
                    }

                }
                if ((statement2 instanceof TCreateViewSqlStatement)) {
                    for (int j = 0; j < statement2.getStatements().size(); j++) {
                        stmt2 = (TSelectSqlStatement) statement2.getStatements().get(j);
                    }

                }
            }

        }
        return stmt2;
    }

    public static void updatebusinessRule(Map<String, String> expressionMap, ArrayList<MappingSpecificationRow> speclist) {
        for (Map.Entry<String, String> entry : expressionMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String sourceTableName = key.split("#")[0].split("\\$")[0];
            String sourceColumnName = "";
            if (key.split("#")[0].split("\\$").length <= 1) {
                sourceColumnName = "";
            } else {
                sourceColumnName = key.split("#")[0].split("\\$")[1];
            }
            String targetTableName = "";
            String targetColumnName = "";
//            String sourceColumnName = key.split("#")[0].split("\\$")[1];
            String businessRule = key.split("#")[1];
            if (businessRule.contains("%")) {
                businessRule = businessRule.replaceAll("%", "");
            }
            boolean updateValue = false;
            if (value.split("##").length >= 2) {
                Set<String> setOfColumns = new LinkedHashSet<>(Arrays.asList(value.split("##")));
                List<String> listofColumns = new LinkedList(setOfColumns);
                for (int target_i = 0; target_i < listofColumns.size(); target_i++) {
                    targetTableName = listofColumns.get(target_i).split("\\$")[0];

                    targetColumnName = listofColumns.get(target_i).split("\\$")[1];
                    if (targetColumnName.contains("*")) {
                        targetColumnName = listofColumns.get(target_i).split("\\$")[2];
                    }
                    updateValue = rowupdate(speclist, sourceTableName, targetTableName, sourceColumnName, targetTableName, targetColumnName, businessRule);
                    if (sourceTableName.equalsIgnoreCase(targetTableName) && sourceColumnName.equalsIgnoreCase(targetColumnName)) {
                        targetTableName = "RESULT_OF_SELECT-QUERY";

                    }

                    if (sourceTableName.equalsIgnoreCase(targetTableName) && !sourceColumnName.equalsIgnoreCase(targetColumnName)) {
                        targetTableName = "RESULT_OF_SELECT-QUERY";

                    }

                    if (!updateValue) {
                        MappingSpecificationRow row1 = new MappingSpecificationRow();
                        if ("".equals(sourceColumnName)) {
                            row1.setTargetSystemName(targetSystemNamegl);
                            row1.setTargetSystemEnvironmentName(targetEnvironmentNamegl);
                            row1.setTargetTableName(targetTableName);
                            row1.setTargetColumnName(targetColumnName);
                            row1.setBusinessRule(businessRule);
                            speclist.add(row1);
                        } else {
                            row1.setSourceSystemEnvironmentName(sourceEnvironmentNamegl);
                            row1.setTargetSystemEnvironmentName(targetEnvironmentNamegl);
                            row1.setSourceSystemName(sourceSystemNamegl);
                            row1.setTargetSystemName(targetSystemNamegl);
                            row1.setSourceTableName(sourceTableName);

                            row1.setSourceColumnName(sourceColumnName);
                            row1.setTargetTableName(targetTableName);
                            row1.setTargetColumnName(targetColumnName);
                            row1.setBusinessRule(businessRule);
                            speclist.add(row1);
                        }
                    }
                }

            } else {
                targetTableName = value.split("\\$")[0];
//                targetColumnName = value.split("\\$")[1];
                if (value.split("\\$").length <= 2) {
                    if (value.split("\\$").length == 1) {
                        targetColumnName = sourceColumnName;
                    } else {
                        targetColumnName = value.split("\\$")[1];
                        if (targetColumnName.contains("*")) {
                            continue;
                        } else {
                            targetColumnName = value.split("\\$")[1];
                        }
                    }
                } else {
                    targetColumnName = value.split("\\$")[2];
                }
                if (targetColumnName.contains("*")) {
                    targetColumnName = value.split("\\$")[0].split("\\$")[2];
                }
                updateValue = rowupdate(speclist, sourceTableName, targetTableName, sourceColumnName, targetTableName, targetColumnName, businessRule);
                if (sourceTableName.equalsIgnoreCase(targetTableName) && sourceColumnName.equalsIgnoreCase(targetColumnName)) {
                    targetTableName = "RESULT_OF_SELECT-QUERY";

                }
                if (sourceTableName.equalsIgnoreCase(targetTableName) && !sourceColumnName.equalsIgnoreCase(targetColumnName)) {
                    targetTableName = "RESULT_OF_SELECT-QUERY";

                }
                if (!updateValue) {
                    MappingSpecificationRow row1 = new MappingSpecificationRow();
                    if ("".equals(sourceColumnName)) {
                        row1.setTargetSystemName(targetSystemNamegl);
                        row1.setTargetSystemEnvironmentName(targetEnvironmentNamegl);
                        row1.setTargetTableName(targetTableName);
                        row1.setTargetColumnName(targetColumnName);
                        row1.setBusinessRule(businessRule);
                        speclist.add(row1);
                    } else {
                        row1.setSourceSystemEnvironmentName(sourceEnvironmentNamegl);
                        row1.setTargetSystemEnvironmentName(targetEnvironmentNamegl);
                        row1.setSourceSystemName(sourceSystemNamegl);
                        row1.setTargetSystemName(targetSystemNamegl);
                        row1.setSourceTableName(sourceTableName);

                        row1.setSourceColumnName(sourceColumnName);
                        row1.setTargetTableName(targetTableName);
                        row1.setTargetColumnName(targetColumnName);
                        row1.setBusinessRule(businessRule);
                        speclist.add(row1);
                    }
                }
            }
        }
    }

    public static boolean rowupdate(ArrayList<MappingSpecificationRow> speclist, String sourceTableName, String TargetTableName, String sourcecolumnName, String targetTableName, String targetColumnName, String businessrule) {
        Iterator<MappingSpecificationRow> iter = speclist.iterator();
        while (iter.hasNext()) {
            MappingSpecificationRow row = iter.next();

            if (row.getSourceTableName().equalsIgnoreCase(sourceTableName) && row.getSourceColumnName().equalsIgnoreCase(sourcecolumnName) && row.getTargetTableName().equalsIgnoreCase(TargetTableName) && row.getTargetColumnName().equalsIgnoreCase(targetColumnName)) {
                row.setBusinessRule(businessrule);
                return true;
            }

        }
        return false;
    }

    public static void getlineageforDelete(ArrayList<MappingSpecificationRow> speclist, String targettableName, String sqlfilecontent, String dbvender) {
        String targetcolumns = getcolumnsFromDeletestatement(sqlfilecontent, dbvender);
        String[] targetcolumnarr = targetcolumns.split(",");
        String selectQuery = "";
        String deleteSelect = "DELETE-SELECT";
        String selectDelete = "";
        String targetlineage = "";
        List<String> columns = new LinkedList<>();
        Set<String> sourcesystem = new LinkedHashSet<>();
        Map<String, String> deleteLineage = new LinkedHashMap<>();
        Iterator<MappingSpecificationRow> iter = speclist.iterator();
        while (iter.hasNext()) {
            MappingSpecificationRow row = iter.next();
            if (!unionDelete) {
                if (row.getTargetTableName().equalsIgnoreCase("RESULT_OF_SELECT-QUERY") || row.getTargetTableName().equalsIgnoreCase("RESULT_OF_SELECT-QUERY-1")) {
                    String tableName = row.getTargetTableName();
                    String columnName = row.getTargetColumnName();
                    if (getcolumns(targetcolumnarr, columnName)) {
                        if ("".equals(selectQuery)) {
                            selectQuery = tableName + "#" + columnName;
                            selectDelete = deleteSelect + "#" + columnName;
                            targetlineage = targettableName + "#" + columnName;
                        } else {
                            selectQuery = selectQuery + "$" + tableName + "#" + columnName;
                            selectDelete = selectDelete + "$" + deleteSelect + "#" + columnName;
                            targetlineage = targetlineage + "$" + targettableName + "#" + columnName;
                        }
                    }
                }
            } else if (row.getTargetTableName().equalsIgnoreCase("RESULT_OF_UNION")) {

                String tableName = row.getTargetTableName();
                String columnName = row.getTargetColumnName();

                if ("".equals(selectQuery)) {
                    selectQuery = tableName + "#" + columnName;
                    selectDelete = deleteSelect + "#" + columnName;
                    targetlineage = targettableName + "#" + columnName;
                } else {
                    selectQuery = selectQuery + "$" + tableName + "#" + columnName;
                    selectDelete = selectDelete + "$" + deleteSelect + "#" + columnName;
                    targetlineage = targetlineage + "$" + targettableName + "#" + columnName;

                }

            }

        }

        deleteLineage.put(selectQuery, targetlineage);

        deleteLineage.put(selectQuery, selectDelete);
        deleteLineage.put(selectDelete, targetlineage);

        addUpdatemapspecrow(deleteLineage);
    }

    public static EDbVendor getEDbVendorType(String dbVender) {
        EDbVendor dbVendor = EDbVendor.dbvpostgresql;

        if (dbVender != null) {
            if (dbVender.equalsIgnoreCase("mssql")) {
                dbVendor = EDbVendor.dbvmssql;
            } else if (dbVender.equalsIgnoreCase("db2")) {
                dbVendor = EDbVendor.dbvdb2;
            } else if (dbVender.equalsIgnoreCase("mysql")) {
                dbVendor = EDbVendor.dbvmysql;
            } else if (dbVender.equalsIgnoreCase("postgresql")) {
                dbVendor = EDbVendor.dbvpostgresql;
            } else if (dbVender.equalsIgnoreCase("mssql")) {
                dbVendor = EDbVendor.dbvmssql;
            } else if (dbVender.equalsIgnoreCase("oracle")) {
                dbVendor = EDbVendor.dbvoracle;
            } else if (dbVender.equalsIgnoreCase("netezza")) {
                dbVendor = EDbVendor.dbvnetezza;
            } else if (dbVender.equalsIgnoreCase("teradata")) {
                dbVendor = EDbVendor.dbvteradata;
            }
        }
        return dbVendor;
    }

    public static Set<String> getJoinStatementsfromList(Map<String, String> parentchildmap, String dbvender) {
        Set<String> jointypes = new LinkedHashSet<>();
        List<TCustomSqlStatement> statementList = getJoinStatementsfromMap(parentchildmap, dbvender);

        for (TCustomSqlStatement stmt : statementList) {
            if (stmt == null) {
                continue;
            }
            for (int join_cond = 0; join_cond < stmt.getJoins().size(); join_cond++) {
                if (stmt.getJoins().size() == 0) {
                    continue;
                }
                TJoin joincond = stmt.getJoins().getJoin(join_cond);
                for (int j_cond = 0; j_cond < joincond.getJoinItems().size(); j_cond++) {
                    TJoinItem joinitem = joincond.getJoinItems().getJoinItem(j_cond);
                    joinconditions.add(joinitem.toString());
                    EJoinType jointype = joinitem.getJoinType();
                    //  System.out.println("jointypes----" + jointype+"join cond"+joinitem.toString());
                    jointypes.add(jointype + "#" + joinitem.toString());

                }

            }
            if (stmt.getWhereClause() != null) {
                String wherecondition = stmt.getWhereClause().toString();

                whereCondition.add(wherecondition);
            }
            if (stmt.getAncestorStmt().getWhereClause() != null) {
                String wherecondition = stmt.getAncestorStmt().getWhereClause().toString();
                whereCondition.add(wherecondition);
            }
        }
        return jointypes;
    }

    public static Set<String> getJoinStatementsfromList(String sqlstatement, String dbvender) {
        Set<String> jointypes = new LinkedHashSet<>();
        try {

            TCustomSqlStatement stmt = (TCustomSqlStatement) getstatementfromMap(sqlstatement, dbvender);

            if (stmt != null) {
                for (int join_cond = 0; join_cond < stmt.getJoins().size(); join_cond++) {
                    if (stmt.getJoins().size() == 0) {
                        continue;
                    }
                    TJoin joincond = stmt.getJoins().getJoin(join_cond);
                    for (int j_cond = 0; j_cond < joincond.getJoinItems().size(); j_cond++) {
                        TJoinItem joinitem = joincond.getJoinItems().getJoinItem(j_cond);
                        joinconditions.add(joinitem.toString());
                        EJoinType jointype = joinitem.getJoinType();
                        //  System.out.println("jointypes----" + jointype+"join cond"+joinitem.toString());
                        jointypes.add(jointype + "#" + joinitem.toString());

                    }

                }
            }

            if (stmt.getWhereClause() != null) {
                String wherecondition = stmt.getWhereClause().toString();

                whereCondition.add(wherecondition);
            }

            if (stmt instanceof TTeradataLock) {
                TCustomSqlStatement stmt2 = stmt.getAncestorStmt();
                String wherecondition = stmt2.getWhereClause().toString();
            }

            if (stmt.getAncestorStmt().getWhereClause() != null) {
                String wherecondition = stmt.getAncestorStmt().getWhereClause().toString();
                whereCondition.add(wherecondition);
            }

            return jointypes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jointypes;
    }

    public static List<TCustomSqlStatement> getJoinStatementsfromMap(Map<String, String> parentchildmap, String dbvender) {
        List<TCustomSqlStatement> joinstatementsquery = new LinkedList<>();
        for (Entry<String, String> entry : parentchildmap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.trim().startsWith("(") && key.trim().endsWith(")")) {
                key = key.substring(1, key.length() - 1);
            }
            if (value.startsWith("(")) {
                value = value.substring(1, value.length() - 1);

            }

            TCustomSqlStatement keystatement = getstatementfromMap(key, dbvender);
            TCustomSqlStatement valuestatement = getstatementfromMap(value, dbvender);
            joinstatementsquery.add(keystatement);
            joinstatementsquery.add(valuestatement);

        }
        return joinstatementsquery;
    }

    public static TCustomSqlStatement getstatementfromMap(String sqlquery, String dbvender) {

        dbVendor = getEDbVendorType(dbvender);
        TCustomSqlStatement stmt2 = null;
        sqlparser = new TGSqlParser(dbVendor);
        sqlparser.sqltext = sqlquery;
        int ret = sqlparser.parse();
        if (ret == 0) {
            stmt2 = sqlparser.sqlstatements.get(0);
        }

        return stmt2;
    }

    public static void createkeyvalueMap(Set<String> keysmap) {
        try {
            List<String> extendedproperties = new LinkedList<>(keysmap);
            int i = 1;
            for (String val : extendedproperties) {
                String key = val.split("#")[0];
                String value = val.split("#")[1];
                if (key.contains("WHERE_CONDITION")) {
                    keyvaluepair.put(key, value);
                } else if (keyvaluepair.get(key + " Join") == null) {
                    keyvaluepair.put(key + " Join", value);
                } else {
                    keyvaluepair.put(key + i + " Join", value);
                    i++;
                }
            }
            List<String> extendedpropertieswherecondition = new LinkedList<>(whereCondition);

            int j = 0;
            int k = 0;
            int h = 0;
            for (String whereclause : extendedpropertieswherecondition) {
                if (whereclause.contains("group_by")) {

                    keyvaluepair.put(whereclause.split("#")[0] + k, whereclause.split("#")[1]);
                    k++;
                } else if (whereclause.contains("order_by")) {
                    keyvaluepair.put(whereclause.split("#")[0] + h, whereclause.split("#")[1]);
                    h++;
                } else {
                    keyvaluepair.put("WHERE_CONDITION_" + j, whereclause);
                    j++;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

//    public static void createkeyvalueMap(Set<String> keysmap) {
//
//        List<String> extendedproperties = new LinkedList<>(keysmap);
//
//        for (String val : extendedproperties) {
//            String key = val.split("#")[0];
//            String value = val.split("#")[1];
//            keyvaluepair.put(key + " Join", value);
//
//        }
//
//    }
    public static String getcolumnsFromDeletestatement(String sql, String dbvender) {
        String columns = "";
        dbVendor = getEDbVendorType(dbvender);
        TCustomSqlStatement stmt2 = null;
        sqlparser = new TGSqlParser(dbVendor);
        sqlparser.sqltext = sql;
        int ret = sqlparser.parse();
        if (ret == 0) {
            TCustomSqlStatement stmnt = sqlparser.sqlstatements.get(0);
            TWhereClause listcolumns = stmnt.getWhereClause();
            TExpression expr = listcolumns.getCondition();
            columns = getcolumnsfromwhereClause(expr.toString());
//            System.out.println("columnsss" + columns);

        }

        return columns;
    }

    public static String getcolumnsfromwhereClause(String whereclause) {

        Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(whereclause);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (m.find()) {
            if (i == 0) {
                sb.append(m.group(1));
            }
            i++;
        }

        return sb.toString();
    }

    public static boolean getcolumns(String[] columnarr, String columnName) {
        boolean flag = false;
        for (String trgtcolumn : columnarr) {
            if (trgtcolumn.toLowerCase().trim().equals(columnName.toLowerCase())) {
                flag = true;

            }

        }
        return flag;
    }

    public static Map<String, List<String>> getresultquerymap(String query, String dbVender) {
        Map<String, List<String>> resultsetcolumns = new LinkedHashMap<>();
        Dlineage resultset = getDataflowObject(query, dbVender);

        for (Object s : resultset.getColumnOrTableOrResultset()) {

            if (s instanceof Dlineage.Resultset) {

                String rsultsetTableName = ((Dlineage.Resultset) s).getName();
                if (rsultsetTableName.contains("RESULT_OF_SELECT-QUERY")) {
                    List<Column> resultcolumnList = ((Dlineage.Resultset) s).getColumn();
                    List<String> resultcolumnsNameList = new LinkedList();
                    for (Column column : resultcolumnList) {
                        resultcolumnsNameList.add(column.getName());
                    }
                    resultsetcolumns.put(rsultsetTableName, resultcolumnsNameList);
                }

            }

        }
        return resultsetcolumns;
    }

    public static String getresultselecttable(Map<String, List<String>> resultsetMap, String columnName) {

        String tabvalue = "";
        for (Entry<String, List<String>> entry : resultsetMap.entrySet()) {
            String key = entry.getKey();
            List<String> value = entry.getValue();
            if (getcolumnsfromlist(value, columnName)) {
                if ("".equals(tabvalue)) {
                    tabvalue = key;
                } else {

                    tabvalue = tabvalue + "#" + key;
                }

            }
        }
        return tabvalue;
    }

    public static boolean getcolumnsfromlist(List<String> columnarr, String columnName) {
        boolean flag = false;
        for (String trgtcolumn : columnarr) {
            if (trgtcolumn.toLowerCase().trim().equals(columnName.toLowerCase())) {
                flag = true;

            }

        }
        return flag;
    }

    public static void updatebusinessRuleforUnionn(Map<String, String> expressionMap, ArrayList<MappingSpecificationRow> speclist, Map<String, List<String>> resultqueryMap) {
        for (Map.Entry<String, String> entry : expressionMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String sourceTableName = key.split("#")[0].split("\\$")[0];
            String sourceColumnName = "";
            boolean updateValue = false;

            if (key.split("#")[0].split("\\$").length <= 1) {
                sourceColumnName = "";
            } else {
                sourceColumnName = key.split("#")[0].split("\\$")[1];
            }

            String businessRule = key.split("#")[1];
            if (businessRule.contains("%")) {
                businessRule = businessRule.replaceAll("%", "");
            }
            if (value.contains("##")) {

                Set<String> setOfColumns = new LinkedHashSet<>(Arrays.asList(value.split("##")));
                List<String> listofColumns = new LinkedList(setOfColumns);
                for (String val : listofColumns) {
                    String targetTableName = val.split("\\$")[0];
                    String targetcolumnName = val.split("\\$")[1];
                    if (targetcolumnName.contains("*")) {
                        targetcolumnName = val.split("\\$")[2];
                    }
                    if (sourceTableName.equalsIgnoreCase(targetTableName) && sourceColumnName.equalsIgnoreCase(targetcolumnName)) {
                        String tablename = getresultselecttable(resultqueryMap, targetcolumnName);
                        String[] resultquery = tablename.split("#");
                        for (String tables : resultquery) {
                            targetTableName = tables;
                            addmapspec(sourceTableName, sourceColumnName, targetcolumnName, targetTableName, businessRule, speclist);

                        }

                    } else if (sourceTableName.equalsIgnoreCase(targetTableName) && !sourceColumnName.equalsIgnoreCase(targetcolumnName)) {
                        String tablename = getresultselecttable(resultqueryMap, targetcolumnName);
                        String[] resultquery = tablename.split("#");
                        for (String tables : resultquery) {
                            targetTableName = tables;
                            addmapspec(sourceTableName, sourceColumnName, targetcolumnName, targetTableName, businessRule, speclist);

                        }

                    } else {
                        addmapspec(sourceTableName, sourceColumnName, targetcolumnName, targetTableName, businessRule, speclist);

                    }

                }
            } else {
                String targetTableName = value.split("\\$")[0];
                String targetcolumnName = "";
                if (value.split("\\$").length <= 2) {
                    if (value.split("\\$").length == 1) {
                        targetcolumnName = sourceColumnName;
                    } else {
                        targetcolumnName = value.split("\\$")[1];
                        if (targetcolumnName.contains("*")) {
                            continue;
                        } else {
                            targetcolumnName = value.split("\\$")[1];
                        }
                    }
                } else {
                    targetcolumnName = value.split("\\$")[2];
                    if (targetcolumnName.contains("*")) {
                        targetcolumnName = value.split("\\$")[0].split("\\$")[2];
                    }
                }

                if (sourceTableName.equalsIgnoreCase(targetTableName) && sourceColumnName.equalsIgnoreCase(targetcolumnName)) {
                    String tablename = getresultselecttable(resultqueryMap, targetcolumnName);
                    String[] resultquery = tablename.split("#");
                    for (String tables : resultquery) {
                        targetTableName = tables;
                        addmapspec(sourceTableName, sourceColumnName, targetcolumnName, targetTableName, businessRule, speclist);

                    }
                } else if (sourceTableName.equalsIgnoreCase(targetTableName) && !sourceColumnName.equalsIgnoreCase(targetcolumnName)) {
                    String tablename = getresultselecttable(resultqueryMap, targetcolumnName);
                    String[] resultquery = tablename.split("#");
                    for (String tables : resultquery) {
                        targetTableName = tables;
                        addmapspec(sourceTableName, sourceColumnName, targetcolumnName, targetTableName, businessRule, speclist);
                    }

                } else {
                    addmapspec(sourceTableName, sourceColumnName, targetcolumnName, targetTableName, businessRule, speclist);

                }
            }

        }
    }

    public static void addmapspec(String sourcetablename, String sourcecolumnName, String targetcolumnName, String targettableName, String Businessrule, ArrayList<MappingSpecificationRow> speclist) {
        MappingSpecificationRow row1 = new MappingSpecificationRow();

        row1.setSourceSystemEnvironmentName(sourceEnvironmentNamegl);
        row1.setTargetSystemEnvironmentName(targetEnvironmentNamegl);
        row1.setSourceSystemName(sourceSystemNamegl);
        row1.setTargetSystemName(targetSystemNamegl);
        row1.setSourceTableName(sourcetablename);

        row1.setSourceColumnName(sourcecolumnName);
        row1.setTargetTableName(targettableName);
        row1.setTargetColumnName(targetcolumnName);
        row1.setBusinessRule(Businessrule);
        speclist.add(row1);

    }

    public static void getXmlHashMap(Map<String, String> childParentSqls, String dbvendor) throws Exception {
        String value = "";
        for (Entry<String, String> entry : childParentSqls.entrySet()) {
            String key = entry.getKey();
            key = key.substring(1, key.length() - 1);
            value = entry.getValue();
            String Insertxml = DataFlowAnalyzer.getanalyzeXmlfromString(key, dbvendor);
            getJavaObjectfromxmls(Insertxml);

        }
        String Insertxml = DataFlowAnalyzer.getanalyzeXmlfromString(value, dbvendor);
        getJavaObjectfromxmls(Insertxml);
    }

    public static void getBrFromParentChildMap(Map<String, String> prentChild) {
        dbVendor = getEDbVendorType("teradata");
        for (Entry<String, String> entry : prentChild.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.startsWith("(") && key.endsWith(")")) {
                key = key.substring(1, key.length() - 1);

            }
            if (value.startsWith("(") && value.endsWith(")")) {
                value = value.substring(1, value.length() - 1);

            }
            getBrfromSelectquery(key);
            getBrfromSelectquery(value);

        }

    }

    public static void getBrfromSelectquery(String selectsqlquery) {

        sqlparser = new TGSqlParser(dbVendor);
        sqlparser.sqltext = selectsqlquery;
        int ret = sqlparser.parse();
        if (ret == 0) {
            TCustomSqlStatement stmnt = sqlparser.sqlstatements.get(0);
            TResultColumnList rsltstatement = stmnt.getResultColumnList();
            if (rsltstatement == null) {
                return;
            }
            for (int j = 0; j < rsltstatement.size(); j++) {
                TResultColumn column = rsltstatement.getResultColumn(j);
                String Expr = column.getExpr().toString();
                //System.out.println("----" + Expr);
                String columnName = column.getColumnNameOnly();
                String columnwithAliasName = "";
                columnwithAliasName = getBrfromselectquery(Expr);
//                if ("".equals(columnwithAliasName)) {
//                    columnwithAliasName = getBrfromselectquerynew(Expr);
//                }
                String aliasName = column.getColumnAlias();
                EExpressionType type = column.getExpr().getExpressionType();
                if (type == type.simple_object_name_t) {
                    continue;
                }
                if (!columnwithAliasName.contains(".")) {
//                    columnwithAliasName = column.getColumnAlias();
                    TTableList list = stmnt.getTables();
                    for (int i = 0; i < list.size(); i++) {
                        TTable table = list.getTable(i);
                        String tablename = table.getFullName();
                        String tableAliasName = table.getAliasName();
                        tableAliasName = tableAliasName.replace("1", "");
                        if ("".equals(tableAliasName)) {
                            tableAliasName = tablename;
                        }
                        if (!columnwithAliasName.contains(".")) {
                            columnwithAliasName = tableAliasName + "." + columnwithAliasName;
                        }
                    }
                }

                ExpressionTarget.put(columnwithAliasName + "#" + aliasName, Expr);
                //System.out.println("brrr" + Expr);

                //System.out.println("aliasss" + aliasName);
                //System.out.println("columns" + getColumnNameFromBr(Expr));
            }
        }
        int i = 0;
    }

    public static Map<String, String> getBrfromOnlySelectquery(String selectsqlquery, Map<String, String> tableAliasMap) {
        Map<String, String> aliasWithBr = new LinkedHashMap();
        sqlparser = new TGSqlParser(dbVendor);
        sqlparser.sqltext = selectsqlquery;
        int ret = sqlparser.parse();
        if (ret == 0) {
            TCustomSqlStatement stmnt = sqlparser.sqlstatements.get(0);
            TResultColumnList rsltstatement = stmnt.getResultColumnList();
            if (rsltstatement == null) {
                return aliasWithBr;
            }
            for (int j = 0; j < rsltstatement.size(); j++) {
                TResultColumn column = rsltstatement.getResultColumn(j);
                String Expr = column.getExpr().toString();
                if (Expr.equals("NULL")) {
                    continue;
                }

                //System.out.println("----" + Expr);
                for (int join_cond = 0; join_cond < stmnt.getJoins().size(); join_cond++) {
                    TJoin joincond = stmnt.getJoins().getJoin(join_cond);
                    for (int j_cond = 0; j_cond < joincond.getJoinItems().size(); j_cond++) {
                        TJoinItem joinitem = joincond.getJoinItems().getJoinItem(j_cond);
//                            joinconditions.add(joinitem.toString());
                        EJoinType jointype = joinitem.getJoinType();
//                            System.out.println("jointypes----" + jointype);
                        joinconditions.add(jointype + "#" + joinitem.toString());
                    }

                }
                if (stmnt.getWhereClause() != null) {
                    String wherecondition = stmnt.getWhereClause().toString();

                    whereCondition.add(wherecondition);
                }

                String columnwithAliasName = getBrfromselectquery(Expr, tableAliasMap);
                columnwithAliasName = columnwithAliasName.replace("(", "");
                if ("".equals(columnwithAliasName)) {
                    columnwithAliasName = column.getColumnAlias();
                    TTableList list = stmnt.getTables();
                    for (int i = 0; i < list.size(); i++) {
                        TTable table = list.getTable(i);
                        String tablename = table.getFullName();
                        String tableAliasName = table.getAliasName();
                        if ("".equals(tableAliasName)) {
                            tableAliasName = tablename;
                        }
                        columnwithAliasName = tableAliasName + "." + columnwithAliasName;
                    }
                }
                //System.out.println("expressionnn" + Expr + "columnwithAliasName" + columnwithAliasName);
//                String columnAliasName = column.getColumnAlias();
                EExpressionType type = column.getExpr().getExpressionType();
                if (type == type.simple_object_name_t) {
                    continue;
                }
                if (aliasWithBr.get(columnwithAliasName) == null) {
                    if (Expr != null) {
                        aliasWithBr.put(columnwithAliasName, Expr);
                    }
                } else {
                    String value = aliasWithBr.get(columnwithAliasName);
                    if (value != null) {
                        aliasWithBr.put(columnwithAliasName, value + "~~" + Expr);
                    }
                }
//
//                System.out.println("brrr" + Expr);
//
//                System.out.println("aliasss" + columnAliasName);
//                System.out.println("columns" + getColumnNameFromBr(Expr));
            }

        }
        int i = 0;
        return aliasWithBr;
    }

    public static String getBrfromselectquery(String businessRule, Map<String, String> tableAliasMap) {
        businessRule = businessRule.toUpperCase();
        Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(businessRule);
        String aliaswithcolumnName = "";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (m.find()) {
            if (i == 0) {
                if (m.group(1).toUpperCase().contains("CASE")) {
                    if (m.group(1).contains("=")) {
                        aliaswithcolumnName = m.group(1).split("=")[0].trim().substring(m.group(1).split("=")[0].trim().lastIndexOf(" "));
                        //System.out.println("0000" + aliaswithcolumnName);
                    }
                    if ("".equals(aliaswithcolumnName.trim())) {
                        List<String> keySet = new LinkedList(tableAliasMap.keySet());
                        for (String tblAlias : keySet) {
                            tblAlias = tblAlias.toUpperCase();
                            if (businessRule != null && businessRule.contains(tblAlias + ".")) {
                                aliaswithcolumnName = businessRule.substring(businessRule.indexOf(tblAlias + "."));
                                if (aliaswithcolumnName.contains(" ")) {
                                    aliaswithcolumnName = aliaswithcolumnName.split(" ")[0];
                                }
                            }
                        }

                    }

                }
            }

        }
        return aliaswithcolumnName;
    }

    public static String getBrfromselectquery(String businessRule) {
        Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(businessRule);
        String aliaswithcolumnName = "";
        String secondColumn = "";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        String temp = "";
        while (m.find()) {
            if (i == 0) {
                if (m.group(1).toUpperCase().contains("CASE")) {
                    if (m.group(1).contains("=") && m.group(1).contains("THEN") && !m.group(1).contains("IN") && !m.group(1).contains("AND")) {
                        aliaswithcolumnName = m.group(1).split("=")[0].trim().substring(m.group(1).split("=")[0].trim().lastIndexOf(" "));
                        if (businessRule.contains("ELSE")) {
                            secondColumn = businessRule.substring(businessRule.indexOf("THEN") + 4, businessRule.indexOf("ELSE")).trim();
                        }

                        aliaswithcolumnName = aliaswithcolumnName + "\n" + secondColumn;
                        //System.out.println("0000" + aliaswithcolumnName);
                    }
                    if (m.group(1).contains("=") && m.group(1).contains("THEN") && !m.group(1).contains("AND")) {
                        aliaswithcolumnName = m.group(1).split("=")[0].trim().substring(m.group(1).split("=")[0].trim().lastIndexOf(" "));
                        if (businessRule.contains("ELSE")) {
                            secondColumn = m.group(1).substring(m.group(1).indexOf("THEN") + 4, m.group(1).indexOf("ELSE")).trim();
                        }
                        aliaswithcolumnName = aliaswithcolumnName + "\n" + secondColumn;
                        //System.out.println("0000" + aliaswithcolumnName);
                    }
//                    if (m.group(1).contains("=") && m.group(1).contains("THEN")&&m.group(1).contains("AND")) {
//                        aliaswithcolumnName = m.group(1).split("=")[0].trim().substring(m.group(1).split("=")[0].trim().lastIndexOf(" "));
//                           if(businessRule.contains("ELSE")){
//                         secondColumn = m.group(1).substring(m.group(1).indexOf("THEN") + 4, m.group(1).indexOf("ELSE")).trim();
//                        }
//                        aliaswithcolumnName = aliaswithcolumnName + "\n" + secondColumn;
//                        //System.out.println("0000" + aliaswithcolumnName);
//                    }

                } else {
                    aliaswithcolumnName = m.group(1);
                }
            }
            if (aliaswithcolumnName.contains(",")) {
                aliaswithcolumnName = aliaswithcolumnName.split(",")[0];
            }
            aliaswithcolumnName = aliaswithcolumnName.replace("NVL(", "");
            temp = temp + aliaswithcolumnName + "\n";
        }
        if (aliaswithcolumnName.contains("from")) {
            aliaswithcolumnName = aliaswithcolumnName.split("from")[1];
        }

        return temp.trim();
    }

    public static String getBrfromselectquerynew(String businessRule) {
        Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(businessRule);
        String aliaswithcolumnName = "";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (m.find()) {
            if (i == 0) {
                if (m.group(1).toUpperCase().contains("CASE")) {
                    if (m.group(1).contains("=")) {
                        aliaswithcolumnName = m.group(1).split("=")[0].trim().substring(m.group(1).split("=")[0].trim().lastIndexOf(" "));
                        // System.out.println("0000" + aliaswithcolumnName);
                    }

                }
            }

        }
        return aliaswithcolumnName;
    }

    public static String getTargetTableNamefrommapspec(String tablename) {
        String targettableName = "";
        try {
            sourceTargetMap.values().removeIf(Objects::isNull);
            targettableName = sourceTargetMap.get(tablename);

            if (targettableName == null&&tablename!=null) {
                if (sourceTargetMap.get(tablename.toUpperCase()) != null) {
                    targettableName = sourceTargetMap.get(tablename.toUpperCase());
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return targettableName;
    }

    public static void addmapspecforBRforNab(Map<String, String> expressiontarget) {
        String sourcetab = " ";
        String columnNames = "";
        for (Entry<String, String> entry : expressiontarget.entrySet()) {
            MappingSpecificationRow row = new MappingSpecificationRow();
            String key = entry.getKey();
            String value = entry.getValue();
//            if (key.equals("#")) {
//                continue;
//            }
            if (key.split("#")[0] == null || "".equals(key.split("#")[0])) {
                continue;
            }
            if (key.contains(".")) {
                sourcetab = "RESULT_OF_" + key.split("#")[0].split("\\.")[0].trim();
            } else {
                sourcetab = "RESULT_OF_";
            }
//            sourcetab = key.split("#")[0].split("\\.")[0].trim();
            //System.out.println("Tab------------" + sourcetab);
            String targetTableName = getTargetTableNamefrommapspec(sourcetab);
            //System.out.println("tttttt" + targetTableName);
            if (targetTableName == null) {
                continue;
            }
            row.setSourceSystemEnvironmentName(sourceEnvironmentNamegl);
            row.setTargetSystemEnvironmentName(targetEnvironmentNamegl);
            row.setSourceSystemName(sourceSystemNamegl);
            row.setTargetSystemName(targetSystemNamegl);
            row.setSourceTableName(sourcetab);
            if (key.contains("#") && key.contains(".")) {
                if (key.split("#")[0].contains("\n")) {
                    String temp = "";
                    String[] columnsarr = key.split("#")[0].split("\n");
                    for (String carr : columnsarr) {
                        if (carr.contains(".")) {
                            temp = temp + "\n" + carr.split("\\.")[1];
                        } else {
                            temp = temp + "\n" + carr;
                        }

                    }
                    row.setSourceColumnName(temp);
                }

                if (key.split("#").length == 2) {
                    row.setTargetColumnName(key.split("#")[1]);
                }
            }

            row.setTargetTableName(targetTableName);
            row.setBusinessRule(value);
            mapspeclist.add(row);
        }
    }

    public static void addmapspecforBRforNabselectquery(Map<String, String> expressiontarget, Map<String, String> tableAlias) {
        String sourcetab = " ";
        String columnNames = "";
        for (Entry<String, String> entry : expressiontarget.entrySet()) {
            MappingSpecificationRow row = new MappingSpecificationRow();
            String key = entry.getKey();
            String value = entry.getValue();
//            if (key.equals("#")) {
//                continue;
//            }
            if (key == null) {
                continue;
            }
            if (key.split("#")[0] == null || "".equals(key.split("#")[0])) {
                continue;
            }
            if (key.contains(".")) {
                sourcetab = tableAlias.get(key.split("#")[0].split("\\.")[0].trim());
                if (sourcetab == null) {
                    continue;
                }
            } else {
                sourcetab = "RESULT_OF_";
            }
//            sourcetab = key.split("#")[0].split("\\.")[0].trim();
            //System.out.println("Tab------------" + sourcetab);
            String targetTableName = getTargetTableNamefrommapspec(sourcetab);
            //System.out.println("tttttt" + targetTableName);
            if (targetTableName == null) {
                targetTableName = "RESULT_OF_SELECT-QUERY";
            }
            row.setSourceSystemEnvironmentName(sourceEnvironmentNamegl);
            row.setTargetSystemEnvironmentName(targetEnvironmentNamegl);
            row.setSourceSystemName(sourceSystemNamegl);
            row.setTargetSystemName(targetSystemNamegl);
            row.setSourceTableName(sourcetab);
            if (key.contains("#") && key.contains(".")) {
                if (key.split("#")[0].contains("\n")) {
                    String temp = "";
                    String[] columnsarr = key.split("#")[0].split("\n");
                    for (String carr : columnsarr) {
                        if (carr.contains(".")) {
                            temp = temp + "\n" + carr.split("\\.")[1];
                        } else {
                            temp = temp + "\n" + carr;
                        }

                    }
                    row.setSourceColumnName(temp);
                }

                if (key.split("#").length == 2) {
                    row.setTargetColumnName(key.split("#")[1]);
                }
            }

            row.setTargetTableName(targetTableName);
            row.setBusinessRule(value);
            mapspeclist.add(row);
        }
    }

    public static void getcolumnsfromBr(Map<String, List<String>> tablecolumnListFromXml, String expr, String columnName, String columnAliasName) {

        for (Entry<String, List<String>> entry : tablecolumnListFromXml.entrySet()) {
            String key = entry.getKey();
            List<String> value = entry.getValue();
            Set<String> valueset = new LinkedHashSet<>(value);
            List<String> values = new LinkedList<>(valueset);
            if (values.contains(columnName)) {

            }

        }

    }

    private static void updateBrForOnlySelectQuery(Map<String, String> tableAlias, Map<String, String> aliasWithBr) {

        for (Entry<String, String> entry : aliasWithBr.entrySet()) {
            MappingSpecificationRow row = new MappingSpecificationRow();
            String columnAndTabAlias = entry.getKey();
            String expression = entry.getValue();

            if (expression.contains("~~")) {
                String[] expArr = expression.split("~~");
                for (String exp : expArr) {
                    row = new MappingSpecificationRow();
                    if (columnAndTabAlias.contains(".")) {
                        String tableAliasName = columnAndTabAlias.split("\\.")[0].trim();
                        String sourceTableName = tableAlias.get(tableAliasName);
                        String sourceColumnName = columnAndTabAlias.split("\\.")[1];
                        row.setSourceTableName(sourceTableName);
                        row.setSourceColumnName(sourceColumnName);
                    }
                    row.setSourceSystemEnvironmentName(sourceEnvironmentNamegl);
                    row.setTargetSystemEnvironmentName(targetEnvironmentNamegl);
                    row.setSourceSystemName(sourceSystemNamegl);
                    row.setTargetSystemName(targetSystemNamegl);
                    row.setTargetTableName("");
                    row.setTargetColumnName("");
                    row.setBusinessRule(exp);
                    mapspeclist.add(row);
                }

            } else {
                if (columnAndTabAlias.contains(".")) {
                    String tableAliasName = columnAndTabAlias.split("\\.")[0].trim();
                    String sourceTableName = tableAlias.get(tableAliasName);
                    String sourceColumnName = columnAndTabAlias.split("\\.")[1];
                    row.setSourceTableName(sourceTableName);
                    row.setSourceColumnName(sourceColumnName);

                }
                row.setSourceSystemEnvironmentName(sourceEnvironmentNamegl);
                row.setTargetSystemEnvironmentName(targetEnvironmentNamegl);
                row.setSourceSystemName(sourceSystemNamegl);
                row.setTargetSystemName(targetSystemNamegl);
                row.setTargetTableName("");
                row.setTargetColumnName("");
                row.setBusinessRule(expression);
                mapspeclist.add(row);
            }
        }

    }

    private static void getMapfromSelectquery(List<String> querylist, String egtfilepath, String dbvender, String orphantabname) {
        long starttime = System.currentTimeMillis();
        Map<List<Map<String, List<String>>>, String> brInfoMap = new LinkedHashMap<>();
        Set<String> uniquequery = new LinkedHashSet<>(querylist);
        List<String> uniquequerylist = new LinkedList(uniquequery);
        List<Map<String, List<String>>> tableMap = null;
        List< List<Map<String, List<String>>>> tablemaptest = null;

        tablemaptest = new LinkedList<>();
        for (String query : uniquequerylist) {

            try {
                if (query.startsWith("(") && query.endsWith(")")) {
                    query = query.substring(1, query.length() - 1);
                }
                dbVendor = getEDbVendorType(dbvender);
                sqlparser = new TGSqlParser(dbVendor);
                sqlparser.sqltext = query;
                int ret = sqlparser.parse();
                if (ret == 0) {
                    TCustomSqlStatement stmnt = sqlparser.sqlstatements.get(0);
                    TResultColumnList rsltstatement = stmnt.getResultColumnList();

                    for (int j = 0; rsltstatement != null && j < rsltstatement.size(); j++) {
                        TResultColumn column = rsltstatement.getResultColumn(j);
                        TExpression expr = column.getExpr();
                        String columnaliasName = column.getColumnAlias();
                        String targetcolumnName = column.getColumnNameOnly();
                        EExpressionType type = expr.getExpressionType();
                        if (type == type.simple_object_name_t) {
                            continue;
                        }

                        String expression = column.getExpr().toString();
                        if (expression.contains("~")) {
                            expression = expression.replace("~", "tild");
                        }
                       // System.out.println("expression" + "::" + expression);
//                        if (expression.contains("/")) {
//                            expression = expression.substring(0, expression.indexOf("/"));
//                        }
//                        if (expression.contains(")*")) {
//                            expression = expression.substring(0, expression.lastIndexOf(")") + 1);
//
//                        }
                        // tableMap = TableBrTreev1.sourceTableMap(expression, columnaliasName, egtfilepath, orphantabname);

//                        if (tableMap.get(0).entrySet().toString().contains(orphantabname)) {
//
//                            for (Map<String, List<String>> list : tableMap) {
//                                if (list.containsKey(orphantabname)) {
//                                    list.remove(orphantabname);
//                                    tableMap.remove(list);
//                                }
//
//                            } 
//
//                        }
                        tableMap = new LinkedList<>();
                        Map<String, List<String>> mapinfo = new LinkedHashMap(getbusinessrule(expr, stmnt));
//                        if (mapinfo.size() != 0 && tableMap.isEmpty()) {
//                            tableMap.add(mapinfo);
//                            
//                        }
                        if (mapinfo.size() != 0) {
                            tableMap = new LinkedList();
                            tableMap.add(mapinfo);

                        }

                        if (tableMap.size() == 0 && !"".equals(expr)) {
                            addmapspecificationorphantargetBr(columnaliasName, expression);
                            continue;
                        }
                        brInfoMap.put(tableMap, expression + "~" + columnaliasName);
                        if (querylist.size() == 1) {
                            addmappingspecificationforselectqueryBr(brInfoMap, tableAliasMap);
                        } else {
                            addmappingspecificationforBr(brInfoMap);
                        }

                        brInfoMap.clear();
                        // System.out.println("-----" + tableMap);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //  System.out.println("-----" + tablemaptest);

        long endtime = System.currentTimeMillis();
        long stat = endtime - starttime;
        long statsec = TimeUnit.MILLISECONDS.toSeconds(stat);
      //  System.out.println("--parsing for businessrule---" + statsec);

    }

    private static void addmappingspecificationforBr(Map<List<Map<String, List<String>>>, String> sourcetargetInfo) {
        String sourceTableName = "";
        String columnName = "";
        String targetColumnName = "";
        String businessRule = "";
        String targetTableName = "";
        int temptab = 0;
        Map<String, List<String>> temptablecolumn = new LinkedHashMap<>();
        MappingSpecificationRow row = new MappingSpecificationRow();
        String tableLevelTemp = "";
        for (Entry<List<Map<String, List<String>>>, String> entry : sourcetargetInfo.entrySet()) {

//            List<Map<String, List<String>>> key = entry.getKey();
            String value = entry.getValue();
            for (Map<String, List<String>> tablecolumn : entry.getKey()) {
                for (Entry<String, List<String>> entry1 : tablecolumn.entrySet()) {

                    sourceTableName = entry1.getKey();//sourcetablename from tree

                    Set<String> columnValues = new HashSet<>(entry1.getValue());// list of columns from tree

                    if ("".equals(columnName)) {
                        columnName = StringUtils.join(columnValues, "\n");
                    } else {
                        columnName = columnName + "\n" + StringUtils.join(columnValues, "\n");
                    }
                    if (columnName.contains("(COLUMN IN DERIVED TABLE)")) {
                        columnName = columnName.replace("(COLUMN IN DERIVED TABLE)", "");
                    }
                    String tblAlias = sourceTableName;
                    sourceTableName = tableAliasMap.get(sourceTableName);

                    if (sourceTableName == null) {
                        //  sourceTableName = "RESULT_OF_" + tblAlias;
                        sourceTableName = tblAlias;
                    }
                    if (checktableNameforsource(mapspeclist, sourceTableName)) {

                        sourceTableName = "RESULT_OF_" + sourceTableName;
                    }

                    List<String> columnValue = new LinkedList<>(columnValues);
                    temptablecolumn.put(sourceTableName, columnValue);
                    targetTableName = getTargetTableNamefrommapspec(sourceTableName);
                    if (targetTableName == null) {
//                        targetTableName = "RESULT_OF_SELECT-QUERY";
                        targetTableName = "";

                    }

                    if (!"".equals(tableLevelTemp)) {
                        tableLevelTemp = tableLevelTemp + "\n" + sourceTableName;
                    } else {
                        tableLevelTemp = sourceTableName;
                    }
                    if (value.split("~").length > 1) {
                        targetColumnName = value.split("~")[1];
                    }

                    businessRule = value.split("~")[0];

                    targetTableName = getTargetTableNamefrommapspec(sourceTableName);
                    if (targetTableName == null) {
//                        targetTableName = "RESULT_OF_SELECT-QUERY";
                        targetTableName = "";
                    }
                    String tempsourcetab = "";
                    for (int i = 0; i < columnValues.size(); i++) {
                        if (i == 0) {
                            tempsourcetab = sourceTableName;
//                            tableLevelTemp = sourceTableName;                            
                        } else if (!"".equals(tableLevelTemp)) {
                            tableLevelTemp = tableLevelTemp + "\n" + tempsourcetab;
                        }
                    }

                }
                deleteduplicaterow(mapspeclist, temptablecolumn, targetTableName, targetColumnName);
            }
            row.setSourceSystemEnvironmentName(sourceEnvironmentNamegl);
            row.setTargetSystemEnvironmentName(targetEnvironmentNamegl);
            row.setSourceSystemName(sourceSystemNamegl);
            row.setTargetSystemName(targetSystemNamegl);
            String[] distincttable = tableLevelTemp.split("\n");
            //Set<String> distinctable = new LinkedHashSet<>(Arrays.asList(distincttable));
            tableLevelTemp = StringUtils.join(distincttable, "\n");
            if (tableLevelTemp.contains("$")) {
                tableLevelTemp.replaceAll("$", "");
            }
            row.setSourceTableName(tableLevelTemp);
            String[] distinctcolumn = columnName.split("\n");

            // Set<String> distinccolumnset = new LinkedHashSet<>(Arrays.asList(distinctcolumn));
            columnName = StringUtils.join(distinctcolumn, "\n");
            row.setSourceColumnName(columnName);
            row.setTargetTableName(targetTableName);
            row.setTargetColumnName(targetColumnName);
            if (businessRule.contains("tild")) {
                businessRule = businessRule.replace("tild", "~");
            }
            if (businessRule.contains("\'")) {
                businessRule = businessRule.replaceAll("\'", "\"");
            }
            row.setBusinessRule(businessRule);
            mapspeclist.add(row);

        }

    }

    private static void addmappingspecificationforselectqueryBr(Map<List<Map<String, List<String>>>, String> sourcetargetInfo, Map<String, String> tablealiasMap) {
        String sourceTableName = "";
        String columnName = "";
        String targetColumnName = "";
        String businessRule = "";
        String targetTableName = "";
        Map<String, List<String>> temptablecolumn = new LinkedHashMap<>();
        MappingSpecificationRow row = new MappingSpecificationRow();
        String tableLevelTemp = "";
        for (Entry<List<Map<String, List<String>>>, String> entry : sourcetargetInfo.entrySet()) {

//            List<Map<String, List<String>>> key = entry.getKey();
            String value = entry.getValue();
            for (Map<String, List<String>> tablecolumn : entry.getKey()) {
                for (Entry<String, List<String>> entry1 : tablecolumn.entrySet()) {

                    sourceTableName = entry1.getKey();//sourcetablename from tree

                    List<String> columnValues = entry1.getValue();// list of columns from tree

                    if ("".equals(columnName)) {
                        columnName = StringUtils.join(columnValues, "\n");
                    } else {
                        columnName = columnName + "\n" + StringUtils.join(columnValues, "\n");
                    }

                    sourceTableName = tablealiasMap.get(sourceTableName);
                    temptablecolumn.put(sourceTableName, columnValues);
                    if (!"".equals(tableLevelTemp)) {
                        tableLevelTemp = tableLevelTemp + "\n" + sourceTableName;
                    } else {
                        tableLevelTemp = sourceTableName;
                    }
                    if (value.split("~").length > 1) {
                        targetColumnName = value.split("~")[1];
                    }

                    businessRule = value.split("~")[0];
                    targetTableName = getTargetTableNamefrommapspec(sourceTableName);
                    if (targetTableName == null) {
//                        targetTableName = "RESULT_OF_SELECT-QUERY";
                        targetTableName = "";
                    }

                    String tempsourcetab = "";
                    for (int i = 0; i < columnValues.size(); i++) {
                        if (i == 0) {
                            tempsourcetab = sourceTableName;
//                            tableLevelTemp = sourceTableName;                            
                        } else if (!"".equals(tableLevelTemp)) {
                            tableLevelTemp = tableLevelTemp + "\n" + tempsourcetab;
                        }
                    }

                }
                deleteduplicaterow(mapspeclist, temptablecolumn, targetTableName, targetColumnName);
            }

            row.setSourceSystemEnvironmentName(sourceEnvironmentNamegl);
            row.setTargetSystemEnvironmentName(targetEnvironmentNamegl);
            row.setSourceSystemName(sourceSystemNamegl);
            row.setTargetSystemName(targetSystemNamegl);
            String[] distincttable = tableLevelTemp.split("\n");
            Set<String> distinctable = new LinkedHashSet<>(Arrays.asList(distincttable));
            tableLevelTemp = StringUtils.join(distinctable, "\n");
            if (tableLevelTemp.contains("$")) {
                tableLevelTemp.replaceAll("$", "");
            }
            row.setSourceTableName(tableLevelTemp);
            String[] distinctcolumn = columnName.split("\n");
            Set<String> distinccolumnset = new LinkedHashSet<>(Arrays.asList(distinctcolumn));
            columnName = StringUtils.join(distinccolumnset, "\n");
            row.setSourceColumnName(columnName);
            row.setTargetTableName(targetTableName);
            row.setTargetColumnName(targetColumnName);
            if (businessRule.contains("tild")) {
                businessRule = businessRule.replace("tild", "~");
            }
            if (businessRule.contains("\'")) {
                businessRule = businessRule.replaceAll("\'", "\"");
            }
            row.setBusinessRule(businessRule);
            mapspeclist.add(row);

        }

    }

    private static void addmapspecificationorphantargetBr(String columnAliasName, String businessRule) {
//        String targetTableName = "RESULT_OF_SELECT-QUERY";
        String targetTableName = "";
        MappingSpecificationRow row = new MappingSpecificationRow();
        row.setSourceSystemEnvironmentName(sourceEnvironmentNamegl);
        row.setTargetSystemEnvironmentName(targetEnvironmentNamegl);
        row.setSourceSystemName(sourceSystemNamegl);
        row.setTargetSystemName(targetSystemNamegl);
        row.setTargetTableName(targetTableName);
        if ("".equalsIgnoreCase(columnAliasName)) {
//            row.setTargetColumnName(businessRule);
//            row.setSourceColumnName(businessRule);
        } else {
            row.setTargetColumnName(columnAliasName);
        }

        row.setBusinessRule(businessRule);
        mapspeclist.add(row);
    }

    public static Map<String, String> getKeyvaluemap(Set<String> joinconditionslist) {

        return keyvaluepair;
    }

    public Map<String, String> getKeyvalueJson() {
        Map<String, String> extendedProp = new LinkedHashMap();
        try {
            extendedProp = getKeyvaluemap(joinconditions);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return extendedProp;
    }

    public static void getgroupbyclause(List<String> querylist, String dbvender) {

        dbVendor = getEDbVendorType(dbvender);
        for (String sql : querylist) {
            if (sql.trim().startsWith("(") && sql.trim().endsWith(")")) {
                sql = sql.substring(1, sql.length() - 1);
            }
            TCustomSqlStatement stmt2 = null;
            sqlparser = new TGSqlParser(dbVendor);
            sqlparser.sqltext = sql;
            int ret = sqlparser.parse();
            if (ret == 0) {

                TCustomSqlStatement stmnt = sqlparser.sqlstatements.get(0);

                TSelectSqlStatement selectstatement = (TSelectSqlStatement) stmnt;
                selectstatement.toString();
                if (selectstatement.getGroupByClause() != null) {
                    String groupbyclause = selectstatement.getGroupByClause().toString();
                    whereCondition.add("group_by" + "#" + groupbyclause);
                }
                if (selectstatement.getOrderbyClause() != null) {
                    String ordeerbyclause = selectstatement.getOrderbyClause().toString();
                    whereCondition.add("order_by" + "#" + ordeerbyclause);
                }

            }
        }

    }

    public static void getquerytype(String sqltext, String dbvender, List<String> querylist, String egtfilepath, String orphantarget) {
        dbVendor = getEDbVendorType(dbvender);
        String lastSubselectQuery = "";
        if (querylist.size() >= 2) {
            lastSubselectQuery = querylist.get(querylist.size() - 2);
        } else {
            lastSubselectQuery = querylist.get(0);
        }
        TCustomSqlStatement stmt2 = null;
        sqlparser = new TGSqlParser(dbVendor);
        sqlparser.sqltext = sqltext;
        int ret = sqlparser.parse();
        try {
            if (ret == 0) {
                TCustomSqlStatement stmnt = (TCustomSqlStatement) sqlparser.sqlstatements.get(0);
                if (stmnt instanceof TInsertSqlStatement) {

                    String Insertxml = DataFlowAnalyzerV1.getanalyzeXmlfromString(sqltext, dbvender);
                    getJavaObjectfromxmls(Insertxml);

                    getMapfromSelectquery(querylist, egtfilepath, dbvender, orphantarget);
                } else if (stmnt instanceof TUpdateSqlStatement) {
                    if (sqltext.contains("UNION") || sqltext.contains("union")) {

                        String lastselectqueryXml = DataFlowAnalyzerV1.getanalyzeXmlfromString(sqltext, dbvender);

                        getJavaObjectfromxmls(lastselectqueryXml);
                        if (!checkForUpdateUnionLineage(mapspeclist)) {
                            getLineageforUpdate(lastSubselectQuery, sqltext, dbvender);
                        }
                        getMapfromSelectquery(querylist, egtfilepath, dbvender, orphantarget);
                        checkForResultOfLineage(mapspeclist);
                    } else {
                        if (lastSubselectQuery.startsWith("(") && lastSubselectQuery.endsWith(")")) {

                            lastSubselectQuery = lastSubselectQuery.substring(1, lastSubselectQuery.length() - 1);
                        }
                        String lastselectqueryXml = DataFlowAnalyzerV1.getanalyzeXmlfromString(lastSubselectQuery, dbvender);
                        // lastselectqueryXml = DataFlowAnalyzerV1.getanalyzeXmlfromString(sqltext,dbvender);
                        getJavaObjectfromxmls(lastselectqueryXml);

                        getLineageforUpdate(lastSubselectQuery, sqltext, dbvender);
                        getMapfromSelectquery(querylist, egtfilepath, dbvender, orphantarget);
                        checkForResultOfLineage(mapspeclist);
                        updatespecrowforselectQuery(mapspeclist);
                    }
                } else if (stmnt instanceof TDeleteSqlStatement) {
                    String lastselectqueryXml = DataFlowAnalyzerV1.getanalyzeXmlfromString(sqltext, dbvender);
                    getJavaObjectfromxmls(lastselectqueryXml);
                    if (sqltext.contains("union") || sqltext.contains("UNION")) {
                        getLineageFordeleteunion(lastSubselectQuery, sqltext, dbvender);
                    } else {
                        getLineageForDelete(lastSubselectQuery, stmnt.toString(), dbvender);
                    }
                    getMapfromSelectquery(querylist, egtfilepath, dbvender, orphantarget);
                } else if (stmnt instanceof TTruncateStatement) {
                    String queryXml = DataFlowAnalyzerV1.getanalyzeXmlfromString(sqltext, dbvender);
                    getJavaObjectfromxmls(queryXml);
                    getMapfromSelectquery(querylist, egtfilepath, dbvender, orphantarget);
                } else if (stmnt instanceof TMssqlCreateProcedure) {
                    String queryXml = DataFlowAnalyzerV1.getanalyzeXmlfromString(sqltext, dbvender);
                    getJavaObjectfromxmls(queryXml);
                    getMapfromSelectquery(querylist, egtfilepath, dbvender, orphantarget);
                } else {

                    String selectxml = DataFlowAnalyzerV1.getanalyzeXmlfromString(sqltext, dbvender);
                    Map<String, List<String>> getresultquerymap = getresultquerymap(sqltext, dbvender);
                    getJavaObjectfromxmls(selectxml);
                    getMapfromSelectquery(querylist, egtfilepath, dbvender, orphantarget);

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            sqlparser.sqlstatements.get(0).getClass().getName();
        }
    }

    public static void deleteduplicaterow(ArrayList<MappingSpecificationRow> mapspeclist, Map<String, List<String>> tabcollist, String targettablename, String targetcolumnName) {

        for (Entry<String, List<String>> entry : tabcollist.entrySet()) {
            String tablename = entry.getKey();
            List<String> columnlist = entry.getValue();
            for (String sourcecolumn : columnlist) {
                Iterator<MappingSpecificationRow> iter = mapspeclist.iterator();
                while (iter.hasNext()) {
                    MappingSpecificationRow row = iter.next();
                    if (row.getTargetTableName().equalsIgnoreCase(targettablename) && row.getSourceTableName().equalsIgnoreCase(tablename) && row.getSourceColumnName().equalsIgnoreCase(sourcecolumn) && row.getTargetColumnName().equalsIgnoreCase(targetcolumnName)) {
                        iter.remove();
                    }
                }
            }

        }

    }

    public static void getLineageforUnionInsert(String unionQuery, String dbvendor) {
        String targetTablename = "";
        String targetColumnName = "";
        String sourceTableName = "";
        String sourceColumnName = "";
        String sourceSide = "";
        String targetSide = "";
        Set<String> insertcolumns = new LinkedHashSet();
        String insertselecttablename = "";
        String insertselectInput = "";
        String insertselect = "";
        String middlecomp = "";
        Map<String, String> insertunionmap = new LinkedHashMap<>();
        Dlineage resultofselectQuery = getDataflowObject(unionQuery, dbvendor);
        int i = 0;
        int j = 0;
        int k = 0;
        for (Object Dlineageobject : resultofselectQuery.getColumnOrTableOrResultset()) {
            if (Dlineageobject instanceof Dlineage.Relation) {
                if (((Dlineage.Relation) Dlineageobject).getType().equals("dataflow")) {
                    for (Dlineage.Relation.Target subselecttarget : ((Dlineage.Relation) Dlineageobject).getTarget()) {
                        if (subselecttarget.getParentName().equals("RESULT_OF_UNION")) {
                            targetTablename = subselecttarget.getParentName();
                            targetColumnName = subselecttarget.getColumn();
                            for (Dlineage.Relation.Source subselectsource : ((Dlineage.Relation) Dlineageobject).getSource()) {
                                sourceTableName = subselectsource.getParentName();
                                sourceColumnName = subselectsource.getColumn();
                                if (i == 0) {
                                    sourceSide = sourceTableName + "#" + sourceColumnName;
                                    targetSide = targetTablename + "#" + targetColumnName;
                                } else {
                                    sourceSide = sourceSide + "$" + sourceTableName + "#" + sourceColumnName;
                                    targetSide = targetSide + "$" + targetTablename + "#" + targetColumnName;
                                }
                                i++;
                            }
                        }

                    }
                    for (Dlineage.Relation.Source subselectsource : ((Dlineage.Relation) Dlineageobject).getSource()) {
                        if (subselectsource.getParentName().equals("RESULT_OF_UNION")) {
                            for (Dlineage.Relation.Target subselecttarget : ((Dlineage.Relation) Dlineageobject).getTarget()) {

                                if (k == 0) {
                                    middlecomp = subselecttarget.getParentName() + "#" + subselecttarget.getColumn();

                                } else {

                                    middlecomp = middlecomp + "$" + subselecttarget.getParentName() + "#" + subselecttarget.getColumn();

                                }
                                k++;
                                String targetTableName = subselecttarget.getParentName();

                            }

                        }
                        targetTablename = subselectsource.getParentName();
                        targetColumnName = subselectsource.getColumn();

                    }

                }

                for (Dlineage.Relation.Target subselecttarget : ((Dlineage.Relation) Dlineageobject).getTarget()) {
                    if (subselecttarget.getParentName().equals("INSERT-SELECT")) {
                        for (Dlineage.Relation.Source outputtarget : ((Dlineage.Relation) Dlineageobject).getSource()) {
                            insertselecttablename = outputtarget.getParentName();
                            break;
                        }

                        for (Dlineage.Relation.Target outputtarget : ((Dlineage.Relation) Dlineageobject).getTarget()) {

                            if (j == 0) {
                                insertselectInput = insertselecttablename + "#" + outputtarget.getColumn();
                                insertselect = subselecttarget.getParentName() + "#" + outputtarget.getColumn();

                            } else {
                                insertselectInput = insertselectInput + "$" + insertselecttablename + "#" + outputtarget.getColumn();
                                insertselect = insertselect + "$" + subselecttarget.getParentName() + "#" + outputtarget.getColumn();
                            }

                            j++;

                        }
                    }
                }
                for (Dlineage.Relation.Source subselectsource : ((Dlineage.Relation) Dlineageobject).getSource()) {
                    if (subselectsource.getParentName().equals("INSERT-SELECT")) {
                        for (Dlineage.Relation.Source outputtarget : ((Dlineage.Relation) Dlineageobject).getSource()) {
                            String insertselecttablename1 = outputtarget.getParentName();
                            String insertcolumns1 = outputtarget.getColumn();
                            if (insertselecttablename1 != null && insertcolumns1 != null) {

                                insertcolumns.add(insertselecttablename1 + "#" + insertcolumns1);
                            }

                            break;
                        }

                    }
                }

            }
        }

        updatespecrow(mapspeclist, "INSERT-SELECT");
        // insertunionmap.put(sourceSide, targetSide);
        insertunionmap.put(targetSide, middlecomp);
        insertunionmap.put(middlecomp, insertselect);

        addUpdatemapspecrow(insertunionmap, insertcolumns);

    }

    public static Map<String, List<String>> getbusinessrule(TExpression businessRule, TCustomSqlStatement query) {
        long starttime = System.currentTimeMillis();
        TExpression expression = (TExpression) businessRule;
//        System.out.println("------" + query.toString());
        columnInClause colclause = new columnInClause();
        colclause.printColumns(businessRule, query);

        LinkedList<String> businessrulelist = new LinkedList(Columnlist.ColumnList);
        if (Columnlist.ColumnList != null) {
            Columnlist.ColumnList.clear();
        }

        Map<String, List<String>> tableCol = getMap(businessrulelist);
        long endtime = System.currentTimeMillis();
        long stat = endtime - starttime;
        Long statsecond = TimeUnit.MILLISECONDS.toSeconds(stat);
       // System.out.println("----time taken in businessrule-----" + statsecond);
        return tableCol;

    }

    public static Map<String, List<String>> getMap(LinkedList<String> columndetailsfromlist) {
        Map<String, List<String>> tablecollist = new LinkedHashMap<>();
        List<String> collist = null;
        for (String columnname : columndetailsfromlist) {
            int size = columnname.split("\\.").length;
            String tablename = columnname.split("\\.")[0];
            String columnName = columnname.split("\\.")[size - 1].toUpperCase();
            String tabcol[] = columnname.split("\\.");
            StringBuilder sb = new StringBuilder();
            List<String> tablelist = new LinkedList<>();
            for (int i = 0; i < tabcol.length - 1; i++) {
                tablelist.add(tabcol[i]);
            }

            tablename = StringUtils.join(tablelist, ".").toUpperCase();
            if (!tablecollist.containsKey(tablename)) {
                collist = new LinkedList<>();
                collist.add(columnName);
                tablecollist.put(tablename, collist);

            } else {
                tablecollist.get(tablename).add(columnName);
//    tablecollist.put(tablename,colname+","+columnName);
            }

        }
        return tablecollist;
    }

//    public static void makelooplineage(Map<String, String> sourcetargetMap) {
//        LinkedList<String> sourcetable = new LinkedList(sourcetargetMap.values());
//        String topmosttargettablename = sourcetable.get(sourcetable.size() - 1);
//        if (!topmosttargettablename.contains("RESULT_OF_")&&!topmosttargettablename.contains("INSERT-SELECT")&&!topmosttargettablename.contains("RESULT_OF_UNION")&&!topmosttargettablename.contains("UPDATE")) {
//            updatespecrowforsource(mapspeclist, topmosttargettablename);
//        }
//
//    }
//    public static void updatespecrowforsource(ArrayList<MappingSpecificationRow> mapspeclist, String tablename) {
//        Iterator<MappingSpecificationRow> iter = mapspeclist.iterator();
//        while (iter.hasNext()) {
//            MappingSpecificationRow row = iter.next();
//            System.out.println(row.getSourceTableName() + "_____" + row.getSourceTableName().equalsIgnoreCase(tablename) + " " + tablename);
//            if (row.getSourceTableName().equalsIgnoreCase(tablename)) {
//                if (!"".equals(row.getBusinessRule())) {
//                    row.setSourceTableName(row.getSourceTableName() + "_l");
//                } else {
//                    row.setTargetTableName(row.getSourceTableName() + "_l");
//                    row.setSourceTableName("");
//                    row.setSourceColumnName("");
//                    row.setSourceSystemEnvironmentName("");
//                    row.setSourceSystemName("");
////                    iter.remove();
//                }
//            }
//        }
//    }
    public static boolean checktableNameforsource(ArrayList<MappingSpecificationRow> mapspeclist, String tablename) {
        boolean flag = false;
        Iterator<MappingSpecificationRow> iter = mapspeclist.iterator();
        String tabName = "RESULT_OF_" + tablename;
        while (iter.hasNext()) {
            MappingSpecificationRow row = iter.next();
            //   System.out.println(row.getSourceTableName() + "_____" + row.getSourceTableName().equalsIgnoreCase(tablename) + " " + tablename);
            if (row.getSourceTableName().equalsIgnoreCase(tabName) || row.getTargetTableName().equalsIgnoreCase(tabName)) {
                flag = true;
                break;

            }

        }
        return flag;
    }

    public static boolean checkForUpdateUnionLineage(ArrayList<MappingSpecificationRow> mapspeclist) {
        boolean flag = false;
        Iterator<MappingSpecificationRow> iter = mapspeclist.iterator();
        String tabName = "UPDATE-SET";
        while (iter.hasNext()) {
            MappingSpecificationRow row = iter.next();
            if (row.getTargetTableName().equalsIgnoreCase(tabName)) {
                flag = true;
                break;

            }

        }
        return flag;
    }

    public static void checkForResultOfLineage(ArrayList<MappingSpecificationRow> mapspeclist) {
        boolean flag = false;
        Set<String> resultOfColSet = new LinkedHashSet();
        Map<String, String> updateSetColMap = new LinkedHashMap();
        Iterator<MappingSpecificationRow> iter = mapspeclist.iterator();
        String updateSet = "UPDATE-SET";
        String resultOfSelect = checkTargettableNameforupdate(mapspeclist);

        while (iter.hasNext()) {
            MappingSpecificationRow row = iter.next();
            if (row.getTargetTableName().equalsIgnoreCase(resultOfSelect)) {
                if (!row.getTargetColumnName().equals("")) {
                    resultOfColSet.add(row.getTargetTableName() + "#" + row.getTargetColumnName());
                }

            } else if (row.getSourceTableName().equalsIgnoreCase(resultOfSelect)) {
                if (!row.getSourceColumnName().equals("")) {
                    resultOfColSet.add(row.getSourceTableName() + "#" + row.getSourceColumnName());
                }

            } else if (row.getSourceTableName().equalsIgnoreCase(updateSet)) {
                updateSetColMap.put(row.getSourceColumnName(), row.getSourceTableName());

            } else if (row.getSourceTableName().equalsIgnoreCase(updateSet)) {
                updateSetColMap.put(row.getTargetColumnName(), row.getTargetTableName());

            }

        }
        updateResultOfLineagespecs(resultOfColSet, updateSetColMap);
    }

    public static void updateResultOfLineagespecs(Set<String> resultOfColSet, Map<String, String> updateSetColMap) {
        try {

            boolean flag = false;
            MappingSpecificationRow newSpec = null;

            String updateSet = "UPDATE-SET";
            //  String resultOfSelect = "RESULT_OF_SELECT-QUERY";
            String resultOfSelect = checkTargettableNameforupdate(mapspeclist);

            for (String string : resultOfColSet) {
                String sourcetabName = string.split("#")[0];
                String sourcecolName = string.split("#")[1];
                if (checkForUpdateUnionLineage(mapspeclist, sourcetabName, sourcecolName)) {
                    continue;
                } else {
                    newSpec = new MappingSpecificationRow();
                    newSpec.setSourceSystemEnvironmentName(sourceEnvironmentNamegl);
                    newSpec.setTargetSystemEnvironmentName(targetEnvironmentNamegl);
                    newSpec.setSourceSystemName(sourceSystemNamegl);
                    newSpec.setTargetSystemName(targetSystemNamegl);
                    newSpec.setSourceTableName(sourcetabName);
                    newSpec.setSourceColumnName(sourcecolName);
                    newSpec.setTargetTableName(updateSet);
                    if (updateSetColMap.get(sourcecolName.toUpperCase()) == null) {
                        continue;
                    }
                    if (updateSetColMap.get(sourcecolName.toUpperCase()).equalsIgnoreCase(updateSet)) {
                        newSpec.setTargetColumnName(sourcecolName);
                    }
                }

                mapspeclist.add(newSpec);
            }

        } catch (Exception e) {
            e.printStackTrace();

        }

    }

    public static boolean checkForUpdateUnionLineage(ArrayList<MappingSpecificationRow> mapspeclist, String srcTblName, String SourceColName) {
        boolean flag = false;
        Iterator<MappingSpecificationRow> iter = mapspeclist.iterator();
        String tabName = "UPDATE-SET";
        while (iter.hasNext()) {
            MappingSpecificationRow row = iter.next();
            if (row.getSourceTableName().equalsIgnoreCase(srcTblName) && row.getSourceColumnName().equalsIgnoreCase(SourceColName)) {
                flag = true;
                break;

            }

        }
        return flag;
    }

    public static String checkTargettableNameforupdate(ArrayList<MappingSpecificationRow> mapspeclist) {
        boolean flag = false;
        String srctabName = "";
        Iterator<MappingSpecificationRow> iter = mapspeclist.iterator();

        while (iter.hasNext()) {
            MappingSpecificationRow row = iter.next();

            if (row.getTargetTableName().equalsIgnoreCase("UPDATE-SET")) {
                srctabName = row.getSourceTableName();

                break;

            }

        }
        return srctabName;
    }

    public static ArrayList<MappingSpecificationRow> getspecificationdetails(ArrayList<MappingSpecificationRow> updatedspeclist) {
        Set<String> checkset = new HashSet();
        ArrayList<MappingSpecificationRow> resultSpeclist = new ArrayList<MappingSpecificationRow>();
        for (int i = 0; i < updatedspeclist.size(); i++) {
            MappingSpecificationRow row = updatedspeclist.get(i);

            String targettablename = row.getTargetTableName();
            String sourceTableName = row.getSourceTableName();
            String sourceColumnName = row.getSourceColumnName();
            String targetColumnName = row.getTargetColumnName();
            String businessRule = row.getBusinessRule();
            if ("".equals(businessRule) || businessRule == null) {
                businessRule = "";
            }
            if (!checkset.contains(targettablename + "," + sourceTableName + "," + sourceColumnName + "," + targetColumnName + "," + businessRule)) {
                checkset.add(targettablename + "," + sourceTableName + "," + sourceColumnName + "," + targetColumnName + "," + businessRule);
                resultSpeclist.add(row);
            }

//            System.out.println("srctab"+sourceTableName+"targetTableName"
//                    +targettablename+"srcColumnName"+sourceColumnName+"targetColumnName"+targetColumnName);
//            checkanddeletefromspec(updatedspeclist, targettablename, sourceTableName, sourceColumnName, targetColumnName);
        }
        return resultSpeclist;

    }

    public static void checkanddeletefromspec(ArrayList<MappingSpecificationRow> mappinglist,
            String targettablename, String tablename, String sourcecolumn, String targetcolumnName) {

        for (int i = 0; i < mappinglist.size(); i++) {

            MappingSpecificationRow row = mappinglist.get(i);
            if (row.getTargetTableName().equalsIgnoreCase(targettablename)
                    && row.getSourceTableName().equalsIgnoreCase(tablename)
                    && row.getSourceColumnName().equalsIgnoreCase(sourcecolumn)
                    && row.getTargetColumnName().equalsIgnoreCase(targetcolumnName)) {

              //  System.out.println("srctab duplicates------" + tablename + "targetTableName"
                  //      + targettablename + "srcColumnName" + sourcecolumn + "targetColumnName" + targetcolumnName);
                mappinglist.remove(i);
            }

        }

    }

}
