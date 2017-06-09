package org.unidal.maven.plugin.wizard.model.transform;

import static org.unidal.maven.plugin.wizard.model.Constants.ATTR_CAT;
import static org.unidal.maven.plugin.wizard.model.Constants.ATTR_DEFAULT;
import static org.unidal.maven.plugin.wizard.model.Constants.ATTR_JSTL;
import static org.unidal.maven.plugin.wizard.model.Constants.ATTR_LAYOUT;
import static org.unidal.maven.plugin.wizard.model.Constants.ATTR_MODULE;
import static org.unidal.maven.plugin.wizard.model.Constants.ATTR_NAME;
import static org.unidal.maven.plugin.wizard.model.Constants.ATTR_PACKAGE;
import static org.unidal.maven.plugin.wizard.model.Constants.ATTR_PATH;
import static org.unidal.maven.plugin.wizard.model.Constants.ATTR_PLUGIN_MANAGEMENT;
import static org.unidal.maven.plugin.wizard.model.Constants.ATTR_STANDALONE;
import static org.unidal.maven.plugin.wizard.model.Constants.ATTR_TEMPLATE;
import static org.unidal.maven.plugin.wizard.model.Constants.ATTR_TITLE;
import static org.unidal.maven.plugin.wizard.model.Constants.ATTR_VIEW;
import static org.unidal.maven.plugin.wizard.model.Constants.ATTR_WEBRES;
import static org.unidal.maven.plugin.wizard.model.Constants.ELEMENT_DESCRIPTION;
import static org.unidal.maven.plugin.wizard.model.Constants.ELEMENT_DRIVER;
import static org.unidal.maven.plugin.wizard.model.Constants.ELEMENT_PASSWORD;
import static org.unidal.maven.plugin.wizard.model.Constants.ELEMENT_PROPERTIES;
import static org.unidal.maven.plugin.wizard.model.Constants.ELEMENT_SAMPLE_MODEL;
import static org.unidal.maven.plugin.wizard.model.Constants.ELEMENT_URL;
import static org.unidal.maven.plugin.wizard.model.Constants.ELEMENT_USER;
import static org.unidal.maven.plugin.wizard.model.Constants.ENTITY_DATASOURCE;
import static org.unidal.maven.plugin.wizard.model.Constants.ENTITY_GROUP;
import static org.unidal.maven.plugin.wizard.model.Constants.ENTITY_JDBC;
import static org.unidal.maven.plugin.wizard.model.Constants.ENTITY_MODEL;
import static org.unidal.maven.plugin.wizard.model.Constants.ENTITY_MODULE;
import static org.unidal.maven.plugin.wizard.model.Constants.ENTITY_PAGE;
import static org.unidal.maven.plugin.wizard.model.Constants.ENTITY_TABLE;
import static org.unidal.maven.plugin.wizard.model.Constants.ENTITY_WEBAPP;
import static org.unidal.maven.plugin.wizard.model.Constants.ENTITY_WIZARD;

import java.lang.reflect.Array;
import java.util.Collection;

import org.unidal.maven.plugin.wizard.model.IEntity;
import org.unidal.maven.plugin.wizard.model.IVisitor;
import org.unidal.maven.plugin.wizard.model.entity.Datasource;
import org.unidal.maven.plugin.wizard.model.entity.Group;
import org.unidal.maven.plugin.wizard.model.entity.Jdbc;
import org.unidal.maven.plugin.wizard.model.entity.Model;
import org.unidal.maven.plugin.wizard.model.entity.Module;
import org.unidal.maven.plugin.wizard.model.entity.Page;
import org.unidal.maven.plugin.wizard.model.entity.Table;
import org.unidal.maven.plugin.wizard.model.entity.Webapp;
import org.unidal.maven.plugin.wizard.model.entity.Wizard;

public class DefaultXmlBuilder implements IVisitor {

   private IVisitor m_visitor = this;

   private int m_level;

   private StringBuilder m_sb;

   private boolean m_compact;

   public DefaultXmlBuilder() {
      this(false);
   }

   public DefaultXmlBuilder(boolean compact) {
      this(compact, new StringBuilder(4096));
   }

   public DefaultXmlBuilder(boolean compact, StringBuilder sb) {
      m_compact = compact;
      m_sb = sb;
      m_sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n");
   }

   public String buildXml(IEntity<?> entity) {
      entity.accept(m_visitor);
      return m_sb.toString();
   }

