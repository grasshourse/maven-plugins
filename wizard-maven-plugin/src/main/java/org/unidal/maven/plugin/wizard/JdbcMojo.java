package org.unidal.maven.plugin.wizard;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.unidal.codegen.meta.TableMeta;
import org.unidal.maven.plugin.common.PropertyProviders;
import org.unidal.maven.plugin.common.PropertyProviders.IValidator;
import org.unidal.maven.plugin.wizard.dom.PomFileBuilder;
import org.unidal.maven.plugin.wizard.model.entity.Datasource;
import org.unidal.maven.plugin.wizard.model.entity.Group;
import org.unidal.maven.plugin.wizard.model.entity.Jdbc;
import org.unidal.maven.plugin.wizard.model.entity.Table;
import org.unidal.maven.plugin.wizard.model.entity.Wizard;
import org.unidal.maven.plugin.wizard.model.transform.BaseVisitor;
import org.unidal.maven.plugin.wizard.model.transform.DefaultSaxParser;
import org.xml.sax.SAXException;

import com.site.helper.Files;
import com.site.helper.Transformers;
import com.site.helper.Transformers.IBuilder;

/**
 * DAL Metadata generator for JDBC
 * 
 * @goal jdbc
 * @author Frankie Wu
 */
public class JdbcMojo extends AbstractMojo {
   /**
    * Current project
    * 
    * @parameter expression="${project}"
    * @required
    * @readonly
    */
   protected MavenProject m_project;

   /**
    * Table meta component
    * 
    * @component
    * @required
    * @readonly
    */
   protected TableMeta m_meta;

   /**
    * Current project base directory
    * 
    * @parameter expression="${basedir}"
    * @required
    * @readonly
    */
   protected File baseDir;

   /**
    * @parameter expression="${outputDir}"
    *            default-value="${basedir}/src/main/resources/META-INF/dal/jdbc"
    * @required
    */
   protected String outputDir;

   /**
    * Location of wizard.xml file
    * 
    * @parameter expression="${manifest}" default-value=
    *            "${basedir}/src/main/resources/META-INF/wizard/jdbc/wizard.xml"
    * @required
    */
   protected String wizard;

   private Connection m_conn;

   private Jdbc m_jdbc;

   protected Jdbc buildWizard(File wizardFile) throws IOException, SAXException {
      Wizard wizard;

      if (wizardFile.isFile()) {
         String content = Files.forIO().readFrom(wizardFile, "utf-8");

         wizard = DefaultSaxParser.parse(content);
      } else {
         String packageName = getPackageName();

         wizard = new Wizard();
         wizard.setPackage(packageName);
      }

      WizardBuilder builder = new WizardBuilder();

      wizard.accept(builder);
      m_jdbc = builder.getJdbc();
      m_conn = builder.getConnection();
      Files.forIO().writeTo(wizardFile, wizard.toString());
      return builder.getJdbc();
   }

   public void execute() throws MojoExecutionException, MojoFailureException {
      try {
         final File wizardFile = getFile(wizard);
         Jdbc jdbc = buildWizard(wizardFile);

         modifyPomFile(m_project.getFile(), m_jdbc);

         for (Group group : jdbc.getGroups()) {
            generateModel(group);
         }
      } catch (Exception e) {
         throw new MojoExecutionException("Error when generating DAL meta: " + e, e);
      } finally {
         if (m_conn != null) {
            try {
               m_conn.close();
            } catch (SQLException e) {
               // ignore it
            }
         }
      }
   }

   protected void generateModel(Group group) throws SQLException, IOException {
      String groupName = group.getName();
      List<Table> tables = group.getTables();
      Element entities = new Element("entities");

      for (Table table : tables) {
         Element entity = m_meta.getTableMeta(m_conn.getMetaData(), table.getName());

         entities.addContent(entity);
      }

      resolveAliasConfliction(entities);

      File outDir = getFile(outputDir);
      File outFile = new File(outDir, groupName + "-codegen.xml");

      if (!outDir.exists()) {
         outDir.mkdirs();
      }

      saveFile(new Document(entities), outFile);

      File modelFile = new File(outDir, groupName + "-dal.xml");

      if (!modelFile.exists()) {
         Document model = m_meta.getModel(m_jdbc.getPackage());

         saveFile(model, modelFile);
      }

      File manifestFile = new File(outDir, groupName + "-manifest.xml");

      if (!manifestFile.exists()) {
         Document manifest = m_meta.getManifest(outFile.getName(), modelFile.getName());

         saveFile(manifest, manifestFile);
      }
   }

