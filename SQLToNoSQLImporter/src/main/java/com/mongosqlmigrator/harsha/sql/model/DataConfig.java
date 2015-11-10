package com.mongosqlmigrator.harsha.sql.model;
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.*;

/**
 * <p>
 * Mapping for data-config.xml
 * </p>
 * <p/>
 * <p>
 * Refer to <a
 * href="http://wiki.apache.org/solr/DataImportHandler">http://wiki.apache.org/solr/DataImportHandler</a>
 * for more details.
 * </p>
 * <p/>
 * <b>This API is experimental and subject to change</b>
 *
 * @since solr 1.3
 */
public class DataConfig {
 

  public Document document;

  public List<Map<String, String >> functions = new ArrayList<Map<String ,String>>();

  public Script script;

  public Map<String, Properties> dataSources = new HashMap<String, Properties>();



  boolean isMultiThreaded = false;

  public static class Document {
    // TODO - remove from here and add it to entity
    public String deleteQuery;

    public List<Entity> entities = new ArrayList<Entity>();

    public String onImportStart, onImportEnd;

    public Document() {
    }

    public Document(Element element) throws IOException {
      this.deleteQuery = getStringAttribute(element, "deleteQuery", null);
      this.onImportStart = getStringAttribute(element, "onImportStart", null);
      this.onImportEnd = getStringAttribute(element, "onImportEnd", null);
      List<Element> l = getChildNodes(element, "entity");
      for (Element e : l)
        entities.add(new Entity(e));
    }
  }

  public static class Entity {
    public String name;

    public String pk;

    public String pkMappingFromSchema;

    public String dataSource;

    public Map<String, String> allAttributes;

    public String proc;

    public String docRoot;

    public boolean isDocRoot = false;

    public List<Field> fields = new ArrayList<Field>();

    public List<Map<String, String>> allFieldsList = new ArrayList<Map<String, String>>();

    public List<Entity> entities;

    public Entity parentEntity;


    public DataSource dataSrc;

    public Map<String, List<Field>> colNameVsField = new HashMap<String, List<Field>>();

    public Entity() {
    }

    public Entity(Element element) throws IOException {
      name = getStringAttribute(element, NAME, null);
    
      if(name.indexOf(".") != -1){
        throw new IOException();
      }      
 
      pk = getStringAttribute(element, "pk", null);
      docRoot = getStringAttribute(element, ROOT_ENTITY, null);
      proc = getStringAttribute(element, PROCESSOR, null);
      dataSource = getStringAttribute(element, DATA_SRC, null);
      allAttributes = getAllAttributes(element);
      List<Element> n = getChildNodes(element, "field");
      for (Element elem : n)  {
        Field field = new Field(elem);
        fields.add(field);
        List<Field> l = colNameVsField.get(field.column);
        if(l == null) l = new ArrayList<Field>();
        boolean alreadyFound = false;
        for (Field f : l) {
          if(f.getName().equals(field.getName())) {
            alreadyFound = true;
            break;
          }
        }
        if(!alreadyFound) l.add(field);
        colNameVsField.put(field.column, l);
      }
      n = getChildNodes(element, "entity");
      if (!n.isEmpty())
        entities = new ArrayList<Entity>();
      for (Element elem : n)
        entities.add(new Entity(elem));

    }

  
    public String getPk(){
      return pk == null ? pkMappingFromSchema : pk;
    }

    public String getSchemaPk(){
      return pkMappingFromSchema != null ? pkMappingFromSchema : pk;
    }
  }

  public static class Script {
    public String language;

    public String text;

    public Script() {
    }

    public Script(Element e) {
      this.language = getStringAttribute(e, "language", "JavaScript");
      StringBuilder buffer = new StringBuilder();
      String script = getTxt(e, buffer);
      if (script != null)
        this.text = script.trim();
    }
  }

  public static class Field {

    public String column;

    public String name;

    public String defaultValue;

    public Float boost = 1.0f;

    public boolean toWrite = true;

    public boolean multiValued = false;

    boolean dynamicName;