   protected void endTag(String name) {
      m_level--;

      indent();
      m_sb.append("</").append(name).append(">\r\n");
   }

   protected String escape(Object value) {
      return escape(value, false);
   }
   
   protected String escape(Object value, boolean text) {
      if (value == null) {
         return null;
      }

      String str = toString(value);
      int len = str.length();
      StringBuilder sb = new StringBuilder(len + 16);

      for (int i = 0; i < len; i++) {
         final char ch = str.charAt(i);

         switch (ch) {
         case '<':
            sb.append("&lt;");
            break;
         case '>':
            sb.append("&gt;");
            break;
         case '&':
            sb.append("&amp;");
            break;
         case '"':
            if (!text) {
               sb.append("&quot;");
               break;
            }
         default:
            sb.append(ch);
            break;
         }
      }

      return sb.toString();
   }
   
   protected void indent() {
      if (!m_compact) {
         for (int i = m_level - 1; i >= 0; i--) {
            m_sb.append("   ");
         }
      }
   }

   protected void startTag(String name) {
      startTag(name, false, null);
   }
   
   protected void startTag(String name, boolean closed, java.util.Map<String, String> dynamicAttributes, Object... nameValues) {
      startTag(name, null, closed, dynamicAttributes, nameValues);
   }

   protected void startTag(String name, java.util.Map<String, String> dynamicAttributes, Object... nameValues) {
      startTag(name, null, false, dynamicAttributes, nameValues);
   }

   protected void startTag(String name, Object text, boolean closed, java.util.Map<String, String> dynamicAttributes, Object... nameValues) {
      indent();

      m_sb.append('<').append(name);

      int len = nameValues.length;

      for (int i = 0; i + 1 < len; i += 2) {
         Object attrName = nameValues[i];
         Object attrValue = nameValues[i + 1];

         if (attrValue != null) {
            m_sb.append(' ').append(attrName).append("=\"").append(escape(attrValue)).append('"');
         }
      }

      if (dynamicAttributes != null) {
         for (java.util.Map.Entry<String, String> e : dynamicAttributes.entrySet()) {
            m_sb.append(' ').append(e.getKey()).append("=\"").append(escape(e.getValue())).append('"');
         }
      }

      if (text != null && closed) {
         m_sb.append('>');
         m_sb.append(escape(text, true));
         m_sb.append("</").append(name).append(">\r\n");
      } else {
         if (closed) {
            m_sb.append('/');
         } else {
            m_level++;
         }
   
         m_sb.append(">\r\n");
      }
   }

   @SuppressWarnings("unchecked")
   protected String toString(Object value) {
      if (value instanceof String) {
         return (String) value;
      } else if (value instanceof Collection) {
         Collection<Object> list = (Collection<Object>) value;
         StringBuilder sb = new StringBuilder(32);
         boolean first = true;

         for (Object item : list) {
            if (first) {
               first = false;
            } else {
               sb.append(',');
            }

            if (item != null) {
               sb.append(item);
            }
         }

         return sb.toString();
      } else if (value.getClass().isArray()) {
         int len = Array.getLength(value);
         StringBuilder sb = new StringBuilder(32);
         boolean first = true;

         for (int i = 0; i < len; i++) {
            Object item = Array.get(value, i);

            if (first) {
               first = false;
            } else {
               sb.append(',');
            }

            if (item != null) {
               sb.append(item);
            }
         }
		
         return sb.toString();
      }
 
      return String.valueOf(value);
   }

   protected void tagWithText(String name, String text, Object... nameValues) {
      if (text == null) {
         return;
      }
      
      indent();

      m_sb.append('<').append(name);

      int len = nameValues.length;

      for (int i = 0; i + 1 < len; i += 2) {
         Object attrName = nameValues[i];
         Object attrValue = nameValues[i + 1];

         if (attrValue != null) {
            m_sb.append(' ').append(attrName).append("=\"").append(escape(attrValue)).append('"');
         }
      }

      m_sb.append(">");
      m_sb.append(escape(text, true));
      m_sb.append("</").append(name).append(">\r\n");
   }

   protected void element(String name, String text, String defaultValue, boolean escape) {
      if (text == null || text.equals(defaultValue)) {
         return;
      }
      
      indent();
      
      m_sb.append('<').append(name).append(">");
      
      if (escape) {
         m_sb.append(escape(text, true));
      } else {
         m_sb.append("<![CDATA[").append(text).append("]]>");
      }
      
      m_sb.append("</").append(name).append(">\r\n");
   }

