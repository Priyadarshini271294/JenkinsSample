/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sqlToJsonenterprise;

import com.ads.api.beans.mm.Mapping;
import com.ads.api.beans.mm.MappingSpecificationRow;
import com.sqlToJson.BindingPojo.Column;
import com.sqlToJson.BindingPojo.Dlineage;
import demos.dlineage.DataFlowAnalyzer;
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
import gudusoft.gsqlparser.stmt.TUpdateSqlStatement;
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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;

/**
 *
 * @author mylaptop
 */
public class SqltoJsonv12 {

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

    public String sqlToDataflow(String sqlfile, String sourceSystemName, String sourceEnvironmentName, String targetSystemName, String targetEnvironmentName, String dbvendor) {
        sourceSystemNamegl = sourceSystemName;
        sourceEnvironmentNamegl = sourceEnvironmentName;
        targetSystemNamegl = targetSystemName;
        targetEnvironmentNamegl = targetEnvironmentName;
        String json = "";
        try {
            json = getJsonfromSql(sqlfile, dbvendor);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public static String getJsonfromSql(String sqlfile, String dbvendor) {
        String json = "";

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<String> querylist = null;
            String lastSubselectQuery = "";
            toXml.getXml(sqlfile, dbvendor);
            File filessql = new File(sqlfile);
            querylist = new LinkedList<>(xmlVisitor.subselectquery);
            Map<String, String> childParentMap = new LinkedHashMap<>(xmlVisitor.childParentQuery);

            if (querylist.size() != 0) {
                lastSubselectQuery = querylist.get(querylist.size() - 1);
            }
            String sqlfileContent = FileUtils.readFileToString(filessql);
            querylist.add(sqlfileContent);
            Set<String> keyvaluepair = getJoinStatementsfromList(childParentMap, dbvendor);
            createkeyvalueMap(keyvaluepair);
            System.out.println("----" + keyvaluepair);
            HashMap<String, String> tableAliasMap = getTableAliasMap(querylist, dbvendor);
            getExpression(querylist, tableAliasMap, dbvendor);
            List<String> appendedList = getAppendedstatement(querylist);
           
            if (sqlfileContent.startsWith("INSERT")) {
                List<String> StarappendedList = cahngelatindexoflist(appendedList);
                appendedList.clear();
                appendedList.addAll(StarappendedList);
                String lastinsertQuery = appendedList.get(appendedList.size() - 1);
                String Insertxml = DataFlowAnalyzer.getanalyzeXmlfromString(lastinsertQuery, dbvendor);
                //      System.out.println("xmlll" + Insertxml);
                getJavaObjectfromxmls(Insertxml);
//                if (sqlfileContent.contains("UNION")) {
                    getLineageforUnionInsert(lastinsertQuery,dbvendor);
//                }
            } else {
                String Insertxml = DataFlowAnalyzer.getanalyzeXmlfromString(sqlfileContent, dbvendor);
                //    System.out.println("-------" + Insertxml);
                getJavaObjectfromxmls(Insertxml);
                Map<String, String> getsourcebrMap = getsourcebrMap(childParentMap, dbvendor);
                updatebusinessRule(getsourcebrMap, mapspeclist);

            }
            if (sqlfileContent.startsWith("UPDATE")) {
                if (sqlfileContent.contains("UNION")) {
                    String lastselectqueryXml = DataFlowAnalyzer.getanalyzeXmlfromString(sqlfileContent, dbvendor);

                    getJavaObjectfromxmls(lastselectqueryXml);
                } else {
                    String lastselectqueryXml = DataFlowAnalyzer.getanalyzeXmlfromString(lastSubselectQuery, dbvendor);

                    getJavaObjectfromxmls(lastselectqueryXml);
                    getLineageforUpdate(lastSubselectQuery, sqlfileContent, dbvendor);
                }

                //updatespecrowforresult(mapspeclist);
                // 
            }
            if (sqlfileContent.toUpperCase().startsWith("DELETE")) {
                String lastselectqueryXml = DataFlowAnalyzer.getanalyzeXmlfromString(sqlfileContent, dbvendor);
                getJavaObjectfromxmls(lastselectqueryXml);
//                if (sqlfileContent.contains("UNION")) {
//                    getLineageFordeleteunion(lastSubselectQuery, sqlfileContent, dbvendor);
//                } else {
                //     getLineageForDelete(lastSubselectQuery, sqlfileContent, dbvendor);

//             }
            }

            if (sqlfileContent.startsWith("INSERT")) {
                Map<String, String> getsourcebrMap = getsourcebrMap(childParentMap, dbvendor);
                updatebusinessRule(getsourcebrMap, mapspeclist);

            }
            if (sqlfileContent.startsWith("UPDATE")) {
                Map<String, String> getsourcebrMap = getsourcebrMap(childParentMap, dbvendor);
                updatebusinessRuleForUpdate(getsourcebrMap, mapspeclist);
            }
            if (sqlfileContent.toUpperCase().startsWith("DELETE")) {
                Map<String, String> getsourcebrMap = getsourcebrMap(childParentMap, dbvendor);
                updatebusinessRuleForDelete(getsourcebrMap, mapspeclist);
                if (sqlfileContent.contains("UNION")) {
                    unionDelete = true;
                }
                String targetTableName = sqlfileContent.split("\n")[0].substring(sqlfileContent.split("\n")[0].lastIndexOf(" "));
                updatespecrowforresult(mapspeclist);
                getlineageforDelete(mapspeclist, targetTableName, sqlfileContent, dbvendor);

            }
            childParentMap.clear();
            List<Mapping> mappings = createmapfromrow(mapspeclist, filessql.getName(), sqlfileContent, appendedList);

            //objectMapper.writeValue(new File(jsonFile + "\\" + filessql.getName() + ".json"), mappings);
            json = objectMapper.writeValueAsString(mappings);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
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

    public void getClear() {
        clearStaticvariable();

    }

    public static void getJavaObjectfromxmls(String xmlfile) {
        try {
            MappingSpecificationRow mapSpec = new MappingSpecificationRow();
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
                    if (((Dlineage.Relation) s).getType().equals("dataflow")) {
                        for (Dlineage.Relation.Source so : ((Dlineage.Relation) s).getSource()) {
                            if (((Dlineage.Relation) s).getSource().size() > 1) {
                                if ("".equals(sourceinfomore)) {
                                    sourceinfomore = so.getParentName() + "$" + so.getColumn();
                                } else {
                                    sourceinfomore = sourceinfomore + "~" + so.getParentName() + "$" + so.getColumn();
                                }
                            } else {
                                sourcetableName = so.getParentName();

                                sourceColumnName = so.getColumn();
                            }

                        }
                        for (Dlineage.Relation.Target target : ((Dlineage.Relation) s).getTarget()) {

                            targetTableName = target.getParentName();
                            targetColumnName = target.getColumn();

                        }

                        String source = sourcetableName + "$" + sourceColumnName;
                        String target = targetTableName + "$" + targetColumnName;
                        if (!"".equals(sourceinfomore)) {
                            maps.put(sourceinfomore, target);
                        } else {
                            maps.put(source, target);
                        }

                        getAllmapp(maps);

                    }

                    i++;
                }
                if (s instanceof Dlineage.Table) {

                    String tableName = ((Dlineage.Table) s).getName();
                    List<Column> columnList = ((Dlineage.Table) s).getColumn();

                    for (Column column : columnList) {
                        columnsNameList.add(column.getName());
                    }
                    tableColRel.put(tableName, columnsNameList);

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
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void getAllmapp(LinkedHashMap<String, String> sourcetarget) {
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
                    mapspec1.setSourceColumnName(unionsource.split("\\$")[1]);
                    mapspec1.setSourceColumnIdentityFlag(true);
                    mapspec1.setTargetTableName(value.split("\\$")[0]);
                    mapspec1.setTargetColumnName(value.split("\\$")[1]);
                    mapspeclist.add(mapspec1);
                }
            } else {
                mapspec.setSourceSystemEnvironmentName(sourceEnvironmentNamegl);
                mapspec.setTargetSystemEnvironmentName(targetEnvironmentNamegl);
                mapspec.setSourceSystemName(sourceSystemNamegl);
                mapspec.setTargetSystemName(targetSystemNamegl);
                mapspec.setSourceTableName(key.split("\\$")[0]);
                mapspec.setSourceColumnName(key.split("\\$")[1]);
                mapspec.setSourceColumnIdentityFlag(true);
                mapspec.setTargetTableName(value.split("\\$")[0]);
                mapspec.setTargetColumnName(value.split("\\$")[1]);
                mapspeclist.add(mapspec);

            }

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
        m.setMappingSpecifications(specrowlist);
        m.setSourceExtractQuery(query);

        mappinglist.add(m);
        return mappinglist;
    }

    public static List<String> getproperquery(List<String> subquries) {
        for (int index = 0; index < subquries.size(); index++) {
            String appendedstar = subquries.get(index).split("\\(")[0];
            if ("".equals(appendedstar)) {
                continue;
            }
            if (appendedstar.split(" ")[1].contains("*")) {
                StringBuilder sb = new StringBuilder();
                StringBuffer querytoreplace = new StringBuffer();
                String subqueries = subquries.get(subquries.size() - 1);
                String[] columnlevellineage = subqueries.split("\n");
                int i = 0;
                Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(subqueries);
                while (m.find()) {
                    if (i == 0) {
                        sb.append(m.group(1));
                    }
                    i++;
                }
                String[] lines = subquries.get(index).split("\n");
                int qa = 0;
                for (String queries : lines) {
                    if (qa == 0) {
                        // String sbstring =  queries.subSequence(queries.indexOf(" "), queries.lastIndexOf(" "));
                        String queries12 = queries.replace(queries.substring(queries.indexOf(" "), queries.lastIndexOf(" ")), sb.toString());
                        querytoreplace.append(queries12);
                    } else {
                        querytoreplace.append(queries);
                    }
                    qa++;
                }
                subquries.set(index, querytoreplace.toString());
            }
            index++;
        }
        return subquries;
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
            String dataflowAnalyzerxml = DataFlowAnalyzer.getanalyzeXmlfromString(sqlquery, dbvendor);
            InputStream in = IOUtils.toInputStream(dataflowAnalyzerxml);
            JAXBContext jaxbContext
                        = JAXBContext.newInstance("com.sqlToJson.BindingPojo");
            Unmarshaller unmarshaller
                        = jaxbContext.createUnmarshaller();
            dataflow = (Dlineage) unmarshaller.unmarshal(in);
        } catch (Exception e) {
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
                                sourceDetails = subselectsource.getParentName() + "#" + subselectsource.getColumn();
                                targetforUpdateset = upDateset + "#" + subselectsource.getColumn();
                            } else {

                                sourceDetails = sourceDetails + "$" + subselectsource.getParentName() + "#" + subselectsource.getColumn();
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
                        if (updatequery.getParentName().equals("UPDATE-SET")) {
                            for (Dlineage.Relation.Target updatequerytgt : ((Dlineage.Relation) Dlineageobject).getTarget()) {
                                targetTableName = updatequerytgt.getParentName();
                                break;
                            }
                            if ("".equals(updatesettarget)) {
                                updatesettarget = updatequery.getParentName() + "#" + updatequery.getColumn();

                            } else {
                                updatesettarget = updatesettarget + "$" + updatequery.getParentName() + "#" + updatequery.getColumn();

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
                mapspec.setSourceTableName(src.split("#")[0]);

                mapspec.setSourceColumnName(src.split("#")[1]);

                if (target.length <= i) {
                    mapspec.setTargetTableName(" ");
                    mapspec.setTargetColumnName(" ");
                } else {
                    mapspec.setTargetTableName(value.split("\\$")[i].split("#")[0]);
                    mapspec.setTargetColumnName(value.split("\\$")[i].split("#")[1]);
                }
                i++;
                mapspeclist.add(mapspec);
            }
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
                if (src.split("#")[0].equalsIgnoreCase("OUTPUT-OF-SELECT-QUERY-1")) {
                    String tableName = "OUTPUT-OF-SELECT-QUERY";
                    mapspec.setSourceTableName(src.split("#")[0]);
                } else {
                    mapspec.setSourceTableName(src.split("#")[0]);

                }
                mapspec.setSourceColumnName(src.split("#")[1]);
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

                    mapspec.setTargetColumnName(value.split("\\$")[i].split("#")[1]);
                }
                i++;
                mapspeclist.add(mapspec);
            }
        }

    }

    public static void getLineageForDelete(String lastselectQuery, String deletequery, String dbvendor) {
        String targetTableName = deletequery.split("\n")[0].substring(deletequery.split("\n")[0].lastIndexOf(" "));
        deleteTargetTableName = targetTableName;
        Dlineage resultofselectQuery = getDataflowObject(lastselectQuery, dbvendor);
        String resultquery = "";
        Map<String, String> deletelineageMap = new LinkedHashMap<>();
        String deletestatement = "DELETE-SELECT";
        for (Object Dlineageobject : resultofselectQuery.getColumnOrTableOrResultset()) {
            if (Dlineageobject instanceof Dlineage.Relation) {
                if (((Dlineage.Relation) Dlineageobject).getType().equals("dataflow")) {
                    for (Dlineage.Relation.Target resultofselectquery : ((Dlineage.Relation) Dlineageobject).getTarget()) {
                        if (resultofselectquery.getParentName().equals("RESULT_OF_SELECT-QUERY")) {
                            if ("".equals(resultquery)) {
                                resultquery = resultofselectquery.getParentName() + "#" + resultofselectquery.getColumn();
                                deletestatement = deletestatement + "#" + resultofselectquery.getColumn();
                                targetTableName = targetTableName + "#" + resultofselectquery.getColumn();

                            } else {
                                resultquery = resultquery + "$" + resultofselectquery.getParentName() + "#" + resultofselectquery.getColumn();
                                targetTableName = targetTableName + "$" + targetTableName + "#" + resultofselectquery.getColumn();
                            }

                        }

                    }
                }
            }

        }
        deletelineageMap.put(resultquery, deletestatement);
        deletelineageMap.put(deletestatement, targetTableName);
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
            String deleteselectvale = "";
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

                                    targetTableName = targetTableName + "#" + resultofselectquery.getColumn();
                                    deleteselect = deletestatement + "#" + resultofselectquery.getColumn();
                                } else {
                                    resultquery = resultquery + "$" + resultofselectquery.getParentName() + "#" + resultofselectquery.getColumn();
                                    targetTableName = targetTableName + "$" + targetTableName + "#" + resultofselectquery.getColumn();
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

            deletelineageMap.put(targetunion, targetTableName);
            addUpdatemapspecrow(deletelineageMap);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void getLineageforUnion(String unionQuery, String dbvendor) {
        String targettableName = "";
        String insertselecttarget = "";
        String outputresult = "";
        int i = 0;
        Map<String, String> unionupdatespec = new LinkedHashMap<>();
        Dlineage resultofselectQuery = getDataflowObject(unionQuery, dbvendor);
        for (Object Dlineageobject : resultofselectQuery.getColumnOrTableOrResultset()) {
            if (Dlineageobject instanceof Dlineage.Relation) {
                if (((Dlineage.Relation) Dlineageobject).getType().equals("dataflow")) {
                    for (Dlineage.Relation.Source subselecttarget : ((Dlineage.Relation) Dlineageobject).getSource()) {
                        if (subselecttarget.getParentName().equals("OUTPUT-OF-UNION")) {
                            for (Dlineage.Relation.Target subselectsource : ((Dlineage.Relation) Dlineageobject).getTarget()) {
                                targettableName = subselectsource.getParentName();
                                break;
                            }
                        }

                    }

                    for (Dlineage.Relation.Source unionsource : ((Dlineage.Relation) Dlineageobject).getSource()) {
                        if (unionsource.getParentName().equals("OUTPUT-OF-UNION")) {
                            if (i == 0) {
                                outputresult = targettableName + "#" + unionsource.getColumn();
                            } else {
                                outputresult = outputresult + "$" + targettableName + "#" + unionsource.getColumn();
                            }
                            i++;
                        }
                        if (unionsource.getParentName().equals("INSERT-SELECT")) {
                            if ("".equals(insertselecttarget)) {
                                insertselecttarget = unionsource.getParentName() + "#" + unionsource.getColumn();
                            } else {
                                insertselecttarget = insertselecttarget + "$" + unionsource.getParentName() + "#" + unionsource.getColumn();
                            }

                        }
                    }

                }

            }
        }
        updatespecrow(mapspeclist, targettableName);
        unionupdatespec.put(outputresult, insertselecttarget);
        addUpdatemapspecrow(unionupdatespec);
    }

    public static void getLineageforUnionInsert(String unionQuery, String dbvendor) {
        String targetTablename = "";
        String targetColumnName = "";
        String sourceTableName = "";
        String sourceColumnName = "";
        String sourceSide = "";
        String targetSide = "";
        String insertselecttablename = "";
        String insertselectInput = "";
        String insertselect = "";

        Map<String, String> insertunionmap = new LinkedHashMap<>();
        Dlineage resultofselectQuery = getDataflowObject(unionQuery, dbvendor);
        int i = 0;
        int j = 0;
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

                }
            }
        }
        updatespecrow(mapspeclist, "INSERT-SELECT");
        insertunionmap.put(sourceSide, targetSide);
//         insertunionmap.put(targetSide,insertselectInput);
        insertunionmap.put(insertselectInput, insertselect);
        addUpdatemapspecrow(insertunionmap);
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