   protected File getFile(String path) {
      File file;

      if (path.startsWith("/") || path.indexOf(':') > 0) {
         file = new File(path);
      } else {
         file = new File(baseDir, path);
      }

      return file;
   }

   protected String getPackageName() {
      String groupId = m_project.getGroupId();
      String artifactId = m_project.getArtifactId();
      int index = artifactId.lastIndexOf('-');
      String packageName = (groupId + "." + artifactId.substring(index + 1) + ".dal").replace('-', '.');

      packageName = PropertyProviders.fromConsole().forString("package", "Please input package name:", packageName, null);
      return packageName;
   }

   protected void modifyPomFile(File pomFile, Jdbc jdbc) throws JDOMException, IOException {
      Document doc = new SAXBuilder().build(pomFile);
      Element root = doc.getRootElement();
      PomFileBuilder b = new PomFileBuilder();
      Element dependencies = b.findOrCreateChild(root, "dependencies");

      if (!b.checkDependency(dependencies, "com.site.dal", "dal-jdbc", "1.1.5", null)) {
         b.checkDependency(dependencies, "mysql", "mysql-connector-java", "5.1.20", "runtime");
      }

      Element build = b.findOrCreateChild(root, "build", null, "dependencies");
      Element plugins = b.findOrCreateChild(build, "plugins");
      Element codegenPlugin = b.checkPlugin(plugins, "org.unidal.maven.plugins", "codegen-maven-plugin", "1.1.7");
      Element codegenGenerate = b.checkPluginExecution(codegenPlugin, "dal-jdbc", "generate-sources", "generate dal jdbc model");
      Element codegenGenerateConfiguration = b.findOrCreateChild(codegenGenerate, "configuration");
      StringBuilder manifest = new StringBuilder();

      for (Group group : jdbc.getGroups()) {
         manifest.append(String.format("${basedir}/src/main/resources/META-INF/dal/jdbc/%s-manifest.xml\r\n", group.getName()));
      }

      b.findOrCreateChild(codegenGenerateConfiguration, "manifest").setText(manifest.toString());

      if (b.isModified()) {
         saveXml(doc, pomFile);
         getLog().info(String.format("Added dependencies to POM file(%s).", pomFile));
         getLog().info("You need run following command to setup eclipse environment:");
         getLog().info("   mvn eclipse:clean eclipse:eclipse");
      }
   }

   @SuppressWarnings("unchecked")
   protected void resolveAliasConfliction(Element entities) {
      Map<String, Integer> map = new HashMap<String, Integer>();
      List<Element> children = entities.getChildren("entity");

      for (Element entity : children) {
         String alias = entity.getAttributeValue("alias");
         Integer count = map.get(alias);

         if (count == null) {
            map.put(alias, 1);
         } else {
            count++;
            map.put(alias, count);
            entity.setAttribute("alias", alias + (count));
         }
      }
   }

   protected void saveFile(Document codegen, File file) throws IOException {
      Format format = Format.getPrettyFormat();
      XMLOutputter outputter = new XMLOutputter(format);
      FileWriter writer = new FileWriter(file);

      try {
         outputter.output(codegen, writer);
         getLog().info("File " + file.getCanonicalPath() + " generated.");
      } finally {
         writer.close();
      }
   }

   protected void saveXml(Document doc, File file) throws IOException {
      File parent = file.getCanonicalFile().getParentFile();

      if (!parent.exists()) {
         parent.mkdirs();
      }

      Format format = Format.getPrettyFormat().setIndent("   ");
      XMLOutputter outputter = new XMLOutputter(format);
      FileWriter writer = new FileWriter(file);

      try {
         outputter.output(doc, writer);
         getLog().info("File " + file.getCanonicalPath() + " generated.");
      } finally {
         writer.close();
      }
   }

   static class WizardBuilder extends BaseVisitor {
      private Jdbc m_jdbc;

      private Connection m_conn;

      private List<String> getAvailableTableNames(Jdbc jdbc) {
         final Set<String> existing = new HashSet<String>();

         for (Group group : jdbc.getGroups()) {
            for (Table table : group.getTables()) {
               existing.add(table.getName());
            }
         }

         try {
            List<String> tables = new ArrayList<String>();
            ResultSet rs = m_conn.getMetaData().getTables(null, null, "%", new String[] { "TABLE" });

            while (rs.next()) {
               String table = rs.getString("TABLE_NAME");

               if (!existing.contains(table)) {
                  tables.add(table);
               }
            }

            rs.close();
            Collections.sort(tables);
            return tables;
         } catch (SQLException e) {
            throw new RuntimeException(e);
         }
      }

