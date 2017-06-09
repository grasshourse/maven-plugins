package org.unidal.maven.plugin.wizard.model.entity;

import static org.unidal.maven.plugin.wizard.model.Constants.ATTR_NAME;
import static org.unidal.maven.plugin.wizard.model.Constants.ENTITY_TABLE;

import org.unidal.maven.plugin.wizard.model.BaseEntity;
import org.unidal.maven.plugin.wizard.model.IVisitor;

public class Table extends BaseEntity<Table> {
   private String m_name;

   public Table() {
   }

   public Table(String name) {
      m_name = name;
   }

   @Override
   public void accept(IVisitor visitor) {
      visitor.visitTable(this);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof Table) {
         Table _o = (Table) obj;

         if (!equals(getName(), _o.getName())) {
            return false;
         }

         return true;
      }

      return false;
   }

   public String getName() {
      return m_name;
   }

   @Override
   public int hashCode() {
      int hash = 0;

      hash = hash * 31 + (m_name == null ? 0 : m_name.hashCode());

      return hash;
   }

   @Override
   public void mergeAttributes(Table other) {
      assertAttributeEquals(other, ENTITY_TABLE, ATTR_NAME, m_name, other.getName());

   }

   public Table setName(String name) {
      m_name = name;
      return this;
   }

}