    public static void updatespecrowforselectQuery(ArrayList<MappingSpecificationRow> mapspeclist, String tablename) {
        Iterator<MappingSpecificationRow> iter = mapspeclist.iterator();
        while (iter.hasNext()) {
            MappingSpecificationRow row = iter.next();
            if (row.getTargetTableName().equals("RESULT_OF_SELECT-QUERY")) {
                row.setTargetTableName("OUTPUT-OF-SELECT-QUERY");
            }
        }
    }

    public static void updatespecrowforresult(ArrayList<MappingSpecificationRow> mapspeclist) {
        Iterator<MappingSpecificationRow> iter = mapspeclist.iterator();
        while (iter.hasNext()) {
            MappingSpecificationRow row = iter.next();
            if (row.getTargetTableName().equals("RESULT_OF_SELECT-QUERY") || row.getTargetTableName().equals("RESULT_OF_SELECT-QUERY-1") || row.getTargetTableName().equals("RESULT_OF_SELECT-QUERY-2") || row.getTargetTableName().equals("RESULT_OF_SELECT-QUERY-3")) {
                row.setTargetTableName("RESULT_OF_SELECT-QUERY");
            }
            if (row.getSourceTableName().equals("RESULT_OF_SELECT-QUERY") || row.getSourceTableName().equals("RESULT_OF_SELECT-QUERY-1") || row.getSourceTableName().equals("RESULT_OF_SELECT-QUERY-2") || row.getSourceTableName().equals("RESULT_OF_SELECT-QUERY-3")) {
                row.setSourceTableName("RESULT_OF_SELECT-QUERY");
            }
        }
    }