      public Connection getConnection() {
         return m_conn;
      }

      public Jdbc getJdbc() {
         return m_jdbc;
      }

      private Connection setupConnection(Datasource ds) {
         Properties info = new Properties();

         info.put("user", ds.getUser());
         info.put("password", ds.getPassword());

         if (ds.getProperties() != null) {
            String[] pairs = ds.getProperties().split(Pattern.quote("&"));

            for (String pair : pairs) {
               int pos = pair.indexOf('=');

               if (pos > 0) {
                  info.put(pair.substring(0, pos), pair.substring(pos + 1));
               } else {
                  System.err.println("invalid property: " + pair + " ignored.");
               }
            }
         }

         try {
            Class.forName(ds.getDriver());

            return DriverManager.getConnection(ds.getUrl(), info);
         } catch (Exception e) {
            throw new RuntimeException("Can't get connection: " + e, e);
         }
      }

      @Override
      public void visitGroup(final Group group) {
         final List<String> existing = new ArrayList<String>();

         System.out.println("Existing tables in group(" + group.getName() + ") is: ");
         Transformers.forList().transform(group.getTables(), new IBuilder<Table, String>() {
            @Override
            public String build(Table table) {
               existing.add(table.getName());
               System.out.println(table.getName());
               return null;
            }
         });

         final List<String> availableTableNames = getAvailableTableNames(m_jdbc);

         System.out.println("Existing tables: " + existing);
         PropertyProviders.fromConsole().forString("table", "Select table name below, or 'end':", availableTableNames, null,
               new IValidator<String>() {
                  @Override
                  public boolean validate(String name) {
                     if ("end".equals(name)) {
                        return true;
                     } else if (availableTableNames.contains(name)) {
                        existing.add(name);
                        availableTableNames.remove(name);
                        System.out.println("Tables selected: " + existing);
                        group.findOrCreateTable(name);
                        return false; // for multiple selection
                     }

                     return false;
                  }
               });
      }

      @Override
      public void visitJdbc(Jdbc jdbc) {
         List<String> groupNames = Transformers.forList().transform(jdbc.getGroups(), new IBuilder<Group, String>() {
            @Override
            public String build(Group group) {
               return group.getName();
            }
         });
         String groupName = PropertyProviders.fromConsole().forString("group", "Select group name below or input a new one:",
               groupNames, null, null);
         Group group = jdbc.findOrCreateGroup(groupName);

         visitGroup(group);
      }

      @Override
      public void visitWizard(Wizard wizard) {
         List<String> names = Transformers.forList().transform(wizard.getJdbcs(), new IBuilder<Jdbc, String>() {
            @Override
            public String build(Jdbc jdbc) {
               return jdbc.getName();
            }
         });
         String name = PropertyProviders.fromConsole().forString("jdbc", "Select jdbc name below or input a new one:", names, null,
               null);
         Jdbc jdbc = wizard.findJdbc(name);

         if (jdbc == null) {
            Datasource ds = new Datasource();

            jdbc = new Jdbc(name);
            wizard.addJdbc(jdbc);

            jdbc.setPackage(PropertyProviders.fromConsole().forString("jdbc.package", "Jdbc Package:",
                  wizard.getPackage() + "." + name, null));
            jdbc.setDatasource(ds);

            ds.setDriver(PropertyProviders.fromConsole().forString("driver", "JDBC driver:", "com.mysql.jdbc.Driver", null));
            ds.setUrl(PropertyProviders.fromConsole().forString("url", "JDBC URL:", "jdbc:mysql://localhost:3306/cat", null));
            ds.setUser(PropertyProviders.fromConsole().forString("user", "User:", null, null));
            ds.setPassword(PropertyProviders.fromConsole().forString("password", "Password:", null, null));
            ds.setProperties(PropertyProviders.fromConsole().forString("connectionProperties", "Connection properties:",
                  "useUnicode=true&autoReconnect=true", null));
         }

         m_jdbc = jdbc;
         m_conn = setupConnection(jdbc.getDatasource());

         visitJdbc(jdbc);
      }
   }
}
