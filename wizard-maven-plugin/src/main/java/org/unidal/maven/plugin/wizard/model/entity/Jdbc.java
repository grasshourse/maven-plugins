package org.unidal.maven.plugin.wizard.model.entity;

import static org.unidal.maven.plugin.wizard.model.Constants.ATTR_NAME;
import static org.unidal.maven.plugin.wizard.model.Constants.ENTITY_JDBC;

import java.util.ArrayList;
import java.util.List;

import org.unidal.maven.plugin.wizard.model.BaseEntity;
import org.unidal.maven.plugin.wizard.model.IVisitor;

public class Jdbc extends BaseEntity<Jdbc> {
   private String m_package;

   private String m_name;

   private Datasource m_datasource;

   private List<Group> m_groups = new ArrayList<Group>();

   public Jdbc() {
   }

   public Jdbc(String name) {
      m_name = name;
   }

   @Override
   public void accept(IVisitor visitor) {
      visitor.visitJdbc(this);
   }

   public Jdbc addGroup(Group group) {
      m_groups.add(group);
      return this;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof Jdbc) {
         Jdbc _o = (Jdbc) obj;

         if (!equals(getName(), _o.getName())) {
            return false;
         }

         return true;
      }

      return false;
   }

   public Group findGroup(String name) {
      for (Group group : m_groups) {
         if (!equals(group.getName(), name)) {
            continue;
         }

         return group;
      }

      return null;
   }

   public Group findOrCreateGroup(String name) {
      synchronized (m_groups) {
         for (Group group : m_groups) {
            if (!equals(group.getName(), name)) {
               continue;
            }

            return group;
         }

         Group group = new Group(name);

         m_groups.add(group);
         return group;
      }
   }

   public Datasource getDatasource() {
      return m_datasource;
   }

   public List<Group> getGroups() {
      return m_groups;
   }

   public String getName() {
      return m_name;
   }

   public String getPackage() {
      return m_package;
   }

   @Override
   public int hashCode() {
      int hash = 0;

      hash = hash * 31 + (m_name == null ? 0 : m_name.hashCode());

      return hash;
   }

   @Override
   public void mergeAttributes(Jdbc other) {
      assertAttributeEquals(other, ENTITY_JDBC, ATTR_NAME, m_name, other.getName());

      if (other.getPackage() != null) {
         m_package = other.getPackage();
      }
   }

   public Group removeGroup(String name) {
      int len = m_groups.size();

      for (int i = 0; i < len; i++) {
         Group group = m_groups.get(i);

         if (!equals(group.getName(), name)) {
            continue;
         }

         return m_groups.remove(i);
      }

      return null;
   }

   public Jdbc setDatasource(Datasource datasource) {
      m_datasource = datasource;
      return this;
   }

   public Jdbc setName(String name) {
      m_name = name;
      return this;
   }

   public Jdbc setPackage(String _package) {
      m_package = _package;
      return this;
   }

}