   @Override
   public void visitDatasource(Datasource datasource) {
      startTag(ENTITY_DATASOURCE, null, ATTR_NAME, datasource.getName());

      element(ELEMENT_DRIVER, datasource.getDriver(), null,  true);

      element(ELEMENT_URL, datasource.getUrl(), null,  true);

      element(ELEMENT_USER, datasource.getUser(), null,  true);

      element(ELEMENT_PASSWORD, datasource.getPassword(), null,  true);

      element(ELEMENT_PROPERTIES, datasource.getProperties(), null,  true);

      endTag(ENTITY_DATASOURCE);
   }

   @Override
   public void visitGroup(Group group) {
      startTag(ENTITY_GROUP, null, ATTR_NAME, group.getName(), ATTR_PACKAGE, group.getPackage());

      if (!group.getTables().isEmpty()) {
         for (Table table : group.getTables()) {
            table.accept(m_visitor);
         }
      }

      endTag(ENTITY_GROUP);
   }

   @Override
   public void visitJdbc(Jdbc jdbc) {
      startTag(ENTITY_JDBC, null, ATTR_PACKAGE, jdbc.getPackage(), ATTR_NAME, jdbc.getName());

      if (jdbc.getDatasource() != null) {
         jdbc.getDatasource().accept(m_visitor);
      }

      if (!jdbc.getGroups().isEmpty()) {
         for (Group group : jdbc.getGroups()) {
            group.accept(m_visitor);
         }
      }

      endTag(ENTITY_JDBC);
   }

   @Override
   public void visitModel(Model model) {
      startTag(ENTITY_MODEL, null, ATTR_PACKAGE, model.getPackage(), ATTR_NAME, model.getName());

      element(ELEMENT_SAMPLE_MODEL, model.getSampleModel(), null,  true);

      endTag(ENTITY_MODEL);
   }

   @Override
   public void visitModule(Module module) {
      startTag(ENTITY_MODULE, null, ATTR_NAME, module.getName(), ATTR_PATH, module.getPath(), ATTR_DEFAULT, module.getDefault(), ATTR_PACKAGE, module.getPackage());

      if (!module.getPages().isEmpty()) {
         for (Page page : module.getPages()) {
            page.accept(m_visitor);
         }
      }

      endTag(ENTITY_MODULE);
   }

   @Override
   public void visitPage(Page page) {
      startTag(ENTITY_PAGE, null, ATTR_NAME, page.getName(), ATTR_TITLE, page.getTitle(), ATTR_DEFAULT, page.getDefault(), ATTR_PACKAGE, page.getPackage(), ATTR_PATH, page.getPath(), ATTR_VIEW, page.getView(), ATTR_STANDALONE, page.getStandalone(), ATTR_TEMPLATE, page.getTemplate());

      element(ELEMENT_DESCRIPTION, page.getDescription(), null,  true);

      endTag(ENTITY_PAGE);
   }

   @Override
   public void visitTable(Table table) {
      startTag(ENTITY_TABLE, true, null, ATTR_NAME, table.getName());
   }

   @Override
   public void visitWebapp(Webapp webapp) {
      startTag(ENTITY_WEBAPP, null, ATTR_PACKAGE, webapp.getPackage(), ATTR_NAME, webapp.getName(), ATTR_MODULE, webapp.getModule(), ATTR_WEBRES, webapp.getWebres(), ATTR_CAT, webapp.getCat(), ATTR_PLUGIN_MANAGEMENT, webapp.getPluginManagement(), ATTR_JSTL, webapp.getJstl(), ATTR_LAYOUT, webapp.getLayout());

      if (!webapp.getModules().isEmpty()) {
         for (Module module : webapp.getModules()) {
            module.accept(m_visitor);
         }
      }

      endTag(ENTITY_WEBAPP);
   }

   @Override
   public void visitWizard(Wizard wizard) {
      startTag(ENTITY_WIZARD, null, ATTR_PACKAGE, wizard.getPackage());

      if (wizard.getWebapp() != null) {
         wizard.getWebapp().accept(m_visitor);
      }

      if (!wizard.getJdbcs().isEmpty()) {
         for (Jdbc jdbc : wizard.getJdbcs()) {
            jdbc.accept(m_visitor);
         }
      }

      if (!wizard.getModels().isEmpty()) {
         for (Model model : wizard.getModels()) {
            model.accept(m_visitor);
         }
      }

      endTag(ENTITY_WIZARD);
   }
}
