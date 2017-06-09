package org.unidal.maven.plugin.wizard.model;

import org.unidal.maven.plugin.wizard.model.entity.Datasource;
import org.unidal.maven.plugin.wizard.model.entity.Group;
import org.unidal.maven.plugin.wizard.model.entity.Jdbc;
import org.unidal.maven.plugin.wizard.model.entity.Model;
import org.unidal.maven.plugin.wizard.model.entity.Module;
import org.unidal.maven.plugin.wizard.model.entity.Page;
import org.unidal.maven.plugin.wizard.model.entity.Table;
import org.unidal.maven.plugin.wizard.model.entity.Webapp;
import org.unidal.maven.plugin.wizard.model.entity.Wizard;

public interface IVisitor {

   public void visitDatasource(Datasource datasource);

   public void visitGroup(Group group);

   public void visitJdbc(Jdbc jdbc);

   public void visitModel(Model model);

   public void visitModule(Module module);

   public void visitPage(Page page);

   public void visitTable(Table table);

   public void visitWebapp(Webapp webapp);

   public void visitWizard(Wizard wizard);
}
