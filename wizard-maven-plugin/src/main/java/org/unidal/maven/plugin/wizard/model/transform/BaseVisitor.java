package org.unidal.maven.plugin.wizard.model.transform;

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

public abstract class BaseVisitor implements IVisitor {
   @Override
   public void visitDatasource(Datasource datasource) {
   }

   @Override
   public void visitGroup(Group group) {
      for (Table table : group.getTables()) {
         visitTable(table);
      }
   }

   @Override
   public void visitJdbc(Jdbc jdbc) {
      if (jdbc.getDatasource() != null) {
         visitDatasource(jdbc.getDatasource());
      }

      for (Group group : jdbc.getGroups()) {
         visitGroup(group);
      }
   }

   @Override
   public void visitModel(Model model) {
   }

   @Override
   public void visitModule(Module module) {
      for (Page page : module.getPages()) {
         visitPage(page);
      }
   }

   @Override
   public void visitPage(Page page) {
   }

   @Override
   public void visitTable(Table table) {
   }

   @Override
   public void visitWebapp(Webapp webapp) {
      for (Module module : webapp.getModules()) {
         visitModule(module);
      }
   }

   @Override
   public void visitWizard(Wizard wizard) {
      if (wizard.getWebapp() != null) {
         visitWebapp(wizard.getWebapp());
      }

      for (Jdbc jdbc : wizard.getJdbcs()) {
         visitJdbc(jdbc);
      }

      for (Model model : wizard.getModels()) {
         visitModel(model);
      }
   }
}