    public static Map<String, String> getTableColumnList(List<String> subselectquerylist, String dbvendor) {
        Map<String, String> sourcebrmap = new LinkedHashMap<>();

        //   for (String query : subselectquerylist) {
        String query = subselectquerylist.get(subselectquerylist.size() - 1);
        ArrayList<String> columnList = null;
        Dlineage dataflowobject = getDataflowObject(query, dbvendor);
        for (Object Dlineageobject : dataflowobject.getColumnOrTableOrResultset()) {
            if (Dlineageobject instanceof Dlineage.Resultset) {
                columnList = new ArrayList<>();
                String tableName = ((Dlineage.Resultset) Dlineageobject).getName();
                List<Column> resultsetcolumn = ((Dlineage.Resultset) Dlineageobject).getColumn();
                for (Column column : resultsetcolumn) {
                    columnList.add(column.getName());
                }
                specTableColumnDetails.put(tableName, columnList);
            }
            if (Dlineageobject instanceof Dlineage.Table) {
                columnList = new ArrayList<>();
                String tableName = ((Dlineage.Table) Dlineageobject).getName();
                List<Column> resultsetcolumn = ((Dlineage.Table) Dlineageobject).getColumn();
                for (Column column : resultsetcolumn) {
                    columnList.add(column.getName());
                }
                specTableColumnDetails.put(tableName, columnList);
            }

        }

        return sourcebrmap;

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

    private static HashMap<String, String> getTableAliasMap(List<String> querylist, String dbvender) {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        dbVendor = EDbVendor.dbvoracle;
        sqlparser = new TGSqlParser(dbVendor);

        for (String query : querylist) {

            sqlparser.sqltext = query;
            int ret = sqlparser.parse();
            if (ret == 0) {
                TCustomSqlStatement stmnt = sqlparser.sqlstatements.get(0);
                TTableList list = stmnt.getTables();
                for (int i = 0; i < list.size(); i++) {
                    TTable table = list.getTable(i);

                    String tablename = table.getFullName();
                    String tableAliasName = table.getAliasName();
                    if (tablename == null) {
                        if (tableAliasName.contains(".")) {
                            tableAliasName = tableAliasName.split("\\.")[1];
                            tablename = "OUTPUT-OF-" + tableAliasName.toUpperCase();
                        } else {
                            tablename = "OUTPUT-OF-" + tableAliasName.toUpperCase();

                        }
                    }
                    if (!"".equals(tableAliasName)) {
                        hashMap.put(tableAliasName, tablename);
                    }
                }

            }
        }
        return hashMap;

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
                if (!"".equals(AliasName)) {
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

    public static void updatebusinessRuleForUpdate(Map<String, String> expressionMap, ArrayList<MappingSpecificationRow> speclist) {
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
                    if (updateTargetTableName.contains(".")) {
                        if (targetTableName.equalsIgnoreCase(updateTargetTableName.split("\\.")[1])) {
                            targetTableName = "UPDATE-SET";
                        }
                    }

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

                if (updateTargetTableName.contains(".")) {
                    if (targetTableName.equalsIgnoreCase(updateTargetTableName.split("\\.")[1])) {
                        targetTableName = "UPDATE-SET";
                    }
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

    public static void updatebusinessRuleForDelete(Map<String, String> expressionMap, ArrayList<MappingSpecificationRow> speclist) {
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
                if (listofColumns.size() == 0) {
                    continue;
                }
                for (int target_i = 0; target_i < listofColumns.size(); target_i++) {
                    targetTableName = listofColumns.get(target_i).split("\\$")[0];
//                     if (updateTargetTableName.contains(".")) {
//                        if (targetTableName.equalsIgnoreCase(updateTargetTableName.split("\\.")[1])) {
//                            targetTableName = "DELETE-SELECT";
//                        }
//                    }

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
                    if (deleteTargetTableName.contains(".")) {
                        if (targetTableName.equalsIgnoreCase(deleteTargetTableName.split("\\.")[1])) {
                            targetTableName = "DELETE-SELECT";
                        }
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

                if (deleteTargetTableName.contains(".")) {
                    if (targetTableName.equalsIgnoreCase(deleteTargetTableName.split("\\.")[1])) {
                        targetTableName = "DELETE-SELECT";
                    }
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
                    if (getcolumns(targetcolumnarr,columnName)) {
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

    public static Map<String, String> getKeyvaluemap(Set<String> joinconditionslist) {

//        String joincondstring = StringUtils.join(joinconditionslist, "\n");
//        String whereConditions = StringUtils.join(whereCondition, "\n");
        int i = 1, j = 1;
//        for (String joins : joinconditionslist) {
//            keyvaluepair.put("JOINCONDITION_" + i, joins);
//            i++;
//        }

        for (String wherecond : whereCondition) {
            keyvaluepair.put("WHERECONDITION_" + j, wherecond);
            j++;
        }
//        keyvaluepair.put("JOINCONDITION", joincondstring);
//        keyvaluepair.put("WHERECONDITION", whereConditions);
        return keyvaluepair;
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

//    public static void getJoinStatements(String sqlquery, String dbvender) {
//        TCustomSqlStatement stmt = getselectstatement(sqlquery, dbvender);
//
//        for (int join_cond = 0; join_cond < stmt.getJoins().size(); join_cond++) {
//            if (stmt.getJoins().size() == 0) {
//                continue;
//            }
//            TJoin joincond = stmt.getJoins().getJoin(join_cond);
//            for (int j_cond = 0; j_cond < joincond.getJoinItems().size(); j_cond++) {
//                TJoinItem joinitem = joincond.getJoinItems().getJoinItem(j_cond);
//                joinconditions.add(joinitem.toString());
//                EJoinType jointype = joinitem.getJoinType();
//                System.out.println("jointypes----" + jointype);
//            }
//
//        }
//    }
    public static Set<String> getJoinStatementsfromList(Map<String, String> parentchildmap, String dbvender) {
        Set<String> jointypes = new LinkedHashSet<>();
        List<TCustomSqlStatement> statementList = getJoinStatementsfromMap(parentchildmap, dbvender);

        for (TCustomSqlStatement stmt : statementList) {
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

        List<String> extendedproperties = new LinkedList<>(keysmap);

        for (String val : extendedproperties) {
            String key = val.split("#")[0];
            String value = val.split("#")[1];
            keyvaluepair.put(key+" Join", value);

        }

    }

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
                flag=true;
                

            }

        }
        return flag;
    }
}