    public Map<String, String> allAttributes = new HashMap<String, String>() {
      /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	@Override
      public String put(String key, String value) {
        if (super.containsKey(key))
          return super.get(key);
        return super.put(key, value);
      }
    };

    public Field() {
    }

    public Field(Element e) throws IOException {
      this.name = getStringAttribute(e, "name", null);
      this.column = getStringAttribute(e, "column", null);
      if (column == null) {
        throw new IOException();
      }
      this.boost = Float.parseFloat(getStringAttribute(e, "boost", "1.0f"));
      allAttributes.putAll(getAllAttributes(e));
    }

    public String getName() {
      return name == null ? column : name;
    }

    public Entity entity;

  }

  public void readFromXml(Element e) throws IOException {
    List<Element> n = getChildNodes(e, "document");
    if (n.isEmpty()) {
      throw new IOException();
    }
    document = new Document(n.get(0));

    n = getChildNodes(e, SCRIPT);
    if (!n.isEmpty()) {
      script = new Script(n.get(0));
    }

    // Add the provided evaluators
    n = getChildNodes(e, FUNCTION);
    if (!n.isEmpty()) {
      for (Element element : n) {
        String func = getStringAttribute(element, NAME, null);
        String clz = getStringAttribute(element, CLASS, null);
        if (func == null || clz == null){
          throw new IOException();
        } else {
          functions.add(getAllAttributes(element));
        }
      }
    }
    n = getChildNodes(e, DATA_SRC);
    if (!n.isEmpty()) {
      for (Element element : n) {
        Properties p = new Properties();
        HashMap<String, String> attrs = getAllAttributes(element);
        for (Map.Entry<String, String> entry : attrs.entrySet()) {
          p.setProperty(entry.getKey(), entry.getValue());
        }
        dataSources.put(p.getProperty("name"), p);
      }
    }
    if(dataSources.get(null) == null){
      for (Properties properties : dataSources.values()) {
        dataSources.put(null,properties);
        break;        
      } 
    }
  }

  private static String getStringAttribute(Element e, String name, String def) {
    String r = e.getAttribute(name);
    if (r == null || "".equals(r.trim()))
      r = def;
    return r;
  }

  private static HashMap<String, String> getAllAttributes(Element e) {
    HashMap<String, String> m = new HashMap<String, String>();
    NamedNodeMap nnm = e.getAttributes();
    for (int i = 0; i < nnm.getLength(); i++) {
      m.put(nnm.item(i).getNodeName(), nnm.item(i).getNodeValue());
    }
    return m;
  }

  public static String getTxt(Node elem, StringBuilder buffer) {
    if (elem.getNodeType() != Node.CDATA_SECTION_NODE) {
      NodeList childs = elem.getChildNodes();
      for (int i = 0; i < childs.getLength(); i++) {
        Node child = childs.item(i);
        short childType = child.getNodeType();
        if (childType != Node.COMMENT_NODE
                && childType != Node.PROCESSING_INSTRUCTION_NODE) {
          getTxt(child, buffer);
        }
      }
    } else {
      buffer.append(elem.getNodeValue());
    }

    return buffer.toString();
  }

  public static List<Element> getChildNodes(Element e, String byName) {
    List<Element> result = new ArrayList<Element>();
    NodeList l = e.getChildNodes();
    for (int i = 0; i < l.getLength(); i++) {
      if (e.equals(l.item(i).getParentNode())
              && byName.equals(l.item(i).getNodeName()))
        result.add((Element) l.item(i));
    }
    return result;
  }



  public static final String SCRIPT = "script";

  public static final String NAME = "name";

  public static final String PROCESSOR = "processor";

  /**
   * @deprecated use IMPORTER_NS_SHORT instead
   */
  @Deprecated
  public static final String IMPORTER_NS = "dataimporter";

  public static final String IMPORTER_NS_SHORT = "dih";

  public static final String ROOT_ENTITY = "rootEntity";

  public static final String FUNCTION = "function";

  public static final String CLASS = "class";

  public static final String DATA_SRC = "dataSource";

}