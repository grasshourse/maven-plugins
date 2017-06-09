package org.unidal.maven.plugin.wizard.model.transform;

import org.unidal.maven.plugin.wizard.model.entity.Datasource;
import org.unidal.maven.plugin.wizard.model.entity.Group;
import org.unidal.maven.plugin.wizard.model.entity.Jdbc;
import org.unidal.maven.plugin.wizard.model.entity.Model;
import org.unidal.maven.plugin.wizard.model.entity.Module;
import org.unidal.maven.plugin.wizard.model.entity.Page;
import org.unidal.maven.plugin.wizard.model.entity.Table;
import org.unidal.maven.plugin.wizard.model.entity.Webapp;
import org.unidal.maven.plugin.wizard.model.entity.Wizard;

public interface ILinker {

   public boolean onDatasource(Jdbc parent, Datasource datasource);

   public boolean onGroup(Jdbc parent, Group group);

   public boolean onJdbc(Wizard parent, Jdbc jdbc);

   public boolean onModel(Wizard parent, Model model);

   public boolean onModule(Webapp parent, Module module);

   public boolean onPage(Module parent, Page page);

   public boolean onTable(Group parent, Table table);

   public boolean onWebapp(Wizard parent, Webapp webapp);
}
